package com.tcc.pjb.backend.core.icp.domain;

public record IcpPkcs11LibView(
        String libraryPath,
        boolean configured,
        boolean available,
        String nodeProfile
) {}
