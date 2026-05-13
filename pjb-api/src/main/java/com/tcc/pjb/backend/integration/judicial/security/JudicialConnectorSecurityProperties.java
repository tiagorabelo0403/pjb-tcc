package com.tcc.pjb.backend.integration.judicial.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integration.judicial.security")
public class JudicialConnectorSecurityProperties {

    private String environmentName = "default";
    private List<String> defaultProtocols = new ArrayList<>(List.of("TLSv1.3", "TLSv1.2"));
    private List<String> defaultCipherSuites = new ArrayList<>();
    private Duration defaultConnectTimeout = Duration.ofSeconds(10);
    private Duration defaultReadTimeout = Duration.ofSeconds(30);
    private boolean defaultHostnameVerification = true;
    private CertificateValidation certificateValidation = new CertificateValidation();
    private Posture posture = new Posture();
    private Map<String, KeyStoreSource> keyStores = new LinkedHashMap<>();
    private Map<String, TrustStoreSource> trustStores = new LinkedHashMap<>();
    private Map<String, Pkcs11ModuleSource> pkcs11Modules = new LinkedHashMap<>();
    private Map<String, SecurityPack> packs = new LinkedHashMap<>();
    private Map<String, ConnectorBinding> bindings = new LinkedHashMap<>();

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public List<String> getDefaultProtocols() {
        return defaultProtocols;
    }

    public void setDefaultProtocols(List<String> defaultProtocols) {
        this.defaultProtocols = defaultProtocols == null ? new ArrayList<>() : new ArrayList<>(defaultProtocols);
    }

    public List<String> getDefaultCipherSuites() {
        return defaultCipherSuites;
    }

    public void setDefaultCipherSuites(List<String> defaultCipherSuites) {
        this.defaultCipherSuites = defaultCipherSuites == null ? new ArrayList<>() : new ArrayList<>(defaultCipherSuites);
    }

    public Duration getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public void setDefaultConnectTimeout(Duration defaultConnectTimeout) {
        this.defaultConnectTimeout = defaultConnectTimeout;
    }

    public Duration getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public void setDefaultReadTimeout(Duration defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public boolean isDefaultHostnameVerification() {
        return defaultHostnameVerification;
    }

    public void setDefaultHostnameVerification(boolean defaultHostnameVerification) {
        this.defaultHostnameVerification = defaultHostnameVerification;
    }

    public CertificateValidation getCertificateValidation() {
        return certificateValidation;
    }

    public void setCertificateValidation(CertificateValidation certificateValidation) {
        this.certificateValidation = certificateValidation == null ? new CertificateValidation() : certificateValidation;
    }

    public Posture getPosture() {
        return posture;
    }

    public void setPosture(Posture posture) {
        this.posture = posture == null ? new Posture() : posture;
    }

    public Map<String, KeyStoreSource> getKeyStores() {
        return keyStores;
    }

    public void setKeyStores(Map<String, KeyStoreSource> keyStores) {
        this.keyStores = keyStores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keyStores);
    }

    public Map<String, TrustStoreSource> getTrustStores() {
        return trustStores;
    }

    public void setTrustStores(Map<String, TrustStoreSource> trustStores) {
        this.trustStores = trustStores == null ? new LinkedHashMap<>() : new LinkedHashMap<>(trustStores);
    }

    public Map<String, Pkcs11ModuleSource> getPkcs11Modules() {
        return pkcs11Modules;
    }

    public void setPkcs11Modules(Map<String, Pkcs11ModuleSource> pkcs11Modules) {
        this.pkcs11Modules = pkcs11Modules == null ? new LinkedHashMap<>() : new LinkedHashMap<>(pkcs11Modules);
    }

    public Map<String, SecurityPack> getPacks() {
        return packs;
    }

    public void setPacks(Map<String, SecurityPack> packs) {
        this.packs = packs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(packs);
    }

    public Map<String, ConnectorBinding> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, ConnectorBinding> bindings) {
        this.bindings = bindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bindings);
    }

    public static class CertificateValidation {

        private JudicialCertificateRevocationMode revocationMode = JudicialCertificateRevocationMode.SOFT_FAIL;
        private boolean ocspEnabled = true;
        private boolean crlEnabled;
        private boolean preferCrl;
        private Duration minimumRemainingValidity = Duration.ofDays(30);
        private Duration allowedClockSkew = Duration.ofMinutes(5);
        private boolean requireDigitalSignatureKeyUsage = true;
        private boolean requireClientAuthExtendedKeyUsage = true;
        private boolean requireTrustStoreForPathValidation;

        public JudicialCertificateRevocationMode getRevocationMode() {
            return revocationMode;
        }

        public void setRevocationMode(JudicialCertificateRevocationMode revocationMode) {
            this.revocationMode = revocationMode;
        }

        public boolean isOcspEnabled() {
            return ocspEnabled;
        }

        public void setOcspEnabled(boolean ocspEnabled) {
            this.ocspEnabled = ocspEnabled;
        }

        public boolean isCrlEnabled() {
            return crlEnabled;
        }

        public void setCrlEnabled(boolean crlEnabled) {
            this.crlEnabled = crlEnabled;
        }

        public boolean isPreferCrl() {
            return preferCrl;
        }

        public void setPreferCrl(boolean preferCrl) {
            this.preferCrl = preferCrl;
        }

        public Duration getMinimumRemainingValidity() {
            return minimumRemainingValidity;
        }

        public void setMinimumRemainingValidity(Duration minimumRemainingValidity) {
            this.minimumRemainingValidity = minimumRemainingValidity;
        }

        public Duration getAllowedClockSkew() {
            return allowedClockSkew;
        }

        public void setAllowedClockSkew(Duration allowedClockSkew) {
            this.allowedClockSkew = allowedClockSkew;
        }

        public boolean isRequireDigitalSignatureKeyUsage() {
            return requireDigitalSignatureKeyUsage;
        }

        public void setRequireDigitalSignatureKeyUsage(boolean requireDigitalSignatureKeyUsage) {
            this.requireDigitalSignatureKeyUsage = requireDigitalSignatureKeyUsage;
        }

        public boolean isRequireClientAuthExtendedKeyUsage() {
            return requireClientAuthExtendedKeyUsage;
        }

        public void setRequireClientAuthExtendedKeyUsage(boolean requireClientAuthExtendedKeyUsage) {
            this.requireClientAuthExtendedKeyUsage = requireClientAuthExtendedKeyUsage;
        }

        public boolean isRequireTrustStoreForPathValidation() {
            return requireTrustStoreForPathValidation;
        }

        public void setRequireTrustStoreForPathValidation(boolean requireTrustStoreForPathValidation) {
            this.requireTrustStoreForPathValidation = requireTrustStoreForPathValidation;
        }
    }


    public static class Posture {

        private long initialDelayMs = 30000L;
        private long fixedDelayMs = 900000L;

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }

    public static class KeyStoreSource {

        private String type;
        private String provider;
        private String location;
        private String base64;
        private String password;
        private String keyPassword;
        private String alias;
        private String pkcs11Module;
        private String pin;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getPkcs11Module() {
            return pkcs11Module;
        }

        public void setPkcs11Module(String pkcs11Module) {
            this.pkcs11Module = pkcs11Module;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }
    }

    public static class TrustStoreSource {

        private String type;
        private String provider;
        private String location;
        private String base64;
        private String password;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Pkcs11ModuleSource {

        private String name;
        private String library;
        private Integer slot;
        private Integer slotListIndex;
        private String tokenLabel;
        private String attributesMode = "compatibility";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
        }

        public Integer getSlot() {
            return slot;
        }

        public void setSlot(Integer slot) {
            this.slot = slot;
        }

        public Integer getSlotListIndex() {
            return slotListIndex;
        }

        public void setSlotListIndex(Integer slotListIndex) {
            this.slotListIndex = slotListIndex;
        }

        public String getTokenLabel() {
            return tokenLabel;
        }

        public void setTokenLabel(String tokenLabel) {
            this.tokenLabel = tokenLabel;
        }

        public String getAttributesMode() {
            return attributesMode;
        }

        public void setAttributesMode(String attributesMode) {
            this.attributesMode = attributesMode;
        }
    }


    public static class SecurityPack {

        private String system;
        private String tribunalCodigo;
        private String environmentName;
        private boolean enabled = true;
        private JudicialConnectorTlsMode tlsMode;
        private String keyStoreRef;
        private String trustStoreRef;
        private String keyAlias;
        private Boolean requireClientCertificate;
        private Boolean hostnameVerification;
        private List<String> protocols = new ArrayList<>();
        private List<String> cipherSuites = new ArrayList<>();
        private Duration connectTimeout;
        private Duration readTimeout;
        private List<String> allowedHosts = new ArrayList<>();
        private JudicialCertificateRevocationMode revocationMode;
        private Boolean ocspEnabled;
        private Boolean crlEnabled;
        private Boolean preferCrl;
        private Duration minimumRemainingValidity;
        private Duration allowedClockSkew;
        private Boolean requireDigitalSignatureKeyUsage;
        private Boolean requireClientAuthExtendedKeyUsage;
        private Boolean requireTrustStoreForPathValidation;

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getTribunalCodigo() {
            return tribunalCodigo;
        }

        public void setTribunalCodigo(String tribunalCodigo) {
            this.tribunalCodigo = tribunalCodigo;
        }

        public String getEnvironmentName() {
            return environmentName;
        }

        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public JudicialConnectorTlsMode getTlsMode() {
            return tlsMode;
        }

        public void setTlsMode(JudicialConnectorTlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        public String getKeyStoreRef() {
            return keyStoreRef;
        }

        public void setKeyStoreRef(String keyStoreRef) {
            this.keyStoreRef = keyStoreRef;
        }

        public String getTrustStoreRef() {
            return trustStoreRef;
        }

        public void setTrustStoreRef(String trustStoreRef) {
            this.trustStoreRef = trustStoreRef;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public Boolean getRequireClientCertificate() {
            return requireClientCertificate;
        }

        public void setRequireClientCertificate(Boolean requireClientCertificate) {
            this.requireClientCertificate = requireClientCertificate;
        }

        public Boolean getHostnameVerification() {
            return hostnameVerification;
        }

        public void setHostnameVerification(Boolean hostnameVerification) {
            this.hostnameVerification = hostnameVerification;
        }

        public List<String> getProtocols() {
            return protocols;
        }

        public void setProtocols(List<String> protocols) {
            this.protocols = protocols == null ? new ArrayList<>() : new ArrayList<>(protocols);
        }

        public List<String> getCipherSuites() {
            return cipherSuites;
        }

        public void setCipherSuites(List<String> cipherSuites) {
            this.cipherSuites = cipherSuites == null ? new ArrayList<>() : new ArrayList<>(cipherSuites);
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts == null ? new ArrayList<>() : new ArrayList<>(allowedHosts);
        }

        public JudicialCertificateRevocationMode getRevocationMode() {
            return revocationMode;
        }

        public void setRevocationMode(JudicialCertificateRevocationMode revocationMode) {
            this.revocationMode = revocationMode;
        }

        public Boolean getOcspEnabled() {
            return ocspEnabled;
        }

        public void setOcspEnabled(Boolean ocspEnabled) {
            this.ocspEnabled = ocspEnabled;
        }

        public Boolean getCrlEnabled() {
            return crlEnabled;
        }

        public void setCrlEnabled(Boolean crlEnabled) {
            this.crlEnabled = crlEnabled;
        }

        public Boolean getPreferCrl() {
            return preferCrl;
        }

        public void setPreferCrl(Boolean preferCrl) {
            this.preferCrl = preferCrl;
        }

        public Duration getMinimumRemainingValidity() {
            return minimumRemainingValidity;
        }

        public void setMinimumRemainingValidity(Duration minimumRemainingValidity) {
            this.minimumRemainingValidity = minimumRemainingValidity;
        }

        public Duration getAllowedClockSkew() {
            return allowedClockSkew;
        }

        public void setAllowedClockSkew(Duration allowedClockSkew) {
            this.allowedClockSkew = allowedClockSkew;
        }

        public Boolean getRequireDigitalSignatureKeyUsage() {
            return requireDigitalSignatureKeyUsage;
        }

        public void setRequireDigitalSignatureKeyUsage(Boolean requireDigitalSignatureKeyUsage) {
            this.requireDigitalSignatureKeyUsage = requireDigitalSignatureKeyUsage;
        }

        public Boolean getRequireClientAuthExtendedKeyUsage() {
            return requireClientAuthExtendedKeyUsage;
        }

        public void setRequireClientAuthExtendedKeyUsage(Boolean requireClientAuthExtendedKeyUsage) {
            this.requireClientAuthExtendedKeyUsage = requireClientAuthExtendedKeyUsage;
        }

        public Boolean getRequireTrustStoreForPathValidation() {
            return requireTrustStoreForPathValidation;
        }

        public void setRequireTrustStoreForPathValidation(Boolean requireTrustStoreForPathValidation) {
            this.requireTrustStoreForPathValidation = requireTrustStoreForPathValidation;
        }
    }

    public static class ConnectorBinding {

        private String system;
        private String tribunalCodigo;
        private String environmentName;
        private boolean enabled = true;
        private JudicialConnectorTlsMode tlsMode = JudicialConnectorTlsMode.TLS;
        private String keyStoreRef;
        private String trustStoreRef;
        private String keyAlias;
        private String certificateAlias;
        private boolean requireClientCertificate;
        private boolean hostnameVerification = true;
        private List<String> protocols = new ArrayList<>();
        private List<String> cipherSuites = new ArrayList<>();
        private Duration connectTimeout;
        private Duration readTimeout;
        private List<String> allowedHosts = new ArrayList<>();

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getTribunalCodigo() {
            return tribunalCodigo;
        }

        public void setTribunalCodigo(String tribunalCodigo) {
            this.tribunalCodigo = tribunalCodigo;
        }

        public String getEnvironmentName() {
            return environmentName;
        }

        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public JudicialConnectorTlsMode getTlsMode() {
            return tlsMode;
        }

        public void setTlsMode(JudicialConnectorTlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        public String getKeyStoreRef() {
            return keyStoreRef;
        }

        public void setKeyStoreRef(String keyStoreRef) {
            this.keyStoreRef = keyStoreRef;
        }

        public String getTrustStoreRef() {
            return trustStoreRef;
        }

        public void setTrustStoreRef(String trustStoreRef) {
            this.trustStoreRef = trustStoreRef;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getCertificateAlias() {
            return certificateAlias;
        }

        public void setCertificateAlias(String certificateAlias) {
            this.certificateAlias = certificateAlias;
        }

        public boolean isRequireClientCertificate() {
            return requireClientCertificate;
        }

        public void setRequireClientCertificate(boolean requireClientCertificate) {
            this.requireClientCertificate = requireClientCertificate;
        }

        public boolean isHostnameVerification() {
            return hostnameVerification;
        }

        public void setHostnameVerification(boolean hostnameVerification) {
            this.hostnameVerification = hostnameVerification;
        }

        public List<String> getProtocols() {
            return protocols;
        }

        public void setProtocols(List<String> protocols) {
            this.protocols = protocols == null ? new ArrayList<>() : new ArrayList<>(protocols);
        }

        public List<String> getCipherSuites() {
            return cipherSuites;
        }

        public void setCipherSuites(List<String> cipherSuites) {
            this.cipherSuites = cipherSuites == null ? new ArrayList<>() : new ArrayList<>(cipherSuites);
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts == null ? new ArrayList<>() : new ArrayList<>(allowedHosts);
        }
    }
}
