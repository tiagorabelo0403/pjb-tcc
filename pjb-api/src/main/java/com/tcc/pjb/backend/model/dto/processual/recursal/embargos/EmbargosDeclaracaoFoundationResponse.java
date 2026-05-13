package com.tcc.pjb.backend.model.dto.processual.recursal.embargos;

import java.util.List;
import java.util.Set;

public record EmbargosDeclaracaoFoundationResponse(
        int prazoDiasUteis,
        boolean cabivelContraQualquerDecisao,
        boolean interrompePrazoRecursalPrincipal,
        boolean efeitoInfringenteExigeFundamentoApto,
        Set<String> fundamentosCabiveis,
        List<EmbargosDeclaracaoGroundView> fundamentosDetalhados,
        boolean contraditorioPrevioNecessario,
        boolean preparoExigivel,
        boolean colegiadoNecessario,
        List<String> formalSectionLabels,
        String observacoes) {
}
