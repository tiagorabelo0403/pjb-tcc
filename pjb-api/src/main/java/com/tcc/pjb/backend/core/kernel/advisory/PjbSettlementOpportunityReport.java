package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;

public record PjbSettlementOpportunityReport(String status,
                                             int score,
                                             boolean humanReviewRequired,
                                             List<String> reasons,
                                             List<String> safeguards) {
}
