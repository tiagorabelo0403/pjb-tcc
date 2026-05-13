package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilValidationSnapshot(boolean valid,
                                          String serialHex,
                                          String acSigla,
                                          String failureReason) {}
