package com.tcc.pjb.backend.model.entity.atlas;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_atlas_acesso_municipio", indexes = {
        @Index(name = "idx_atlas_municipio_codigo_ibge", columnList = "codigo_ibge", unique = true),
        @Index(name = "idx_atlas_municipio_uf_class", columnList = "uf, classificacao"),
        @Index(name = "idx_atlas_municipio_score", columnList = "score_total"),
        @Index(name = "idx_atlas_municipio_pop", columnList = "populacao")
})
public class AtlasAcessoMunicipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @Column(name = "codigo_ibge", nullable = false, length = 7)
    private String codigoIbge;

    @Column(name = "nome_municipio", nullable = false, length = 160)
    private String nomeMunicipio;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false, length = 30)
    private String regiao;

    @Column(nullable = false)
    private Integer populacao;

    @Column(name = "varas_instaladas", nullable = false)
    private Integer varasInstaladas;

    @Column(name = "juizes_em_exercicio", nullable = false)
    private Integer juizesEmExercicio;

    @Column(name = "defensorias_por_municipio", nullable = false)
    private Integer defensoriasPorMunicipio;

    @Column(name = "advogados_oab_ativos", nullable = false)
    private Integer advogadosOabAtivos;

    @Column(name = "tem_juizado_especial", nullable = false)
    private Boolean temJuizadoEspecial;

    @Column(name = "tem_cejusc", nullable = false)
    private Boolean temCejusc;

    @Column(name = "processos_por_mil_habitantes", nullable = false)
    private Integer processosPorMilHabitantes;

    @Column(name = "novos_processos_mes", nullable = false)
    private Integer novosProcessosMes;

    @Column(name = "taxa_resolutividade_pct", precision = 10, scale = 4, nullable = false)
    private BigDecimal taxaResolutividadePct;

    @Column(name = "tempo_medio_resolucao_dias", precision = 10, scale = 2, nullable = false)
    private BigDecimal tempoMedioResolucaoDias;

    @Column(name = "indice_congestionamento", precision = 10, scale = 4, nullable = false)
    private BigDecimal indiceCongestionamento;

    @Column(name = "taxa_justica_gratuita_pct", precision = 10, scale = 4, nullable = false)
    private BigDecimal taxaJusticaGratuitaPct;

    @Column(name = "taxa_auto_representacao_pct", precision = 10, scale = 4, nullable = false)
    private BigDecimal taxaAutoRepresentacaoPct;

    @Column(name = "taxa_prescricao_aparente_pct", precision = 10, scale = 4, nullable = false)
    private BigDecimal taxaPrescricaoAparentePct;

    @Column(name = "score_infraestrutura", precision = 10, scale = 4, nullable = false)
    private BigDecimal scoreInfraestrutura;

    @Column(name = "score_representacao", precision = 10, scale = 4, nullable = false)
    private BigDecimal scoreRepresentacao;

    @Column(name = "score_celeridade", precision = 10, scale = 4, nullable = false)
    private BigDecimal scoreCeleridade;

    @Column(name = "score_efetividade", precision = 10, scale = 4, nullable = false)
    private BigDecimal scoreEfetividade;

    @Column(name = "score_total", precision = 10, scale = 4, nullable = false)
    private BigDecimal scoreTotal;

    @Column(nullable = false, length = 1)
    private String grau;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClassificacaoDesertoAtlas classificacao;

    @Column(name = "origem_dados", length = 60)
    private String origemDados;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (atualizadoEm == null) {
            atualizadoEm = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
