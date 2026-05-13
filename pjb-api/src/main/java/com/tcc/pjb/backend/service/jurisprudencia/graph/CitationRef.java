package com.tcc.pjb.backend.service.jurisprudencia.graph;

public record CitationRef(
        CitationRelationType relation,
        CitationTargetType targetType,
        String targetRef,
        String raw
) {
}
