package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilSignatureTimelineEntry(String evento, Instant ocorridoEm, String detalhe) {}
