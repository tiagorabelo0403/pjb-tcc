package com.tcc.pjb.backend.model.entity.eleitoral;

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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_calendario_eleitoral", indexes = {
        @Index(name = "idx_cal_eleitoral_fase", columnList = "data_inicio,data_fim")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarioEleitoral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ano_eleitoral", nullable = false)
    private Integer anoEleitoral;

    @Column(name = "tipo_eleicao", nullable = false, length = 32)
    private String tipoEleicao;

    @Column(name = "fase", nullable = false, length = 64)
    private String fase;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "descricao", length = 4000)
    private String descricao;

    @Column(name = "zona_eleitoral", length = 16)
    private String zonaEleitoral;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
