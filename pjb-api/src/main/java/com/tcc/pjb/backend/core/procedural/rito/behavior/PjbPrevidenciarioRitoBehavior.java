package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbPrevidenciarioRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_PREVIDENCIARIO"; }
    @Override public String movementPolicy() { return "PREVIDENCIARIO_COMPATIVEL"; }
    @Override public boolean simplifiedProcedure() { return rito == RitoProcessual.PREVIDENCIARIO_JEF; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return rito != RitoProcessual.PREVIDENCIARIO_JEF; }
    @Override public boolean digitalHearingDefault() { return rito == RitoProcessual.PREVIDENCIARIO_JEF; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        if (rito == RitoProcessual.PREVIDENCIARIO_JEF) {
            return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "DESIGNAR_CONCILIACAO", "EXPEDIR_CITACAO_SIMPLIFICADA", "PROTOCOLO_JEF");
        }
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "ATOS_PREVIDENCIARIOS", "CALCULO_BENEFICIO", "PERICIA_MEDICA_PREVIDENCIARIA");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return rito == RitoProcessual.PREVIDENCIARIO_JEF
                ? List.of("GUIA_CUSTAS_PRIMEIRO_GRAU")
                : List.of();
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPETENCIA_PREVIDENCIARIA", "VALIDAR_BENEFICIO_OBJETO_ACAO");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("VERIFICAR_BENEFICIO_PREVIDENCIARIO");
        movements.add("AGENDAR_PERICIA_MEDICA_SE_NECESSARIO");
        return List.copyOf(movements);
    }
}
