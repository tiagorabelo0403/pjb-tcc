package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbInternacionalRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_INTERNACIONAL"; }
    @Override public String movementPolicy() { return "COOPERACAO_INTERNACIONAL"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "CARTA_ROGATORIA", "HOMOLOGACAO_SENTENCA_ESTRANGEIRA", "COOPERACAO_JURIDICA_ATIVA");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_STJ_PARA_HOMOLOGACAO", "VALIDAR_TRATADO_APLICAVEL");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_COOPERACAO_INTERNACIONAL");
        movements.add("VERIFICAR_TRATADO_OU_CONVENCAO_APLICAVEL");
        return List.copyOf(movements);
    }
}
