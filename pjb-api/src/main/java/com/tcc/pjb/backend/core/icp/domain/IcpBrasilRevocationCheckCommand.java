package com.tcc.pjb.backend.core.icp.domain;

import java.security.cert.X509Certificate;

public record IcpBrasilRevocationCheckCommand(X509Certificate certificate) {}
