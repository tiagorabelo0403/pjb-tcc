package com.tcc.pjb.backend.platform.runtime.domain;

import com.tcc.pjb.backend.platform.runtime.PjbKafkaPressureService;
import com.tcc.pjb.backend.platform.runtime.PjbLivePressureService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeDrainService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureService;
import java.time.Instant;

public record PjbRuntimeReadinessView(Instant generatedAt,
                                      boolean up,
                                      String status,
                                      boolean functionalUp,
                                      PjbRuntimePressureService.Snapshot runtimePressure,
                                      PjbRuntimeDrainService.Snapshot runtimeDrain,
                                      PjbLivePressureService.Snapshot livePressure,
                                      PjbKafkaPressureService.Snapshot kafkaPressure,
                                      PjbTransactionPressureView transactions) {
}
