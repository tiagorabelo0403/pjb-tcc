package com.tcc.pjb.backend.core.security.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.device")
public class DeviceSecurityProperties {

    public static final class ActionRule {
        private String id;
        private String pattern;
        private List<String> methods = new ArrayList<>();
        private String action;
        private int order;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }

    public static final class ActionPolicy {
        private Boolean deviceRequired;
        private Boolean verifiedDeviceRequired;
        private Boolean attestationTrustedRequired;
        private Boolean advogadoBaptismRequired;
        private Boolean passkeyRequiredForAdvogado;
        private Boolean passkeyRequiredForAdmin;
        private Boolean allowReadDuringQuarantine;
        private Boolean allowWriteDuringQuarantine;
        private Boolean justificativaRequired;
        private Boolean stepUpRequired;
        private Integer strongAuthMaxAgeSeconds;
        private Boolean bindStrongAuthToDevice;
        private Boolean oneTimeStepUp;
        private Boolean dualApprovalRequired;
        private Integer dualApprovalTtlSeconds;
        private Boolean auditRequired;
        private Boolean govBrRequired;
        private Integer govBrMaxAgeSeconds;

        public Boolean getDeviceRequired() { return deviceRequired; }
        public void setDeviceRequired(Boolean v) { this.deviceRequired = v; }
        public Boolean getVerifiedDeviceRequired() { return verifiedDeviceRequired; }
        public void setVerifiedDeviceRequired(Boolean v) { this.verifiedDeviceRequired = v; }
        public Boolean getAttestationTrustedRequired() { return attestationTrustedRequired; }
        public void setAttestationTrustedRequired(Boolean v) { this.attestationTrustedRequired = v; }
        public Boolean getAdvogadoBaptismRequired() { return advogadoBaptismRequired; }
        public void setAdvogadoBaptismRequired(Boolean v) { this.advogadoBaptismRequired = v; }
        public Boolean getPasskeyRequiredForAdvogado() { return passkeyRequiredForAdvogado; }
        public void setPasskeyRequiredForAdvogado(Boolean v) { this.passkeyRequiredForAdvogado = v; }
        public Boolean getPasskeyRequiredForAdmin() { return passkeyRequiredForAdmin; }
        public void setPasskeyRequiredForAdmin(Boolean v) { this.passkeyRequiredForAdmin = v; }
        public Boolean getAllowReadDuringQuarantine() { return allowReadDuringQuarantine; }
        public void setAllowReadDuringQuarantine(Boolean v) { this.allowReadDuringQuarantine = v; }
        public Boolean getAllowWriteDuringQuarantine() { return allowWriteDuringQuarantine; }
        public void setAllowWriteDuringQuarantine(Boolean v) { this.allowWriteDuringQuarantine = v; }
        public Boolean getJustificativaRequired() { return justificativaRequired; }
        public void setJustificativaRequired(Boolean v) { this.justificativaRequired = v; }
        public Boolean getStepUpRequired() { return stepUpRequired; }
        public void setStepUpRequired(Boolean v) { this.stepUpRequired = v; }
        public Integer getStrongAuthMaxAgeSeconds() { return strongAuthMaxAgeSeconds; }
        public void setStrongAuthMaxAgeSeconds(Integer v) { this.strongAuthMaxAgeSeconds = v; }
        public Boolean getBindStrongAuthToDevice() { return bindStrongAuthToDevice; }
        public void setBindStrongAuthToDevice(Boolean v) { this.bindStrongAuthToDevice = v; }
        public Boolean getOneTimeStepUp() { return oneTimeStepUp; }
        public void setOneTimeStepUp(Boolean v) { this.oneTimeStepUp = v; }
        public Boolean getDualApprovalRequired() { return dualApprovalRequired; }
        public void setDualApprovalRequired(Boolean v) { this.dualApprovalRequired = v; }
        public Integer getDualApprovalTtlSeconds() { return dualApprovalTtlSeconds; }
        public void setDualApprovalTtlSeconds(Integer v) { this.dualApprovalTtlSeconds = v; }
        public Boolean getAuditRequired() { return auditRequired; }
        public void setAuditRequired(Boolean v) { this.auditRequired = v; }
        public Boolean getGovBrRequired() { return govBrRequired; }
        public void setGovBrRequired(Boolean v) { this.govBrRequired = v; }
        public Integer getGovBrMaxAgeSeconds() { return govBrMaxAgeSeconds; }
        public void setGovBrMaxAgeSeconds(Integer v) { this.govBrMaxAgeSeconds = v; }
    }

    private boolean enabled = true;
    private int quarantineHours = 24;
    private boolean denyPrivateIps = false;
    private boolean requireVerifiedDevice = true;
    private boolean requireAttestationTrusted = true;
    private boolean requireAdvogadoBaptism = true;
    private boolean enforceOnSensitiveReads = true;
    private boolean quarantineReadOnlyEnabled = true;
    private boolean denySensitiveReadsDuringQuarantine = true;
    private boolean requireVerifiedForSensitiveRead = true;
    private boolean requireVerifiedForHighRiskWrite = true;
    private boolean requireAttestationForHighRiskWrite = true;
    private int otpMaxAttempts = 5;
    private int otpLockMinutes = 15;
    private int panicDefaultFreezeMinutes = 1440;
    private int restrictedDownloadMaxPerHour = 30;
    private long restrictedDownloadMaxBytesPerHour = 104857600L;
    private int restrictedDownloadMaxPerHourPerProcess = 20;
    private long restrictedDownloadMaxBytesPerHourPerProcess = 52428800L;
    private int restrictedDownloadMaxPerHourPerDocument = 12;
    private long restrictedDownloadMaxBytesPerHourPerDocument = 26214400L;
    private boolean requireBodyHashForOneTimeStepUpWrites = true;
    private int bodyHashMaxBytes = 65536;
    private boolean watermarkEnabled = true;
    private long watermarkMaxBytes = 26214400L;
    private boolean policyDocumentEnabled = true;
    private String policyDocumentSource = "classpath:policies/device-security-policy.json";
    private boolean policyDocumentRequireSignature = false;
    private String policyDocumentHmacKey;
    private int policyDocumentReloadIntervalSeconds = 0;
    private int dualApprovalDefaultTtlSeconds = 900;

    private List<String> sensitiveReadPaths = new ArrayList<>(List.of(
            "/api/v1/documents/**",
            "/api/v1/pdf/**",
            "/api/v1/attachments/**",
            "/api/processos/download**"
    ));

    private List<String> highRiskWritePaths = new ArrayList<>(List.of(
            "/api/v1/judge/**",
            "/api/v1/admin/**",
            "/api/v1/processos/*/sentenca",
            "/api/v1/processos/*/despacho"
    ));

    private List<ActionRule> defaultActionRules = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getQuarantineHours() { return quarantineHours; }
    public void setQuarantineHours(int quarantineHours) { this.quarantineHours = quarantineHours; }

    public boolean isDenyPrivateIps() { return denyPrivateIps; }
    public void setDenyPrivateIps(boolean denyPrivateIps) { this.denyPrivateIps = denyPrivateIps; }

    public boolean isRequireVerifiedDevice() { return requireVerifiedDevice; }
    public void setRequireVerifiedDevice(boolean requireVerifiedDevice) { this.requireVerifiedDevice = requireVerifiedDevice; }

    public boolean isRequireAttestationTrusted() { return requireAttestationTrusted; }
    public void setRequireAttestationTrusted(boolean requireAttestationTrusted) { this.requireAttestationTrusted = requireAttestationTrusted; }

    public boolean isRequireAdvogadoBaptism() { return requireAdvogadoBaptism; }
    public void setRequireAdvogadoBaptism(boolean requireAdvogadoBaptism) { this.requireAdvogadoBaptism = requireAdvogadoBaptism; }

    public boolean isEnforceOnSensitiveReads() { return enforceOnSensitiveReads; }
    public void setEnforceOnSensitiveReads(boolean enforceOnSensitiveReads) { this.enforceOnSensitiveReads = enforceOnSensitiveReads; }

    public boolean isQuarantineReadOnlyEnabled() { return quarantineReadOnlyEnabled; }
    public void setQuarantineReadOnlyEnabled(boolean quarantineReadOnlyEnabled) { this.quarantineReadOnlyEnabled = quarantineReadOnlyEnabled; }

    public boolean isDenySensitiveReadsDuringQuarantine() { return denySensitiveReadsDuringQuarantine; }
    public void setDenySensitiveReadsDuringQuarantine(boolean denySensitiveReadsDuringQuarantine) { this.denySensitiveReadsDuringQuarantine = denySensitiveReadsDuringQuarantine; }

    public boolean isRequireVerifiedForSensitiveRead() { return requireVerifiedForSensitiveRead; }
    public void setRequireVerifiedForSensitiveRead(boolean requireVerifiedForSensitiveRead) { this.requireVerifiedForSensitiveRead = requireVerifiedForSensitiveRead; }

    public boolean isRequireVerifiedForHighRiskWrite() { return requireVerifiedForHighRiskWrite; }
    public void setRequireVerifiedForHighRiskWrite(boolean requireVerifiedForHighRiskWrite) { this.requireVerifiedForHighRiskWrite = requireVerifiedForHighRiskWrite; }

    public boolean isRequireAttestationForHighRiskWrite() { return requireAttestationForHighRiskWrite; }
    public void setRequireAttestationForHighRiskWrite(boolean requireAttestationForHighRiskWrite) { this.requireAttestationForHighRiskWrite = requireAttestationForHighRiskWrite; }

    public int getOtpMaxAttempts() { return otpMaxAttempts; }
    public void setOtpMaxAttempts(int otpMaxAttempts) { this.otpMaxAttempts = otpMaxAttempts; }

    public int getOtpLockMinutes() { return otpLockMinutes; }
    public void setOtpLockMinutes(int otpLockMinutes) { this.otpLockMinutes = otpLockMinutes; }

    public int getPanicDefaultFreezeMinutes() { return panicDefaultFreezeMinutes; }
    public void setPanicDefaultFreezeMinutes(int panicDefaultFreezeMinutes) { this.panicDefaultFreezeMinutes = panicDefaultFreezeMinutes; }

    public int getRestrictedDownloadMaxPerHour() { return restrictedDownloadMaxPerHour; }
    public void setRestrictedDownloadMaxPerHour(int restrictedDownloadMaxPerHour) { this.restrictedDownloadMaxPerHour = restrictedDownloadMaxPerHour; }

    public long getRestrictedDownloadMaxBytesPerHour() { return restrictedDownloadMaxBytesPerHour; }
    public void setRestrictedDownloadMaxBytesPerHour(long restrictedDownloadMaxBytesPerHour) { this.restrictedDownloadMaxBytesPerHour = restrictedDownloadMaxBytesPerHour; }

    public int getRestrictedDownloadMaxPerHourPerProcess() { return restrictedDownloadMaxPerHourPerProcess; }
    public void setRestrictedDownloadMaxPerHourPerProcess(int v) { this.restrictedDownloadMaxPerHourPerProcess = v; }

    public long getRestrictedDownloadMaxBytesPerHourPerProcess() { return restrictedDownloadMaxBytesPerHourPerProcess; }
    public void setRestrictedDownloadMaxBytesPerHourPerProcess(long v) { this.restrictedDownloadMaxBytesPerHourPerProcess = v; }

    public int getRestrictedDownloadMaxPerHourPerDocument() { return restrictedDownloadMaxPerHourPerDocument; }
    public void setRestrictedDownloadMaxPerHourPerDocument(int v) { this.restrictedDownloadMaxPerHourPerDocument = v; }

    public long getRestrictedDownloadMaxBytesPerHourPerDocument() { return restrictedDownloadMaxBytesPerHourPerDocument; }
    public void setRestrictedDownloadMaxBytesPerHourPerDocument(long v) { this.restrictedDownloadMaxBytesPerHourPerDocument = v; }

    public boolean isRequireBodyHashForOneTimeStepUpWrites() { return requireBodyHashForOneTimeStepUpWrites; }
    public void setRequireBodyHashForOneTimeStepUpWrites(boolean requireBodyHashForOneTimeStepUpWrites) { this.requireBodyHashForOneTimeStepUpWrites = requireBodyHashForOneTimeStepUpWrites; }

    public int getBodyHashMaxBytes() { return bodyHashMaxBytes; }
    public void setBodyHashMaxBytes(int bodyHashMaxBytes) { this.bodyHashMaxBytes = bodyHashMaxBytes; }

    public boolean isWatermarkEnabled() { return watermarkEnabled; }
    public void setWatermarkEnabled(boolean watermarkEnabled) { this.watermarkEnabled = watermarkEnabled; }

    public long getWatermarkMaxBytes() { return watermarkMaxBytes; }
    public void setWatermarkMaxBytes(long watermarkMaxBytes) { this.watermarkMaxBytes = watermarkMaxBytes; }

    public boolean isPolicyDocumentEnabled() { return policyDocumentEnabled; }
    public void setPolicyDocumentEnabled(boolean policyDocumentEnabled) { this.policyDocumentEnabled = policyDocumentEnabled; }

    public String getPolicyDocumentSource() { return policyDocumentSource; }
    public void setPolicyDocumentSource(String policyDocumentSource) { this.policyDocumentSource = policyDocumentSource; }

    public boolean isPolicyDocumentRequireSignature() { return policyDocumentRequireSignature; }
    public void setPolicyDocumentRequireSignature(boolean v) { this.policyDocumentRequireSignature = v; }

    public String getPolicyDocumentHmacKey() { return policyDocumentHmacKey; }
    public void setPolicyDocumentHmacKey(String policyDocumentHmacKey) { this.policyDocumentHmacKey = policyDocumentHmacKey; }

    public int getPolicyDocumentReloadIntervalSeconds() { return policyDocumentReloadIntervalSeconds; }
    public void setPolicyDocumentReloadIntervalSeconds(int v) { this.policyDocumentReloadIntervalSeconds = v; }

    public int getDualApprovalDefaultTtlSeconds() { return dualApprovalDefaultTtlSeconds; }
    public void setDualApprovalDefaultTtlSeconds(int dualApprovalDefaultTtlSeconds) { this.dualApprovalDefaultTtlSeconds = dualApprovalDefaultTtlSeconds; }

    public List<String> getSensitiveReadPaths() { return List.copyOf(sensitiveReadPaths); }
    public void setSensitiveReadPaths(List<String> sensitiveReadPaths) { this.sensitiveReadPaths = sensitiveReadPaths == null ? new ArrayList<>() : new ArrayList<>(sensitiveReadPaths); }

    public List<String> getHighRiskWritePaths() { return List.copyOf(highRiskWritePaths); }
    public void setHighRiskWritePaths(List<String> highRiskWritePaths) { this.highRiskWritePaths = highRiskWritePaths == null ? new ArrayList<>() : new ArrayList<>(highRiskWritePaths); }

    public List<ActionRule> getDefaultActionRules() { return List.copyOf(defaultActionRules); }
    public void setDefaultActionRules(List<ActionRule> defaultActionRules) { this.defaultActionRules = defaultActionRules == null ? new ArrayList<>() : new ArrayList<>(defaultActionRules); }

    public boolean hasSensitiveReadPaths() { return !sensitiveReadPaths.isEmpty(); }

    public boolean hasHighRiskWritePaths() { return !highRiskWritePaths.isEmpty(); }


    public List<String> getExemptPaths() { return List.of(); }

    public List<String> getSensitivePaths() { return getSensitiveReadPaths(); }

    public List<ActionRule> getActionRules() { return getDefaultActionRules(); }

    public List<String> getReadMethods() { return List.of("GET", "HEAD"); }

    public List<String> getEnforceMethods() { return List.of("POST", "PUT", "PATCH", "DELETE"); }

    public Map<String, ActionPolicy> getActionPolicies() { return Map.of(); }

    public Map<String, Object> pathPolicySnapshot() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("sensitiveReadCount", sensitiveReadPaths.size());
        out.put("highRiskWriteCount", highRiskWritePaths.size());
        out.put("sensitiveReadPaths", List.copyOf(sensitiveReadPaths));
        out.put("highRiskWritePaths", List.copyOf(highRiskWritePaths));
        return Collections.unmodifiableMap(out);
    }
}
