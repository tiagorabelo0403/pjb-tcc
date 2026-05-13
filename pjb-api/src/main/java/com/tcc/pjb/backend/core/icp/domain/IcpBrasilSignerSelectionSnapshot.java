package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilSignerSelectionSnapshot(String source,
                                               String keyAlias,
                                               boolean available,
                                               boolean pkcs11) {
}
