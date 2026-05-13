package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BaptismCompleteRequest {

    @NotNull
    private Long challengeId;

    @NotBlank
    private String signatureBase64;

    @NotBlank
    private String certificateDerBase64;

    private String signatureAlgorithm;

    public Long getChallengeId() { return challengeId; }
    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }

    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }

    public String getCertificateDerBase64() { return certificateDerBase64; }
    public void setCertificateDerBase64(String certificateDerBase64) { this.certificateDerBase64 = certificateDerBase64; }

    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public void setSignatureAlgorithm(String signatureAlgorithm) { this.signatureAlgorithm = signatureAlgorithm; }
}
