package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_process_intelligence_snapshot")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessIntelligenceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "snapshot_scope", length = 80, nullable = false)
    private String snapshotScope;

    @Column(name = "policy_key", length = 160)
    private String policyKey;

    @Column(name = "policy_tier", length = 80)
    private String policyTier;

    @Column(name = "risk_level", length = 60)
    private String riskLevel;

    @Column(name = "decision_code", length = 80)
    private String decisionCode;

    @Lob
    @Column(name = "strategic_focus", columnDefinition = "TEXT")
    private String strategicFocus;

    @Lob
    @Column(name = "telemetry", columnDefinition = "TEXT")
    private String telemetry;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
