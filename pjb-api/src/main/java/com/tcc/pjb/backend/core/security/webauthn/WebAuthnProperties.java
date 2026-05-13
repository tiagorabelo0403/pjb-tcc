package com.tcc.pjb.backend.core.security.webauthn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.webauthn")
public class WebAuthnProperties {

    private String rpId = "localhost";

    private String rpName = "PJB";

    private Set<String> origins = new HashSet<>(Set.of("http://localhost:3000", "http://localhost:8080"));

    private boolean requireUserVerification = true;

    private boolean preferResidentKey = true;

    private boolean requireAttestationOnEnroll = true;

    private List<String> allowedAttestationFormats = new ArrayList<>(List.of("tpm", "apple", "android-key", "packed"));

    private Set<String> allowedAaguids = new HashSet<>();

    private int sessionTtlMinutes = 5;

    private int passkeySessionTtlMinutes = 30;


    private int passkeyStepUpTtlMinutes = 3;

    public String getRpId() { return rpId; }
    public void setRpId(String rpId) { this.rpId = rpId; }

    public String getRpName() { return rpName; }
    public void setRpName(String rpName) { this.rpName = rpName; }

    public Set<String> getOrigins() { return origins; }
    public void setOrigins(Set<String> origins) { this.origins = origins; }

    public boolean isRequireUserVerification() { return requireUserVerification; }
    public void setRequireUserVerification(boolean requireUserVerification) { this.requireUserVerification = requireUserVerification; }

    public boolean isPreferResidentKey() { return preferResidentKey; }
    public void setPreferResidentKey(boolean preferResidentKey) { this.preferResidentKey = preferResidentKey; }

    public boolean isRequireAttestationOnEnroll() { return requireAttestationOnEnroll; }
    public void setRequireAttestationOnEnroll(boolean requireAttestationOnEnroll) { this.requireAttestationOnEnroll = requireAttestationOnEnroll; }

    public List<String> getAllowedAttestationFormats() { return allowedAttestationFormats; }
    public void setAllowedAttestationFormats(List<String> allowedAttestationFormats) { this.allowedAttestationFormats = allowedAttestationFormats; }

    public Set<String> getAllowedAaguids() { return allowedAaguids; }
    public void setAllowedAaguids(Set<String> allowedAaguids) { this.allowedAaguids = allowedAaguids; }

    public int getSessionTtlMinutes() { return sessionTtlMinutes; }
    public void setSessionTtlMinutes(int sessionTtlMinutes) { this.sessionTtlMinutes = sessionTtlMinutes; }

    public int getPasskeySessionTtlMinutes() { return passkeySessionTtlMinutes; }
    public void setPasskeySessionTtlMinutes(int passkeySessionTtlMinutes) { this.passkeySessionTtlMinutes = passkeySessionTtlMinutes; }

    public int getPasskeyStepUpTtlMinutes() { return passkeyStepUpTtlMinutes; }
    public void setPasskeyStepUpTtlMinutes(int passkeyStepUpTtlMinutes) { this.passkeyStepUpTtlMinutes = passkeyStepUpTtlMinutes; }

}
