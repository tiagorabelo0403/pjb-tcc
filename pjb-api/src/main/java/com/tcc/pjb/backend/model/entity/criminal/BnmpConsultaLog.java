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
@Table(name = "pjb_bnmp_consulta_log", indexes = {
        @Index(name = "idx_bnmp_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BnmpConsultaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "cpf_consultado", length = 11)
    private String cpfConsultado;

    @Column(name = "mandado_ativo")
    private Boolean mandadoAtivo;

    @Column(name = "numero_mandado", length = 64)
    private String numeroMandado;

    @Column(name = "consultado_em", nullable = false)
    private Instant consultadoEm;

    @Column(name = "operador_id")
    private Long operadorId;
}
