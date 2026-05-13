package com.tcc.pjb.backend.core.quality.codebase.presentation;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityIssueResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;

public final class PjbCodebaseSanityResponseMapper {

    private PjbCodebaseSanityResponseMapper() {
    }

    public static ProcessoCodebaseSanityResponse toProcessual(PjbCodebaseSanityAggregate aggregate) {
        return new ProcessoCodebaseSanityResponse(
                aggregate.disponivel(),
                aggregate.limpo(),
                aggregate.score(),
                aggregate.arquivosEscaneados(),
                aggregate.fqnsDuplicados(),
                aggregate.importsInternosQuebrados(),
                aggregate.virtualThreadsDiretas(),
                aggregate.diretoriosOrfaos(),
                aggregate.issues().stream().map(PjbCodebaseSanityResponseMapper::toProcessualIssue).toList(),
                aggregate.geradoEm()
        );
    }

    private static ProcessoCodebaseSanityIssueResponse toProcessualIssue(PjbCodebaseSanityIssue issue) {
        return new ProcessoCodebaseSanityIssueResponse(
                issue.codigo(),
                issue.severidade(),
                issue.arquivo(),
                issue.linhas(),
                issue.detalhe()
        );
    }
}
