package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_store_ref")
public class MemoryStoreJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "anthropic_store_id", unique = true, nullable = false, length = 255)
    private String anthropicStoreId;

    @Column(name = "modulo_origem", length = 100)
    private String moduloOrigem;

    @Column(name = "sigilo_nivel", length = 50)
    private String sigiloNivel;

    @Column(name = "access_type", length = 50)
    private String accessType;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "arquivado_em")
    private Instant arquivadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAnthropicStoreId() { return anthropicStoreId; }
    public void setAnthropicStoreId(String anthropicStoreId) { this.anthropicStoreId = anthropicStoreId; }
    public String getModuloOrigem() { return moduloOrigem; }
    public void setModuloOrigem(String moduloOrigem) { this.moduloOrigem = moduloOrigem; }
    public String getSigiloNivel() { return sigiloNivel; }
    public void setSigiloNivel(String sigiloNivel) { this.sigiloNivel = sigiloNivel; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getArquivadoEm() { return arquivadoEm; }
    public void setArquivadoEm(Instant arquivadoEm) { this.arquivadoEm = arquivadoEm; }
}
