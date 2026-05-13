package com.tcc.pjb.backend.core.icp.domain;

public record IcpPkcsProfileView(
        String profileCandidate,
        String profileAchieved,
        boolean valid
) {}
