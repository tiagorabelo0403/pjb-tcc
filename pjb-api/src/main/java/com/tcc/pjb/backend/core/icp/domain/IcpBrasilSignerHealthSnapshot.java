package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilSignerHealthSnapshot(String source, String keyAlias, boolean available, boolean pkcs11) {}
