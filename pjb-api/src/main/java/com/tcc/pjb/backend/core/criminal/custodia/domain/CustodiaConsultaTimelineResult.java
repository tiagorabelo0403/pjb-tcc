package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.util.List;

public record CustodiaConsultaTimelineResult(Long custodiaId,
                                             List<CustodiaTimelineEntry> entries) {}
