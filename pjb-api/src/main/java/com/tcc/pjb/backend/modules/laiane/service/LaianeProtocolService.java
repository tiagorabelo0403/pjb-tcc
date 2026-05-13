package com.tcc.pjb.backend.modules.laiane.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolCreateRequest;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeDocumentFingerprint;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeDocumentFingerprintRepository;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProtocolPackageRepository;
import com.tcc.pjb.backend.modules.laiane.util.LaianeCrypto;

@Service
public class LaianeProtocolService {

    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final LaianeProtocolPackageRepository protocolRepo;
    private final LaianeDocumentFingerprintRepository fingerprintRepo;
    private final LaianeSubmissionGuardrailService laianeSubmissionGuardrailService;

    public LaianeProtocolService(ObjectMapper objectMapper,
                                CurrentUserService currentUserService,
                                LaianeProtocolPackageRepository protocolRepo,
                                LaianeDocumentFingerprintRepository fingerprintRepo,
                                LaianeSubmissionGuardrailService laianeSubmissionGuardrailService) {
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.protocolRepo = protocolRepo;
        this.fingerprintRepo = fingerprintRepo;
        this.laianeSubmissionGuardrailService = laianeSubmissionGuardrailService;
    }

    @Transactional
    public LaianeProtocolPackageDto create(LaianeProtocolCreateRequest req) {
        Usuario u = currentUserService.get();
        if (req == null) req = LaianeProtocolCreateRequest.builder().build();

        
        String title = (req.getTitle() == null || req.getTitle().isBlank())
                ? "Protocolo PJB - " + OffsetDateTime.now().toString()
                : req.getTitle().trim();

        Object enrichedPayload = req.getPayload();
        if (enrichedPayload instanceof java.util.Map<?, ?> map) {
            enrichedPayload = laianeSubmissionGuardrailService.enrichPayload(objectMapper.convertValue(map, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {}));
        }
        LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails = laianeSubmissionGuardrailService.analyze(enrichedPayload);
        String canonicalJson = canonicalize(enrichedPayload);

        byte[] bytes = canonicalJson.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 1_000_000) {
            throw new IllegalArgumentException("Payload excede 1MB; reduza o conteúdo do protocolo.");
        }

        String hash = LaianeCrypto.sha256Hex(canonicalJson);
        String statusInicial = resolveInitialStatus(guardrails);

        LaianeProtocolPackage entity = LaianeProtocolPackage.builder()
                .usuario(u)
                .title(title)
                .payloadJson(canonicalJson)
                .integrityHash(hash)
                .status(statusInicial)
                .lastError(guardrails.blocking() ? guardrails.summary() : null)
                .build();

        entity = protocolRepo.save(entity);

        
        fingerprintRepo.findByUsuario_IdAndSha256(u.getId(), hash)
                .orElseGet(() -> fingerprintRepo.save(LaianeDocumentFingerprint.builder()
                        .usuario(u)
                        .sha256(hash)
                        .mime("application/json")
                        .sizeBytes((long) bytes.length)
                        .build()));

        return toDto(entity);
    }

    public boolean verify(Long protocolId) {
        Usuario u = currentUserService.get();
        LaianeProtocolPackage p = protocolRepo.findById(protocolId)
                .filter(x -> x.getUsuario() != null && x.getUsuario().getId().equals(u.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Protocolo não encontrado: id=" + protocolId));

        String expected = LaianeCrypto.sha256Hex(p.getPayloadJson());
        return expected.equalsIgnoreCase(p.getIntegrityHash());
    }

    @Transactional(readOnly = true)
    public List<LaianeProtocolPackageDto> listMine() {
        Usuario u = currentUserService.get();
        return protocolRepo.findTop50ByUsuario_IdOrderByCreatedAtDesc(u.getId()).stream()
                .map(this::toDto)
                .toList();
    }


    private String resolveInitialStatus(LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails) {
        if (guardrails == null) {
            return "DRAFT";
        }
        boolean assistedPetition = Boolean.TRUE.equals(guardrails.envelope().get("assistedPetition"));
        if (guardrails.blocking()) {
            return "PRECHECK_PENDING";
        }
        if (!assistedPetition) {
            return "DRAFT";
        }
        return guardrails.readyForSubmission() ? "READY_REVIEW" : "DRAFT";
    }

    private LaianeProtocolPackageDto toDto(LaianeProtocolPackage p) {
        LaianeSubmissionGuardrailService.GuardrailSnapshot guardrails = laianeSubmissionGuardrailService.analyze(p.getPayloadJson());
        return LaianeProtocolPackageDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .integrityHash(p.getIntegrityHash())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .equipeId(p.getEquipe() != null ? p.getEquipe().getId() : null)
                .executorUserId(p.getExecutorUserId())
                .signerUserId(p.getSignerUserId())
                .officeQueueItemId(p.getOfficeQueueItemId())
                .submissionJobId(p.getSubmissionJobId())
                .externalProtocolRef(p.getExternalProtocolRef())
                .submittedAt(p.getSubmittedAt())
                .lastError(p.getLastError())
                .guardrailStatus(guardrails.status())
                .readyForSubmission(guardrails.readyForSubmission())
                .guardrailNextAction(guardrails.nextAction())
                .guardrailBlockers(guardrails.blockers())
                .build();
    }

    private String canonicalize(Object payload) {
        try {
            ObjectMapper om = objectMapper.copy();
            om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            om.configure(SerializationFeature.INDENT_OUTPUT, false);
            return om.writeValueAsString(payload == null ? java.util.Map.of() : payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar payload", e);
        }
    }
}
