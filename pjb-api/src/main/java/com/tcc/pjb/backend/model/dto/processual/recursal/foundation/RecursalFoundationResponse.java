package com.tcc.pjb.backend.model.dto.processual.recursal.foundation;

import java.util.List;
import java.util.Set;

public record RecursalFoundationResponse(
        Set<String> classificacaoAmbito,
        Set<String> classificacaoFundamentacao,
        Set<String> classificacaoEfeitos,
        Set<String> classificacaoMomento,
        List<RecursalPrazoRuleView> regrasDePrazo,
        RecursoAdesivoRuleView recursoAdesivo,
        RecursalApelacaoBlueprintView apelacao,
        Set<String> juizoAdmissibilidadeCompetencia,
        Set<String> meritoErroTipos,
        List<String> formalSectionLabels) {
}
