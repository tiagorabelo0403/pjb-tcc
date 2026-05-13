package com.tcc.pjb.backend.model.entity.painel;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_painel_alerta_prazo",
        indexes = {
                @Index(name = "idx_painel_alerta_nupn", columnList = "nupn"),
                @Index(name = "idx_painel_alerta_tribunal", columnList = "tribunal_codigo, ativo"),
                @Index(name = "idx_painel_alerta_nivel", columnList = "nivel, ativo")
        }
)
public class PainelAlertaPrazo {

    public PainelAlertaPrazo() {
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nupn", nullable = false, length = 80)
    private String nupn;

    @Column(name = "tribunal_codigo", nullable = false, length = 20)
    private String tribunalCodigo;

    @Column(name = "ramo_direito", nullable = false, length = 80)
    private String ramoDireito;

    @Column(name = "dias_em_andamento", nullable = false)
    private long diasEmAndamento;

    @Column(name = "prazo_razoavel_dias", nullable = false)
    private long prazoRazoavelDias;

    @Column(name = "dias_excedidos", nullable = false)
    private long diasExcedidos;

    @Column(name = "fase", length = 80)
    private String fase;

    @Column(name = "nivel", nullable = false, length = 20)
    private String nivel;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.nupn = normalizeText(nupn);
        this.tribunalCodigo = normalizeUpper(tribunalCodigo);
        this.ramoDireito = normalizeUpper(ramoDireito);
        this.fase = normalizeText(fase);
        this.nivel = normalizeUpper(nivel);
        if (this.diasEmAndamento < 0L) {
            this.diasEmAndamento = 0L;
        }
        if (this.prazoRazoavelDias < 0L) {
            this.prazoRazoavelDias = 0L;
        }
        if (this.diasExcedidos < 0L) {
            this.diasExcedidos = 0L;
        }
        Instant now = Instant.now();
        if (this.criadoEm == null) {
            this.criadoEm = now;
        }
        this.atualizadoEm = now;
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNupn() {
        return nupn;
    }

    public void setNupn(String nupn) {
        this.nupn = nupn;
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

    public long getDiasEmAndamento() {
        return diasEmAndamento;
    }

    public void setDiasEmAndamento(long diasEmAndamento) {
        this.diasEmAndamento = diasEmAndamento;
    }

    public long getPrazoRazoavelDias() {
        return prazoRazoavelDias;
    }

    public void setPrazoRazoavelDias(long prazoRazoavelDias) {
        this.prazoRazoavelDias = prazoRazoavelDias;
    }

    public long getDiasExcedidos() {
        return diasExcedidos;
    }

    public void setDiasExcedidos(long diasExcedidos) {
        this.diasExcedidos = diasExcedidos;
    }

    public String getFase() {
        return fase;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
