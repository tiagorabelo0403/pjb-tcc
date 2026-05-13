package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dream")
public class DreamJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "anthropic_dream_id", unique = true, length = 255)
    private String anthropicDreamId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "input_store_id", columnDefinition = "uuid")
    private UUID inputStoreId;

    @Column(name = "output_store_id", columnDefinition = "uuid")
    private UUID outputStoreId;

    @Column(name = "session_ids_json")
    private String sessionIdsJson;

    @Column(name = "instrucoes", length = 4096)
    private String instrucoes;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "erro")
    private String erro;

    @Column(name = "anthropic_output_store_id", length = 255)
    private String anthropicOutputStoreId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAnthropicDreamId() { return anthropicDreamId; }
    public void setAnthropicDreamId(String anthropicDreamId) { this.anthropicDreamId = anthropicDreamId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getInputStoreId() { return inputStoreId; }
    public void setInputStoreId(UUID inputStoreId) { this.inputStoreId = inputStoreId; }
    public UUID getOutputStoreId() { return outputStoreId; }
    public void setOutputStoreId(UUID outputStoreId) { this.outputStoreId = outputStoreId; }
    public String getSessionIdsJson() { return sessionIdsJson; }
    public void setSessionIdsJson(String sessionIdsJson) { this.sessionIdsJson = sessionIdsJson; }
    public String getInstrucoes() { return instrucoes; }
    public void setInstrucoes(String instrucoes) { this.instrucoes = instrucoes; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getEncerradoEm() { return encerradoEm; }
    public void setEncerradoEm(Instant encerradoEm) { this.encerradoEm = encerradoEm; }
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }
    public String getErro() { return erro; }
    public void setErro(String erro) { this.erro = erro; }
    public String getAnthropicOutputStoreId() { return anthropicOutputStoreId; }
    public void setAnthropicOutputStoreId(String anthropicOutputStoreId) { this.anthropicOutputStoreId = anthropicOutputStoreId; }
}
