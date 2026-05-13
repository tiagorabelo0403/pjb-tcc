package com.tcc.pjb.backend.model.entity.painel;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_painel_tribunal_metrica",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_painel_tribunal_codigo", columnNames = "codigo_tribunal")
        },
        indexes = {
                @Index(name = "idx_painel_tribunal_congestionamento", columnList = "indice_congestionamento"),
                @Index(name = "idx_painel_tribunal_classificacao", columnList = "classificacao_desempenho"),
                @Index(name = "idx_painel_tribunal_uf", columnList = "uf")
        }
)
public class PainelTribunalMetrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_tribunal", nullable = false, length = 20)
    private String codigoTribunal;

    @Column(name = "tribunal_nome", nullable = false, length = 180)
    private String tribunalNome;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "processos_ativos", nullable = false)
    private long processosAtivos;

    @Column(name = "ajuizados_hoje", nullable = false)
    private long ajuizadosHoje;

    @Column(name = "ajuizados_semana", nullable = false)
    private long ajuizadosSemana;

    @Column(name = "ajuizados_mes", nullable = false)
    private long ajuizadosMes;

    @Column(name = "sentenciados_mes", nullable = false)
    private long sentenciadosMes;

    @Column(name = "arquivados_mes", nullable = false)
    private long arquivadosMes;

    @Column(name = "acordos_mes", nullable = false)
    private long acordosMes;

    @Column(name = "indice_congestionamento", precision = 10, scale = 6, nullable = false)
    private BigDecimal indiceCongestionamento;

    @Column(name = "tempo_medio_resolucao_dias", precision = 12, scale = 2, nullable = false)
    private BigDecimal tempoMedioResolucaoDias;

    @Column(name = "taxa_conciliacao_pct", precision = 12, scale = 4, nullable = false)
    private BigDecimal taxaConciliacaoPct;

    @Column(name = "processos_com_prazo_excedido", nullable = false)
    private long processosComPrazoExcedido;

    @Column(name = "classificacao_desempenho", nullable = false, length = 20)
    private String classificacaoDesempenho;

    @Column(name = "ultima_ocorrencia_em")
    private Instant ultimaOcorrenciaEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PainelTribunalMetrica() {
    }

    public PainelTribunalMetrica(String codigoTribunal, String tribunalNome, String uf) {
        this.codigoTribunal = codigoTribunal;
        this.tribunalNome = tribunalNome;
        this.uf = uf;
        this.indiceCongestionamento = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        this.tempoMedioResolucaoDias = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.taxaConciliacaoPct = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        this.classificacaoDesempenho = "BOM";
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.codigoTribunal = normalizeUpper(codigoTribunal);
        this.tribunalNome = normalizeText(tribunalNome);
        this.uf = normalizeUpper(uf);
        if (this.indiceCongestionamento == null) {
            this.indiceCongestionamento = BigDecimal.ZERO;
        }
        if (this.tempoMedioResolucaoDias == null) {
            this.tempoMedioResolucaoDias = BigDecimal.ZERO;
        }
        if (this.taxaConciliacaoPct == null) {
            this.taxaConciliacaoPct = BigDecimal.ZERO;
        }
        this.indiceCongestionamento = clamp(this.indiceCongestionamento, 6, BigDecimal.ZERO, BigDecimal.ONE);
        this.tempoMedioResolucaoDias = nonNegative(this.tempoMedioResolucaoDias, 2);
        this.taxaConciliacaoPct = clamp(this.taxaConciliacaoPct, 4, BigDecimal.ZERO, new BigDecimal("100"));
        if (this.processosAtivos < 0L) {
            this.processosAtivos = 0L;
        }
        if (this.ajuizadosHoje < 0L) {
            this.ajuizadosHoje = 0L;
        }
        if (this.ajuizadosSemana < 0L) {
            this.ajuizadosSemana = 0L;
        }
        if (this.ajuizadosMes < 0L) {
            this.ajuizadosMes = 0L;
        }
        if (this.sentenciadosMes < 0L) {
            this.sentenciadosMes = 0L;
        }
        if (this.arquivadosMes < 0L) {
            this.arquivadosMes = 0L;
        }
        if (this.acordosMes < 0L) {
            this.acordosMes = 0L;
        }
        if (this.processosComPrazoExcedido < 0L) {
            this.processosComPrazoExcedido = 0L;
        }
        this.classificacaoDesempenho = normalizeUpper(Objects.requireNonNullElse(this.classificacaoDesempenho, "REGULAR"));
        this.atualizadoEm = Instant.now();
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private static BigDecimal nonNegative(BigDecimal value, int scale) {
        BigDecimal normalized = value.setScale(scale, RoundingMode.HALF_UP);
        return normalized.signum() < 0 ? BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP) : normalized;
    }

    private static BigDecimal clamp(BigDecimal value, int scale, BigDecimal min, BigDecimal max) {
        BigDecimal normalized = value.setScale(scale, RoundingMode.HALF_UP);
        if (normalized.compareTo(min) < 0) {
            return min.setScale(scale, RoundingMode.HALF_UP);
        }
        if (normalized.compareTo(max) > 0) {
            return max.setScale(scale, RoundingMode.HALF_UP);
        }
        return normalized;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoTribunal() {
        return codigoTribunal;
    }

    public void setCodigoTribunal(String codigoTribunal) {
        this.codigoTribunal = codigoTribunal;
    }

    public String getTribunalNome() {
        return tribunalNome;
    }

    public void setTribunalNome(String tribunalNome) {
        this.tribunalNome = tribunalNome;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public long getProcessosAtivos() {
        return processosAtivos;
    }

    public void setProcessosAtivos(long processosAtivos) {
        this.processosAtivos = processosAtivos;
    }

    public long getAjuizadosHoje() {
        return ajuizadosHoje;
    }

    public void setAjuizadosHoje(long ajuizadosHoje) {
        this.ajuizadosHoje = ajuizadosHoje;
    }

    public long getAjuizadosSemana() {
        return ajuizadosSemana;
    }

    public void setAjuizadosSemana(long ajuizadosSemana) {
        this.ajuizadosSemana = ajuizadosSemana;
    }

    public long getAjuizadosMes() {
        return ajuizadosMes;
    }

    public void setAjuizadosMes(long ajuizadosMes) {
        this.ajuizadosMes = ajuizadosMes;
    }

    public long getSentenciadosMes() {
        return sentenciadosMes;
    }

    public void setSentenciadosMes(long sentenciadosMes) {
        this.sentenciadosMes = sentenciadosMes;
    }

    public long getArquivadosMes() {
        return arquivadosMes;
    }

    public void setArquivadosMes(long arquivadosMes) {
        this.arquivadosMes = arquivadosMes;
    }

    public long getAcordosMes() {
        return acordosMes;
    }

    public void setAcordosMes(long acordosMes) {
        this.acordosMes = acordosMes;
    }

    public BigDecimal getIndiceCongestionamento() {
        return indiceCongestionamento;
    }

    public void setIndiceCongestionamento(BigDecimal indiceCongestionamento) {
        this.indiceCongestionamento = indiceCongestionamento;
    }

    public BigDecimal getTempoMedioResolucaoDias() {
        return tempoMedioResolucaoDias;
    }

    public void setTempoMedioResolucaoDias(BigDecimal tempoMedioResolucaoDias) {
        this.tempoMedioResolucaoDias = tempoMedioResolucaoDias;
    }

    public BigDecimal getTaxaConciliacaoPct() {
        return taxaConciliacaoPct;
    }

    public void setTaxaConciliacaoPct(BigDecimal taxaConciliacaoPct) {
        this.taxaConciliacaoPct = taxaConciliacaoPct;
    }

    public long getProcessosComPrazoExcedido() {
        return processosComPrazoExcedido;
    }

    public void setProcessosComPrazoExcedido(long processosComPrazoExcedido) {
        this.processosComPrazoExcedido = processosComPrazoExcedido;
    }

    public String getClassificacaoDesempenho() {
        return classificacaoDesempenho;
    }

    public void setClassificacaoDesempenho(String classificacaoDesempenho) {
        this.classificacaoDesempenho = classificacaoDesempenho;
    }

    public Instant getUltimaOcorrenciaEm() {
        return ultimaOcorrenciaEm;
    }

    public void setUltimaOcorrenciaEm(Instant ultimaOcorrenciaEm) {
        this.ultimaOcorrenciaEm = ultimaOcorrenciaEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
