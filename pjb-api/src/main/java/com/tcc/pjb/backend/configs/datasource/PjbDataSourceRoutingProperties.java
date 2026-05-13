package com.tcc.pjb.backend.configs.datasource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.datasource.routing")
public class PjbDataSourceRoutingProperties {

    private boolean enabled;
    private boolean strict = true;
    private boolean verifyTopologyOnStartup = true;
    private Duration readYourWritesWindow = Duration.ofSeconds(5);
    private final Replica replica = new Replica();
    private final Map<String, Replica> regionalReplicas = new LinkedHashMap<>();
    private final RegionalSelection regionalSelection = new RegionalSelection();
    private final Observability observability = new Observability();
    private final Propagation propagation = new Propagation();
    private final AdaptivePlane adaptivePlane = new AdaptivePlane();
    private final SovereignFallback sovereignFallback = new SovereignFallback();
    private final ProcessualReadModels processualReadModels = new ProcessualReadModels();
    private final WriteFailover writeFailover = new WriteFailover();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isVerifyTopologyOnStartup() {
        return verifyTopologyOnStartup;
    }

    public void setVerifyTopologyOnStartup(boolean verifyTopologyOnStartup) {
        this.verifyTopologyOnStartup = verifyTopologyOnStartup;
    }

    public Duration getReadYourWritesWindow() {
        return readYourWritesWindow;
    }

    public void setReadYourWritesWindow(Duration readYourWritesWindow) {
        this.readYourWritesWindow = readYourWritesWindow;
    }

    public Replica getReplica() {
        return replica;
    }

    public Map<String, Replica> getRegionalReplicas() {
        return regionalReplicas;
    }

    public RegionalSelection getRegionalSelection() {
        return regionalSelection;
    }

    public Observability getObservability() {
        return observability;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public AdaptivePlane getAdaptivePlane() {
        return adaptivePlane;
    }

    public SovereignFallback getSovereignFallback() {
        return sovereignFallback;
    }

    public ProcessualReadModels getProcessualReadModels() {
        return processualReadModels;
    }

    public WriteFailover getWriteFailover() {
        return writeFailover;
    }

    public static final class Replica {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private boolean fallbackToWriteOnError = true;
        private Duration failureCooldown = Duration.ofSeconds(30);
        private final Hikari hikari = new Hikari();
        private final Map<String, String> dataSourceProperties = new LinkedHashMap<>();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public boolean isFallbackToWriteOnError() {
            return fallbackToWriteOnError;
        }

        public void setFallbackToWriteOnError(boolean fallbackToWriteOnError) {
            this.fallbackToWriteOnError = fallbackToWriteOnError;
        }

        public Duration getFailureCooldown() {
            return failureCooldown;
        }

        public void setFailureCooldown(Duration failureCooldown) {
            this.failureCooldown = failureCooldown;
        }

        public Hikari getHikari() {
            return hikari;
        }

        public Map<String, String> getDataSourceProperties() {
            return dataSourceProperties;
        }
    }


    public static final class SovereignFallback {
        private boolean enabled = true;
        private boolean forcePrimaryOnCriticalExhaustion = true;
        private Duration regionalReplicaLagTolerance = Duration.ofSeconds(4);
        private final Map<String, String> tribunalToFallbackReplica = new LinkedHashMap<>();
        private final Map<String, String> ufToFallbackReplica = new LinkedHashMap<>();
        private final Map<String, String> scopeToFallbackReplica = new LinkedHashMap<>();

        public SovereignFallback() {
            tribunalToFallbackReplica.put("STJ", "SUPERIOR");
            tribunalToFallbackReplica.put("STF", "SUPERIOR");
            tribunalToFallbackReplica.put("TSE", "SUPERIOR");
            tribunalToFallbackReplica.put("TST", "SUPERIOR");
            tribunalToFallbackReplica.put("STM", "SUPERIOR");
            tribunalToFallbackReplica.put("CNJ", "SUPERIOR");
            scopeToFallbackReplica.put("NORTE", "CENTRO_OESTE");
            scopeToFallbackReplica.put("NORDESTE", "SUDESTE");
            scopeToFallbackReplica.put("CENTRO_OESTE", "SUDESTE");
            scopeToFallbackReplica.put("SUL", "SUDESTE");
            scopeToFallbackReplica.put("SUDESTE", "READ");
            scopeToFallbackReplica.put("SUPERIOR", "SUPERIOR");
            ufToFallbackReplica.put("DF", "SUPERIOR");
            ufToFallbackReplica.put("SP", "SUDESTE");
            ufToFallbackReplica.put("RJ", "SUDESTE");
            ufToFallbackReplica.put("MG", "SUDESTE");
            ufToFallbackReplica.put("PR", "SUL");
            ufToFallbackReplica.put("RS", "SUL");
            ufToFallbackReplica.put("SC", "SUL");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isForcePrimaryOnCriticalExhaustion() {
            return forcePrimaryOnCriticalExhaustion;
        }

        public void setForcePrimaryOnCriticalExhaustion(boolean forcePrimaryOnCriticalExhaustion) {
            this.forcePrimaryOnCriticalExhaustion = forcePrimaryOnCriticalExhaustion;
        }

        public Duration getRegionalReplicaLagTolerance() {
            return regionalReplicaLagTolerance;
        }

        public void setRegionalReplicaLagTolerance(Duration regionalReplicaLagTolerance) {
            this.regionalReplicaLagTolerance = regionalReplicaLagTolerance;
        }

        public Map<String, String> getTribunalToFallbackReplica() {
            return tribunalToFallbackReplica;
        }

        public Map<String, String> getUfToFallbackReplica() {
            return ufToFallbackReplica;
        }

        public Map<String, String> getScopeToFallbackReplica() {
            return scopeToFallbackReplica;
        }
    }

    public static final class ProcessualReadModels {
        private boolean enabled = true;
        private boolean emitResponseHeaders = true;
        private boolean persistenceEnabled = true;
        private String kafkaTopic = "pjb.processual.read-models.v1";
        private int recompositionBatchSize = 25;
        private int recompositionMaxAttempts = 5;
        private long recompositionPollMs = 20_000L;
        private Duration recompositionRetryDelay = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEmitResponseHeaders() {
            return emitResponseHeaders;
        }

        public void setEmitResponseHeaders(boolean emitResponseHeaders) {
            this.emitResponseHeaders = emitResponseHeaders;
        }

        public boolean isPersistenceEnabled() {
            return persistenceEnabled;
        }

        public void setPersistenceEnabled(boolean persistenceEnabled) {
            this.persistenceEnabled = persistenceEnabled;
        }

        public String getKafkaTopic() {
            return kafkaTopic;
        }

        public void setKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = kafkaTopic;
        }

        public int getRecompositionBatchSize() {
            return recompositionBatchSize;
        }

        public void setRecompositionBatchSize(int recompositionBatchSize) {
            this.recompositionBatchSize = recompositionBatchSize;
        }

        public int getRecompositionMaxAttempts() {
            return recompositionMaxAttempts;
        }

        public void setRecompositionMaxAttempts(int recompositionMaxAttempts) {
            this.recompositionMaxAttempts = recompositionMaxAttempts;
        }

        public long getRecompositionPollMs() {
            return recompositionPollMs;
        }

        public void setRecompositionPollMs(long recompositionPollMs) {
            this.recompositionPollMs = recompositionPollMs;
        }

        public Duration getRecompositionRetryDelay() {
            return recompositionRetryDelay;
        }

        public void setRecompositionRetryDelay(Duration recompositionRetryDelay) {
            this.recompositionRetryDelay = recompositionRetryDelay;
        }
    }


    public static final class WriteFailover {
        private boolean enabled;
        private boolean strict;
        private Duration failureCooldown = Duration.ofSeconds(10);
        private Duration successStickiness = Duration.ofSeconds(20);
        private final Map<String, Replica> candidates = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        public Duration getFailureCooldown() {
            return failureCooldown;
        }

        public void setFailureCooldown(Duration failureCooldown) {
            this.failureCooldown = failureCooldown;
        }

        public Duration getSuccessStickiness() {
            return successStickiness;
        }

        public void setSuccessStickiness(Duration successStickiness) {
            this.successStickiness = successStickiness;
        }

        public Map<String, Replica> getCandidates() {
            return candidates;
        }
    }

    public static final class RegionalSelection {
        private boolean enabled = true;
        private String requestHeaderReplica = "X-PJB-Read-Replica";
        private String requestHeaderTribunal = "X-PJB-Tribunal";
        private String requestHeaderUf = "X-PJB-UF";
        private String requestHeaderOrgao = "X-PJB-Orgao";
        private String requestHeaderUnidade = "X-PJB-Unidade";
        private String requestHeaderCaixa = "X-PJB-Caixa";
        private final Map<String, String> orgaoToReplica = new LinkedHashMap<>();
        private final Map<String, String> unidadeToReplica = new LinkedHashMap<>();
        private final Map<String, String> caixaToReplica = new LinkedHashMap<>();
        private final Map<String, String> tribunalToReplica = new LinkedHashMap<>();
        private final Map<String, String> ufToReplica = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRequestHeaderReplica() {
            return requestHeaderReplica;
        }

        public void setRequestHeaderReplica(String requestHeaderReplica) {
            this.requestHeaderReplica = requestHeaderReplica;
        }

        public String getRequestHeaderTribunal() {
            return requestHeaderTribunal;
        }

        public void setRequestHeaderTribunal(String requestHeaderTribunal) {
            this.requestHeaderTribunal = requestHeaderTribunal;
        }

        public String getRequestHeaderUf() {
            return requestHeaderUf;
        }

        public void setRequestHeaderUf(String requestHeaderUf) {
            this.requestHeaderUf = requestHeaderUf;
        }

        public Map<String, String> getOrgaoToReplica() {
            return orgaoToReplica;
        }

        public Map<String, String> getUnidadeToReplica() {
            return unidadeToReplica;
        }

        public Map<String, String> getCaixaToReplica() {
            return caixaToReplica;
        }

        public Map<String, String> getTribunalToReplica() {
            return tribunalToReplica;
        }

        public Map<String, String> getUfToReplica() {
            return ufToReplica;
        }

        public String getRequestHeaderOrgao() {
            return requestHeaderOrgao;
        }

        public void setRequestHeaderOrgao(String requestHeaderOrgao) {
            this.requestHeaderOrgao = requestHeaderOrgao;
        }

        public String getRequestHeaderUnidade() {
            return requestHeaderUnidade;
        }

        public void setRequestHeaderUnidade(String requestHeaderUnidade) {
            this.requestHeaderUnidade = requestHeaderUnidade;
        }

        public String getRequestHeaderCaixa() {
            return requestHeaderCaixa;
        }

        public void setRequestHeaderCaixa(String requestHeaderCaixa) {
            this.requestHeaderCaixa = requestHeaderCaixa;
        }

    }

    public static final class Observability {
        private Duration refreshTtl = Duration.ofSeconds(5);
        private Duration connectionTimeout = Duration.ofSeconds(2);
        private Duration statementTimeout = Duration.ofSeconds(2);

        public Duration getRefreshTtl() {
            return refreshTtl;
        }

        public void setRefreshTtl(Duration refreshTtl) {
            this.refreshTtl = refreshTtl;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Duration getStatementTimeout() {
            return statementTimeout;
        }

        public void setStatementTimeout(Duration statementTimeout) {
            this.statementTimeout = statementTimeout;
        }
    }

    public static final class Propagation {
        private boolean enabled = true;
        private String requestHeaderName = "X-PJB-Prefer-Primary-Until";
        private String responseHeaderName = "X-PJB-Primary-Read-Until";
        private String cookieName = "PJB_RWY_PRIMARY_UNTIL";
        private String cookiePath = "/";
        private boolean secureCookie = true;
        private boolean httpOnlyCookie = true;
        private String sameSite = "Lax";
        private Duration cookieClockSkew = Duration.ofSeconds(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRequestHeaderName() {
            return requestHeaderName;
        }

        public void setRequestHeaderName(String requestHeaderName) {
            this.requestHeaderName = requestHeaderName;
        }

        public String getResponseHeaderName() {
            return responseHeaderName;
        }

        public void setResponseHeaderName(String responseHeaderName) {
            this.responseHeaderName = responseHeaderName;
        }

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getCookiePath() {
            return cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        public boolean isSecureCookie() {
            return secureCookie;
        }

        public void setSecureCookie(boolean secureCookie) {
            this.secureCookie = secureCookie;
        }

        public boolean isHttpOnlyCookie() {
            return httpOnlyCookie;
        }

        public void setHttpOnlyCookie(boolean httpOnlyCookie) {
            this.httpOnlyCookie = httpOnlyCookie;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }

        public Duration getCookieClockSkew() {
            return cookieClockSkew;
        }

        public void setCookieClockSkew(Duration cookieClockSkew) {
            this.cookieClockSkew = cookieClockSkew;
        }
    }

    public static final class AdaptivePlane {
        private boolean enabled = true;
        private boolean emitResponseHeaders = true;
        private Duration replicaLagTolerance = Duration.ofSeconds(2);
        private Duration degradedReplicaLagTolerance = Duration.ofSeconds(6);
        private double readPoolActiveRatioThreshold = 0.82d;
        private double writePoolActiveRatioThreshold = 0.90d;
        private int readPoolAwaitingThreshold = 8;
        private int writePoolAwaitingThreshold = 16;
        private final List<String> primaryCriticalPrefixes = new ArrayList<>(List.of(
                "/api/v1/processos",
                "/api/v1/peticionamento",
                "/api/v1/protocolos",
                "/api/v1/documentos/assinatura",
                "/api/v1/comunicacoes"
        ));
        private final List<String> hotCachePrefixes = new ArrayList<>(List.of(
                "/api/v1/dashboard",
                "/api/v1/painel",
                "/api/v1/timeline",
                "/api/v1/agenda",
                "/api/v1/processos/resumo",
                "/api/v1/caixas"
        ));
        private final List<String> searchBackedPrefixes = new ArrayList<>(List.of(
                "/api/v1/busca",
                "/api/v1/search",
                "/api/v1/elastic",
                "/api/v1/indexacao"
        ));
        private final List<String> asyncWritePrefixes = new ArrayList<>(List.of(
                "/api/v1/documentos/upload",
                "/api/v1/anexos",
                "/api/v1/reprocessamento",
                "/api/v1/eventos",
                "/api/v1/integracoes"
        ));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEmitResponseHeaders() {
            return emitResponseHeaders;
        }

        public void setEmitResponseHeaders(boolean emitResponseHeaders) {
            this.emitResponseHeaders = emitResponseHeaders;
        }

        public Duration getReplicaLagTolerance() {
            return replicaLagTolerance;
        }

        public void setReplicaLagTolerance(Duration replicaLagTolerance) {
            this.replicaLagTolerance = replicaLagTolerance;
        }

        public Duration getDegradedReplicaLagTolerance() {
            return degradedReplicaLagTolerance;
        }

        public void setDegradedReplicaLagTolerance(Duration degradedReplicaLagTolerance) {
            this.degradedReplicaLagTolerance = degradedReplicaLagTolerance;
        }

        public double getReadPoolActiveRatioThreshold() {
            return readPoolActiveRatioThreshold;
        }

        public void setReadPoolActiveRatioThreshold(double readPoolActiveRatioThreshold) {
            this.readPoolActiveRatioThreshold = readPoolActiveRatioThreshold;
        }

        public double getWritePoolActiveRatioThreshold() {
            return writePoolActiveRatioThreshold;
        }

        public void setWritePoolActiveRatioThreshold(double writePoolActiveRatioThreshold) {
            this.writePoolActiveRatioThreshold = writePoolActiveRatioThreshold;
        }

        public int getReadPoolAwaitingThreshold() {
            return readPoolAwaitingThreshold;
        }

        public void setReadPoolAwaitingThreshold(int readPoolAwaitingThreshold) {
            this.readPoolAwaitingThreshold = readPoolAwaitingThreshold;
        }

        public int getWritePoolAwaitingThreshold() {
            return writePoolAwaitingThreshold;
        }

        public void setWritePoolAwaitingThreshold(int writePoolAwaitingThreshold) {
            this.writePoolAwaitingThreshold = writePoolAwaitingThreshold;
        }

        public List<String> getPrimaryCriticalPrefixes() {
            return primaryCriticalPrefixes;
        }

        public List<String> getHotCachePrefixes() {
            return hotCachePrefixes;
        }

        public List<String> getSearchBackedPrefixes() {
            return searchBackedPrefixes;
        }

        public List<String> getAsyncWritePrefixes() {
            return asyncWritePrefixes;
        }
    }


    public static final class Hikari {
        private int maximumPoolSize = 16;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000L;
        private long validationTimeout = 5000L;
        private long maxLifetime = 1800000L;
        private long keepaliveTime = 30000L;
        private long initializationFailTimeout = 1L;
        private long leakDetectionThreshold;
        private boolean readOnly = true;
        private boolean registerMbeans = true;

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getValidationTimeout() {
            return validationTimeout;
        }

        public void setValidationTimeout(long validationTimeout) {
            this.validationTimeout = validationTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public long getKeepaliveTime() {
            return keepaliveTime;
        }

        public void setKeepaliveTime(long keepaliveTime) {
            this.keepaliveTime = keepaliveTime;
        }

        public long getInitializationFailTimeout() {
            return initializationFailTimeout;
        }

        public void setInitializationFailTimeout(long initializationFailTimeout) {
            this.initializationFailTimeout = initializationFailTimeout;
        }

        public long getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isRegisterMbeans() {
            return registerMbeans;
        }

        public void setRegisterMbeans(boolean registerMbeans) {
            this.registerMbeans = registerMbeans;
        }
    }
}
