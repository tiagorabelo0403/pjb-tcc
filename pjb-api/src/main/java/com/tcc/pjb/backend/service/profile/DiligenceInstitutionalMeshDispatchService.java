package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class DiligenceInstitutionalMeshDispatchService {

    private static final HexFormat HEX = HexFormat.of();
    private static final String IDEMPOTENCY_SCOPE_PREFIX = "DILIGENCE_MESH_DISPATCH";

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final ActionIdempotencyService actionIdempotencyService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository;
    private final DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository;
    private final ProcessoRepository processoRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    public DiligenceInstitutionalMeshDispatchService(CurrentUserService currentUserService,
                                                     PjbAuthorizationService authorizationService,
                                                     ActionIdempotencyService actionIdempotencyService,
                                                     KeyMaterialService keyMaterialService,
                                                     DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository,
                                                     DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository,
                                                     ProcessoRepository processoRepository,
                                                     OutboxPublisher outboxPublisher,
                                                     ObjectMapper objectMapper) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.actionIdempotencyService = Objects.requireNonNull(actionIdempotencyService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.annexationRepository = Objects.requireNonNull(annexationRepository);
        this.dispatchRepository = Objects.requireNonNull(dispatchRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public DiligenceInstitutionalMeshDispatchResponse dispatch(TelemetriaOperacionalCanal canal,
                                                               String diligenceReference,
                                                               DiligenceInstitutionalMeshDispatchRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorAnexacaoInstitucional annexation = resolveAnnexation(actor, canal, normalizedReference, request != null ? request.annexationId() : null);
        Processo processo = processoRepository.findById(annexation.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_da_anexacao_nao_encontrado"));
        authorizationService.requireWriteProcesso(processo);
        DispatchContext context = buildContext(actor, canal, normalizedReference, annexation, processo, request, null);
        return createDispatch(actor, canal, normalizedReference, annexation, context);
    }

    @Transactional(readOnly = true)
    public java.util.List<DiligenceInstitutionalMeshDispatchResponse> history(TelemetriaOperacionalCanal canal,
                                                                               String diligenceReference,
                                                                               int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return dispatchRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(item -> toResponse(actor, item))
                .toList();
    }

    @Transactional
    public DiligenceInstitutionalMeshAckResponse acknowledge(TelemetriaOperacionalCanal canal,
                                                             String diligenceReference,
                                                             Long dispatchId,
                                                             DiligenceInstitutionalMeshAckRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (dispatchId == null) {
            throw new IllegalArgumentException("dispatch_id_obrigatorio");
        }
        Usuario actor = currentUserService.getRequired();
        DiligenciaOperadorMalhaInstitucionalDispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new IllegalArgumentException("dispatch_nao_encontrado"));
        validateOwnership(actor, canal, diligenceReference, dispatch);
        Processo processo = processoRepository.findById(dispatch.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_do_dispatch_nao_encontrado"));
        authorizationService.requireWriteProcesso(processo);
        if (dispatch.getDeliveredAt() == null) {
            dispatch.setDeliveredAt(Instant.now());
        }
        dispatch.setAcknowledgedAt(dispatch.getAcknowledgedAt() == null ? Instant.now() : dispatch.getAcknowledgedAt());
        dispatch.setDispatchStatus(Boolean.TRUE.equals(request != null ? request.definitiveReceipt() : null) ? "ACKNOWLEDGED" : "DELIVERED_CONFIRMED");
        dispatch.setAckProtocol(normalize(firstNonBlank(request != null ? request.ackProtocol() : null, dispatch.getAckProtocol()), 120));
        dispatch.setAckReference(normalize(firstNonBlank(request != null ? request.ackReference() : null, dispatch.getAckReference()), 160));
        dispatch.setObservacoes(joinObservacoes(dispatch.getObservacoes(), request != null ? request.observacoes() : null));
        DiligenciaOperadorMalhaInstitucionalDispatch saved = dispatchRepository.save(dispatch);
        return new DiligenceInstitutionalMeshAckResponse(
                saved.getId(),
                saved.getCanal().name(),
                saved.getDiligenceReference(),
                saved.getDispatchStatus(),
                saved.getAckProtocol(),
                saved.getAckReference(),
                saved.getDeliveredAt(),
                saved.getAcknowledgedAt()
        );
    }

    @Transactional
    public DiligenceInstitutionalMeshReplayResponse replay(TelemetriaOperacionalCanal canal,
                                                           String diligenceReference,
                                                           DiligenceInstitutionalMeshReplayRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorMalhaInstitucionalDispatch original = resolveReplaySource(actor, canal, normalizedReference, request);
        if ("ACKNOWLEDGED".equalsIgnoreCase(original.getDispatchStatus())) {
            throw new IllegalArgumentException("dispatch_ja_acknowledged_sem_replay");
        }
        DiligenciaOperadorAnexacaoInstitucional annexation = annexationRepository.findById(original.getAnnexationId())
                .orElseThrow(() -> new IllegalArgumentException("anexacao_origem_nao_encontrada"));
        Processo processo = processoRepository.findById(original.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_do_dispatch_nao_encontrado"));
        authorizationService.requireWriteProcesso(processo);
        DispatchContext context = buildContext(
                actor,
                canal,
                normalizedReference,
                annexation,
                processo,
                new DiligenceInstitutionalMeshDispatchRequest(
                        annexation.getId(),
                        original.getExternalSystemCode(),
                        original.getDestinationBox(),
                        original.getMeshOrgKey(),
                        original.getMeshUnitKey(),
                        request != null ? request.routingKey() : null,
                        request != null ? request.idempotencyKey() : null,
                        joinObservacoes(original.getObservacoes(), request != null ? request.observacoes() : null)
                ),
                original
        );
        DiligenceInstitutionalMeshDispatchResponse replay = createDispatch(actor, canal, normalizedReference, annexation, context);
        return new DiligenceInstitutionalMeshReplayResponse(toResponse(actor, original), replay);
    }

    private DiligenceInstitutionalMeshDispatchResponse createDispatch(Usuario actor,
                                                                      TelemetriaOperacionalCanal canal,
                                                                      String normalizedReference,
                                                                      DiligenciaOperadorAnexacaoInstitucional annexation,
                                                                      DispatchContext context) {
        String scope = IDEMPOTENCY_SCOPE_PREFIX + ":" + canal.name();
        var begin = actionIdempotencyService.begin(scope, context.chainIdempotencyKey(), context.requestHash(), Duration.ofSeconds(60));
        if (begin.decision() == IdempotencyDecision.REPLAY) {
            return resolveReplay(actor, canal, normalizedReference, context.chainIdempotencyKey(), begin);
        }
        if (begin.decision() == IdempotencyDecision.IN_PROGRESS) {
            throw new IdempotencyInProgressException("diligence_mesh_dispatch", context.requestHash());
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(context.payload());
            String payloadDigest = sha256(payloadJson);
            String signature = signPayloadDigest(payloadDigest);
            String replayToken = sha256(String.join("|",
                    context.requestHash(),
                    payloadDigest,
                    context.routingKey(),
                    context.meshOrgKey(),
                    context.meshUnitKey(),
                    String.valueOf(actor.getId()),
                    String.valueOf(annexation.getId())));
            java.util.UUID outboxEventId = outboxPublisher.enqueueTracked(
                    context.routingKey(),
                    OutboxPublisher.EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH,
                    context.payload(),
                    Map.of(
                            "meshOrgKey", context.meshOrgKey(),
                            "meshUnitKey", context.meshUnitKey(),
                            "destinationBox", context.destinationBox(),
                            "externalSystemCode", context.externalSystemCode(),
                            "ackProtocol", nv(annexation.getAckProtocol()),
                            "ackReference", nv(annexation.getAckReference()),
                            "requestId", nv(RequestContext.getRequestId().orElse(null)),
                            "replayToken", replayToken
                    ),
                    "outbox:diligence-mesh:" + payloadDigest,
                    "DILIGENCE_MESH_DISPATCH",
                    replayToken
            );
            DiligenciaOperadorMalhaInstitucionalDispatch entity = DiligenciaOperadorMalhaInstitucionalDispatch.builder()
                    .operatorUserId(actor.getId())
                    .operatorTipoUsuario(actor.getTipoUsuario())
                    .canal(canal)
                    .diligenceReference(normalizedReference)
                    .processoId(annexation.getProcessoId())
                    .processoNumero(annexation.getProcessoNumero())
                    .workItemId(annexation.getWorkItemId())
                    .annexationId(annexation.getId())
                    .juntadaId(annexation.getJuntadaId())
                    .pacoteDocumentoId(annexation.getPacoteDocumentoId())
                    .outboxEventId(outboxEventId)
                    .eventType(OutboxPublisher.EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH)
                    .routingKey(context.routingKey())
                    .externalSystemCode(context.externalSystemCode())
                    .destinationBox(context.destinationBox())
                    .meshOrgKey(context.meshOrgKey())
                    .meshUnitKey(context.meshUnitKey())
                    .dispatchStatus("OUTBOX_ENQUEUED")
                    .replayToken(replayToken)
                    .chainIdempotencyKey(context.chainIdempotencyKey())
                    .requestHashSha256(context.requestHash())
                    .payloadDigestSha256(payloadDigest)
                    .payloadSignatureHmacSha256(signature)
                    .ackProtocol(annexation.getAckProtocol())
                    .ackReference(annexation.getAckReference())
                    .observacoes(context.observacoes())
                    .requestId(RequestContext.getRequestId().orElse(null))
                    .createdAt(Instant.now())
                    .build();
            DiligenciaOperadorMalhaInstitucionalDispatch saved = dispatchRepository.save(entity);
            String responseJson = objectMapper.writeValueAsString(Map.of("dispatchId", saved.getId(), "status", "OUTBOX_ENQUEUED"));
            actionIdempotencyService.complete(scope, context.chainIdempotencyKey(), payloadDigest, "DILIGENCE_MESH_DISPATCH", String.valueOf(saved.getId()), responseJson);
            return toResponse(actor, saved);
        } catch (RuntimeException ex) {
            actionIdempotencyService.fail(scope, context.chainIdempotencyKey(), sha256(ex.getClass().getName() + ":" + nv(ex.getMessage())), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            actionIdempotencyService.fail(scope, context.chainIdempotencyKey(), sha256(ex.getClass().getName() + ":" + nv(ex.getMessage())), ex.getMessage());
            throw new IllegalStateException("diligence_mesh_dispatch_error", ex);
        }
    }

    private DiligenciaOperadorAnexacaoInstitucional resolveAnnexation(Usuario actor,
                                                                      TelemetriaOperacionalCanal canal,
                                                                      String diligenceReference,
                                                                      Long annexationId) {
        if (annexationId != null) {
            DiligenciaOperadorAnexacaoInstitucional annexation = annexationRepository.findById(annexationId)
                    .orElseThrow(() -> new IllegalArgumentException("anexacao_nao_encontrada"));
            if (!Objects.equals(annexation.getOperatorUserId(), actor.getId())
                    || annexation.getCanal() != canal
                    || !Objects.equals(annexation.getDiligenceReference(), diligenceReference)) {
                throw new IllegalArgumentException("anexacao_incompativel_com_diligencia");
            }
            return annexation;
        }
        return annexationRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("anexacao_recente_obrigatoria"));
    }

    private DiligenciaOperadorMalhaInstitucionalDispatch resolveReplaySource(Usuario actor,
                                                                             TelemetriaOperacionalCanal canal,
                                                                             String diligenceReference,
                                                                             DiligenceInstitutionalMeshReplayRequest request) {
        Long originalDispatchId = request != null ? request.originalDispatchId() : null;
        if (originalDispatchId != null) {
            DiligenciaOperadorMalhaInstitucionalDispatch dispatch = dispatchRepository.findById(originalDispatchId)
                    .orElseThrow(() -> new IllegalArgumentException("dispatch_origem_nao_encontrado"));
            validateOwnership(actor, canal, diligenceReference, dispatch);
            return dispatch;
        }
        return dispatchRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("dispatch_origem_recente_obrigatorio"));
    }

    private DispatchContext buildContext(Usuario actor,
                                         TelemetriaOperacionalCanal canal,
                                         String diligenceReference,
                                         DiligenciaOperadorAnexacaoInstitucional annexation,
                                         Processo processo,
                                         DiligenceInstitutionalMeshDispatchRequest request,
                                         DiligenciaOperadorMalhaInstitucionalDispatch original) {
        String externalSystemCode = normalize(firstNonBlank(request != null ? request.externalSystemCode() : null, annexation.getExternalSystemCode(), "MALHA_INSTITUCIONAL"), 40);
        String destinationBox = normalize(firstNonBlank(request != null ? request.destinationBox() : null, annexation.getDestinationBox(), externalSystemCode + ":" + canal.name()), 160);
        String meshOrgKey = normalize(firstNonBlank(request != null ? request.meshOrgKey() : null, processo.getTribunalCodigoRoteado(), actor.getUf(), externalSystemCode), 80);
        String meshUnitKey = normalize(firstNonBlank(request != null ? request.meshUnitKey() : null, processo.getUnidadeJudiciariaCodigo(), actor.getComarca(), destinationBox), 120);
        String routingKey = normalize(firstNonBlank(request != null ? request.routingKey() : null, "MESH:DILIGENCE:" + canal.name() + ":" + meshOrgKey + ":" + meshUnitKey), 180);
        String requestHash = sha256(String.join("|",
                canal.name(),
                diligenceReference,
                String.valueOf(actor.getId()),
                String.valueOf(annexation.getId()),
                nv(annexation.getJuntadaId()),
                nv(annexation.getPacoteDocumentoId()),
                nv(externalSystemCode),
                nv(destinationBox),
                nv(meshOrgKey),
                nv(meshUnitKey),
                nv(original != null ? original.getId() : null),
                nv(request != null ? request.observacoes() : null)));
        String chainIdempotencyKey = resolveChainIdempotencyKey(request, annexation, original, requestHash);
        String observacoes = normalize(request != null ? request.observacoes() : null, 3000);
        InstitutionalMeshDispatchPayload payload = new InstitutionalMeshDispatchPayload(
                canal.name(),
                diligenceReference,
                annexation.getProcessoId(),
                annexation.getProcessoNumero(),
                annexation.getId(),
                annexation.getJuntadaId(),
                annexation.getPacoteDocumentoId() != null ? annexation.getPacoteDocumentoId().toString() : null,
                externalSystemCode,
                destinationBox,
                meshOrgKey,
                meshUnitKey,
                actor.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                original != null ? original.getId() : null,
                annexation.getAckProtocol(),
                annexation.getAckReference(),
                observacoes,
                RequestContext.getRequestId().orElse(null)
        );
        return new DispatchContext(externalSystemCode, destinationBox, meshOrgKey, meshUnitKey, routingKey, chainIdempotencyKey, requestHash, observacoes, payload);
    }

    private void validateOwnership(Usuario actor,
                                   TelemetriaOperacionalCanal canal,
                                   String diligenceReference,
                                   DiligenciaOperadorMalhaInstitucionalDispatch dispatch) {
        String normalizedReference = diligenceReference != null ? diligenceReference.trim() : null;
        if (!Objects.equals(dispatch.getOperatorUserId(), actor.getId())
                || dispatch.getCanal() != canal
                || !Objects.equals(dispatch.getDiligenceReference(), normalizedReference)) {
            throw new IllegalArgumentException("dispatch_incompativel_com_diligencia");
        }
    }

    private DiligenceInstitutionalMeshDispatchResponse resolveReplay(Usuario actor,
                                                                     TelemetriaOperacionalCanal canal,
                                                                     String diligenceReference,
                                                                     String idempotencyKey,
                                                                     com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult begin) {
        Long resourceId = begin.resourceIdOptional().map(Long::valueOf).orElse(null);
        DiligenciaOperadorMalhaInstitucionalDispatch replay = resourceId != null
                ? dispatchRepository.findById(resourceId).orElse(null)
                : null;
        if (replay == null) {
            replay = dispatchRepository.findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndChainIdempotencyKey(actor.getId(), canal, diligenceReference, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("replay_dispatch_inconsistente"));
        }
        return toResponse(actor, replay);
    }

    private DiligenceInstitutionalMeshDispatchResponse toResponse(Usuario actor,
                                                                  DiligenciaOperadorMalhaInstitucionalDispatch entity) {
        return new DiligenceInstitutionalMeshDispatchResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getAnnexationId(),
                entity.getJuntadaId(),
                entity.getPacoteDocumentoId(),
                entity.getOutboxEventId(),
                entity.getEventType(),
                entity.getRoutingKey(),
                entity.getExternalSystemCode(),
                entity.getDestinationBox(),
                entity.getMeshOrgKey(),
                entity.getMeshUnitKey(),
                entity.getDispatchStatus(),
                entity.getReplayToken(),
                entity.getChainIdempotencyKey(),
                entity.getAckProtocol(),
                entity.getAckReference(),
                entity.getPayloadDigestSha256(),
                entity.getPayloadSignatureHmacSha256(),
                entity.getDeliveredAt(),
                entity.getAcknowledgedAt(),
                entity.getCreatedAt()
        );
    }

    private String resolveChainIdempotencyKey(DiligenceInstitutionalMeshDispatchRequest request,
                                              DiligenciaOperadorAnexacaoInstitucional annexation,
                                              DiligenciaOperadorMalhaInstitucionalDispatch original,
                                              String requestHash) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return normalize(request.idempotencyKey(), 64);
        }
        return sha256(String.join("|",
                nv(annexation.getCanal()),
                nv(annexation.getDiligenceReference()),
                nv(annexation.getId()),
                nv(annexation.getBundleDigestSha256()),
                nv(original != null ? original.getId() : null),
                requestHash));
    }

    private String joinObservacoes(String current, String extra) {
        String normalizedCurrent = normalize(current, 3000);
        String normalizedExtra = normalize(extra, 3000);
        if (normalizedCurrent == null) {
            return normalizedExtra;
        }
        if (normalizedExtra == null) {
            return normalizedCurrent;
        }
        return normalize(normalizedCurrent + " | " + normalizedExtra, 3000);
    }

    public String signMeshDigest(String payloadDigest) {
        return signPayloadDigest(payloadDigest);
    }

    public String routingKeyForProcessualCommunication(String namespace, String targetOrg, String targetUnit) {
        String prefix = firstNonBlank(namespace, "MESH:PROCESSUAL:COMMUNICATION");
        return normalize(prefix + ":" + nv(targetOrg) + ":" + nv(targetUnit), 180);
    }

    private String signPayloadDigest(String payloadDigest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getOperationalMeshDispatchSigningKey());
            return HEX.formatHex(mac.doFinal(payloadDigest.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("mesh_dispatch_hmac", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(nv(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_indisponivel", ex);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value, 4000);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static String nv(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record DispatchContext(
            String externalSystemCode,
            String destinationBox,
            String meshOrgKey,
            String meshUnitKey,
            String routingKey,
            String chainIdempotencyKey,
            String requestHash,
            String observacoes,
            InstitutionalMeshDispatchPayload payload
    ) {
    }

    private record InstitutionalMeshDispatchPayload(
            String canal,
            String diligenceReference,
            Long processoId,
            String processoNumero,
            Long annexationId,
            Long juntadaId,
            String pacoteDocumentoId,
            String externalSystemCode,
            String destinationBox,
            String meshOrgKey,
            String meshUnitKey,
            Long operatorUserId,
            String operatorPerfil,
            Long replayOfDispatchId,
            String ackProtocol,
            String ackReference,
            String observacoes,
            String requestId
    ) {
    }
}
