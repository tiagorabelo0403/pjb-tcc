package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbAutocompositivoRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_CONSENSUAL"; }
    @Override public String movementPolicy() { return "AUTOCOMPOSICAO_VOLUNTARIA"; }
    @Override public boolean simplifiedProcedure() { return true; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return true; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "SESSAO_MEDIACAO", "SESSAO_CONCILIACAO", "ACORDO_EXTRAJUDICIAL");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "CITACAO_POR_EDITAL_PADRAO", "PROVA_PERICIAL_COMPLEXA_SEM_TRIAGEM");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_OBJETO_CONSENSUAL_DISPONIVEL", "VALIDAR_VOLUNTARIEDADE_DAS_PARTES");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        movements.add("DESIGNAR_SESSAO_AUTOCOMPOSITIVA");
        movements.add("VERIFICAR_DISPONIBILIDADE_DO_MEDIADOR_OU_CONCILIADOR");
        return List.copyOf(movements);
    }
}
