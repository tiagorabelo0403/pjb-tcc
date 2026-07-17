package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_jurisdicao_territorial",
        indexes = {
                @Index(name = "idx_jurisdicao_lookup", columnList = "municipio_ibge, tipo_justica, vigencia_inicio DESC")
        }
)
public class JurisdicaoTerritorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "municipio_ibge", length = 7, nullable = false)
    private String municipioIbge;

    @Column(name = "municipio_nome", length = 120, nullable = false)
    private String municipioNome;

    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    @Column(name = "tipo_justica", length = 30, nullable = false)
    private String tipoJustica;

    @Column(name = "modo_competencia", length = 30, nullable = false)
    private String modoCompetencia;

    @Column(name = "unidade_codigo", length = 80, nullable = false)
    private String unidadeCodigo;

    @Column(name = "tribunal_codigo", length = 20, nullable = false)
    private String tribunalCodigo;

    @Column(name = "fonte_normativa", length = 240, nullable = false)
    private String fonteNormativa;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    protected JurisdicaoTerritorial() {
    }

    public JurisdicaoTerritorial(String municipioIbge, String municipioNome, String uf, String tipoJustica,
            String modoCompetencia, String unidadeCodigo, String tribunalCodigo, String fonteNormativa,
            LocalDate vigenciaInicio, LocalDate vigenciaFim) {
        this.municipioIbge = Objects.requireNonNull(municipioIbge, "municipioIbge");
        this.municipioNome = Objects.requireNonNull(municipioNome, "municipioNome");
        this.uf = Objects.requireNonNull(uf, "uf");
        this.tipoJustica = Objects.requireNonNull(tipoJustica, "tipoJustica");
        this.modoCompetencia = Objects.requireNonNull(modoCompetencia, "modoCompetencia");
        this.unidadeCodigo = Objects.requireNonNull(unidadeCodigo, "unidadeCodigo");
        this.tribunalCodigo = Objects.requireNonNull(tribunalCodigo, "tribunalCodigo");
        this.fonteNormativa = Objects.requireNonNull(fonteNormativa, "fonteNormativa");
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        this.vigenciaFim = vigenciaFim;
    }

    public Long getId() {
        return id;
    }

    public String getMunicipioIbge() {
        return municipioIbge;
    }

    public String getMunicipioNome() {
        return municipioNome;
    }

    public String getUf() {
        return uf;
    }

    public String getTipoJustica() {
        return tipoJustica;
    }

    public String getModoCompetencia() {
        return modoCompetencia;
    }

    public String getUnidadeCodigo() {
        return unidadeCodigo;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public String getFonteNormativa() {
        return fonteNormativa;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }
}
