package com.tcc.pjb.backend.core.icp.domain;
public record IcpValidationHealthResult(boolean valid, boolean healthy, String acSigla, String failureReason) {}
