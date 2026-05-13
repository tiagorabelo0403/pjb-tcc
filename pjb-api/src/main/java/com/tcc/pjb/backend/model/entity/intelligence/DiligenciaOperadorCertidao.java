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
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_certidao", indexes = {
        @Index(name = "idx_diligencia_certidao_user_ref", columnList = "operator_user_id, canal, diligence_reference, created_at"),
        @Index(name = "idx_diligencia_certidao_workitem", columnList = "work_item_id, created_at"),
        @Index(name = "idx_diligencia_certidao_checkpoint", columnList = "checkpoint_event_id"),
        @Index(name = "idx_diligencia_certidao_digest", columnList = "certificate_digest_sha256")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorCertidao {

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

    @Column(name = "work_item_id")
    private Long workItemId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "processo_numero", length = 32)
    private String processoNumero;

    @Column(name = "checkpoint_event_id")
    private Long checkpointEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certidao_tipo", nullable = false, length = 40)
    private DiligenciaCertidaoTipo certidaoTipo;

    @Column(name = "titulo", nullable = false, length = 180)
    private String titulo;

    @Column(name = "narrativa", nullable = false, columnDefinition = "TEXT")
    private String narrativa;

    @Column(name = "certificate_digest_sha256", nullable = false, length = 64)
    private String certificateDigestSha256;

    @Column(name = "signature_hmac_sha256", nullable = false, length = 64)
    private String signatureHmacSha256;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "destino_latitude")
    private Double destinoLatitude;

    @Column(name = "destino_longitude")
    private Double destinoLongitude;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "inside_geofence")
    private Boolean insideGeofence;

    @Column(name = "tentativa_sequencia")
    private Integer tentativaSequencia;

    @Column(name = "evidence_chave_custodia", length = 32)
    private String evidenceChaveCustodia;

    @Column(name = "attempt_trail_digest_sha256", length = 64)
    private String attemptTrailDigestSha256;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
