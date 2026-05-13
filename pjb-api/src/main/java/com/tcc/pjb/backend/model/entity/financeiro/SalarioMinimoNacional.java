package com.tcc.pjb.backend.model.entity.financeiro;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "salario_minimo_nacional",
        indexes = {
                @Index(name = "idx_salario_minimo_ano", columnList = "ano_referencia"),
                @Index(name = "idx_salario_minimo_vigencia", columnList = "vigente_desde,vigente_ate")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_salario_minimo_ano", columnNames = "ano_referencia")
        }
)
public class SalarioMinimoNacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(name = "valor_mensal", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "valor_diario", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorDiario;

    @Column(name = "valor_hora", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorHora;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_ate")
    private LocalDate vigenteAte;

    @Column(name = "norma_referencia", nullable = false, length = 200)
    private String normaReferencia;

    @Column(name = "fonte_oficial", nullable = false, length = 255)
    private String fonteOficial;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private Long versao;

    public boolean vigenteEm(LocalDate data) {
        if (data == null || vigenteDesde == null) {
            return false;
        }
        boolean inicio = !data.isBefore(vigenteDesde);
        boolean fim = vigenteAte == null || !data.isAfter(vigenteAte);
        return inicio && fim && Boolean.TRUE.equals(ativo);
    }

    @PrePersist
    @PreUpdate
    private void preparar() {
        if (ativo == null) {
            ativo = true;
        }
        if (vigenteDesde == null && anoReferencia != null) {
            vigenteDesde = LocalDate.of(anoReferencia, 1, 1);
        }
        atualizadoEm = Instant.now();
    }
}
