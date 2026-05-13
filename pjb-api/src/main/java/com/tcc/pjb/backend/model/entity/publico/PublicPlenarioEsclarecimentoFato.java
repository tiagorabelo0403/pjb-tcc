package com.tcc.pjb.backend.model.entity.publico;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_public_plenario_esclarecimento_fato",
        indexes = {
                @Index(name = "idx_public_plenario_esclarecimento_sessao", columnList = "sessao_id"),
                @Index(name = "idx_public_plenario_esclarecimento_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PublicPlenarioEsclarecimentoFato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private JulgamentoColegiado sessao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por_id")
    private Usuario respondidoPor;

    @Column(name = "resumo_duvida", nullable = false, columnDefinition = "TEXT")
    private String resumoDuvida;

    @Column(name = "resposta_publica", columnDefinition = "TEXT")
    private String respostaPublica;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "visivel_publicamente", nullable = false)
    private boolean visivelPublicamente;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ABERTO";
        }
    }
}
