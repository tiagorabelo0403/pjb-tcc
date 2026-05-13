package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeviceRegisterRequest {

    @NotBlank
    @Size(max = 512)
    private String credentialId;

    @NotBlank
    @Size(max = 4096)
    private String publicKey;

    @NotBlank
    @Size(max = 80)
    private String alias;

    @Size(max = 64)
    private String aaguid;

    @Size(max = 40)
    private String attestationFmt;

    private boolean attestationTrusted;

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getAaguid() { return aaguid; }
    public void setAaguid(String aaguid) { this.aaguid = aaguid; }

    public String getAttestationFmt() { return attestationFmt; }
    public void setAttestationFmt(String attestationFmt) { this.attestationFmt = attestationFmt; }

    public boolean isAttestationTrusted() { return attestationTrusted; }
    public void setAttestationTrusted(boolean attestationTrusted) { this.attestationTrusted = attestationTrusted; }
}
