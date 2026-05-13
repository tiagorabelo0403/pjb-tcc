package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbAdministrativoRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_ADMINISTRATIVO"; }
    @Override public String movementPolicy() { return "ORDINARIO_ADMINISTRATIVO"; }
    @Override public boolean simplifiedProcedure() { return false; }
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
        return List.of("VALIDAR_COMPETENCIA_ADMINISTRATIVA", "VALIDAR_ESGOTAMENTO_VIA_ADMINISTRATIVA");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("REALIZAR_TRIAGEM_DE_COMPETENCIA_ADMINISTRATIVA");
        movements.add("VERIFICAR_ESGOTAMENTO_DA_VIA_ADMINISTRATIVA");
        return List.copyOf(movements);
    }
}
