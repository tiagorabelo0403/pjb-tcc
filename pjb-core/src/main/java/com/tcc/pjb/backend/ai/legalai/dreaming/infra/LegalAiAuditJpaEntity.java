package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_ai_audit_log")
public class LegalAiAuditJpaEntity {

    @Id
    private UUID id;

    @Column(name = "acao", nullable = false, length = 60)
    private String acao;

    @Column(name = "entidade_tipo", length = 100)
    private String entidadeTipo;

    @Column(name = "entidade_id", length = 255)
    private String entidadeId;

    @Column(name = "ator_id", length = 255)
    private String atorId;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "detalhes", columnDefinition = "JSONB")
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getEntidadeTipo() { return entidadeTipo; }
    public void setEntidadeTipo(String entidadeTipo) { this.entidadeTipo = entidadeTipo; }

    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }

    public String getAtorId() { return atorId; }
    public void setAtorId(String atorId) { this.atorId = atorId; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
