package com.tcc.pjb.backend.service.processual.inventario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InventarioArrolamentoReadinessService {

    private static final BigDecimal LIMITE_ARROLAMENTO_SIMPLES = new BigDecimal("1000").multiply(
            new BigDecimal("1518"));

    public enum ViaInventario { JUDICIAL_FORMAL, ARROLAMENTO_SUMARIO,
            ARROLAMENTO_COMUM, ESCRITURA_PUBLICA }

    public record InventarioInput(
            UUID processoId,
            BigDecimal valorBens,
            boolean haHerdeirosIncapazes,
            boolean testamentoExistente,
            boolean herdeirosConcordam,
            boolean advogadoHabilitado
    ) {}

    public record InventarioReadiness(
            UUID processoId,
            ViaInventario viaAdequada,
            boolean aptaParaArrolamento,
            List<String> fundamentos,
            List<String> pendencias
    ) {}

    public InventarioReadiness avaliar(InventarioInput input) {
        List<String> fundamentos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        ViaInventario via;
        if (input.testamentoExistente() || input.haHerdeirosIncapazes()) {
            via = ViaInventario.JUDICIAL_FORMAL;
            fundamentos.add("Testamento ou herdeiro incapaz exige inventário judicial formal (CPC, art. 610).");
        } else if (input.herdeirosConcordam()
                && input.valorBens().compareTo(LIMITE_ARROLAMENTO_SIMPLES) <= 0) {
            via = ViaInventario.ARROLAMENTO_SUMARIO;
            fundamentos.add("Valor dos bens até 1.000 salários mínimos e herdeiros concordes — arrolamento sumário (CPC, art. 659).");
        } else if (input.herdeirosConcordam()) {
            via = ViaInventario.ARROLAMENTO_COMUM;
            fundamentos.add("Herdeiros concordes — arrolamento comum (CPC, art. 664).");
        } else {
            via = ViaInventario.JUDICIAL_FORMAL;
            fundamentos.add("Herdeiros discordantes — inventário judicial formal necessário.");
        }
        if (!input.advogadoHabilitado()) {
            pendencias.add("Advogado não habilitado nos autos — constituir antes de prosseguir.");
        }
        boolean apta = via != ViaInventario.JUDICIAL_FORMAL
                && pendencias.isEmpty();
        return new InventarioReadiness(input.processoId(), via, apta,
                List.copyOf(fundamentos), List.copyOf(pendencias));
    }
}
