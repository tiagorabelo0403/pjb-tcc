package com.tcc.pjb.backend.model.entity.painel;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
        name = "tb_painel_materia_metrica",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_painel_materia_chave", columnNames = "chave_metrica")
        },
        indexes = {
                @Index(name = "idx_painel_materia_total", columnList = "total_processos"),
                @Index(name = "idx_painel_materia_ramo", columnList = "ramo_direito"),
                @Index(name = "idx_painel_materia_tribunal", columnList = "tribunal_mais_ativo")
        }
)
public class PainelMateriaMetrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_metrica", nullable = false, length = 240)
    private String chaveMetrica;

    @Column(name = "ramo_direito", nullable = false, length = 80)
    private String ramoDireito;

    @Column(name = "assunto_tpu", nullable = false, length = 180)
    private String assuntoTpu;

    @Column(name = "total_processos", nullable = false)
    private long totalProcessos;

    @Column(name = "tempo_medio_resolucao_dias", precision = 12, scale = 2, nullable = false)
    private BigDecimal tempoMedioResolucaoDias;

    @Column(name = "taxa_prescricao_pct", precision = 12, scale = 4, nullable = false)
    private BigDecimal taxaPrescricaoPct;

    @Column(name = "taxa_conciliacao_pct", precision = 12, scale = 4, nullable = false)
    private BigDecimal taxaConciliacaoPct;

    @Column(name = "tribunal_mais_ativo", length = 20)
    private String tribunalMaisAtivo;

    @Column(name = "distribuicao_tribunais_json", columnDefinition = "TEXT")
    private String distribuicaoTribunaisJson;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PainelMateriaMetrica() {
    }

    public PainelMateriaMetrica(String chaveMetrica, String ramoDireito, String assuntoTpu) {
        this.chaveMetrica = chaveMetrica;
        this.ramoDireito = ramoDireito;
        this.assuntoTpu = assuntoTpu;
        this.tempoMedioResolucaoDias = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.taxaPrescricaoPct = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        this.taxaConciliacaoPct = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.chaveMetrica = normalizeText(chaveMetrica);
        this.ramoDireito = normalizeUpper(ramoDireito);
        this.assuntoTpu = normalizeText(assuntoTpu);
        this.tribunalMaisAtivo = normalizeUpper(tribunalMaisAtivo);
        if (this.totalProcessos < 0L) {
            this.totalProcessos = 0L;
        }
        this.tempoMedioResolucaoDias = normalized(this.tempoMedioResolucaoDias, 2);
        this.taxaPrescricaoPct = clampPct(this.taxaPrescricaoPct);
        this.taxaConciliacaoPct = clampPct(this.taxaConciliacaoPct);
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

    private static BigDecimal normalized(BigDecimal value, int scale) {
        BigDecimal base = value == null ? BigDecimal.ZERO : value;
        BigDecimal normalized = base.setScale(scale, RoundingMode.HALF_UP);
        return normalized.signum() < 0 ? BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP) : normalized;
    }

    private static BigDecimal clampPct(BigDecimal value) {
        BigDecimal normalized = normalized(value, 4);
        if (normalized.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100.0000");
        }
        return normalized;
    }

    public Long getId() {
        return id;
    }

    public String getChaveMetrica() {
        return chaveMetrica;
    }

    public void setChaveMetrica(String chaveMetrica) {
        this.chaveMetrica = chaveMetrica;
    }

    public String getRamoDireito() {
        return ramoDireito;
    }

    public void setRamoDireito(String ramoDireito) {
        this.ramoDireito = ramoDireito;
    }

    public String getAssuntoTpu() {
        return assuntoTpu;
    }

    public void setAssuntoTpu(String assuntoTpu) {
        this.assuntoTpu = assuntoTpu;
    }

    public long getTotalProcessos() {
        return totalProcessos;
    }

    public void setTotalProcessos(long totalProcessos) {
        this.totalProcessos = totalProcessos;
    }

    public BigDecimal getTempoMedioResolucaoDias() {
        return tempoMedioResolucaoDias;
    }

    public void setTempoMedioResolucaoDias(BigDecimal tempoMedioResolucaoDias) {
        this.tempoMedioResolucaoDias = tempoMedioResolucaoDias;
    }

    public BigDecimal getTaxaPrescricaoPct() {
        return taxaPrescricaoPct;
    }

    public void setTaxaPrescricaoPct(BigDecimal taxaPrescricaoPct) {
        this.taxaPrescricaoPct = taxaPrescricaoPct;
    }

    public BigDecimal getTaxaConciliacaoPct() {
        return taxaConciliacaoPct;
    }

    public void setTaxaConciliacaoPct(BigDecimal taxaConciliacaoPct) {
        this.taxaConciliacaoPct = taxaConciliacaoPct;
    }

    public String getTribunalMaisAtivo() {
        return tribunalMaisAtivo;
    }

    public void setTribunalMaisAtivo(String tribunalMaisAtivo) {
        this.tribunalMaisAtivo = tribunalMaisAtivo;
    }

    public String getDistribuicaoTribunaisJson() {
        return distribuicaoTribunaisJson;
    }

    public void setDistribuicaoTribunaisJson(String distribuicaoTribunaisJson) {
        this.distribuicaoTribunaisJson = distribuicaoTribunaisJson;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
