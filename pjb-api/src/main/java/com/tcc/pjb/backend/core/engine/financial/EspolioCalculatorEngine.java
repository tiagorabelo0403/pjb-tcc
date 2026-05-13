package com.tcc.pjb.backend.core.engine.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import lombok.Builder;
import lombok.Value;

@Component
public class EspolioCalculatorEngine {

    public EspolioBreakdown calcular(BigDecimal principal,
                                    BigDecimal jurosAoMes,
                                    Instant dataInicial,
                                    Instant dataObito,
                                    Instant dataCalculo) {

        BigDecimal p = nz(principal);
        BigDecimal j = nz(jurosAoMes);
        Instant inicio = dataInicial == null ? Instant.now() : dataInicial;
        Instant obito = dataObito;
        Instant fim = dataCalculo == null ? Instant.now() : dataCalculo;

        if (obito == null || obito.isBefore(inicio)) {
            
            BigDecimal total = aplicarJurosSimples(p, j, mesesEntre(inicio, fim));
            return EspolioBreakdown.builder()
                    .saldoDeCujus(total)
                    .saldoEspolio(BigDecimal.ZERO)
                    .mesesDeCujus(mesesEntre(inicio, fim))
                    .mesesEspolio(0)
                    .observacao("óbito não informado/fora do intervalo")
                    .build();
        }

        int mesesDeCujus = mesesEntre(inicio, obito);
        BigDecimal saldoAteObito = aplicarJurosSimples(p, j, mesesDeCujus);

        int mesesEspolio = mesesEntre(obito, fim);

        
        
        BigDecimal principalEspolio = saldoAteObito;
        BigDecimal saldoPosObito = aplicarJurosSimples(principalEspolio, j, mesesEspolio)
                .subtract(principalEspolio);

        return EspolioBreakdown.builder()
                .saldoDeCujus(saldoAteObito)
                .saldoEspolio(saldoPosObito)
                .mesesDeCujus(mesesDeCujus)
                .mesesEspolio(mesesEspolio)
                .observacao("separação temporal para inventário/ITCMD")
                .build();
    }

    private int mesesEntre(Instant a, Instant b) {
        if (a == null || b == null) return 0;
        if (b.isBefore(a)) return 0;
        long days = ChronoUnit.DAYS.between(a, b);
        return (int) Math.max(0, days / 30); 
    }

    private BigDecimal aplicarJurosSimples(BigDecimal principal, BigDecimal jurosAoMes, int meses) {
        BigDecimal p = nz(principal);
        BigDecimal j = nz(jurosAoMes);
        BigDecimal fator = j.multiply(BigDecimal.valueOf(meses));
        return p.add(p.multiply(fator)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    @Value
    @Builder
    public static class EspolioBreakdown {
        BigDecimal saldoDeCujus;
        BigDecimal saldoEspolio;
        int mesesDeCujus;
        int mesesEspolio;
        String observacao;

        public BigDecimal total() {
            return nz(saldoDeCujus).add(nz(saldoEspolio));
        }

        private static BigDecimal nz(BigDecimal b) {
            return b == null ? BigDecimal.ZERO : b;
        }
    }
}
