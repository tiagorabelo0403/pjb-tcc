package com.tcc.pjb.backend.model.entity.processo;

import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
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
@Table(name = "tb_processo_estado_log")
@Getter
public class ProcessoEstadoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private StatusProcesso estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false)
    private StatusProcesso estadoNovo;

    @Column(name = "operador_id", nullable = false)
    private Long operadorId;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProcessoEstadoLog() {
    }

    public ProcessoEstadoLog(Long processoId, StatusProcesso estadoAnterior,
                              StatusProcesso estadoNovo, Long operadorId, String motivo) {
        this.processoId = processoId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.operadorId = operadorId;
        this.motivo = motivo;
        this.createdAt = Instant.now();
    }
}
