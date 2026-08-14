package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_unidade_judiciaria_competencia",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_unidade_judiciaria_competencia_codigo", columnNames = "codigo")
        },
        indexes = {
                @Index(name = "idx_unidade_competencia_tribunal", columnList = "tribunal_id"),
                @Index(name = "idx_unidade_competencia_territorio", columnList = "uf, comarca_id"),
                @Index(name = "idx_unidade_competencia_status", columnList = "aceita_distribuicao, status_operacional"),
                @Index(name = "idx_unidade_competencia_justica", columnList = "tipo_justica, ramo_direito, tipo_vara")
        }
)
public class UnidadeJudiciariaCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tribunal_id", nullable = false)
    private Tribunal tribunal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    private Comarca comarca;

    @Column(name = "uf", length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_justica", length = 30)
    private TipoJustica tipoJustica;

    @Enumerated(EnumType.STRING)
    @Column(name = "ramo_direito", length = 60)
    private RamoDireito ramoDireito;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vara", nullable = false, length = 40)
    private TipoVaraDistribuicao tipoVara;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_unidade_judiciaria_especialidade", joinColumns = @JoinColumn(name = "unidade_id"))
    @Column(name = "especialidade", nullable = false, length = 120)
    private Set<String> especialidades = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_unidade_judiciaria_classe_tpu", joinColumns = @JoinColumn(name = "unidade_id"))
    @Column(name = "classe_tpu", nullable = false, length = 80)
    private Set<String> classesTpu = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_unidade_judiciaria_assunto_tpu", joinColumns = @JoinColumn(name = "unidade_id"))
    @Column(name = "assunto_tpu", nullable = false, length = 80)
    private Set<String> assuntosTpu = new LinkedHashSet<>();

    @Column(name = "processos_ativos", nullable = false)
    private int processosAtivos;

    @Column(name = "capacidade_maxima", nullable = false)
    private int capacidadeMaxima;

    @Column(name = "capacidade_reserva_percentual", nullable = false)
    private int capacidadeReservaPercentual;

    @Column(name = "indice_congestionamento", precision = 6, scale = 4, nullable = false)
    private BigDecimal indiceCongestionamento;

    @Column(name = "aceita_distribuicao", nullable = false)
    private boolean aceitaDistribuicao;

    @Column(name = "permite_juizado_especial", nullable = false)
    private boolean permiteJuizadoEspecial;

    @Column(name = "permite_urgencia", nullable = false)
    private boolean permiteUrgencia;

    @Column(name = "permite_distribuicao_automatica", nullable = false)
    private boolean permiteDistribuicaoAutomatica;

    @Column(name = "somente_digital", nullable = false)
    private boolean somenteDigital;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 20)
    private StatusOperacionalUnidadeJudiciaria statusOperacional;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_operacao", nullable = false, length = 15)
    private ModoOperacaoUnidadeJudiciaria modoOperacao;

    @Column(name = "endereco_fisico", length = 240)
    private String enderecoFisico;

    @Column(name = "endereco_digital", length = 240)
    private String enderecoDigital;

    @Column(name = "valor_causa_minimo", precision = 19, scale = 2)
    private BigDecimal valorCausaMinimo;

    @Column(name = "valor_causa_maximo", precision = 19, scale = 2)
    private BigDecimal valorCausaMaximo;

    @Column(name = "prioridade_estrategica", nullable = false)
    private int prioridadeEstrategica;

    @Column(name = "distribuicoes_ultimas_24h", nullable = false)
    private int distribuicoesUltimas24h;

    @Column(name = "ultima_distribuicao_em")
    private Instant ultimaDistribuicaoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(name = "versao")
    private Long versao;

    protected UnidadeJudiciariaCompetencia() {
    }

    public UnidadeJudiciariaCompetencia(String codigo,
                                        Tribunal tribunal,
                                        Comarca comarca,
                                        String uf,
                                        TipoJustica tipoJustica,
                                        RamoDireito ramoDireito,
                                        TipoVaraDistribuicao tipoVara) {
        this.codigo = Objects.requireNonNull(codigo);
        this.tribunal = Objects.requireNonNull(tribunal);
        this.comarca = comarca;
        this.uf = uf;
        this.tipoJustica = tipoJustica;
        this.ramoDireito = ramoDireito;
        this.tipoVara = Objects.requireNonNull(tipoVara);
        this.capacidadeMaxima = 1;
        this.indiceCongestionamento = BigDecimal.ZERO;
        this.statusOperacional = StatusOperacionalUnidadeJudiciaria.ATIVA;
        this.modoOperacao = ModoOperacaoUnidadeJudiciaria.HIBRIDA;
        this.aceitaDistribuicao = true;
        this.permiteDistribuicaoAutomatica = true;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (this.id == null && this.versao == null) {
            this.versao = 0L;
        }
        this.codigo = normalizeUpper(this.codigo);
        this.uf = normalizeUpper(this.uf);
        this.enderecoFisico = normalizeText(this.enderecoFisico);
        this.enderecoDigital = normalizeText(this.enderecoDigital);
        this.classesTpu = sanitizeSet(this.classesTpu, true);
        this.assuntosTpu = sanitizeSet(this.assuntosTpu, true);
        this.especialidades = sanitizeSet(this.especialidades, false);
        if (this.capacidadeMaxima <= 0) {
            this.capacidadeMaxima = 1;
        }
        if (this.capacidadeReservaPercentual < 0) {
            this.capacidadeReservaPercentual = 0;
        }
        if (this.capacidadeReservaPercentual > 95) {
            this.capacidadeReservaPercentual = 95;
        }
        if (this.processosAtivos < 0) {
            this.processosAtivos = 0;
        }
        if (this.distribuicoesUltimas24h < 0) {
            this.distribuicoesUltimas24h = 0;
        }
        if (this.prioridadeEstrategica < 0) {
            this.prioridadeEstrategica = 0;
        }
        if (this.prioridadeEstrategica > 100) {
            this.prioridadeEstrategica = 100;
        }
        if (this.indiceCongestionamento == null) {
            this.indiceCongestionamento = BigDecimal.ZERO;
        }
        if (this.indiceCongestionamento.compareTo(BigDecimal.ZERO) < 0) {
            this.indiceCongestionamento = BigDecimal.ZERO;
        }
        if (this.indiceCongestionamento.compareTo(BigDecimal.ONE) > 0) {
            this.indiceCongestionamento = BigDecimal.ONE;
        }
        if (this.valorCausaMinimo != null && this.valorCausaMaximo != null && this.valorCausaMinimo.compareTo(this.valorCausaMaximo) > 0) {
            BigDecimal tmp = this.valorCausaMinimo;
            this.valorCausaMinimo = this.valorCausaMaximo;
            this.valorCausaMaximo = tmp;
        }
        if (this.statusOperacional == null) {
            this.statusOperacional = StatusOperacionalUnidadeJudiciaria.ATIVA;
        }
        if (this.modoOperacao == null) {
            this.modoOperacao = ModoOperacaoUnidadeJudiciaria.HIBRIDA;
        }
        if (this.tipoVara == null) {
            this.tipoVara = TipoVaraDistribuicao.VARA_UNICA;
        }
        Instant now = Instant.now();
        if (this.criadoEm == null) {
            this.criadoEm = now;
        }
        this.atualizadoEm = now;
    }

    public boolean estaAptaParaDistribuicao() {
        return aceitaDistribuicao
                && statusOperacional == StatusOperacionalUnidadeJudiciaria.ATIVA
                && disponibilidadeOperacional() > 0.0d;
    }

    public boolean suportaMateria(String materia) {
        if (materia == null || materia.isBlank()) {
            return especialidades.isEmpty();
        }
        String normalized = normalizeKey(materia);
        if (especialidades.isEmpty()) {
            return true;
        }
        return especialidades.stream().anyMatch(item -> normalizeKey(item).equals(normalized));
    }

    public boolean suportaClasse(String classeTpu) {
        if (classeTpu == null || classeTpu.isBlank() || classesTpu.isEmpty()) {
            return true;
        }
        String normalized = normalizeKey(classeTpu);
        return classesTpu.stream().anyMatch(item -> normalizeKey(item).equals(normalized));
    }

    public boolean suportaAssunto(String assuntoTpu) {
        if (assuntoTpu == null || assuntoTpu.isBlank() || assuntosTpu.isEmpty()) {
            return true;
        }
        String normalized = normalizeKey(assuntoTpu);
        return assuntosTpu.stream().anyMatch(item -> normalizeKey(item).equals(normalized));
    }

    public boolean suportaValorCausa(BigDecimal valorCausa) {
        if (valorCausa == null) {
            return true;
        }
        if (valorCausaMinimo != null && valorCausa.compareTo(valorCausaMinimo) < 0) {
            return false;
        }
        if (valorCausaMaximo != null && valorCausa.compareTo(valorCausaMaximo) > 0) {
            return false;
        }
        return true;
    }

    public double disponibilidadeOperacional() {
        if (!aceitaDistribuicao) {
            return 0.0d;
        }
        if (statusOperacional != StatusOperacionalUnidadeJudiciaria.ATIVA) {
            return 0.0d;
        }
        BigDecimal capacidadeLiquida = BigDecimal.valueOf(capacidadeMaxima)
                .multiply(BigDecimal.valueOf(100 - capacidadeReservaPercentual))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (capacidadeLiquida.compareTo(BigDecimal.ONE) < 0) {
            capacidadeLiquida = BigDecimal.ONE;
        }
        BigDecimal ocupacao = BigDecimal.valueOf(processosAtivos)
                .divide(capacidadeLiquida, 4, RoundingMode.HALF_UP);
        BigDecimal score = BigDecimal.ONE.subtract(ocupacao.max(indiceCongestionamento));
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            score = BigDecimal.ONE;
        }
        return score.doubleValue();
    }

    public void registrarDistribuicaoAplicada() {
        this.processosAtivos = this.processosAtivos + 1;
        this.distribuicoesUltimas24h = this.distribuicoesUltimas24h + 1;
        this.ultimaDistribuicaoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public void atualizarCarga(int processosAtivos, BigDecimal indiceCongestionamento) {
        this.processosAtivos = Math.max(0, processosAtivos);
        this.indiceCongestionamento = indiceCongestionamento == null
                ? BigDecimal.ZERO
                : indiceCongestionamento.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Tribunal getTribunal() {
        return tribunal;
    }

    public Comarca getComarca() {
        return comarca;
    }

    public String getUf() {
        return uf;
    }

    public TipoJustica getTipoJustica() {
        return tipoJustica;
    }

    public RamoDireito getRamoDireito() {
        return ramoDireito;
    }

    public TipoVaraDistribuicao getTipoVara() {
        return tipoVara;
    }

    public Set<String> getEspecialidades() {
        return especialidades;
    }

    public Set<String> getClassesTpu() {
        return classesTpu;
    }

    public Set<String> getAssuntosTpu() {
        return assuntosTpu;
    }

    public int getProcessosAtivos() {
        return processosAtivos;
    }

    public void setProcessosAtivos(int processosAtivos) {
        this.processosAtivos = processosAtivos;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public void setCapacidadeMaxima(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public int getCapacidadeReservaPercentual() {
        return capacidadeReservaPercentual;
    }

    public void setCapacidadeReservaPercentual(int capacidadeReservaPercentual) {
        this.capacidadeReservaPercentual = capacidadeReservaPercentual;
    }

    public BigDecimal getIndiceCongestionamento() {
        return indiceCongestionamento;
    }

    public void setIndiceCongestionamento(BigDecimal indiceCongestionamento) {
        this.indiceCongestionamento = indiceCongestionamento;
    }

    public boolean isAceitaDistribuicao() {
        return aceitaDistribuicao;
    }

    public void setAceitaDistribuicao(boolean aceitaDistribuicao) {
        this.aceitaDistribuicao = aceitaDistribuicao;
    }

    public boolean isPermiteJuizadoEspecial() {
        return permiteJuizadoEspecial;
    }

    public void setPermiteJuizadoEspecial(boolean permiteJuizadoEspecial) {
        this.permiteJuizadoEspecial = permiteJuizadoEspecial;
    }

    public boolean isPermiteUrgencia() {
        return permiteUrgencia;
    }

    public void setPermiteUrgencia(boolean permiteUrgencia) {
        this.permiteUrgencia = permiteUrgencia;
    }

    public boolean isPermiteDistribuicaoAutomatica() {
        return permiteDistribuicaoAutomatica;
    }

    public void setPermiteDistribuicaoAutomatica(boolean permiteDistribuicaoAutomatica) {
        this.permiteDistribuicaoAutomatica = permiteDistribuicaoAutomatica;
    }

    public boolean isSomenteDigital() {
        return somenteDigital;
    }

    public void setSomenteDigital(boolean somenteDigital) {
        this.somenteDigital = somenteDigital;
    }

    public StatusOperacionalUnidadeJudiciaria getStatusOperacional() {
        return statusOperacional;
    }

    public void setStatusOperacional(StatusOperacionalUnidadeJudiciaria statusOperacional) {
        this.statusOperacional = statusOperacional;
    }

    public ModoOperacaoUnidadeJudiciaria getModoOperacao() {
        return modoOperacao;
    }

    public void setModoOperacao(ModoOperacaoUnidadeJudiciaria modoOperacao) {
        this.modoOperacao = modoOperacao;
    }

    public String getEnderecoFisico() {
        return enderecoFisico;
    }

    public void setEnderecoFisico(String enderecoFisico) {
        this.enderecoFisico = enderecoFisico;
    }

    public String getEnderecoDigital() {
        return enderecoDigital;
    }

    public void setEnderecoDigital(String enderecoDigital) {
        this.enderecoDigital = enderecoDigital;
    }

    public BigDecimal getValorCausaMinimo() {
        return valorCausaMinimo;
    }

    public void setValorCausaMinimo(BigDecimal valorCausaMinimo) {
        this.valorCausaMinimo = valorCausaMinimo;
    }

    public BigDecimal getValorCausaMaximo() {
        return valorCausaMaximo;
    }

    public void setValorCausaMaximo(BigDecimal valorCausaMaximo) {
        this.valorCausaMaximo = valorCausaMaximo;
    }

    public int getPrioridadeEstrategica() {
        return prioridadeEstrategica;
    }

    public void setPrioridadeEstrategica(int prioridadeEstrategica) {
        this.prioridadeEstrategica = prioridadeEstrategica;
    }

    public int getDistribuicoesUltimas24h() {
        return distribuicoesUltimas24h;
    }

    public void setDistribuicoesUltimas24h(int distribuicoesUltimas24h) {
        this.distribuicoesUltimas24h = distribuicoesUltimas24h;
    }

    public Instant getUltimaDistribuicaoEm() {
        return ultimaDistribuicaoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    private static Set<String> sanitizeSet(Set<String> items, boolean strictUpper) {
        Set<String> sanitized = new LinkedHashSet<>();
        if (items == null) {
            return sanitized;
        }
        for (String item : items) {
            String value = strictUpper ? normalizeUpper(item) : normalizeText(item);
            if (value != null && !value.isBlank()) {
                sanitized.add(value);
            }
        }
        return sanitized;
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeKey(String value) {
        String normalized = normalizeUpper(value);
        if (normalized == null) {
            return null;
        }
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return switch (normalized) {
            case "CIVEL" -> "CIVIL";
            case "TRIBUTARIA" -> "TRIBUTARIO";
            case "PREVIDENCIARIA" -> "PREVIDENCIARIO";
            case "ACAO_DE_COBRANCA", "COBRANCA", "PETICAO_INICIAL" -> "PROCEDIMENTO_COMUM_CIVEL";
            default -> normalized;
        };
    }
}
