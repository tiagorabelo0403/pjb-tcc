package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbAmbientalRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_AMBIENTAL"; }
    @Override public String movementPolicy() { return "AMBIENTAL_COMPATIVEL"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return true; }
    @Override public boolean costsAtFirstDegree() { return true; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "MEDIDA_CAUTELAR_AMBIENTAL", "ACP_AMBIENTAL", "PERICIA_AMBIENTAL", "ATOS_ORDINARIOS");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of();
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_AMBIENTAL", "VALIDAR_LEGITIMIDADE_ATIVA_ACP");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_VARA_AMBIENTAL");
        movements.add("AVALIAR_NECESSIDADE_DE_MEDIDA_CAUTELAR");
        return List.copyOf(movements);
    }
}
