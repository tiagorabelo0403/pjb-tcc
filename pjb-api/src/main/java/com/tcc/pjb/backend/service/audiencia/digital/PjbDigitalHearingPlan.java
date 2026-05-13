package com.tcc.pjb.backend.service.audiencia.digital;

import java.util.List;

public record PjbDigitalHearingPlan(PjbDigitalHearingStatus status,
                                    boolean readyForNotice,
                                    boolean humanReviewRequired,
                                    List<String> missingCapabilities,
                                    List<String> operationalSteps) {
}
