package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbExecucaoPenalRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_EXECUCAO_PENAL"; }
    @Override public String movementPolicy() { return "EXECUCAO_PENAL_LEP"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return true; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "ATOS_EXECUCAO_PENAL", "PROGRESSO_DE_REGIME", "PARECER_CONSELHOS_PENITENCIARIOS");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("GUIA_CUSTAS_PRIMEIRO_GRAU", "PETICIONAMENTO_CIVEL_PADRAO");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_TITULO_EXECUTIVO_PENAL", "VALIDAR_REGIME_INICIAL_DE_CUMPRIMENTO");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_TITULO_EXECUTIVO_PENAL");
        movements.add("VERIFICAR_REGIME_DE_CUMPRIMENTO_E_PROGRESSAO");
        return List.copyOf(movements);
    }
}
