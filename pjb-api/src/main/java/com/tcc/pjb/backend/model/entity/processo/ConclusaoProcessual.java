package com.tcc.pjb.backend.model.entity.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "tb_conclusao_processual")
@Getter
public class ConclusaoProcessual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "magistrado_id", nullable = false)
    private Long magistradoId;

    @Column(name = "servidor_id", nullable = false)
    private Long servidorId;

    @Column(name = "tipo_conclusao", nullable = false)
    private String tipoConclusao;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "data_conclusao", nullable = false)
    private Instant dataConclusao;

    @Column(name = "data_limite", nullable = false)
    private Instant dataLimite;

    @Column(name = "data_devolucao")
    private Instant dataDevolucao;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "decisao_id")
    private Long decisaoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConclusaoProcessual() {
    }

    public ConclusaoProcessual(Long processoId, Long magistradoId, Long servidorId,
                                String tipoConclusao, String motivo, Instant dataLimite) {
        this.processoId = processoId;
        this.magistradoId = magistradoId;
        this.servidorId = servidorId;
        this.tipoConclusao = tipoConclusao;
        this.motivo = motivo;
        this.dataLimite = dataLimite;
        this.status = "PENDENTE";
        Instant now = Instant.now();
        this.dataConclusao = now;
        this.createdAt = now;
    }

    public void devolver() {
        this.status = "DEVOLVIDA";
        this.dataDevolucao = Instant.now();
    }

    public void expirar() {
        this.status = "EXPIRADA";
    }

    public boolean isPendente() {
        return "PENDENTE".equals(status);
    }
}
