package com.tcc.pjb.backend.model.entity.painel;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
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
        name = "tb_painel_serie_temporal_diaria",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_painel_serie_chave", columnNames = "chave_serie")
        },
        indexes = {
                @Index(name = "idx_painel_serie_data", columnList = "data_referencia"),
                @Index(name = "idx_painel_serie_tribunal", columnList = "tribunal_codigo"),
                @Index(name = "idx_painel_serie_ramo", columnList = "ramo_direito")
        }
)
public class PainelSerieTemporalDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_serie", nullable = false, length = 220)
    private String chaveSerie;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "tribunal_codigo", length = 20)
    private String tribunalCodigo;

    @Column(name = "ramo_direito", length = 80)
    private String ramoDireito;

    @Column(name = "ajuizados", nullable = false)
    private long ajuizados;

    @Column(name = "sentenciados", nullable = false)
    private long sentenciados;

    @Column(name = "arquivados", nullable = false)
    private long arquivados;

    @Column(name = "acordos", nullable = false)
    private long acordos;

    @Column(name = "tempo_medio_novos", precision = 12, scale = 2, nullable = false)
    private BigDecimal tempoMedioNovos;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PainelSerieTemporalDiaria() {
    }

    public PainelSerieTemporalDiaria(String chaveSerie, LocalDate dataReferencia, String tribunalCodigo, String ramoDireito) {
        this.chaveSerie = chaveSerie;
        this.dataReferencia = dataReferencia;
        this.tribunalCodigo = tribunalCodigo;
        this.ramoDireito = ramoDireito;
        this.tempoMedioNovos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.chaveSerie = normalizeText(chaveSerie);
        this.tribunalCodigo = normalizeUpper(tribunalCodigo);
        this.ramoDireito = normalizeUpper(ramoDireito);
        if (this.ajuizados < 0L) {
            this.ajuizados = 0L;
        }
        if (this.sentenciados < 0L) {
            this.sentenciados = 0L;
        }
        if (this.arquivados < 0L) {
            this.arquivados = 0L;
        }
        if (this.acordos < 0L) {
            this.acordos = 0L;
        }
        if (this.tempoMedioNovos == null) {
            this.tempoMedioNovos = BigDecimal.ZERO;
        }
        this.tempoMedioNovos = this.tempoMedioNovos.setScale(2, RoundingMode.HALF_UP);
        if (this.tempoMedioNovos.signum() < 0) {
            this.tempoMedioNovos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
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

    public Long getId() {
        return id;
    }

    public String getChaveSerie() {
        return chaveSerie;
    }

    public void setChaveSerie(String chaveSerie) {
        this.chaveSerie = chaveSerie;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public String getTribunalCodigo() {
        return tribunalCodigo;
    }

    public void setTribunalCodigo(String tribunalCodigo) {
        this.tribunalCodigo = tribunalCodigo;
    }

    public String getRamoDireito() {
        return ramoDireito;
    }

    public void setRamoDireito(String ramoDireito) {
        this.ramoDireito = ramoDireito;
    }

    public long getAjuizados() {
        return ajuizados;
    }

    public void setAjuizados(long ajuizados) {
        this.ajuizados = ajuizados;
    }

    public long getSentenciados() {
        return sentenciados;
    }

    public void setSentenciados(long sentenciados) {
        this.sentenciados = sentenciados;
    }

    public long getArquivados() {
        return arquivados;
    }

    public void setArquivados(long arquivados) {
        this.arquivados = arquivados;
    }

    public long getAcordos() {
        return acordos;
    }

    public void setAcordos(long acordos) {
        this.acordos = acordos;
    }

    public BigDecimal getTempoMedioNovos() {
        return tempoMedioNovos;
    }

    public void setTempoMedioNovos(BigDecimal tempoMedioNovos) {
        this.tempoMedioNovos = tempoMedioNovos;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
