package com.tcc.pjb.backend.model.dto.security;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class TrustedDeviceResponse {
    private Long id;
    private String alias;
    private boolean verified;
    private boolean attestationTrusted;
    private int riskScoreEnroll;
    private boolean enrollSuspectNetwork;
    private LocalDateTime quarentenaAte;
    private LocalDateTime criadoEm;
    private LocalDateTime ultimoUsoEm;
    private Long pendingChallengeId;
    private String pendingChallengeType;
    private String pendingChallengeHint;

    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public boolean isVerified() { return verified; }
    public boolean isAttestationTrusted() { return attestationTrusted; }
    public int getRiskScoreEnroll() { return riskScoreEnroll; }
    public boolean isEnrollSuspectNetwork() { return enrollSuspectNetwork; }
    public LocalDateTime getQuarentenaAte() { return quarentenaAte; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getUltimoUsoEm() { return ultimoUsoEm; }
    public Long getPendingChallengeId() { return pendingChallengeId; }
    public String getPendingChallengeType() { return pendingChallengeType; }
    public String getPendingChallengeHint() { return pendingChallengeHint; }
}
