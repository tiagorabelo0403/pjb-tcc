package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;
import java.util.List;

public record PjbRuntimeExecutionGovernanceView(Instant generatedAt,
                                                List<PjbRuntimeExecutionLaneView> lanes,
                                                List<PjbRuntimeExecutionOperationView> operations) {
}
