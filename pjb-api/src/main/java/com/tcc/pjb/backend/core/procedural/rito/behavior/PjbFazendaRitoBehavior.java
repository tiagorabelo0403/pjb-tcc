package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public record PjbFazendaRitoBehavior(RitoProcessual rito) implements PjbRitoBehavior {

    @Override public String prazoPolicy() { return "PRAZO_FAZENDA"; }
    @Override public String movementPolicy() { return "ORDINARIO_FAZENDA"; }
    @Override public boolean simplifiedProcedure() { return false; }
    @Override public boolean allowsEdital() { return true; }
    @Override public boolean costsAtFirstDegree() { return true; }
    @Override public boolean digitalHearingDefault() { return false; }
    @Override public boolean requiresHumanReview() { return false; }

    @Override
    public List<String> defaultVisibleActions() {
        return List.of("VISUALIZAR_CAPA", "JUNTAR_DOCUMENTO", "CONSULTAR_PRAZOS", "EMISSAO_CERTIDAO_FISCAL", "PENHORA_FISCAL", "EXCECAO_DE_EXECUTIVIDADE", "CUSTAS_INICIAIS");
    }

    @Override
    public List<String> defaultHiddenActions() {
        return List.of();
    }

    @Override
    public List<String> defaultValidations() {
        return List.of("VALIDAR_CDA_ANTES_DA_DISTRIBUICAO", "VALIDAR_COMPETENCIA_FAZENDA_PUBLICA");
    }

    @Override
    public List<String> firstMovements(int urgencyScore) {
        List<String> movements = new ArrayList<>();
        if (urgencyScore >= 85) movements.add("SUBMETER_TUTELA_URGENTE_A_ANALISE_IMEDIATA");
        movements.add("REALIZAR_TRIAGEM_DE_COMPETENCIA_FAZENDA");
        movements.add("VERIFICAR_CERTIDAO_DA_DIVIDA_ATIVA");
        return List.copyOf(movements);
    }
}
