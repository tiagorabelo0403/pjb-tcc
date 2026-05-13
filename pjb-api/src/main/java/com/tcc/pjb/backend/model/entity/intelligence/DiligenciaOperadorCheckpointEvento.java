package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
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
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_checkpoint", indexes = {
        @Index(name = "idx_diligencia_checkpoint_user_ref", columnList = "operator_user_id, canal, diligence_reference, occurred_at"),
        @Index(name = "idx_diligencia_checkpoint_request", columnList = "request_id"),
        @Index(name = "idx_diligencia_checkpoint_device", columnList = "device_hash, occurred_at"),
        @Index(name = "idx_diligencia_checkpoint_workitem", columnList = "work_item_id, occurred_at"),
        @Index(name = "idx_diligencia_checkpoint_actor_workitem_time", columnList = "operator_user_id, canal, work_item_id, occurred_at"),
        @Index(name = "idx_diligencia_checkpoint_processo", columnList = "processo_id, occurred_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorCheckpointEvento {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint_tipo", nullable = false, length = 40)
    private DiligenciaCheckpointTipo checkpointTipo;

    @Column(name = "target_latitude", nullable = false)
    private double targetLatitude;

    @Column(name = "target_longitude", nullable = false)
    private double targetLongitude;

    @Column(name = "observed_latitude", nullable = false)
    private double observedLatitude;

    @Column(name = "observed_longitude", nullable = false)
    private double observedLongitude;

    @Column(name = "distance_meters", nullable = false)
    private double distanceMeters;

    @Column(name = "geofence_radius_meters", nullable = false)
    private double geofenceRadiusMeters;

    @Column(name = "inside_geofence", nullable = false)
    private boolean insideGeofence;

    @Column(name = "classification", nullable = false, length = 40)
    private String classification;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "device_hash", length = 64)
    private String deviceHash;

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "processo_numero", length = 32)
    private String processoNumero;

    @Column(name = "work_item_template_code", length = 120)
    private String workItemTemplateCode;

    @Column(name = "work_item_type", length = 40)
    private String workItemType;

    @Column(name = "work_item_status", length = 30)
    private String workItemStatus;

    @Column(name = "tentativa_sequencia", nullable = false)
    private Integer tentativaSequencia;

    @Column(name = "location_signature_sha256", length = 64)
    private String locationSignatureSha256;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "ip", length = 80)
    private String ip;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        occurredAt = occurredAt == null ? createdAt : occurredAt;
        source = normalize(source);
        tentativaSequencia = tentativaSequencia == null || tentativaSequencia < 1 ? 1 : tentativaSequencia;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "TELEMETRIA_RECENTE";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.length() <= 40 ? normalized : normalized.substring(0, 40);
    }
}
