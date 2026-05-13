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
@Table(name = "pjb_custa_judicial", indexes = {
        @Index(name = "idx_custa_processo", columnList = "processo_id"),
        @Index(name = "idx_custa_status", columnList = "status,vencimento")
})
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustaJudicial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "tipo", nullable = false, length = 64)
    private String tipo;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "codigo_receita", length = 16)
    private String codigoReceita;

    @Column(name = "competencia_uf", length = 2)
    private String competenciaUf;

    @Column(name = "isento", nullable = false)
    private boolean isento;

    @Column(name = "motivo_isencao", length = 255)
    private String motivoIsencao;

    @Column(name = "linha_digitavel", length = 120)
    private String linhaDigitavel;

    @Column(name = "codigo_barras", length = 60)
    private String codigoBarras;

    @Column(name = "pix_payload", length = 4000)
    private String pixPayload;

    @Column(name = "pix_txid", length = 77)
    private String pixTxid;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "vencimento", nullable = false)
    private LocalDate vencimento;

    @Column(name = "pago_em")
    private Instant pagoEm;

    @Column(name = "valor_pago", precision = 19, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "nosso_numero", length = 64)
    private String nossoNumero;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static CustaJudicial isento(Long processoId, String tipo, BigDecimal valor, String motivo) {
        return CustaJudicial.builder()
                .processoId(processoId)
                .tipo(tipo)
                .valor(valor)
                .isento(true)
                .motivoIsencao(motivo)
                .status("PAGO")
                .createdAt(Instant.now())
                .vencimento(LocalDate.now())
                .build();
    }
}
