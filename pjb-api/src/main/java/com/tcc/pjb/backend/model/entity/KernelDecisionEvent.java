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

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_kernel_decision_event")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class KernelDecisionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "proposta_id")
    private Long propostaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "scope", length = 80, nullable = false)
    private String scope;

    @Column(name = "decision_code", length = 80, nullable = false)
    private String decisionCode;

    @Column(name = "release_allowed", nullable = false)
    private boolean releaseAllowed;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "internal_draft_required", nullable = false)
    private boolean internalDraftRequired;

    @Column(name = "risk_level", length = 60)
    private String riskLevel;

    @Lob
    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons;

    @Lob
    @Column(name = "mandatory_actions", columnDefinition = "TEXT")
    private String mandatoryActions;

    @Lob
    @Column(name = "diagnostics", columnDefinition = "TEXT")
    private String diagnostics;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
