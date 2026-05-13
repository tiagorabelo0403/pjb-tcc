package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_malha_institucional_dispatch", indexes = {
        @Index(name = "idx_diligencia_mesh_dispatch_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_mesh_dispatch_processo", columnList = "processo_id, created_at"),
        @Index(name = "idx_diligencia_mesh_dispatch_annex", columnList = "annexation_id, created_at"),
        @Index(name = "idx_diligencia_mesh_dispatch_outbox", columnList = "outbox_event_id", unique = true),
        @Index(name = "idx_diligencia_mesh_dispatch_chain", columnList = "operator_user_id, canal, diligence_reference, chain_idempotency_key", unique = true),
        @Index(name = "idx_diligencia_mesh_dispatch_replay", columnList = "replay_token", unique = true),
        @Index(name = "idx_diligencia_mesh_dispatch_org_unit", columnList = "mesh_org_key, mesh_unit_key, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorMalhaInstitucionalDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_tipo_usuario", nullable = false, length = 80)
    private TipoUsuario operatorTipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 40)
    private TelemetriaOperacionalCanal canal;

    @Column(name = "diligence_reference", nullable = false, length = 120)
    private String diligenceReference;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "processo_numero", length = 32)
    private String processoNumero;

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "annexation_id", nullable = false)
    private Long annexationId;

    @Column(name = "juntada_id")
    private Long juntadaId;

    @Column(name = "pacote_documento_id")
    private UUID pacoteDocumentoId;

    @Column(name = "outbox_event_id", nullable = false, unique = true)
    private UUID outboxEventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 180)
    private String routingKey;

    @Column(name = "external_system_code", nullable = false, length = 40)
    private String externalSystemCode;

    @Column(name = "destination_box", nullable = false, length = 160)
    private String destinationBox;

    @Column(name = "mesh_org_key", nullable = false, length = 80)
    private String meshOrgKey;

    @Column(name = "mesh_unit_key", nullable = false, length = 120)
    private String meshUnitKey;

    @Column(name = "dispatch_status", nullable = false, length = 40)
    private String dispatchStatus;

    @Column(name = "replay_token", nullable = false, length = 64)
    private String replayToken;

    @Column(name = "chain_idempotency_key", nullable = false, length = 64)
    private String chainIdempotencyKey;

    @Column(name = "request_hash_sha256", nullable = false, length = 64)
    private String requestHashSha256;

    @Column(name = "payload_digest_sha256", nullable = false, length = 64)
    private String payloadDigestSha256;

    @Column(name = "payload_signature_hmac_sha256", nullable = false, length = 64)
    private String payloadSignatureHmacSha256;

    @Column(name = "ack_protocol", length = 120)
    private String ackProtocol;

    @Column(name = "ack_reference", length = 160)
    private String ackReference;

    @Column(name = "observacoes", length = 3000)
    private String observacoes;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        eventType = normalize(eventType, 120);
        routingKey = normalize(routingKey, 180);
        externalSystemCode = normalize(externalSystemCode, 40);
        destinationBox = normalize(destinationBox, 160);
        meshOrgKey = normalize(meshOrgKey, 80);
        meshUnitKey = normalize(meshUnitKey, 120);
        dispatchStatus = normalize(dispatchStatus, 40);
        replayToken = normalize(replayToken, 64);
        chainIdempotencyKey = normalize(chainIdempotencyKey, 64);
        ackProtocol = normalize(ackProtocol, 120);
        ackReference = normalize(ackReference, 160);
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
