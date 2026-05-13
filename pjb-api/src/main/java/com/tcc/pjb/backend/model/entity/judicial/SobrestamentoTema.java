package com.tcc.pjb.backend.model.entity.judicial;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_sobrestamento_tema", indexes = {
        @Index(name = "idx_sobrestamento_tema", columnList = "tema_id"),
        @Index(name = "idx_sobrestamento_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SobrestamentoTema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tema_id", nullable = false)
    private Long temaId;

    @Column(name = "status_anterior", nullable = false, length = 64)
    private String statusAnterior;

    @Column(name = "sobrestado_em", nullable = false)
    private Instant sobrestadoEm;

    @Column(name = "retomado_em")
    private Instant retomadoEm;

    @Column(name = "resultado_aplicado", length = 64)
    private String resultadoAplicado;

    @Column(name = "operador_id")
    private Long operadorId;
}
