package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.List;

public record PjbDigitalJusticeUnitReadiness(String status,
                                             double occupancyRatio,
                                             boolean eligibleForNewDistribution,
                                             List<String> blockers,
                                             List<String> recommendations) {
}
