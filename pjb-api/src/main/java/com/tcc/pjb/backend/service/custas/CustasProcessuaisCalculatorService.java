package com.tcc.pjb.backend.service.custas;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustasProcessuaisCalculatorService {

    public record CustasInput(
            UUID processoId,
            RitoProcessual rito,
            BigDecimal valorCausa,
            TipoCusta tipo,
            boolean justicaGratuita,
            boolean fazendaPublica,
            boolean ministerioPublico,
            String uf
    ) {}

    public record CustasCalculadas(
            UUID processoId,
            TipoCusta tipo,
            BigDecimal valorBase,
            BigDecimal percentualAplicado,
            BigDecimal valorFinal,
            boolean isento,
            String fundamentacao,
            List<String> alertas
    ) {}

    private static final BigDecimal PERC_PREPARO_DEFAULT = new BigDecimal("0.02");
    private static final BigDecimal PERC_MULTA_1026 = new BigDecimal("0.01");
    private static final BigDecimal PERC_MA_FE = new BigDecimal("0.10");

    public CustasCalculadas calcular(CustasInput input) {
        List<String> alertas = new ArrayList<>();
        if (input.justicaGratuita()) {
            return new CustasCalculadas(input.processoId(), input.tipo(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    true, "Parte beneficiária da gratuidade da justiça (Lei 1.060/50, art. 3º).",
                    List.of());
        }
        if (input.fazendaPublica() || input.ministerioPublico()) {
            return new CustasCalculadas(input.processoId(), input.tipo(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    true, "Fazenda Pública/MP isentos de custas (CPC, art. 91).", List.of());
        }
        BigDecimal perc = resolverPercentual(input.tipo());
        BigDecimal base = input.valorCausa() != null ? input.valorCausa() : BigDecimal.ZERO;
        BigDecimal valor = base.multiply(perc).setScale(2, RoundingMode.HALF_UP);
        if (input.tipo().requerDespacho()) {
            alertas.add("Multa requer despacho jurisdicional antes da cobrança.");
        }
        return new CustasCalculadas(input.processoId(), input.tipo(),
                base, perc, valor, false,
                "Calculado com base no valor da causa (" + input.uf() + ").",
                List.copyOf(alertas));
    }

    private BigDecimal resolverPercentual(TipoCusta tipo) {
        return switch (tipo) {
            case PREPARO_RECURSAL -> PERC_PREPARO_DEFAULT;
            case MULTA_ART_1026_CPC -> PERC_MULTA_1026;
            case MULTA_LITIGANCIA_MA_FE, MULTA_IMPROBIDADE_ART77 -> PERC_MA_FE;
            default -> PERC_PREPARO_DEFAULT;
        };
    }
}
