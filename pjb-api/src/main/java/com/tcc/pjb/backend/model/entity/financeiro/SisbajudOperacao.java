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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_sisbajud_operacao", indexes = {
        @Index(name = "idx_sisbajud_processo", columnList = "processo_id"),
        @Index(name = "idx_sisbajud_status", columnList = "status")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SisbajudOperacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo", nullable = false, length = 32)
    private String tipo;

    @Column(name = "cpf_devedor", length = 14)
    private String cpfDevedor;

    @Column(name = "valor_solicitado", precision = 19, scale = 2)
    private BigDecimal valorSolicitado;

    @Column(name = "numero_oficio", length = 64)
    private String numeroOficio;

    @Column(name = "protocolo_bacen", length = 128)
    private String protocoloBacen;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "retorno_bacen", length = 12000)
    private String retornoBacen;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "proximo_retry_em")
    private Instant proximoRetryEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "operador_id")
    private Long operadorId;

    @Column(name = "authz_trail_id", length = 128)
    private String authzTrailId;
}
