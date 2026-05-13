package com.tcc.pjb.backend.model.entity.pericia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_perito_nomeacao",
        indexes = {
                @Index(name = "idx_perito_nomeacao_processo", columnList = "processo_id"),
                @Index(name = "idx_perito_nomeacao_perito", columnList = "perito_id"),
                @Index(name = "idx_perito_nomeacao_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeritoNomeacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perito_id", nullable = false)
    private Usuario perito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomeado_por")
    private Usuario nomeadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PeritoNomeacaoStatus status = PeritoNomeacaoStatus.NOMEADO;

    @Column(name = "nomeado_em", nullable = false)
    @Builder.Default
    private LocalDateTime nomeadoEm = LocalDateTime.now();

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por")
    private Usuario respondidoPor;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revogado_por")
    private Usuario revogadoPor;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;
}
