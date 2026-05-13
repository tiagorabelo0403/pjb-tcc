package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbEleitoralRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_ELEITORAL"; }
    @Override public String movementPolicy() { return "ELEITORAL_COMPLETO"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "REGISTRO_CANDIDATURA", "ATOS_ELEITORAIS", "DIREITO_DE_RESPOSTA_ELEITORAL");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_PERIODO_ELEITORAL", "VALIDAR_COMPETENCIA_ELEITORAL");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_ELEITORAL");
        movements.add("VERIFICAR_PERIODO_ELEITORAL_VIGENTE");
        return List.copyOf(movements);
    }
}
