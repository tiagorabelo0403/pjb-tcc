package com.tcc.pjb.backend.core.icp.domain;

public record IcpSignerMaterialView(
        String keyAlias,
        boolean certificatePresent,
        boolean privateKeyPresent
) {}
