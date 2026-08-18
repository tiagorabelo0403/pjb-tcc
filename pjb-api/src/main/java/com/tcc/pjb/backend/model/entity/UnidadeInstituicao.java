package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_unidade_institucional")
public class UnidadeInstituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UnidadeInstituicao parent;

    @Column(nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private TipoUnidadeInstitucional tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_unidade", nullable = false, length = 20)
    private StatusUnidadeInstitucional statusUnidade = StatusUnidadeInstitucional.ATIVA;

    @Column(length = 120)
    private String comarca;

    @Column(length = 2)
    private String uf;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public Instituicao getInstituicao() { return instituicao; }
    public void setInstituicao(Instituicao instituicao) { this.instituicao = instituicao; }
    public UnidadeInstituicao getParent() { return parent; }
    public void setParent(UnidadeInstituicao parent) { this.parent = parent; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoUnidadeInstitucional getTipo() { return tipo; }
    public void setTipo(TipoUnidadeInstitucional tipo) { this.tipo = tipo; }
    public StatusUnidadeInstitucional getStatusUnidade() { return statusUnidade; }
    public void setStatusUnidade(StatusUnidadeInstitucional statusUnidade) { this.statusUnidade = statusUnidade; }
    public String getComarca() { return comarca; }
    public void setComarca(String comarca) { this.comarca = comarca; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }

    @PrePersist
    void inicializar() {
        if (uuid == null) uuid = com.tcc.pjb.backend.core.id.PjbUuidV7Generator.generate();
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
        if (statusUnidade == null) statusUnidade = StatusUnidadeInstitucional.ATIVA;
    }
}
