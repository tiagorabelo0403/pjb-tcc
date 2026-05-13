package com.tcc.pjb.backend.integration.judicial;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integration.judicial")
public class JudicialIntegrationProperties {

    private Connector pje = new Connector();
    private Connector esaj = new Connector();
    private Connector projudi = new Connector();
    private Connector eproc = new Connector();
    private Connector creta = new Connector();
    private Connector pdpj = new Connector();
    private Connector mni = new Connector();
    private Connector mp = new Connector();

    public Connector getPje() {
        return pje;
    }

    public void setPje(Connector pje) {
        this.pje = pje;
    }

    public Connector getEsaj() {
        return esaj;
    }

    public void setEsaj(Connector esaj) {
        this.esaj = esaj;
    }

    public Connector getProjudi() {
        return projudi;
    }

    public void setProjudi(Connector projudi) {
        this.projudi = projudi;
    }

    public Connector getEproc() {
        return eproc;
    }

    public void setEproc(Connector eproc) {
        this.eproc = eproc;
    }

    public Connector getCreta() {
        return creta;
    }

    public void setCreta(Connector creta) {
        this.creta = creta;
    }

    public Connector getPdpj() {
        return pdpj;
    }

    public void setPdpj(Connector pdpj) {
        this.pdpj = pdpj;
    }

    public Connector getMni() {
        return mni;
    }

    public void setMni(Connector mni) {
        this.mni = mni;
    }

    public Connector getMp() {
        return mp;
    }

    public void setMp(Connector mp) {
        this.mp = mp;
    }

    public Connector connectorFor(JudicialSystem system) {
        if (system == null) {
            return new Connector();
        }
        return switch (system) {
            case PJE -> pje;
            case ESAJ -> esaj;
            case PROJUDI -> projudi;
            case EPROC -> eproc;
            case CRETA -> creta;
            case PDPJ -> pdpj;
            case MNI -> mni;
            case MP -> mp;
            case OUTRO -> new Connector();
        };
    }

    public static class Connector {
        private boolean enabled = false;
        private String baseUrl;
        private boolean requiresStepUpGovBr = false;
        private boolean requiresCertificate = false;
        private boolean supportsDryRun = true;
        private boolean supportsSnapshotSync = true;
        private boolean supportsEventSync = true;
        private boolean supportsExternalMedia = true;
        private boolean authRequired = false;
        private String bearerToken;
        private String apiKey;
        private String apiKeyHeader = "X-API-Key";
        private String basicUsername;
        private String basicPassword;
        private String oauthTokenUrl;
        private String oauthClientId;
        private String oauthClientSecret;
        private String oauthAudience;
        private String oauthScope;
        private String certificateAlias;
        private String snapshotPath;
        private String eventsPath;
        private String dryRunPath;
        private String submitPath;
        private boolean productionReady = false;
        private java.util.List<String> homologatedTribunals = new java.util.ArrayList<>();
        private java.util.List<String> blockedTribunals = new java.util.ArrayList<>();
        private Map<String, String> tribunalSubmitPaths = new LinkedHashMap<>();
        private Map<String, String> tribunalDryRunPaths = new LinkedHashMap<>();
        private Map<String, String> tribunalSnapshotPaths = new LinkedHashMap<>();
        private Map<String, String> tribunalEventsPaths = new LinkedHashMap<>();
        private Map<String, String> staticHeaders = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isRequiresStepUpGovBr() {
            return requiresStepUpGovBr;
        }

        public void setRequiresStepUpGovBr(boolean requiresStepUpGovBr) {
            this.requiresStepUpGovBr = requiresStepUpGovBr;
        }

        public boolean isRequiresCertificate() {
            return requiresCertificate;
        }

        public void setRequiresCertificate(boolean requiresCertificate) {
            this.requiresCertificate = requiresCertificate;
        }

        public boolean isSupportsDryRun() {
            return supportsDryRun;
        }

        public void setSupportsDryRun(boolean supportsDryRun) {
            this.supportsDryRun = supportsDryRun;
        }

        public boolean isSupportsSnapshotSync() {
            return supportsSnapshotSync;
        }

        public void setSupportsSnapshotSync(boolean supportsSnapshotSync) {
            this.supportsSnapshotSync = supportsSnapshotSync;
        }

        public boolean isSupportsEventSync() {
            return supportsEventSync;
        }

        public void setSupportsEventSync(boolean supportsEventSync) {
            this.supportsEventSync = supportsEventSync;
        }

        public boolean isSupportsExternalMedia() {
            return supportsExternalMedia;
        }

        public void setSupportsExternalMedia(boolean supportsExternalMedia) {
            this.supportsExternalMedia = supportsExternalMedia;
        }

        public boolean isAuthRequired() {
            return authRequired;
        }

        public void setAuthRequired(boolean authRequired) {
            this.authRequired = authRequired;
        }

        public String getBearerToken() {
            return bearerToken;
        }

        public void setBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
        }

        public String getBasicUsername() {
            return basicUsername;
        }

        public void setBasicUsername(String basicUsername) {
            this.basicUsername = basicUsername;
        }

        public String getBasicPassword() {
            return basicPassword;
        }

        public void setBasicPassword(String basicPassword) {
            this.basicPassword = basicPassword;
        }

        public String getOauthTokenUrl() {
            return oauthTokenUrl;
        }

        public void setOauthTokenUrl(String oauthTokenUrl) {
            this.oauthTokenUrl = oauthTokenUrl;
        }

        public String getOauthClientId() {
            return oauthClientId;
        }

        public void setOauthClientId(String oauthClientId) {
            this.oauthClientId = oauthClientId;
        }

        public String getOauthClientSecret() {
            return oauthClientSecret;
        }

        public void setOauthClientSecret(String oauthClientSecret) {
            this.oauthClientSecret = oauthClientSecret;
        }

        public String getOauthAudience() {
            return oauthAudience;
        }

        public void setOauthAudience(String oauthAudience) {
            this.oauthAudience = oauthAudience;
        }

        public String getOauthScope() {
            return oauthScope;
        }

        public void setOauthScope(String oauthScope) {
            this.oauthScope = oauthScope;
        }

        public String getCertificateAlias() {
            return certificateAlias;
        }

        public void setCertificateAlias(String certificateAlias) {
            this.certificateAlias = certificateAlias;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }

        public String getEventsPath() {
            return eventsPath;
        }

        public void setEventsPath(String eventsPath) {
            this.eventsPath = eventsPath;
        }

        public String getDryRunPath() {
            return dryRunPath;
        }

        public void setDryRunPath(String dryRunPath) {
            this.dryRunPath = dryRunPath;
        }

        public String getSubmitPath() {
            return submitPath;
        }

        public void setSubmitPath(String submitPath) {
            this.submitPath = submitPath;
        }

        public boolean isProductionReady() {
            return productionReady;
        }

        public void setProductionReady(boolean productionReady) {
            this.productionReady = productionReady;
        }

        public java.util.List<String> getHomologatedTribunals() {
            return homologatedTribunals;
        }

        public void setHomologatedTribunals(java.util.List<String> homologatedTribunals) {
            this.homologatedTribunals = homologatedTribunals == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(homologatedTribunals);
        }

        public java.util.List<String> getBlockedTribunals() {
            return blockedTribunals;
        }

        public void setBlockedTribunals(java.util.List<String> blockedTribunals) {
            this.blockedTribunals = blockedTribunals == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(blockedTribunals);
        }

        public Map<String, String> getTribunalSubmitPaths() {
            return tribunalSubmitPaths;
        }

        public void setTribunalSubmitPaths(Map<String, String> tribunalSubmitPaths) {
            this.tribunalSubmitPaths = tribunalSubmitPaths == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tribunalSubmitPaths);
        }

        public Map<String, String> getTribunalDryRunPaths() {
            return tribunalDryRunPaths;
        }

        public void setTribunalDryRunPaths(Map<String, String> tribunalDryRunPaths) {
            this.tribunalDryRunPaths = tribunalDryRunPaths == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tribunalDryRunPaths);
        }

        public Map<String, String> getTribunalSnapshotPaths() {
            return tribunalSnapshotPaths;
        }

        public void setTribunalSnapshotPaths(Map<String, String> tribunalSnapshotPaths) {
            this.tribunalSnapshotPaths = tribunalSnapshotPaths == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tribunalSnapshotPaths);
        }

        public Map<String, String> getTribunalEventsPaths() {
            return tribunalEventsPaths;
        }

        public void setTribunalEventsPaths(Map<String, String> tribunalEventsPaths) {
            this.tribunalEventsPaths = tribunalEventsPaths == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tribunalEventsPaths);
        }

        public Map<String, String> getStaticHeaders() {
            return staticHeaders;
        }

        public void setStaticHeaders(Map<String, String> staticHeaders) {
            this.staticHeaders = staticHeaders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(staticHeaders);
        }
    }
}
