package com.tcc.pjb.backend.core.icp.domain;

import java.util.List;

public record IcpBrasilTimelineResult(String docHash, List<IcpBrasilSignatureTimelineEntry> entries) {}
