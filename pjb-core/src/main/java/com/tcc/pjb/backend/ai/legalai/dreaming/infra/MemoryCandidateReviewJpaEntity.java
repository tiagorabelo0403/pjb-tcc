package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_candidate_review")
public class MemoryCandidateReviewJpaEntity {

    @Id
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(name = "modulo_origem", nullable = false, length = 100)
    private String moduloOrigem;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "chave", nullable = false, length = 500)
    private String chave;

    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "motivo_extracao", columnDefinition = "TEXT")
    private String motivoExtracao;

    @Column(name = "sigilo_nivel", nullable = false, length = 50)
    private String sigiloNivel;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "revisado_por", length = 255)
    private String revisadoPor;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "revisado_em")
    private Instant revisadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getModuloOrigem() { return moduloOrigem; }
    public void setModuloOrigem(String moduloOrigem) { this.moduloOrigem = moduloOrigem; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }

    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }

    public String getMotivoExtracao() { return motivoExtracao; }
    public void setMotivoExtracao(String motivoExtracao) { this.motivoExtracao = motivoExtracao; }

    public String getSigiloNivel() { return sigiloNivel; }
    public void setSigiloNivel(String sigiloNivel) { this.sigiloNivel = sigiloNivel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }

    public String getMotivoRejeicao() { return motivoRejeicao; }
    public void setMotivoRejeicao(String motivoRejeicao) { this.motivoRejeicao = motivoRejeicao; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    public Instant getRevisadoEm() { return revisadoEm; }
    public void setRevisadoEm(Instant revisadoEm) { this.revisadoEm = revisadoEm; }
}
