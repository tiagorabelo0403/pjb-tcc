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
@Table(name = "tb_negotiation_round_snapshot")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRoundSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "proposta_id")
    private Long propostaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "round_scope", length = 80, nullable = false)
    private String roundScope;

    @Column(name = "approval_band", length = 80)
    private String approvalBand;

    @Column(name = "release_mode", length = 80)
    private String releaseMode;

    @Column(name = "risk_level", length = 60)
    private String riskLevel;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "suggested_message", columnDefinition = "TEXT")
    private String suggestedMessage;

    @Lob
    @Column(name = "mandatory_actions", columnDefinition = "TEXT")
    private String mandatoryActions;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
