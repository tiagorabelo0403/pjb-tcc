package com.tcc.pjb.backend.core.icp.domain;
public record IcpBrasilValidationProjection(boolean valid, String serialHex, String acSigla, String failureReason) {}
