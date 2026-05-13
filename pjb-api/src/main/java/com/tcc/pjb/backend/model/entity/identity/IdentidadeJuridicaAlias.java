package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
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
import jakarta.persistence.UniqueConstraint;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_identidade_juridica_alias",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_identidade_juridica_alias", columnNames = {"identidade_id", "tipo_alias", "valor_normalizado"})
        }
)
public class IdentidadeJuridicaAlias {

    public enum TipoAlias {
        NOME_ALTERNATIVO,
        NOME_SOCIAL,
        NOME_LEGADO,
        OAB_LEGADA,
        DOCUMENTO_LEGADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identidade_id", nullable = false)
    private IdentidadeJuridicaNacional identidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alias", nullable = false, length = 30)
    private TipoAlias tipoAlias;

    @Column(name = "valor", nullable = false, length = 180)
    private String valor;

    @Column(name = "valor_normalizado", nullable = false, length = 180)
    private String valorNormalizado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected IdentidadeJuridicaAlias() {
    }

    public IdentidadeJuridicaAlias(IdentidadeJuridicaNacional identidade, TipoAlias tipoAlias, String valor, String valorNormalizado) {
        this.identidade = Objects.requireNonNull(identidade);
        this.tipoAlias = Objects.requireNonNull(tipoAlias);
        this.valor = Objects.requireNonNull(valor);
        this.valorNormalizado = Objects.requireNonNull(valorNormalizado);
    }

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public IdentidadeJuridicaNacional getIdentidade() {
        return identidade;
    }

    public TipoAlias getTipoAlias() {
        return tipoAlias;
    }

    public String getValor() {
        return valor;
    }

    public String getValorNormalizado() {
        return valorNormalizado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
