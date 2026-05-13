package com.tcc.pjb.backend.model.entity.criminal;

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
@Table(name = "pjb_medida_cautelar", indexes = {
        @Index(name = "idx_medida_ativa", columnList = "proximo_comparecimento")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedidaCautelar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo", nullable = false, length = 64)
    private String tipo;

    @Column(name = "descricao", length = 4000)
    private String descricao;

    @Column(name = "periodicidade_dias")
    private Integer periodicidadeDias;

    @Column(name = "proximo_comparecimento")
    private Instant proximoComparecimento;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
