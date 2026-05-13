package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;
import java.util.List;

public record PjbRuntimeGuardrailsSnapshot(Instant generatedAt,
                                           boolean healthy,
                                           int riskScore,
                                           int runtimeHeadroomScore,
                                           int executionHeadroomScore,
                                           int datasourceHeadroomScore,
                                           int memoryHeadroomScore,
                                           int gcHeadroomScore,
                                           int transactionHeadroomScore,
                                           List<PjbRuntimeGuardrailFinding> findings) {
}
