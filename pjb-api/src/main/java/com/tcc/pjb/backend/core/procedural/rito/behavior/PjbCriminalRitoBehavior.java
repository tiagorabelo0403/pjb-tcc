package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbCriminalRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_PENAL"; }
    @Override public String movementPolicy() { return "CRIMINAL_COMPLETO"; }
    @Override public boolean simplifiedProcedure() { return rito == RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO; }
    @Override public boolean allowsEdital() { return true; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "AUDIENCIA_INSTRUCAO_JULGAMENTO", "REGISTRO_JUDICIAL_PENAL", "TUTELA_CAUTELAR_PENAL");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_PENAL_ANTES_DA_DISTRIBUICAO");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_PENAL");
        movements.add("DESIGNAR_AUDIENCIA_DE_INSTRUCAO_JULGAMENTO");
        return List.copyOf(movements);
    }
}
