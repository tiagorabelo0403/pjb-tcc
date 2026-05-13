package com.tcc.pjb.backend.service.api;

import java.util.List;

public record PjbJudicialServiceMarketplaceDecision(String status,
                                                   boolean availableForContracting,
                                                   List<String> blockers,
                                                   List<String> governanceRequirements) {
}
