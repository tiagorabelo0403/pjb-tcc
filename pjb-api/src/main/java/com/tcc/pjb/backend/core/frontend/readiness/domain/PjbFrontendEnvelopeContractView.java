package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendEnvelopeContractView(
        boolean ready,
        boolean queryEnvelopePresent,
        boolean commandEnvelopePresent,
        boolean responseFactoryPresent,
        boolean problemDetailHardeningPresent,
        boolean sensitiveResponseHardeningPresent,
        List<String> notes
) {
}
