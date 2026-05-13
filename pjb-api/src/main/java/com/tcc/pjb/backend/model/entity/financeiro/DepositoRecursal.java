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
@Table(name = "pjb_deposito_recursal", indexes = {
        @Index(name = "idx_deposito_recursal_processo", columnList = "processo_id")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositoRecursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "instancia", nullable = false, length = 16)
    private String instancia;

    @Column(name = "valor_teto", precision = 19, scale = 2)
    private BigDecimal valorTeto;

    @Column(name = "valor_depositado", precision = 19, scale = 2)
    private BigDecimal valorDepositado;

    @Column(name = "data_deposito")
    private LocalDate dataDeposito;

    @Column(name = "comprovante_hash", length = 128)
    private String comprovanteHash;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
