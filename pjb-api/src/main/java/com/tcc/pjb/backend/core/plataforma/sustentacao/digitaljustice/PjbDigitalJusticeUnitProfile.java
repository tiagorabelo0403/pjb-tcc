package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.Set;

public record PjbDigitalJusticeUnitProfile(String unitCode,
                                           String subjectMatter,
                                           Set<String> territoryCodes,
                                           boolean remoteFirst,
                                           boolean digitalHearingEnabled,
                                           boolean specializedStaffAllocated,
                                           int activeCaseLoad,
                                           int monthlyCapacity) {
}
