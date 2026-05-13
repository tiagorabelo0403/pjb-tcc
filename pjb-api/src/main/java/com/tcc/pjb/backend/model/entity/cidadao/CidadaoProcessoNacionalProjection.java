package com.tcc.pjb.backend.model.entity.cidadao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.model.entity.enums.GrauConfiancaVinculoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_cidadao_processo_nacional_projection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cidadao_processo_projection", columnNames = {"identidade_id", "nupn"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CidadaoProcessoNacionalProjection {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "identidade_id", nullable = false)
    private UUID identidadeId;

    @Column(name = "documento_hash", nullable = false, length = 64)
    private String documentoHash;

    @Column(name = "nupn", nullable = false, length = 50)
    private String nupn;

    @Column(name = "processo_local_id")
    private Long processoLocalId;

    @Column(name = "numero_exibicao", nullable = false, length = 50)
    private String numeroExibicao;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "sistema_origem", length = 20)
    private String sistemaOrigem;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "comarca", length = 120)
    private String comarca;

    @Column(name = "unidade_judicial", length = 180)
    private String unidadeJudicial;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel_processual", nullable = false, length = 40)
    private PapelProcessualNacional papelProcessual;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau_confianca", nullable = false, length = 40)
    private GrauConfiancaVinculoProcessual grauConfianca;

    @Column(name = "score_confianca", nullable = false)
    private Integer scoreConfianca;

    @Column(name = "origem_vinculo", nullable = false, length = 40)
    private String origemVinculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processo", length = 60)
    private StatusProcesso statusProcesso;

    @Column(name = "fase_atual", length = 60)
    private String faseAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_direito", length = 60)
    private RamoDireito ramoDireito;

    @Column(name = "classe_processual", length = 160)
    private String classeProcessual;

    @Column(name = "assunto", length = 240)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sigilo", length = 40)
    private NivelSigilo nivelSigilo;

    @Column(name = "data_distribuicao")
    private LocalDateTime dataDistribuicao;

    @Column(name = "data_ultima_movimentacao")
    private LocalDateTime dataUltimaMovimentacao;

    @Column(name = "ultima_movimentacao_resumo", length = 240)
    private String ultimaMovimentacaoResumo;

    @Column(name = "arquivado", nullable = false)
    private boolean arquivado;

    @Column(name = "oculto_por_politica_arquivo", nullable = false)
    private boolean ocultoPorPoliticaArquivo;

    @Column(name = "reexposto_secretaria", nullable = false)
    private boolean reexpostoSecretaria;

    @Column(name = "visivel_painel_pessoal", nullable = false)
    private boolean visivelPainelPessoal;

    @Column(name = "exige_step_up", nullable = false)
    private boolean exigeStepUp;

    @Column(name = "origem_externa_uri", length = 240)
    private String origemExternaUri;

    @Column(name = "sort_key", nullable = false)
    private long sortKey;

    @Column(name = "gerado_em", nullable = false)
    private Instant geradoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (geradoEm == null) {
            geradoEm = now;
        }
        atualizadoEm = now;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
