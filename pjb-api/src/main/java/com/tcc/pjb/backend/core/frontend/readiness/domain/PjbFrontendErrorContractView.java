package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendErrorContractView(
        boolean ready,
        boolean apiQueryEnvelopePresent,
        boolean apiCommandEnvelopePresent,
        boolean exceptionAdvicePresent,
        int duplicateRoutes,
        int wildcardResponses,
        int dtoSurfaceIssues,
        List<String> notes
) {
}
