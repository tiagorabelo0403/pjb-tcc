package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;

import java.util.Collections;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.RegiaoBrasil;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;

@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "jurisdicoes",
        indexes = {
                @Index(name = "idx_jurisdicao_tipo", columnList = "tipo"),
                @Index(name = "idx_jurisdicao_esfera", columnList = "esfera"),
                @Index(name = "idx_jurisdicao_materia", columnList = "materia"),
                @Index(name = "idx_jurisdicao_comarca", columnList = "comarca_id"),
                @Index(name = "idx_jurisdicao_competencia",
                        columnList = "competencia_material,competencia_territorial")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_jurisdicao_codigo", columnNames = "codigo"),
                @UniqueConstraint(name = "uk_jurisdicao_sigla", columnNames = "sigla")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Jurisdicao {

    private static final int LIMITE_HIERARQUIA_DEFAULT = 12;

    public Jurisdicao() {
    }

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Z0-9-]+$")
    @Column(nullable = false, length = 50)
    private String codigo;

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(nullable = false, length = 20)
    private String sigla;

    @NotBlank
    @Size(min = 5, max = 200)
    @Column(nullable = false, length = 200)
    private String nome;

    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private TipoJurisdicao tipo;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 15)
    private NaturezaJurisdicao natureza;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private GrauJurisdicao grau;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private EsferaJurisdicao esfera;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private MateriaJurisdicao materia;

    
    @Size(max = 1000)
    @Column(name = "competencia_material", length = 1000)
    private String competenciaMaterial;

    @Size(max = 500)
    @Column(name = "competencia_territorial", length = 500)
    private String competenciaTerritorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    private Comarca comarcaEntidade;

    @Size(max = 120)
    @Column(name = "secao_judiciaria", length = 120)
    private String secaoJudiciaria;

    @Size(max = 120)
    @Column(name = "subsecao_judiciaria", length = 120)
    private String subsecaoJudiciaria;

    @Size(max = 120)
    @Column(length = 120)
    private String circunscricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private RegiaoBrasil regiao;

    
    @DecimalMin("0.0")
    @Column(name = "teto_valor_causa", precision = 17, scale = 2)
    private BigDecimal tetoValorCausa;

    @DecimalMin("0.0")
    @Column(name = "valor_minimo_causa", precision = 17, scale = 2)
    private BigDecimal valorMinimoCausa;

    
    @ElementCollection
    @CollectionTable(name = "jurisdicao_regras", joinColumns = @JoinColumn(name = "jurisdicao_id"))
    @Column(name = "regra")
    private List<String> regrasProcessuais = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "jurisdicao_normas", joinColumns = @JoinColumn(name = "jurisdicao_id"))
    @Column(name = "norma")
    private List<String> normasAplicaveis = new ArrayList<>();

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_para_id")
    private Jurisdicao recursoPara;

    @OneToMany(mappedBy = "recursoPara", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<Jurisdicao> recursosDe = new ArrayList<>();

    
    @Min(1)
    private Integer prazoComumDias;

    @Min(1)
    private Integer prazoRecursosDias;

    @Min(1)
    private Integer prazoRespostaDias;

    
    public String getCidade() {
        return comarcaEntidade != null ? comarcaEntidade.getNome() : null;
    }

    public String getUf() {
        return comarcaEntidade != null ? comarcaEntidade.getUf() : null;
    }

    public void setUf(String uf) {
        throw new UnsupportedOperationException("uf agora deriva de comarcaEntidade — usar setComarcaEntidade(Comarca)");
    }

    public String getMunicipioOuComarca() {
        return getCidade();
    }

    public String getForo() {
        if (comarcaEntidade != null && comarcaEntidade.getNomeForo() != null) {
            return comarcaEntidade.getNomeForo();
        }
        return nome;
    }

    public Comarca getComarcaEntidade() {
        return comarcaEntidade;
    }

    public void setComarcaEntidade(Comarca comarcaEntidade) {
        this.comarcaEntidade = comarcaEntidade;
    }

    public String getSecaoOuSubsecao() {
        return firstNonBlank(subsecaoJudiciaria, secaoJudiciaria);
    }

    public Integer getPrazoRespostaDiasJE() {
        return prazoRespostaDias != null ? prazoRespostaDias : 10;
    }

    
    @Column(nullable = false)
    private Boolean permiteJuizadoEspecial = false;

    @Column(nullable = false)
    private Boolean permiteTutelaAntecipada = false;

    @Column(nullable = false)
    private Boolean exigeAudienciaConciliacao = false;

    @Column(nullable = false)
    private Boolean permiteAcaoColetiva = false;

    @Column(nullable = false)
    private Boolean ativo = true;

    
    @Version
    private Long versao;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    
    public Jurisdicao(String codigo, String sigla, String nome,
                      TipoJurisdicao tipo, NaturezaJurisdicao natureza,
                      GrauJurisdicao grau, EsferaJurisdicao esfera,
                      MateriaJurisdicao materia) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.nome = nome;
        this.tipo = tipo;
        this.natureza = natureza;
        this.grau = grau;
        this.esfera = esfera;
        this.materia = materia;
    }

    
    @PrePersist
    @PreUpdate
    private void normalizarAntesDeSalvar() {
        
        this.codigo = normalizeUpperTrim(this.codigo);
        this.sigla = normalizeUpperTrim(this.sigla);


        this.nome = normalizeSpaces(this.nome);
        this.secaoJudiciaria = normalizeSpaces(this.secaoJudiciaria);
        this.subsecaoJudiciaria = normalizeSpaces(this.subsecaoJudiciaria);
        this.circunscricao = normalizeSpaces(this.circunscricao);

        
        this.competenciaMaterial = normalizeSpaces(this.competenciaMaterial);
        this.competenciaTerritorial = normalizeSpaces(this.competenciaTerritorial);

        
        if (this.permiteJuizadoEspecial == null) this.permiteJuizadoEspecial = false;
        if (this.permiteTutelaAntecipada == null) this.permiteTutelaAntecipada = false;
        if (this.exigeAudienciaConciliacao == null) this.exigeAudienciaConciliacao = false;
        if (this.permiteAcaoColetiva == null) this.permiteAcaoColetiva = false;
        if (this.ativo == null) this.ativo = true;

        if (this.regrasProcessuais == null) this.regrasProcessuais = new ArrayList<>();
        if (this.normasAplicaveis == null) this.normasAplicaveis = new ArrayList<>();
        if (this.recursosDe == null) this.recursosDe = new ArrayList<>();

        
        this.regrasProcessuais = sanitizeUnique(this.regrasProcessuais);
        this.normasAplicaveis = sanitizeUnique(this.normasAplicaveis);

        
        
        if (this.valorMinimoCausa != null && this.tetoValorCausa != null) {
            if (this.valorMinimoCausa.compareTo(this.tetoValorCausa) > 0) {
                
                BigDecimal tmp = this.valorMinimoCausa;
                this.valorMinimoCausa = this.tetoValorCausa;
                this.tetoValorCausa = tmp;
            }
        }
    }

    

    
    public boolean aceitaJuizadoEspecial() {
        return Boolean.TRUE.equals(permiteJuizadoEspecial)
                && tetoValorCausa != null
                && tetoValorCausa.compareTo(BigDecimal.ZERO) > 0;
    }

    
    public boolean isJurisdicaoSuperior() {
        return grau == GrauJurisdicao.SUPERIOR
                || grau == GrauJurisdicao.CONSTITUCIONAL;
    }

    
    public boolean isAtiva() {
        return Boolean.TRUE.equals(ativo);
    }

    
    public boolean temCompetenciaDefinida() {
        return notBlank(competenciaMaterial) || notBlank(competenciaTerritorial);
    }

    
    public boolean faixaFinanceiraValida() {
        if (valorMinimoCausa == null || tetoValorCausa == null) return true;
        return valorMinimoCausa.compareTo(tetoValorCausa) <= 0;
    }

    public boolean valorCausaDentroDosLimites(BigDecimal valorCausa) {
        if (valorCausa == null) return false;
        if (valorCausa.compareTo(BigDecimal.ZERO) < 0) return false;

        if (valorMinimoCausa != null && valorCausa.compareTo(valorMinimoCausa) < 0) return false;
        if (tetoValorCausa != null && tetoValorCausa.compareTo(BigDecimal.ZERO) > 0) {
            if (valorCausa.compareTo(tetoValorCausa) > 0) return false;
        }
        return true;
    }

    
    public String getComarcaUf() {
        if (comarcaEntidade == null) {
            return null;
        }
        return comarcaEntidade.getNome() + "/" + comarcaEntidade.getUf();
    }

    
    public String chaveInstitucional() {
        StringBuilder sb = new StringBuilder();
        sb.append(nullSafe(codigo)).append('|')
                .append(nullSafe(sigla)).append('|')
                .append(grau != null ? grau.name() : "").append('|')
                .append(esfera != null ? esfera.name() : "").append('|')
                .append(materia != null ? materia.name() : "").append('|')
                .append(nullSafe(getUf()));
        return sb.toString();
    }

    
    public boolean addRegraProcessual(String regra) {
        String r = normalizeSpaces(regra);
        if (!notBlank(r)) return false;
        if (regrasProcessuais == null) regrasProcessuais = new ArrayList<>();
        if (regrasProcessuais.stream().anyMatch(x -> x.equalsIgnoreCase(r))) return false;
        return regrasProcessuais.add(r);
    }

    
    public boolean addNormaAplicavel(String norma) {
        String n = normalizeSpaces(norma);
        if (!notBlank(n)) return false;
        if (normasAplicaveis == null) normasAplicaveis = new ArrayList<>();
        if (normasAplicaveis.stream().anyMatch(x -> x.equalsIgnoreCase(n))) return false;
        return normasAplicaveis.add(n);
    }

    
    public List<String> cadeiaRecursalCodigos() {
        return cadeiaRecursalCodigos(LIMITE_HIERARQUIA_DEFAULT);
    }

    
    public List<String> cadeiaRecursalCodigos(int limite) {
        int lim = Math.max(1, limite);
        List<String> cadeia = new ArrayList<>();
        Set<String> vistos = new HashSet<>();

        Jurisdicao atual = this;
        int passos = 0;

        while (atual != null && passos < lim) {
            String cod = normalizeUpperTrim(atual.codigo);
            if (cod != null) {
                if (vistos.contains(cod)) {
                    cadeia.add(cod + " (CICLO_DETECTADO)");
                    break;
                }
                vistos.add(cod);
                cadeia.add(cod);
            } else {
                cadeia.add("(SEM_CODIGO)");
            }

            atual = atual.recursoPara;
            passos++;
        }

        return Collections.unmodifiableList(cadeia);
    }

    
    public String resumoOperacional() {
        return "Jurisdicao{" +
                "codigo='" + codigo + '\'' +
                ", sigla='" + sigla + '\'' +
                ", nome='" + nome + '\'' +
                ", grau=" + (grau != null ? grau.name() : null) +
                ", esfera=" + (esfera != null ? esfera.name() : null) +
                ", materia=" + (materia != null ? materia.name() : null) +
                ", comarcaUf=" + getComarcaUf() +
                ", foro='" + getForo() + '\'' +
                ", ativo=" + isAtiva() +
                '}';
    }

    public Map<String, Object> snapshotTerritorial() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", codigo);
        out.put("sigla", sigla);
        out.put("uf", getUf());
        out.put("cidade", getCidade());
        out.put("comarca", getCidade());
        out.put("foro", getForo());
        out.put("secaoJudiciaria", secaoJudiciaria);
        out.put("subsecaoJudiciaria", subsecaoJudiciaria);
        out.put("circunscricao", circunscricao);
        out.put("grau", grau != null ? grau.name() : null);
        out.put("esfera", esfera != null ? esfera.name() : null);
        out.put("materia", materia != null ? materia.name() : null);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    public JurisdictionEngine.Context toEngineContext(RitoProcessual ritoProcessual) {

        JurisdictionEngine.Rite rite = mapRite(ritoProcessual, this.materia);
        String orgao = this.nome; 
        String pais = "BR";
        return new JurisdictionEngine.Context(
                this.materia != null ? this.materia.name() : null,
                orgao,
                pais,
                null,
                rite
        );
    }

    
    public RitoProcessual sugerirRitoDefault() {
        if (materia == null) return RitoProcessual.COMUM_ORDINARIO;
        return switch (materia) {
            case PENAL -> RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
            case EXECUCAO_PENAL -> RitoProcessual.EXECUCAO_PENAL;
            case MILITAR -> RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
            case ELEITORAL -> RitoProcessual.ELEITORAL;
            case TRABALHISTA -> RitoProcessual.TRABALHISTA_ORDINARIO;
            case PREVIDENCIARIA -> RitoProcessual.PREVIDENCIARIO_COMUM;
            case TRIBUTARIA, EXECUCAO_FISCAL -> RitoProcessual.EXECUCAO_FISCAL;
            case FAMILIA, SUCESSOES, INFANCIA_JUVENTUDE -> RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
            case CONSUMIDOR -> RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
            default -> RitoProcessual.COMUM_ORDINARIO;
        };
    }

    private static JurisdictionEngine.Rite mapRite(RitoProcessual rito, MateriaJurisdicao materia) {
        if (rito != null) {
            String r = rito.name();
            if (r.startsWith("PENAL_") || r.startsWith("PROCEDIMENTO_PENAL") || r.equals("TRIBUNAL_JURI") || r.equals("JUIZADO_ESPECIAL_CRIMINAL")) {
                return JurisdictionEngine.Rite.PENAL;
            }
            if (r.equals("EXECUCAO_PENAL")) return JurisdictionEngine.Rite.EXECUCAO_PENAL;
            if (r.startsWith("MILITAR")) return JurisdictionEngine.Rite.MILITAR;
            if (r.startsWith("TRIBUTARIO") || r.contains("EXECUCAO_FISCAL") || r.startsWith("FAZENDA_PUBLICA")) return JurisdictionEngine.Rite.TRIBUTARIO;
            if (r.startsWith("TRABALHISTA")) return JurisdictionEngine.Rite.TRABALHISTA;
            if (r.startsWith("PREVIDENCIARIO")) return JurisdictionEngine.Rite.PREVIDENCIARIO;
            if (r.startsWith("ELEITORAL")) return JurisdictionEngine.Rite.ELEITORAL;
        }
        if (materia == null) return JurisdictionEngine.Rite.COMUM;
        return switch (materia) {
            case PENAL -> JurisdictionEngine.Rite.PENAL;
            case EXECUCAO_PENAL -> JurisdictionEngine.Rite.EXECUCAO_PENAL;
            case MILITAR -> JurisdictionEngine.Rite.MILITAR;
            case TRIBUTARIA, EXECUCAO_FISCAL -> JurisdictionEngine.Rite.TRIBUTARIO;
            case TRABALHISTA -> JurisdictionEngine.Rite.TRABALHISTA;
            case PREVIDENCIARIA -> JurisdictionEngine.Rite.PREVIDENCIARIO;
            case ELEITORAL -> JurisdictionEngine.Rite.ELEITORAL;
            case AGRARIO -> JurisdictionEngine.Rite.AGRARIO;
            case FAMILIA, SUCESSOES, INFANCIA_JUVENTUDE -> JurisdictionEngine.Rite.FAMILIA;
            case CONSUMIDOR -> JurisdictionEngine.Rite.CONSUMIDOR;
            case EMPRESARIAL, FALENCIAS -> JurisdictionEngine.Rite.EMPRESARIAL;
            case CONSTITUCIONAL -> JurisdictionEngine.Rite.CONSTITUCIONAL;
            default -> JurisdictionEngine.Rite.COMUM;
        };
    }

    
    public void validarInvariantes() {
        if (!notBlank(codigo)) throw new IllegalStateException("codigo obrigatório");
        if (!notBlank(sigla)) throw new IllegalStateException("sigla obrigatória");
        if (!notBlank(nome)) throw new IllegalStateException("nome obrigatório");

        if (tipo == null) throw new IllegalStateException("tipo obrigatório");
        if (natureza == null) throw new IllegalStateException("natureza obrigatória");
        if (grau == null) throw new IllegalStateException("grau obrigatório");
        if (esfera == null) throw new IllegalStateException("esfera obrigatória");
        if (materia == null) throw new IllegalStateException("materia obrigatória");

        if (!faixaFinanceiraValida()) {
            throw new IllegalStateException("faixa financeira inválida: valorMinimoCausa > tetoValorCausa");
        }

        
        List<String> cadeia = cadeiaRecursalCodigos(50);
        if (cadeia.stream().anyMatch(s -> s != null && s.contains("CICLO_DETECTADO"))) {
            throw new IllegalStateException("ciclo detectado na hierarquia recursal: " + cadeia);
        }
    }

    
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalizeUpperTrim(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isEmpty() ? null : s.toUpperCase(Locale.ROOT);
    }

    private static String normalizeSpaces(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
    }

    private static List<String> sanitizeUnique(List<String> in) {
        if (in == null || in.isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : in) {
            String v = normalizeSpaces(s);
            if (notBlank(v)) set.add(v);
        }
        return new ArrayList<>(set);
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jurisdicao)) return false;
        Jurisdicao that = (Jurisdicao) o;
        return codigo != null && codigo.equals(that.codigo);
    }

    @Override
    public int hashCode() {
        return codigo != null ? codigo.hashCode() : 0;
    }


    @jakarta.persistence.Transient
    public String getEspecie() {
        
        
        if (tipo != null) {
            return tipo.name();
        }
        if (esfera != null) {
            return esfera.name();
        }
        if (materia != null) {
            return materia.name();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoJurisdicao getTipo() {
        return tipo;
    }

    public void setTipo(TipoJurisdicao tipo) {
        this.tipo = tipo;
    }

    public NaturezaJurisdicao getNatureza() {
        return natureza;
    }

    public void setNatureza(NaturezaJurisdicao natureza) {
        this.natureza = natureza;
    }

    public GrauJurisdicao getGrau() {
        return grau;
    }

    public void setGrau(GrauJurisdicao grau) {
        this.grau = grau;
    }

    public EsferaJurisdicao getEsfera() {
        return esfera;
    }

    public void setEsfera(EsferaJurisdicao esfera) {
        this.esfera = esfera;
    }

    public MateriaJurisdicao getMateria() {
        return materia;
    }

    public void setMateria(MateriaJurisdicao materia) {
        this.materia = materia;
    }

    public String getCompetenciaMaterial() {
        return competenciaMaterial;
    }

    public void setCompetenciaMaterial(String competenciaMaterial) {
        this.competenciaMaterial = competenciaMaterial;
    }

    public String getCompetenciaTerritorial() {
        return competenciaTerritorial;
    }

    public void setCompetenciaTerritorial(String competenciaTerritorial) {
        this.competenciaTerritorial = competenciaTerritorial;
    }

    public String getSecaoJudiciaria() {
        return secaoJudiciaria;
    }

    public void setSecaoJudiciaria(String secaoJudiciaria) {
        this.secaoJudiciaria = secaoJudiciaria;
    }

    public String getSubsecaoJudiciaria() {
        return subsecaoJudiciaria;
    }

    public void setSubsecaoJudiciaria(String subsecaoJudiciaria) {
        this.subsecaoJudiciaria = subsecaoJudiciaria;
    }

    public String getCircunscricao() {
        return circunscricao;
    }

    public void setCircunscricao(String circunscricao) {
        this.circunscricao = circunscricao;
    }

    public RegiaoBrasil getRegiao() {
        return regiao;
    }

    public void setRegiao(RegiaoBrasil regiao) {
        this.regiao = regiao;
    }

    public BigDecimal getTetoValorCausa() {
        return tetoValorCausa;
    }

    public void setTetoValorCausa(BigDecimal tetoValorCausa) {
        this.tetoValorCausa = tetoValorCausa;
    }

    public BigDecimal getValorMinimoCausa() {
        return valorMinimoCausa;
    }

    public void setValorMinimoCausa(BigDecimal valorMinimoCausa) {
        this.valorMinimoCausa = valorMinimoCausa;
    }

    public List<String> getRegrasProcessuais() {
        return regrasProcessuais;
    }

    public void setRegrasProcessuais(List<String> regrasProcessuais) {
        this.regrasProcessuais = regrasProcessuais;
    }

    public List<String> getNormasAplicaveis() {
        return normasAplicaveis;
    }

    public void setNormasAplicaveis(List<String> normasAplicaveis) {
        this.normasAplicaveis = normasAplicaveis;
    }

    public Jurisdicao getRecursoPara() {
        return recursoPara;
    }

    public void setRecursoPara(Jurisdicao recursoPara) {
        this.recursoPara = recursoPara;
    }

    public List<Jurisdicao> getRecursosDe() {
        return recursosDe;
    }

    public void setRecursosDe(List<Jurisdicao> recursosDe) {
        this.recursosDe = recursosDe;
    }

    public Integer getPrazoComumDias() {
        return prazoComumDias;
    }

    public void setPrazoComumDias(Integer prazoComumDias) {
        this.prazoComumDias = prazoComumDias;
    }

    public Integer getPrazoRecursosDias() {
        return prazoRecursosDias;
    }

    public void setPrazoRecursosDias(Integer prazoRecursosDias) {
        this.prazoRecursosDias = prazoRecursosDias;
    }

    public Integer getPrazoRespostaDias() {
        return prazoRespostaDias;
    }

    public void setPrazoRespostaDias(Integer prazoRespostaDias) {
        this.prazoRespostaDias = prazoRespostaDias;
    }

    public Boolean getPermiteJuizadoEspecial() {
        return permiteJuizadoEspecial;
    }

    public void setPermiteJuizadoEspecial(Boolean permiteJuizadoEspecial) {
        this.permiteJuizadoEspecial = permiteJuizadoEspecial;
    }

    public Boolean getPermiteTutelaAntecipada() {
        return permiteTutelaAntecipada;
    }

    public void setPermiteTutelaAntecipada(Boolean permiteTutelaAntecipada) {
        this.permiteTutelaAntecipada = permiteTutelaAntecipada;
    }

    public Boolean getExigeAudienciaConciliacao() {
        return exigeAudienciaConciliacao;
    }

    public void setExigeAudienciaConciliacao(Boolean exigeAudienciaConciliacao) {
        this.exigeAudienciaConciliacao = exigeAudienciaConciliacao;
    }

    public Boolean getPermiteAcaoColetiva() {
        return permiteAcaoColetiva;
    }

    public void setPermiteAcaoColetiva(Boolean permiteAcaoColetiva) {
        this.permiteAcaoColetiva = permiteAcaoColetiva;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

}
