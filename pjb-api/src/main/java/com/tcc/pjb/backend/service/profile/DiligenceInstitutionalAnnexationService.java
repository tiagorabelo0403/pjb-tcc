package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;

@Service
public class DiligenceInstitutionalAnnexationService {

    private static final HexFormat HEX = HexFormat.of();
    private static final String IDEMPOTENCY_SCOPE_PREFIX = "DILIGENCE_INSTITUTIONAL_ANNEXATION";

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final ActionIdempotencyService actionIdempotencyService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenciaOperadorJuntadaProcessualRepository juntadaRepository;
    private final DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository;
    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessEventStore processEventStore;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceInstitutionalAnnexationService(CurrentUserService currentUserService,
                                                   PjbAuthorizationService authorizationService,
                                                   ActionIdempotencyService actionIdempotencyService,
                                                   KeyMaterialService keyMaterialService,
                                                   DiligenciaOperadorJuntadaProcessualRepository juntadaRepository,
                                                   DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository,
                                                   ProcessoRepository processoRepository,
                                                   DocumentoProcessualRepository documentoRepository,
                                                   ProcessEventStore processEventStore,
                                                   QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.actionIdempotencyService = Objects.requireNonNull(actionIdempotencyService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.juntadaRepository = Objects.requireNonNull(juntadaRepository);
        this.anexacaoRepository = Objects.requireNonNull(anexacaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processEventStore = Objects.requireNonNull(processEventStore);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public DiligenceInstitutionalAnnexationResponse annex(TelemetriaOperacionalCanal canal,
                                                          String diligenceReference,
                                                          DiligenceInstitutionalAnnexationRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorJuntadaProcessual juntada = resolveJuntada(actor, canal, normalizedReference, request);
        if (juntada.getPacoteDocumentoId() == null) {
            throw new IllegalArgumentException("pacote_documental_obrigatorio_para_anexacao");
        }
        if (Boolean.TRUE.equals(request != null ? request.exigirJuntadaExportavel() : null) && !Boolean.TRUE.equals(juntada.getExportarMalhaExterna())) {
            throw new IllegalArgumentException("juntada_sem_habilitacao_externa");
        }
        Processo processo = processoRepository.findById(juntada.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_da_juntada_nao_encontrado"));
        authorizationService.requireWriteProcesso(processo);
        DocumentoProcessual pacote = documentoRepository.findById(juntada.getPacoteDocumentoId())
                .orElseThrow(() -> new IllegalArgumentException("pacote_documental_nao_encontrado"));
        String externalSystemCode = normalize(resolveExternalSystemCode(juntada, request), 40);
        String destinationBox = normalize(resolveDestinationBox(actor, canal, externalSystemCode, request), 160);
        String requestHash = sha256(String.join("|",
                canal.name(),
                normalizedReference,
                String.valueOf(actor.getId()),
                String.valueOf(juntada.getId()),
                String.valueOf(juntada.getFormalizacaoId()),
                String.valueOf(juntada.getPacoteDocumentoId()),
                nv(juntada.getBundleReference()),
                nv(juntada.getBundleDigestSha256()),
                nv(externalSystemCode),
                nv(destinationBox),
                nv(request != null ? request.observacoes() : null)));
        String idempotencyKey = resolveChainIdempotencyKey(juntada, request, requestHash);
        String scope = IDEMPOTENCY_SCOPE_PREFIX + ":" + canal.name();
        var begin = actionIdempotencyService.begin(scope, idempotencyKey, requestHash, Duration.ofSeconds(60));
        if (begin.decision() == IdempotencyDecision.REPLAY) {
            return resolveReplay(actor, canal, normalizedReference, idempotencyKey, begin);
        }
        if (begin.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new IdempotencyInProgressException("diligence_institutional_annexation", requestHash);
        }
        try {
            boolean atualizarDocumento = request == null || request.atualizarDocumentoComoExternalizado() == null || request.atualizarDocumentoComoExternalizado();
            boolean registrarEvento = request == null || request.registrarEventoProcessual() == null || request.registrarEventoProcessual();
            Instant externalizedAt = atualizarDocumento ? Instant.now() : pacote.getExternalizedAt();
            if (atualizarDocumento) {
                pacote.setExternalizedAt(externalizedAt);
                pacote.setOrigemSistema(externalSystemCode);
                documentoRepository.save(pacote);
            }
            ProcessEventEnvelope processEvent = registrarEvento
                    ? processEventStore.append(processo.getId(), ProcessEventType.DOCUMENTS_BULK_ADDED, new ExternalInstitutionalAnnexationPayload(
                            processo.getId(),
                            processo.getNumeroProcesso(),
                            canal.name(),
                            normalizedReference,
                            juntada.getId(),
                            juntada.getFormalizacaoId(),
                            juntada.getPacoteDocumentoId(),
                            externalSystemCode,
                            destinationBox,
                            juntada.getBundleReference(),
                            juntada.getBundleDigestSha256(),
                            RequestContext.getRequestId().orElse(null)
                    ))
                    : null;
            String ackProtocol = generateAckProtocol(processo.getId(), juntada.getId(), externalSystemCode);
            String ackReference = generateAckReference(canal, destinationBox, pacote.getId(), processEvent != null ? processEvent.getSeq() : null);
            String receiptDigest = sha256(String.join("|",
                    nv(ackProtocol),
                    nv(ackReference),
                    nv(externalSystemCode),
                    nv(destinationBox),
                    nv(juntada.getBundleDigestSha256()),
                    nv(processEvent != null ? processEvent.getPayloadHash() : null)));
            String executionDigest = signExecutionDigest(canal, normalizedReference, juntada, externalSystemCode, destinationBox, idempotencyKey, receiptDigest);
            DiligenciaOperadorAnexacaoInstitucional entity = DiligenciaOperadorAnexacaoInstitucional.builder()
                    .operatorUserId(actor.getId())
                    .operatorTipoUsuario(actor.getTipoUsuario())
                    .canal(canal)
                    .diligenceReference(normalizedReference)
                    .workItemId(juntada.getWorkItemId())
                    .processoId(juntada.getProcessoId())
                    .processoNumero(juntada.getProcessoNumero())
                    .juntadaId(juntada.getId())
                    .formalizacaoId(juntada.getFormalizacaoId())
                    .encerramentoId(juntada.getEncerramentoId())
                    .certidaoId(juntada.getCertidaoId())
                    .pacoteDocumentoId(juntada.getPacoteDocumentoId())
                    .bundleReference(juntada.getBundleReference())
                    .bundleDigestSha256(juntada.getBundleDigestSha256())
                    .bundleSignatureHmacSha256(juntada.getBundleSignatureHmacSha256())
                    .externalSystemCode(externalSystemCode)
                    .destinationBox(destinationBox)
                    .ackProtocol(ackProtocol)
                    .ackReference(ackReference)
                    .annexationStatus("ACKED")
                    .externalReceiptDigestSha256(receiptDigest)
                    .chainIdempotencyKey(idempotencyKey)
                    .processEventSeq(processEvent != null ? processEvent.getSeq() : null)
                    .requestHashSha256(requestHash)
                    .executionDigestSha256(executionDigest)
                    .observacoes(normalize(request != null ? request.observacoes() : null, 3000))
                    .externalizedAt(externalizedAt != null ? OffsetDateTime.ofInstant(externalizedAt, ZoneOffset.UTC) : null)
                    .requestId(RequestContext.getRequestId().orElse(null))
                    .createdAt(Instant.now())
                    .build();
            DiligenciaOperadorAnexacaoInstitucional saved = anexacaoRepository.save(entity);
            String responseJson = "{\"annexationId\":" + saved.getId() + ",\"status\":\"ACKED\"}";
            actionIdempotencyService.complete(scope, idempotencyKey, executionDigest, "DILIGENCE_EXTERNAL_ANNEXATION", String.valueOf(saved.getId()), responseJson);
            return toResponse(actor, saved);
        } catch (RuntimeException ex) {
            actionIdempotencyService.fail(scope, idempotencyKey, sha256(ex.getClass().getName() + ":" + nv(ex.getMessage())), ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<DiligenceInstitutionalAnnexationResponse> history(TelemetriaOperacionalCanal canal,
                                                                            String diligenceReference,
                                                                            int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return anexacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(item -> toResponse(actor, item))
                .toList();
    }

    private DiligenceInstitutionalAnnexationResponse resolveReplay(Usuario actor,
                                                                   TelemetriaOperacionalCanal canal,
                                                                   String diligenceReference,
                                                                   String idempotencyKey,
                                                                   com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult begin) {
        Long resourceId = begin.resourceIdOptional().map(Long::valueOf).orElse(null);
        DiligenciaOperadorAnexacaoInstitucional replay = resourceId != null
                ? anexacaoRepository.findById(resourceId).orElse(null)
                : null;
        if (replay == null) {
            replay = anexacaoRepository.findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndChainIdempotencyKey(actor.getId(), canal, diligenceReference, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("replay_anexacao_inconsistente"));
        }
        return toResponse(actor, replay);
    }

    private DiligenciaOperadorJuntadaProcessual resolveJuntada(Usuario actor,
                                                               TelemetriaOperacionalCanal canal,
                                                               String diligenceReference,
                                                               DiligenceInstitutionalAnnexationRequest request) {
        if (request != null && request.juntadaId() != null) {
            DiligenciaOperadorJuntadaProcessual juntada = juntadaRepository.findById(request.juntadaId())
                    .orElseThrow(() -> new IllegalArgumentException("juntada_nao_encontrada"));
            if (!Objects.equals(juntada.getOperatorUserId(), actor.getId())
                    || juntada.getCanal() != canal
                    || !Objects.equals(juntada.getDiligenceReference(), diligenceReference)) {
                throw new IllegalArgumentException("juntada_incompativel_com_diligencia");
            }
            return juntada;
        }
        return juntadaRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("juntada_recente_obrigatoria"));
    }

    private DiligenceInstitutionalAnnexationResponse toResponse(Usuario actor,
                                                                DiligenciaOperadorAnexacaoInstitucional entity) {
        SignedDocumentEnvelope signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                annexationTitle(entity),
                annexationCanonicalText(entity),
                resolveSigningRole(entity.getCanal(), actor),
                resolveSigningPolicy(entity.getCanal(), "ANEXACAO_INSTITUCIONAL"),
                true,
                java.util.List.of(
                        "anexacao_institucional_assinada",
                        "assinatura_transversal_completa",
                        entity.getCanal().name().toLowerCase(java.util.Locale.ROOT),
                        nv(normalize(entity.getAnnexationStatus(), 40)).toLowerCase(java.util.Locale.ROOT)
                )
        );
        return new DiligenceInstitutionalAnnexationResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getJuntadaId(),
                entity.getFormalizacaoId(),
                entity.getEncerramentoId(),
                entity.getCertidaoId(),
                entity.getPacoteDocumentoId(),
                entity.getExternalSystemCode(),
                entity.getDestinationBox(),
                entity.getAckProtocol(),
                entity.getAckReference(),
                entity.getAnnexationStatus(),
                entity.getBundleReference(),
                entity.getChainIdempotencyKey(),
                entity.getExecutionDigestSha256(),
                entity.getProcessEventSeq(),
                entity.getExternalizedAt(),
                entity.getCreatedAt(),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String annexationTitle(DiligenciaOperadorAnexacaoInstitucional entity) {
        return switch (entity.getCanal()) {
            case OFICIAL_JUSTICA -> "Anexação institucional soberana de cumprimento";
            case DELEGADO -> "Anexação institucional soberana investigativa";
        };
    }

    private String annexationCanonicalText(DiligenciaOperadorAnexacaoInstitucional entity) {
        return String.join("\n",
                annexationTitle(entity),
                "canal=" + nv(entity.getCanal()),
                "diligencia_referencia=" + nv(entity.getDiligenceReference()),
                "processo_id=" + nv(entity.getProcessoId()),
                "processo_numero=" + nv(entity.getProcessoNumero()),
                "juntada_id=" + nv(entity.getJuntadaId()),
                "formalizacao_id=" + nv(entity.getFormalizacaoId()),
                "encerramento_id=" + nv(entity.getEncerramentoId()),
                "certidao_id=" + nv(entity.getCertidaoId()),
                "pacote_documento_id=" + nv(entity.getPacoteDocumentoId()),
                "external_system_code=" + nv(entity.getExternalSystemCode()),
                "destination_box=" + nv(entity.getDestinationBox()),
                "ack_protocol=" + nv(entity.getAckProtocol()),
                "ack_reference=" + nv(entity.getAckReference()),
                "annexation_status=" + nv(entity.getAnnexationStatus()),
                "bundle_reference=" + nv(entity.getBundleReference()),
                "chain_idempotency_key=" + nv(entity.getChainIdempotencyKey()),
                "execution_digest_sha256=" + nv(entity.getExecutionDigestSha256()),
                "process_event_seq=" + nv(entity.getProcessEventSeq()),
                "externalized_at=" + nv(entity.getExternalizedAt()),
                "created_at=" + nv(entity.getCreatedAt())
        );
    }

    private String resolveSigningRole(TelemetriaOperacionalCanal canal,
                                      Usuario actor) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA";
        }
        return actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "PERFIL_NAO_IDENTIFICADO";
    }

    private String resolveSigningPolicy(TelemetriaOperacionalCanal canal,
                                        String axis) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA_" + axis + "_QUALIFICADA_SOBERANA";
        }
        return canal.name() + '_' + axis + "_QUALIFICADA_SOBERANA";
    }

    private String resolveExternalSystemCode(DiligenciaOperadorJuntadaProcessual juntada,
                                             DiligenceInstitutionalAnnexationRequest request) {
        String fromRequest = request != null ? request.externalSystemCode() : null;
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest.trim();
        }
        if (juntada.getExternalSystemCode() != null && !juntada.getExternalSystemCode().isBlank()) {
            return juntada.getExternalSystemCode().trim();
        }
        return "MALHA_INSTITUCIONAL";
    }

    private String resolveDestinationBox(Usuario actor,
                                         TelemetriaOperacionalCanal canal,
                                         String externalSystemCode,
                                         DiligenceInstitutionalAnnexationRequest request) {
        String explicit = request != null ? request.destinationBox() : null;
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        return String.join(":",
                externalSystemCode,
                canal.name(),
                actor.getUf() == null || actor.getUf().isBlank() ? "BR" : actor.getUf().trim().toUpperCase(),
                actor.getComarca() == null || actor.getComarca().isBlank() ? "CENTRAL" : actor.getComarca().trim().replace(' ', '_').toUpperCase());
    }

    private String resolveChainIdempotencyKey(DiligenciaOperadorJuntadaProcessual juntada,
                                              DiligenceInstitutionalAnnexationRequest request,
                                              String requestHash) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return normalize(request.idempotencyKey().trim(), 64);
        }
        return sha256(String.join("|",
                nv(juntada.getCanal()),
                nv(juntada.getDiligenceReference()),
                nv(juntada.getId()),
                nv(juntada.getFormalizacaoId()),
                nv(juntada.getPacoteDocumentoId()),
                nv(juntada.getBundleDigestSha256()),
                requestHash));
    }

    private String generateAckProtocol(Long processoId,
                                       Long juntadaId,
                                       String externalSystemCode) {
        return normalize(externalSystemCode, 24) + "-" + processoId + "-" + juntadaId + "-" + Instant.now().toEpochMilli();
    }

    private String generateAckReference(TelemetriaOperacionalCanal canal,
                                        String destinationBox,
                                        java.util.UUID pacoteDocumentoId,
                                        Long processEventSeq) {
        return normalize(String.join(":",
                canal.name(),
                destinationBox,
                String.valueOf(pacoteDocumentoId),
                nv(processEventSeq)), 160);
    }

    private String signExecutionDigest(TelemetriaOperacionalCanal canal,
                                       String diligenceReference,
                                       DiligenciaOperadorJuntadaProcessual juntada,
                                       String externalSystemCode,
                                       String destinationBox,
                                       String idempotencyKey,
                                       String receiptDigest) {
        String canonical = String.join("|",
                canal.name(),
                diligenceReference,
                nv(juntada.getId()),
                nv(juntada.getFormalizacaoId()),
                nv(juntada.getPacoteDocumentoId()),
                nv(externalSystemCode),
                nv(destinationBox),
                nv(idempotencyKey),
                nv(receiptDigest));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getOperationalAnnexationSigningKey());
            return HEX.formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("falha_assinatura_anexacao_institucional", e);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256_indisponivel", e);
        }
    }

    private String normalize(String value,
                             int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private record ExternalInstitutionalAnnexationPayload(
            Long processoId,
            String processoNumero,
            String canal,
            String diligenceReference,
            Long juntadaId,
            Long formalizacaoId,
            java.util.UUID pacoteDocumentoId,
            String externalSystemCode,
            String destinationBox,
            String bundleReference,
            String bundleDigestSha256,
            String requestId
    ) {
    }
}
