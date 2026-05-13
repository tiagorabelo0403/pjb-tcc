package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import lombok.*;






@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_audiencia")
public class Audiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 60, nullable = false)
    private TipoAudiencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", length = 40, nullable = false)
    private ModalidadeAudiencia modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private StatusAudiencia status;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "duracao_min")
    private Integer duracaoMin;

    @Column(name = "local", length = 260)
    private String local;

    @Column(name = "link_video", length = 600)
    private String linkVideo;

    @Column(name = "pauta", columnDefinition = "TEXT")
    private String pauta;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "criado_por")
    private Long criadoPor;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
