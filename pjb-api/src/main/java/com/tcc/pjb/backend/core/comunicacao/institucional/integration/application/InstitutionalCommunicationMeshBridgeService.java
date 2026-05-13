package com.tcc.pjb.backend.core.comunicacao.institucional.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalCommunicationMeshMirrorResult;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalMeshDispatchService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalCommunicationMeshBridgeService {

    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService;

    public InstitutionalCommunicationMeshBridgeService(OutboxPublisher outboxPublisher,
                                                       ObjectMapper objectMapper,
                                                       DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService) {
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher, "outboxPublisher");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.diligenceInstitutionalMeshDispatchService = Objects.requireNonNull(diligenceInstitutionalMeshDispatchService, "diligenceInstitutionalMeshDispatchService");
    }

    public InstitutionalCommunicationMeshMirrorResult espelhar(InstitutionalDeliveryJob job) {
        Objects.requireNonNull(job, "job");
        Instant now = Instant.now();
        String meshOrgKey = normalizeRequired(job.destinatarioKind().name(), "meshOrgKey");
        String meshUnitKey = normalizeRequired(job.unidadeCodigo(), "meshUnitKey");
        String externalSystemCode = normalizeRequired(providerName(job.currentChannel()), "externalSystemCode");
        String destinationBox = normalizeRequired(job.caixaCodigo(), "destinationBox");
        String routingKey = normalizeRoutingKey(diligenceInstitutionalMeshDispatchService.routingKeyForProcessualCommunication(
                "MESH:PROCESSUAL:COMMUNICATION",
                meshOrgKey,
                meshUnitKey
        ), meshOrgKey, meshUnitKey);
        String mirrorId = UUID.nameUUIDFromBytes((job.jobId() + "|" + job.currentChannel().name() + "|MESH").getBytes(StandardCharsets.UTF_8)).toString();
        Map<String, Object> payload = buildPayload(job, externalSystemCode, destinationBox, meshOrgKey, meshUnitKey, routingKey, mirrorId, now);
        String payloadJson = toJson(payload);
        String payloadDigest = Hashes.sha256Hex(payloadJson);
        String signature = Objects.requireNonNull(diligenceInstitutionalMeshDispatchService.signMeshDigest(payloadDigest), "signature");
        UUID outboxId = Objects.requireNonNull(outboxPublisher.enqueueTracked(
                routingKey.toLowerCase(),
                "PROCESSUAL_INSTITUTIONAL_MESH_MIRROR",
                payload,
                trackingMetadata(mirrorId, externalSystemCode, destinationBox, meshOrgKey, meshUnitKey, payloadDigest, signature),
                "outbox:processual-mesh:" + payloadDigest,
                "INSTITUTIONAL_COMMUNICATION_MESH",
                job.expedicaoUuid()
        ), "outboxId");
        return new InstitutionalCommunicationMeshMirrorResult(
                mirrorId,
                job.expedicaoUuid(),
                job.currentChannel().name(),
                routingKey,
                externalSystemCode,
                destinationBox,
                meshOrgKey,
                meshUnitKey,
                payloadDigest,
                signature,
                outboxId.toString(),
                now,
                Hashes.sha256Hex(mirrorId + "|" + payloadDigest + "|" + signature + "|" + outboxId)
        );
    }


    private Map<String, Object> trackingMetadata(String mirrorId,
                                                 String externalSystemCode,
                                                 String destinationBox,
                                                 String meshOrgKey,
                                                 String meshUnitKey,
                                                 String payloadDigest,
                                                 String signature) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "mirrorId", mirrorId);
        putIfPresent(metadata, "externalSystemCode", externalSystemCode);
        putIfPresent(metadata, "destinationBox", destinationBox);
        putIfPresent(metadata, "meshOrgKey", meshOrgKey);
        putIfPresent(metadata, "meshUnitKey", meshUnitKey);
        putIfPresent(metadata, "payloadDigestSha256", payloadDigest);
        putIfPresent(metadata, "payloadSignatureHmacSha256", signature);
        return PayloadMaps.deepCopyWithoutNulls(metadata);
    }

    private String normalizeRoutingKey(String routingKey, String meshOrgKey, String meshUnitKey) {
        if (routingKey != null && !routingKey.isBlank()) {
            return routingKey.trim();
        }
        return "MESH:PROCESSUAL:COMMUNICATION:" + meshOrgKey + ":" + meshUnitKey;
    }

    private void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private Map<String, Object> buildPayload(InstitutionalDeliveryJob job,
                                             String externalSystemCode,
                                             String destinationBox,
                                             String meshOrgKey,
                                             String meshUnitKey,
                                             String routingKey,
                                             String mirrorId,
                                             Instant mirroredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", "pjb.processual.communication.mesh/2026-03");
        payload.put("mirrorId", mirrorId);
        payload.put("jobId", job.jobId());
        payload.put("expedicaoUuid", job.expedicaoUuid());
        payload.put("processoId", job.processoId());
        payload.put("processoNumero", job.processoNumero());
        payload.put("destinatarioKind", job.destinatarioKind().name());
        payload.put("papelProcessual", job.papelProcessual().name());
        payload.put("channel", job.currentChannel().name());
        payload.put("externalSystemCode", externalSystemCode);
        payload.put("destinationBox", destinationBox);
        payload.put("meshOrgKey", meshOrgKey);
        payload.put("meshUnitKey", meshUnitKey);
        payload.put("routingKey", routingKey);
        payload.put("correlationKey", job.correlationKey());
        payload.put("justificativas", job.justificativas());
        payload.put("mirroredAt", mirroredAt.toString());
        return PayloadMaps.deepCopyWithoutNulls(payload);
    }


    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("institutionalCommunicationMeshBridgeJson", ex);
        }
    }

    private String providerName(CanalComunicacaoInstitucional channel) {
        return switch (channel) {
            case DOMICILIO_JUDICIAL_ELETRONICO -> "DJE";
            case DJEN -> "DJEN";
            case WEBHOOK_INSTITUCIONAL -> "WEBHOOK";
            case COMUNICACAO_FISICA_OFICIAL -> "OFFICIAL";
            default -> "PJB";
        };
    }
}
