package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.util.List;

public record NationalCommunicationInstitutionalPanelBlueprintResponse(
        String codigo,
        String escopo,
        String panel,
        String audience,
        String titulo,
        String rotaInicial,
        List<String> secoesPrimarias,
        List<String> acoesRapidas,
        List<String> guardasSeguranca,
        List<String> regrasVisibilidade,
        List<String> fundamentos
) {
}
