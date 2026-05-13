package com.tcc.pjb.backend.core.icp.domain;

import java.time.Instant;

public record IcpBrasilCertificateTimelineEntry(String evento, Instant instante, String detalhe) {}
