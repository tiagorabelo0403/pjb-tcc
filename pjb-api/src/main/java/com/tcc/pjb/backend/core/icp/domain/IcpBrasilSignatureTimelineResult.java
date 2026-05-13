package com.tcc.pjb.backend.core.icp.domain;

import java.util.List;

public record IcpBrasilSignatureTimelineResult(String docHash, List<IcpBrasilSignatureTimelineEntry> entries) {}
