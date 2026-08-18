package com.tcc.pjb.backend.model.entity.servidor;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Entity
@Table(name = "tb_funcao_servidor_judiciario")
@Getter
public class FuncaoServidorJudiciarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "funcao", nullable = false)
    private FuncaoServidorJudiciario funcao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "designado_por_id")
    private Long designadoPorId;

    @Column(name = "portaria_referencia")
    private String portariaReferencia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FuncaoServidorJudiciarioEntity() {
    }

    public FuncaoServidorJudiciarioEntity(Long usuarioId, Long unidadeId,
                                          FuncaoServidorJudiciario funcao,
                                          LocalDate dataInicio, Long designadoPorId,
                                          String portariaReferencia) {
        this.usuarioId = usuarioId;
        this.unidadeId = unidadeId;
        this.funcao = funcao;
        this.dataInicio = dataInicio;
        this.designadoPorId = designadoPorId;
        this.portariaReferencia = portariaReferencia;
        this.ativo = true;
        this.createdAt = Instant.now();
    }

    public void encerrar(LocalDate dataFim) {
        this.dataFim = dataFim;
        this.ativo = false;
    }

    public boolean isVigente(LocalDate data) {
        if (!ativo) {
            return false;
        }
        if (data.isBefore(dataInicio)) {
            return false;
        }
        return dataFim == null || !data.isAfter(dataFim);
    }
}
