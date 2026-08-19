package com.tcc.pjb.backend.service.processual.honorarios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class HonorariosSucumbenciaCalculatorService {

    private static final BigDecimal PERC_MIN = new BigDecimal("0.10");
    private static final BigDecimal PERC_MAX = new BigDecimal("0.20");
    private static final BigDecimal PERC_FAZENDA_BAIXO = new BigDecimal("0.10");
    private static final BigDecimal PERC_FAZENDA_ALTO = new BigDecimal("0.03");
    private static final BigDecimal TETO_FAZENDA_BAIXO = new BigDecimal("200000");
    private static final BigDecimal TETO_FAZENDA_ALTO = new BigDecimal("100000000");

    public record HonorariosInput(
            Long processoId,
            BigDecimal valorCondenacao,
            boolean fazendaPublicaVencida,
            boolean causaSimples,
            boolean trabalhoComplexo,
            BigDecimal percentualFixadoMagistrado
    ) {}

    public record HonorariosCalculados(
            Long processoId,
            BigDecimal percentualAplicado,
            BigDecimal valorHonorarios,
            String fundamentacao
    ) {}

    public HonorariosCalculados calcular(HonorariosInput input) {
        if (input.percentualFixadoMagistrado() != null) {
            BigDecimal valor = input.valorCondenacao()
                    .multiply(input.percentualFixadoMagistrado())
                    .setScale(2, RoundingMode.HALF_UP);
            return new HonorariosCalculados(input.processoId(),
                    input.percentualFixadoMagistrado(), valor,
                    "Percentual fixado pelo magistrado (CPC, art. 85).");
        }
        if (input.fazendaPublicaVencida()) {
            return calcularFazenda(input);
        }
        BigDecimal perc = input.trabalhoComplexo() ? PERC_MAX : PERC_MIN;
        BigDecimal valor = input.valorCondenacao().multiply(perc)
                .setScale(2, RoundingMode.HALF_UP);
        return new HonorariosCalculados(input.processoId(), perc, valor,
                "Honorários de " + perc.multiply(new BigDecimal("100")) + "% (CPC, art. 85, §2º).");
    }

    private HonorariosCalculados calcularFazenda(HonorariosInput input) {
        BigDecimal perc = input.valorCondenacao().compareTo(TETO_FAZENDA_ALTO) >= 0
                ? PERC_FAZENDA_ALTO : PERC_FAZENDA_BAIXO;
        BigDecimal valor = input.valorCondenacao().multiply(perc)
                .setScale(2, RoundingMode.HALF_UP);
        return new HonorariosCalculados(input.processoId(), perc, valor,
                "Honorários contra Fazenda Pública (CPC, art. 85, §3º).");
    }
}
