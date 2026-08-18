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
@Table(name = "tb_impedimento_ministro")
@Getter
public class ImpedimentoMinistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "ministro_id", nullable = false)
    private Long ministroId;

    @Column(name = "tipo_impedimento", nullable = false)
    private String tipoImpedimento;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "declarado_em", nullable = false)
    private Instant declaradoEm;

    protected ImpedimentoMinistro() {
    }

    public ImpedimentoMinistro(Long processoId, Long ministroId,
                                String tipoImpedimento, String motivo) {
        this.processoId = processoId;
        this.ministroId = ministroId;
        this.tipoImpedimento = tipoImpedimento;
        this.motivo = motivo;
        this.declaradoEm = Instant.now();
    }
}
