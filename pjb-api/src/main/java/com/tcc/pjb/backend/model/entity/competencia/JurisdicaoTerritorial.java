package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_jurisdicao_territorial_unidade", joinColumns = @JoinColumn(name = "jurisdicao_territorial_id"))
    @Column(name = "unidade_codigo", nullable = false, length = 80)
    private Set<String> unidadesElegiveis = new LinkedHashSet<>();

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
            String modoCompetencia, Set<String> unidadesElegiveis, String tribunalCodigo, String fonteNormativa,
            LocalDate vigenciaInicio, LocalDate vigenciaFim) {
        this.municipioIbge = Objects.requireNonNull(municipioIbge, "municipioIbge");
        this.municipioNome = Objects.requireNonNull(municipioNome, "municipioNome");
        this.uf = Objects.requireNonNull(uf, "uf");
        this.tipoJustica = Objects.requireNonNull(tipoJustica, "tipoJustica");
        this.modoCompetencia = Objects.requireNonNull(modoCompetencia, "modoCompetencia");
        Objects.requireNonNull(unidadesElegiveis, "unidadesElegiveis");
        if (unidadesElegiveis.isEmpty()) {
            throw new IllegalArgumentException("unidadesElegiveis não pode ser vazio");
        }
        this.unidadesElegiveis = new LinkedHashSet<>(unidadesElegiveis);
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

    public Set<String> getUnidadesElegiveis() {
        return Collections.unmodifiableSet(unidadesElegiveis);
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
