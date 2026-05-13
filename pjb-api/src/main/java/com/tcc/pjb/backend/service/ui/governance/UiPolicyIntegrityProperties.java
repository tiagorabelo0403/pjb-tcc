package com.tcc.pjb.backend.service.ui.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pjb.ui.policy.integrity")
public class UiPolicyIntegrityProperties {

  public enum SignatureSource {
    FILE,
    ENV
  }

  public enum SigningKeySource {
    KEY_MATERIAL,
    ENV_EPHEMERAL
  }

  @NotNull
  private boolean requireSignature = false;

  @NotNull
  private boolean enforcePresentationBaseline = false;

  @NotNull
  private SignatureSource signatureSource = SignatureSource.FILE;

  @NotNull
  private SigningKeySource signingKeySource = SigningKeySource.KEY_MATERIAL;

  @NotBlank
  private String signingKeyEnvName = "PJB_UI_POLICY_SIGNING_KEY_B64";

  @NotBlank
  private String accessibilityPolicySignatureEnvName = "PJB_UI_POLICY_SIG_ACCESSIBILITY_POLICY";

  @NotBlank
  private String accessibilityAbacSignatureEnvName = "PJB_UI_POLICY_SIG_ACCESSIBILITY_ABAC";

  @NotNull
  private String accessibilityPolicyLocation = "classpath:ui/accessibility-policy.json";

  @NotNull
  private String accessibilityAbacLocation = "classpath:ui/accessibility-abac.json";

  @NotNull
  private String presentationBaselineLocation = "classpath:ui/presentation-baseline.json";

  public boolean isRequireSignature() {
    return requireSignature;
  }

  public void setRequireSignature(boolean requireSignature) {
    this.requireSignature = requireSignature;
  }

  public boolean isEnforcePresentationBaseline() {
    return enforcePresentationBaseline;
  }

  public void setEnforcePresentationBaseline(boolean enforcePresentationBaseline) {
    this.enforcePresentationBaseline = enforcePresentationBaseline;
  }

  public SignatureSource getSignatureSource() {
    return signatureSource;
  }

  public void setSignatureSource(SignatureSource signatureSource) {
    this.signatureSource = signatureSource;
  }

  public SigningKeySource getSigningKeySource() {
    return signingKeySource;
  }

  public void setSigningKeySource(SigningKeySource signingKeySource) {
    this.signingKeySource = signingKeySource;
  }

  public String getSigningKeyEnvName() {
    return signingKeyEnvName;
  }

  public void setSigningKeyEnvName(String signingKeyEnvName) {
    this.signingKeyEnvName = signingKeyEnvName;
  }

  public String getAccessibilityPolicySignatureEnvName() {
    return accessibilityPolicySignatureEnvName;
  }

  public void setAccessibilityPolicySignatureEnvName(String accessibilityPolicySignatureEnvName) {
    this.accessibilityPolicySignatureEnvName = accessibilityPolicySignatureEnvName;
  }

  public String getAccessibilityAbacSignatureEnvName() {
    return accessibilityAbacSignatureEnvName;
  }

  public void setAccessibilityAbacSignatureEnvName(String accessibilityAbacSignatureEnvName) {
    this.accessibilityAbacSignatureEnvName = accessibilityAbacSignatureEnvName;
  }

  public String getAccessibilityPolicyLocation() {
    return accessibilityPolicyLocation;
  }

  public void setAccessibilityPolicyLocation(String accessibilityPolicyLocation) {
    this.accessibilityPolicyLocation = accessibilityPolicyLocation;
  }

  public String getAccessibilityAbacLocation() {
    return accessibilityAbacLocation;
  }

  public void setAccessibilityAbacLocation(String accessibilityAbacLocation) {
    this.accessibilityAbacLocation = accessibilityAbacLocation;
  }

  public String getPresentationBaselineLocation() {
    return presentationBaselineLocation;
  }

  public void setPresentationBaselineLocation(String presentationBaselineLocation) {
    this.presentationBaselineLocation = presentationBaselineLocation;
  }
}
