package com.tcc.pjb.backend.model.entity.workflow;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_movimentacao_processual",
        indexes = {
                @Index(name = "idx_mov_processo", columnList = "processo_id,data_movimentacao")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoProcessual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_de", length = 60)
    private FaseProcessual faseDe;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_para", length = 60)
    private FaseProcessual fasePara;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ator_user_id")
    private Usuario ator;

    @Column(name = "data_movimentacao", updatable = false)
    private Instant dataMovimentacao;

    @PrePersist
    void prePersist() {
        if (dataMovimentacao == null) {
            dataMovimentacao = Instant.now();
        }
    }
}
