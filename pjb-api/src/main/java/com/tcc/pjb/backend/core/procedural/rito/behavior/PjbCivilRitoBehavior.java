package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbCivilRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_CIVIL_ORDINARIO"; }
    @Override public String movementPolicy() { return "ORDINARIO_COMPLETO"; }
    @Override public boolean simplifiedProcedure() { return rito == RitoProcessual.SUMARIO || rito == RitoProcessual.SUMARIO_ESPECIAL; }
    @Override public boolean allowsEdital() { return true; }
    @Override public boolean costsAtFirstDegree() { return true; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "ATOS_ORDINARIOS", "SANEAMENTO", "PROVA_PERICIAL", "CUSTAS_INICIAIS");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of();
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_CIVIL_ANTES_DA_DISTRIBUICAO");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("REALIZAR_TRIAGEM_DE_COMPETENCIA_E_CUSTAS");
        return List.copyOf(movements);
    }
}
