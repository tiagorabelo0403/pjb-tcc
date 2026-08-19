package com.tcc.pjb.backend.model.entity.servidor;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.StatusFuncaoServidorSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "tb_funcao_servidor_solicitacao")
@Getter
public class FuncaoServidorSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "funcao", nullable = false)
    private FuncaoServidorJudiciario funcao;

    @Column(name = "motivo")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusFuncaoServidorSolicitacao status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decidido_por_id")
    private Long decididoPorId;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    protected FuncaoServidorSolicitacao() {
    }

    public FuncaoServidorSolicitacao(Long solicitanteId, Long unidadeId,
                                      FuncaoServidorJudiciario funcao, String motivo) {
        this.solicitanteId = solicitanteId;
        this.unidadeId = unidadeId;
        this.funcao = funcao;
        this.motivo = motivo;
        this.status = StatusFuncaoServidorSolicitacao.PENDENTE;
        this.requestedAt = Instant.now();
    }

    public void aprovar(Long decisorId) {
        exigirPendente();
        this.status = StatusFuncaoServidorSolicitacao.APROVADA;
        this.decididoPorId = decisorId;
        this.decididoEm = Instant.now();
    }

    public void rejeitar(Long decisorId, String motivoRejeicao) {
        exigirPendente();
        this.status = StatusFuncaoServidorSolicitacao.REJEITADA;
        this.decididoPorId = decisorId;
        this.decididoEm = Instant.now();
        this.motivoRejeicao = motivoRejeicao;
    }

    private void exigirPendente() {
        if (this.status != StatusFuncaoServidorSolicitacao.PENDENTE) {
            throw new IllegalStateException("Solicitação já foi decidida: " + this.status);
        }
    }
}
