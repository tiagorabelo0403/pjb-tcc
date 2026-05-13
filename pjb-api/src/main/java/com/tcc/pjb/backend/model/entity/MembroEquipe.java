package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "membros_equipe",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_equipe",
                        columnNames = {"usuario_id", "equipe_id"}
                )
        },
        indexes = {
                @Index(name = "idx_membro_equipe_usuario", columnList = "usuario_id"),
                @Index(name = "idx_membro_equipe_equipe", columnList = "equipe_id")
        }
)
public class MembroEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membro_usuario")
    )
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "equipe_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_membro_equipe")
    )
    private Equipe equipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PapelEquipe papel;

    @Size(max = 100)
    @Column(length = 100)
    private String cargo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataEntrada;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Equipe getEquipe() { return equipe; }
    public void setEquipe(Equipe equipe) { this.equipe = equipe; }

    public PapelEquipe getPapel() { return papel; }
    public void setPapel(PapelEquipe papel) { this.papel = papel; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembroEquipe)) return false;
        MembroEquipe that = (MembroEquipe) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MembroEquipe{" +
                "id=" + id +
                ", usuario=" + (usuario != null ? usuario.getId() : null) +
                ", equipe=" + (equipe != null ? equipe.getId() : null) +
                ", papel=" + papel +
                ", cargo='" + cargo + '\'' +
                ", ativo=" + ativo +
                ", dataAdmissao=" + dataAdmissao +
                '}';
    }
}
