package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.model.entity.enums.StatusInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_instituicao")
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private TipoInstituicao tipo;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 30)
    private String sigla;

    @Column(name = "identificador_oficial", length = 20)
    private String identificadorOficial;

    @Column(length = 20)
    private String esfera;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusInstituicao status = StatusInstituicao.ATIVA;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public TipoInstituicao getTipo() { return tipo; }
    public void setTipo(TipoInstituicao tipo) { this.tipo = tipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public String getIdentificadorOficial() { return identificadorOficial; }
    public void setIdentificadorOficial(String identificadorOficial) { this.identificadorOficial = identificadorOficial; }
    public String getEsfera() { return esfera; }
    public void setEsfera(String esfera) { this.esfera = esfera; }
    public StatusInstituicao getStatus() { return status; }
    public void setStatus(StatusInstituicao status) { this.status = status; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }

    @jakarta.persistence.PrePersist
    void inicializar() {
        if (uuid == null) uuid = com.tcc.pjb.backend.core.id.PjbUuidV7Generator.generate();
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
