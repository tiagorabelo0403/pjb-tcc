package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbJuizadoRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_JUIZADO_ESPECIAL"; }
    @Override public String movementPolicy() { return "SUMARISSIMO_COMPATIVEL"; }
    @Override public boolean simplifiedProcedure() { return true; }
    @Override public boolean allowsEdital() { return false; }
    @Override public boolean costsAtFirstDegree() { return false; }
    @Override public boolean digitalHearingDefault() { return true; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "DESIGNAR_CONCILIACAO", "EXPEDIR_CITACAO_SIMPLIFICADA", "PROPOR_ACORDO_EXPRESSO");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of("CITACAO_POR_EDITAL_PADRAO", "PROVA_PERICIAL_COMPLEXA_SEM_TRIAGEM", "GUIA_CUSTAS_PRIMEIRO_GRAU");
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_COMPATIBILIDADE_SUMARISSIMA_ANTES_DA_DISTRIBUICAO");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("EXPEDIR_CITACAO_SIMPLIFICADA");
        movements.add("DESIGNAR_AUDIENCIA_DE_CONCILIACAO");
        movements.add("OFERTAR_CONCILIACAO_EXPRESSA_QUANDO_HOUVER_LITIGANTE_RECORRENTE");
        return List.copyOf(movements);
    }
}
