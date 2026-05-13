package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbEmpresarialRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_FALENCIA"; }
    @Override public String movementPolicy() { return "RECUPERACAO_JUDICIAL_COMPLETO"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return true; }
    @Override public boolean costsAtFirstDegree() { return true; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "ASSEMBLEIA_CREDORES", "PLANO_RECUPERACAO", "HABILITACAO_CREDITO", "ARRECADACAO_BENS");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of();
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_EMPRESARIAL", "VALIDAR_NATUREZA_FALIMENTAR_OU_RECUPERACIONAL");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_VARA_EMPRESARIAL");
        movements.add("VERIFICAR_NATUREZA_FALIMENTAR_OU_RECUPERACIONAL");
        return List.copyOf(movements);
    }
}
