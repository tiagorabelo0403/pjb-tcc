package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbEspecialConstitucionalRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_CONSTITUCIONAL"; }
    @Override public String movementPolicy() { return "ESPECIAL_CONSTITUCIONAL"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "INFORMACOES_AUTORIDADE_COATORA", "LIMINAR_MANDAMENTAL", "SOLICITAR_INFORMACOES");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_CONSTITUCIONAL", "VALIDAR_LEGITIMIDADE_ATIVA_REMEDIO_CONSTITUCIONAL");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_LIMINAR_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_COMPETENCIA_CONSTITUCIONAL");
        movements.add("NOTIFICAR_AUTORIDADE_COATORA");
        return List.copyOf(movements);
    }
}
