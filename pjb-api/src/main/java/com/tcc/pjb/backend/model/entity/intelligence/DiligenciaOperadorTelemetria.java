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
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_diligencia_operador_telemetria", indexes = {
        @Index(name = "idx_diligencia_telemetria_user_channel_created", columnList = "operator_user_id, canal, created_at"),
        @Index(name = "idx_diligencia_telemetria_device_created", columnList = "device_hash, created_at"),
        @Index(name = "idx_diligencia_telemetria_request", columnList = "request_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiligenciaOperadorTelemetria {

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

    @Column(name = "device_hash", length = 64)
    private String deviceHash;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "precisao_metros")
    private Double precisaoMetros;

    @Column(name = "velocidade_kmh")
    private Double velocidadeKmh;

    @Column(name = "bateria_percentual")
    private Integer bateriaPercentual;

    @Column(name = "fonte", nullable = false, length = 32)
    private String fonte;

    @Column(name = "foreground", nullable = false)
    private boolean foreground;

    @Column(name = "capturado_em", nullable = false)
    private Instant capturadoEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "ip", length = 80)
    private String ip;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        capturadoEm = capturadoEm == null ? createdAt : capturadoEm;
        fonte = normalizeFonte(fonte);
    }

    private static String normalizeFonte(String value) {
        if (value == null || value.isBlank()) {
            return "GPS";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }
}
