package com.tcc.pjb.backend.core.icp.domain;

public record IcpBrasilChainValidationDetails(boolean trustAnchorsLoaded,
                                              boolean acceptedAc,
                                              String acSigla,
                                              String failureReason) {
}
