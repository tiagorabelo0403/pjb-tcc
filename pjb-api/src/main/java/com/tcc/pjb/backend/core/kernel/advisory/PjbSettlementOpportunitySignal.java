package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;

public record PjbSettlementOpportunitySignal(String processNumber,
                                             BigDecimal claimValue,
                                             boolean repeatLitigant,
                                             boolean documentaryEvidence,
                                             boolean stableJurisprudence,
                                             boolean vulnerableParty,
                                             boolean publicPolicyRestriction) {
}
