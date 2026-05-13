package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbInfanciaRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_INFANCIA"; }
    @Override public String movementPolicy() { return "PROTECAO_INTEGRAL"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return true; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "AUDIENCIA_PROTECAO_INTEGRAL", "REGISTRO_INFRACIONAL", "MEDIDA_SOCIOEDUCATIVA");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("CITACAO_POR_EDITAL_PADRAO", "GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_INFANCIA_JUVENTUDE", "VALIDAR_PRINCIPIO_PROTECAO_INTEGRAL");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 70) movements.add("SUBMETER_MEDIDA_URGENTE_PROTECAO_CRIANCA");
        movements.add("VERIFICAR_COMPETENCIA_VARA_INFANCIA");
        movements.add("ACIONAR_REDE_PROTECAO_SE_NECESSARIO");
        return List.copyOf(movements);
    }
}
