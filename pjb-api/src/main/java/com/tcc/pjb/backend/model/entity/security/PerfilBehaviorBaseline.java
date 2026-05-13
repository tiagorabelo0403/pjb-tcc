package com.tcc.pjb.backend.model.entity.security;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_perfil_behavior_baseline", indexes = {
        @Index(name = "idx_perfil_behavior_tipo", columnList = "tipo_usuario", unique = true),
        @Index(name = "idx_perfil_behavior_ativo", columnList = "ativo")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilBehaviorBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 80, unique = true)
    private TipoUsuario tipoUsuario;

    @Column(name = "expected_volume", nullable = false)
    @Builder.Default
    private Integer expectedVolume = 90;

    @Column(name = "alert_threshold_ratio", nullable = false)
    @Builder.Default
    private Double alertThresholdRatio = 1.10d;

    @Column(name = "anomaly_threshold_ratio", nullable = false)
    @Builder.Default
    private Double anomalyThresholdRatio = 1.50d;

    @Column(name = "rationale", length = 240)
    private String rationale;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
        if (expectedVolume == null || expectedVolume <= 0) {
            expectedVolume = 90;
        }
        if (alertThresholdRatio == null || alertThresholdRatio <= 0d) {
            alertThresholdRatio = 1.10d;
        }
        if (anomalyThresholdRatio == null || anomalyThresholdRatio < alertThresholdRatio) {
            anomalyThresholdRatio = Math.max(1.50d, alertThresholdRatio);
        }
    }
}
