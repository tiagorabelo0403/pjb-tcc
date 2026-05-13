package com.tcc.pjb.backend.core.kernel.casefile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.kernel.CaseFileEventEnvelope;
import com.tcc.pjb.backend.model.repository.CaseFileEventRepository;

@Service
public class CaseFileEventStore {

    private final CaseFileEventRepository repository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public CaseFileEventStore(CaseFileEventRepository repository,
                              ObjectMapper objectMapper,
                              CurrentUserService currentUserService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public CaseFileEventEnvelope append(Long caseFileId, CaseFileEventType type, Object payload) {
        Objects.requireNonNull(caseFileId, "caseFileId é obrigatório");
        Objects.requireNonNull(type, "type é obrigatório");
        Objects.requireNonNull(payload, "payload é obrigatório");

        String json = toJson(payload);
        String hash = sha256Hex(json);

        CaseFileEventEnvelope existing = repository.findFirstByCaseFileIdAndPayloadHash(caseFileId, hash).orElse(null);
        if (existing != null) return existing;

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;
        String actorRole = actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : null;

        for (int attempts = 1; ; attempts++) {
            try {
                long nextSeq = repository.findMaxSeq(caseFileId).orElse(0L) + 1L;
                CaseFileEventEnvelope env = CaseFileEventEnvelope.builder()
                        .caseFileId(caseFileId)
                        .seq(nextSeq)
                        .eventType(type.name())
                        .payload(json)
                        .payloadHash(hash)
                        .actorUserId(actorId)
                        .actorRole(actorRole)
                        .createdAt(Instant.now())
                        .build();
                return repository.save(env);
            } catch (DataIntegrityViolationException race) {
                CaseFileEventEnvelope nowExisting = repository.findFirstByCaseFileIdAndPayloadHash(caseFileId, hash).orElse(null);
                if (nowExisting != null) return nowExisting;
                if (attempts >= 3) throw race;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CaseFileEventEnvelope> stream(Long caseFileId) {
        Objects.requireNonNull(caseFileId, "caseFileId é obrigatório");
        return repository.findAllByCaseFileIdOrderBySeqAsc(caseFileId);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar payload do evento", e);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
