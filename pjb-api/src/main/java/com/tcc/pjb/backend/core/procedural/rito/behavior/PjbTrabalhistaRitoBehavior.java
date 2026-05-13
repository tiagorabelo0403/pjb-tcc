package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbTrabalhistaRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_TRABALHISTA"; }
    @Override public String movementPolicy() { return "TRABALHISTA_COMPATIVEL"; }

    @Override
    public boolean simplifiedProcedure() {
        return rito == RitoProcessual.TRABALHISTA_SUMARISSIMO || rito == RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
    }

    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return true; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "AUDIENCIA_TRABALHISTA", "DESIGNAR_CONCILIACAO_TRABALHISTA", "CALCULO_LIQUIDACAO_TRABALHISTA");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_TRABALHISTA");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("DESIGNAR_AUDIENCIA_TRABALHISTA");
        movements.add("VERIFICAR_CUSTAS_TRABALHISTAS");
        return List.copyOf(movements);
    }
}
