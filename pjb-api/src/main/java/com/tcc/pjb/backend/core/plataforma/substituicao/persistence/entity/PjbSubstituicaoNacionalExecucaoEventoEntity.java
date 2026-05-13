package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_pjb_substituicao_execucao_evento")
public class PjbSubstituicaoNacionalExecucaoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execucao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pjb_subst_exec_event_exec"))
    private PjbSubstituicaoNacionalExecucaoEntity execucao;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "severidade", nullable = false, length = 16)
    private String severidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase", nullable = false, length = 32)
    private PjbSubstituicaoExecucaoFase fase;

    @Column(name = "descricao", nullable = false, length = 1000)
    private String descricao;

    @Lob
    @Column(name = "detalhes_json", columnDefinition = "text")
    private String detalhesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PjbSubstituicaoNacionalExecucaoEventoEntity() {
    }

    public PjbSubstituicaoNacionalExecucaoEventoEntity(PjbSubstituicaoNacionalExecucaoEntity execucao,
                                                       String codigo,
                                                       String severidade,
                                                       PjbSubstituicaoExecucaoFase fase,
                                                       String descricao,
                                                       String detalhesJson) {
        this.execucao = Objects.requireNonNull(execucao);
        this.codigo = Objects.requireNonNull(codigo).trim();
        this.severidade = Objects.requireNonNull(severidade).trim();
        this.fase = Objects.requireNonNull(fase);
        this.descricao = Objects.requireNonNull(descricao).trim();
        this.detalhesJson = detalhesJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public PjbSubstituicaoNacionalExecucaoEntity getExecucao() { return execucao; }
    public String getCodigo() { return codigo; }
    public String getSeveridade() { return severidade; }
    public PjbSubstituicaoExecucaoFase getFase() { return fase; }
    public String getDescricao() { return descricao; }
    public String getDetalhesJson() { return detalhesJson; }
    public Instant getCreatedAt() { return createdAt; }
}
