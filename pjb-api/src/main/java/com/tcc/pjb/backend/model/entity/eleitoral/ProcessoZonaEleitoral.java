package com.tcc.pjb.backend.model.entity.eleitoral;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_processo_zona_eleitoral")
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessoZonaEleitoral {

    @Id
    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "zona_eleitoral", length = 16)
    private String zonaEleitoral;

    @Column(name = "municipio", length = 120)
    private String municipio;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cartorio_codigo", length = 16)
    private String cartorioCodigo;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
