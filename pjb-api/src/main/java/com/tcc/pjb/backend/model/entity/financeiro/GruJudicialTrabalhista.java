package com.tcc.pjb.backend.model.entity.financeiro;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_gru_judicial_trabalhista", indexes = {
        @Index(name = "idx_gru_trab_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GruJudicialTrabalhista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo", nullable = false, length = 64)
    private String tipo;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "indice_atualizacao", length = 32)
    private String indiceAtualizacao;

    @Column(name = "nosso_numero", length = 64)
    private String nossoNumero;

    @Column(name = "linha_digitavel", length = 120)
    private String linhaDigitavel;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "vencimento")
    private LocalDate vencimento;

    @Column(name = "pago_em")
    private Instant pagoEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "tribunal_trt", length = 16)
    private String tribunalTrt;
}
