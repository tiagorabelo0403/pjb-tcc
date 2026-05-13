package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_identidade_juridica_auditoria")
public class IdentidadeJuridicaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identidade_id", nullable = false)
    private IdentidadeJuridicaNacional identidade;

    @Column(name = "evento", nullable = false, length = 50)
    private String evento;

    @Column(name = "origem", nullable = false, length = 40)
    private String origem;

    @Column(name = "ator", length = 120)
    private String ator;

    @Column(name = "descricao", nullable = false, length = 300)
    private String descricao;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected IdentidadeJuridicaAuditoria() {
    }

    public IdentidadeJuridicaAuditoria(IdentidadeJuridicaNacional identidade,
                                       String evento,
                                       String origem,
                                       String ator,
                                       String descricao,
                                       String payloadHash) {
        this.identidade = Objects.requireNonNull(identidade);
        this.evento = Objects.requireNonNull(evento);
        this.origem = Objects.requireNonNull(origem);
        this.ator = ator;
        this.descricao = Objects.requireNonNull(descricao);
        this.payloadHash = payloadHash;
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

    public String getEvento() {
        return evento;
    }

    public String getOrigem() {
        return origem;
    }

    public String getAtor() {
        return ator;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
