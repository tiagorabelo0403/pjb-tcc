package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.SQLRestriction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;

@Getter
@Setter
@NoArgsConstructor 
@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "orgaos_judiciarios",
        indexes = {
                @Index(name = "idx_orgao_nome", columnList = "nome"),
                @Index(name = "idx_orgao_tipo", columnList = "tipo"),
                @Index(name = "idx_orgao_comarca", columnList = "comarca")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orgao_sigla", columnNames = {"sigla"})
        }
)
@SQLRestriction("ativo = true")
public class OrgaoJudiciario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Nome do órgão é obrigatório")
    @Size(min = 5, max = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 30)
    @NotBlank(message = "Sigla do órgão é obrigatória")
    @Size(min = 2, max = 30)
    private String sigla;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Tipo de órgão é obrigatório")
    private String tipo;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Esfera é obrigatória")
    private String esfera;
    @Column(length = 100)
    private String comarca;

    @Column(length = 2)
    @Size(min = 2, max = 2)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comarca_id")
    private Comarca comarcaEntidade;

    @Column(name = "adapter_bean_name", length = 100)
    private String adapterBeanName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "orgao_competencia_jurisdicao",
            joinColumns = @JoinColumn(name = "orgao_id"),
            inverseJoinColumns = @JoinColumn(name = "jurisdicao_id")
    )
    private Set<Jurisdicao> competencias = new HashSet<>();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    
    public OrgaoJudiciario(String nome, String sigla, String tipo, String esfera, String comarca, String estado) {
        this.nome = nome;
        this.sigla = sigla;
        this.tipo = tipo;
        this.esfera = esfera;
        this.comarca = comarca;
        this.estado = estado;
    }

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
    }

    
    @Override
    public String toString() {
        return String.format("OrgaoJudiciario{sigla='%s', nome='%s'}", sigla, nome);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrgaoJudiciario)) return false;
        OrgaoJudiciario that = (OrgaoJudiciario) o;
        return sigla != null && sigla.equals(that.sigla);
    }

    @Override
    public int hashCode() {
        return sigla != null ? sigla.hashCode() : 0;
    }
}