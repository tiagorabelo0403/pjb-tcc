package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_comarca",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comarca_municipio", columnNames = "municipio_sede_ibge")
        },
        indexes = {
                @Index(name = "idx_comarca_ibge", columnList = "municipio_sede_ibge"),
                @Index(name = "idx_comarca_nome_uf", columnList = "nome, uf")
        }
)
public class Comarca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "municipio_sede_ibge", nullable = false, length = 7)
    private String municipioSedeIbge;

    @Column(name = "nome_foro", length = 200)
    private String nomeForo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tribunal_id")
    private Tribunal tribunal;

    protected Comarca() {
    }

    public Comarca(String nome, String uf, String municipioSedeIbge, String nomeForo) {
        this(nome, uf, municipioSedeIbge, nomeForo, null);
    }

    public Comarca(String nome, String uf, String municipioSedeIbge, String nomeForo, Tribunal tribunal) {
        this.nome = Objects.requireNonNull(nome, "nome");
        this.uf = Objects.requireNonNull(uf, "uf");
        this.municipioSedeIbge = Objects.requireNonNull(municipioSedeIbge, "municipioSedeIbge");
        this.nomeForo = nomeForo;
        this.tribunal = tribunal;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getUf() {
        return uf;
    }

    public String getMunicipioSedeIbge() {
        return municipioSedeIbge;
    }

    public String getNomeForo() {
        return nomeForo;
    }

    public Tribunal getTribunal() {
        return tribunal;
    }
}
