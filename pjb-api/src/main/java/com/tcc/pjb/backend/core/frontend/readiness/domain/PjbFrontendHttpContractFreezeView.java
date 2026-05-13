package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendHttpContractFreezeView(
        PjbFrontendPublicContractSummary summary,
        PjbFrontendAuthContractView auth,
        PjbFrontendErrorContractView errors,
        PjbFrontendEnvelopeContractView envelopes,
        PjbFrontendValidationContractView validation,
        List<PjbFrontendHttpErrorCatalogEntry> errorCatalog,
        List<String> nextSteps,
        Instant generatedAt
) {
}
