package com.tcc.pjb.backend.model.entity.competencia;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_tribunal",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tribunal_sigla", columnNames = "sigla")
        }
)
public class Tribunal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sigla", nullable = false, length = 20)
    private String sigla;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_justica", nullable = false, length = 30)
    private TipoJustica tipoJustica;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau", nullable = false, length = 20)
    private GrauJurisdicao grau;

    @Column(name = "uf_sede", length = 2)
    private String ufSede;

    protected Tribunal() {
    }

    public Tribunal(String sigla, String nome, TipoJustica tipoJustica, GrauJurisdicao grau, String ufSede) {
        this.sigla = Objects.requireNonNull(sigla, "sigla");
        this.nome = Objects.requireNonNull(nome, "nome");
        this.tipoJustica = Objects.requireNonNull(tipoJustica, "tipoJustica");
        this.grau = Objects.requireNonNull(grau, "grau");
        this.ufSede = ufSede;
    }

    public Long getId() {
        return id;
    }

    public String getSigla() {
        return sigla;
    }

    public String getNome() {
        return nome;
    }

    public TipoJustica getTipoJustica() {
        return tipoJustica;
    }

    public GrauJurisdicao getGrau() {
        return grau;
    }

    public String getUfSede() {
        return ufSede;
    }
}
