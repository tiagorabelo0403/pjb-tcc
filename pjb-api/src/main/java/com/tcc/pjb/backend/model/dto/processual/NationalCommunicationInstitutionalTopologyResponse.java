package com.tcc.pjb.backend.model.dto.processual;

import java.util.Set;

public record NationalCommunicationInstitutionalTopologyResponse(
        String destinatarioInstitucionalKind,
        String organizacaoExtraJudicialKind,
        Set<String> legadosCompativeis,
        boolean instituicaoEssencialJustica,
        boolean apoioTecnicoOuAuxiliar,
        boolean admiteCanalNacionalPessoal
) {
}
