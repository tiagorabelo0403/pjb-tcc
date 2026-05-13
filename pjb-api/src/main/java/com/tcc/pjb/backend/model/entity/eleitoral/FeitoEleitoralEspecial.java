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
@Table(name = "pjb_feito_eleitoral_especial", indexes = {
        @Index(name = "idx_feito_eleitoral_processo", columnList = "processo_id"),
        @Index(name = "idx_feito_eleitoral_partido", columnList = "partido_sigla,ano_eleitoral")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeitoEleitoralEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo_feito", nullable = false, length = 64)
    private String tipoFeito;

    @Column(name = "numero_candidato", length = 16)
    private String numeroCandidato;

    @Column(name = "partido_sigla", length = 16)
    private String partidoSigla;

    @Column(name = "cargo", length = 64)
    private String cargo;

    @Column(name = "ano_eleitoral")
    private Integer anoEleitoral;

    @Column(name = "status_eleitoral", nullable = false, length = 32)
    private String statusEleitoral;

    @Column(name = "diplomado_em")
    private LocalDate diplomadoEm;

    @Column(name = "extinto_em")
    private Instant extintoEm;

    @Column(name = "motivo_extincao", length = 4000)
    private String motivoExtincao;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
