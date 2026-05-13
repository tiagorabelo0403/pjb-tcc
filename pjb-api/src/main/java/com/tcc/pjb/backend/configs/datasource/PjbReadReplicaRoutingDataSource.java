package com.tcc.pjb.backend.configs.datasource;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class PjbReadReplicaRoutingDataSource extends AbstractRoutingDataSource {

    private static final String PREFIX = "READ_";

    private final PjbPrimaryReadPreferenceContext primaryReadPreferenceContext;
    private final Set<String> regionalKeys;
    private final PjbDataSourceRoutingProperties properties;
    private final PjbAdaptiveDataPlaneContext adaptiveDataPlaneContext;

    public PjbReadReplicaRoutingDataSource(PjbPrimaryReadPreferenceContext primaryReadPreferenceContext) {
        this(primaryReadPreferenceContext, Set.of(), null, null);
    }

    public PjbReadReplicaRoutingDataSource(PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                           Set<String> regionalKeys) {
        this(primaryReadPreferenceContext, regionalKeys, null, null);
    }

    public PjbReadReplicaRoutingDataSource(PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                           Set<String> regionalKeys,
                                           PjbDataSourceRoutingProperties properties) {
        this(primaryReadPreferenceContext, regionalKeys, properties, null);
    }

    public PjbReadReplicaRoutingDataSource(PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                           Set<String> regionalKeys,
                                           PjbDataSourceRoutingProperties properties,
                                           PjbAdaptiveDataPlaneContext adaptiveDataPlaneContext) {
        this.primaryReadPreferenceContext = Objects.requireNonNull(primaryReadPreferenceContext, "primaryReadPreferenceContext");
        this.regionalKeys = Set.copyOf(Objects.requireNonNullElse(regionalKeys, Set.of()));
        this.properties = properties;
        this.adaptiveDataPlaneContext = adaptiveDataPlaneContext;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        PjbAdaptiveDataPlaneService.AdaptiveDecision adaptiveDecision = adaptiveDecision();
        if (adaptiveDecision != null && adaptiveDecision.forcePrimary()) {
            return PjbDataSourceRole.WRITE;
        }
        if (primaryReadPreferenceContext.isPrimaryPreferred()) {
            return PjbDataSourceRole.WRITE;
        }
        if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return PjbDataSourceRole.WRITE;
        }
        if (adaptiveDecision != null && adaptiveDecision.preferredReplicaKey() != null) {
            String preferred = normalizeKey(adaptiveDecision.preferredReplicaKey());
            if (PjbDataSourceRole.READ.name().equals(preferred)) {
                return PjbDataSourceRole.READ;
            }
            if (preferred != null) {
                return preferred;
            }
        }
        String regional = determineRegionalReplicaKey();
        return regional != null ? regional : PjbDataSourceRole.READ;
    }

    private String determineRegionalReplicaKey() {
        String explicit = normalizeKey(resolveFromRequestHeaders());
        if (explicit != null) {
            return explicit;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        String resolved = normalizeKey(resolveFromAuthentication(authentication));
        if (resolved != null) {
            return resolved;
        }
        String region = inferRegion(authentication);
        return normalizeKey(region);
    }

    private String resolveFromRequestHeaders() {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || !regionalSelection.isEnabled()) {
            return null;
        }
        ServletRequestAttributes attributes = servletRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String byReplica = normalizeKey(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderReplica()));
        if (byReplica != null) {
            return byReplica;
        }
        String tribunal = firstNonBlank(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderTribunal()));
        String orgao = firstNonBlank(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderOrgao()));
        String unidade = firstNonBlank(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderUnidade()));
        String caixa = firstNonBlank(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderCaixa()));
        String uf = firstNonBlank(attributes.getRequest().getHeader(regionalSelection.getRequestHeaderUf()));
        String byCaixa = mapReplicaByCaixa(caixa);
        if (byCaixa != null) {
            return byCaixa;
        }
        String byUnidade = mapReplicaByUnidade(unidade);
        if (byUnidade != null) {
            return byUnidade;
        }
        String byOrgao = mapReplicaByOrgao(orgao);
        if (byOrgao != null) {
            return byOrgao;
        }
        String byTribunal = mapReplicaByTribunal(tribunal);
        if (byTribunal != null) {
            return byTribunal;
        }
        String byUf = mapReplicaByUf(uf);
        if (byUf != null) {
            return byUf;
        }
        String inferred = inferRegionByInstitutionalCode(firstNonBlank(caixa, unidade, orgao, tribunal));
        if (inferred != null) {
            return inferred;
        }
        return inferRegionByUf(uf);
    }

    private String resolveFromAuthentication(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jat) {
            Jwt jwt = jat.getToken();
            String explicitReplica = normalizeKey(firstNonBlank(jwt.getClaimAsString("readReplica"), jwt.getClaimAsString("replica"), jwt.getClaimAsString("read_replica")));
            if (explicitReplica != null) {
                return explicitReplica;
            }
            String tribunal = firstNonBlank(jwt.getClaimAsString("tribunal"), jwt.getClaimAsString("tribunalCodigo"), jwt.getClaimAsString("court"));
            String orgao = firstNonBlank(jwt.getClaimAsString("orgao"), jwt.getClaimAsString("orgaoCodigo"), jwt.getClaimAsString("institution"));
            String unidade = firstNonBlank(jwt.getClaimAsString("unidade"), jwt.getClaimAsString("unidadeCodigo"), jwt.getClaimAsString("unit"));
            String caixa = firstNonBlank(jwt.getClaimAsString("caixa"), jwt.getClaimAsString("caixaCodigo"), jwt.getClaimAsString("lane"));
            String uf = firstNonBlank(jwt.getClaimAsString("uf"), jwt.getClaimAsString("estado"), jwt.getClaimAsString("state"));
            String byCaixa = mapReplicaByCaixa(caixa);
            if (byCaixa != null) {
                return byCaixa;
            }
            String byUnidade = mapReplicaByUnidade(unidade);
            if (byUnidade != null) {
                return byUnidade;
            }
            String byOrgao = mapReplicaByOrgao(orgao);
            if (byOrgao != null) {
                return byOrgao;
            }
            String byTribunal = mapReplicaByTribunal(tribunal);
            if (byTribunal != null) {
                return byTribunal;
            }
            String byUf = mapReplicaByUf(uf);
            if (byUf != null) {
                return byUf;
            }
        }
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map) {
            String explicitReplica = normalizeKey(firstNonBlank(asString(map.get("readReplica")), asString(map.get("replica")), asString(map.get("read_replica"))));
            if (explicitReplica != null) {
                return explicitReplica;
            }
            String tribunal = firstNonBlank(asString(map.get("tribunal")), asString(map.get("tribunalCodigo")), asString(map.get("court")));
            String orgao = firstNonBlank(asString(map.get("orgao")), asString(map.get("orgaoCodigo")), asString(map.get("institution")));
            String unidade = firstNonBlank(asString(map.get("unidade")), asString(map.get("unidadeCodigo")), asString(map.get("unit")));
            String caixa = firstNonBlank(asString(map.get("caixa")), asString(map.get("caixaCodigo")), asString(map.get("lane")));
            String uf = firstNonBlank(asString(map.get("uf")), asString(map.get("estado")), asString(map.get("state")));
            String byCaixa = mapReplicaByCaixa(caixa);
            if (byCaixa != null) {
                return byCaixa;
            }
            String byUnidade = mapReplicaByUnidade(unidade);
            if (byUnidade != null) {
                return byUnidade;
            }
            String byOrgao = mapReplicaByOrgao(orgao);
            if (byOrgao != null) {
                return byOrgao;
            }
            String byTribunal = mapReplicaByTribunal(tribunal);
            if (byTribunal != null) {
                return byTribunal;
            }
            String byUf = mapReplicaByUf(uf);
            if (byUf != null) {
                return byUf;
            }
            return inferRegionByInstitutionalCode(firstNonBlank(caixa, unidade, orgao, tribunal));
        }
        return null;
    }

    private String inferRegion(Authentication authentication) {
        String authorityBased = inferRegionByAuthority(authentication);
        if (authorityBased != null) {
            return authorityBased;
        }
        if (authentication instanceof JwtAuthenticationToken jat) {
            Jwt jwt = jat.getToken();
            String tribunal = firstNonBlank(jwt.getClaimAsString("tribunal"), jwt.getClaimAsString("tribunalCodigo"), jwt.getClaimAsString("court"));
            String orgao = firstNonBlank(jwt.getClaimAsString("orgao"), jwt.getClaimAsString("orgaoCodigo"), jwt.getClaimAsString("institution"));
            String unidade = firstNonBlank(jwt.getClaimAsString("unidade"), jwt.getClaimAsString("unidadeCodigo"), jwt.getClaimAsString("unit"));
            String caixa = firstNonBlank(jwt.getClaimAsString("caixa"), jwt.getClaimAsString("caixaCodigo"), jwt.getClaimAsString("lane"));
            String uf = firstNonBlank(jwt.getClaimAsString("uf"), jwt.getClaimAsString("estado"), jwt.getClaimAsString("state"));
            String byInstitutionalCode = inferRegionByInstitutionalCode(firstNonBlank(caixa, unidade, orgao, tribunal));
            if (byInstitutionalCode != null) {
                return byInstitutionalCode;
            }
            return inferRegionByUf(uf);
        }
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map) {
            String tribunal = firstNonBlank(asString(map.get("tribunal")), asString(map.get("tribunalCodigo")), asString(map.get("court")));
            String orgao = firstNonBlank(asString(map.get("orgao")), asString(map.get("orgaoCodigo")), asString(map.get("institution")));
            String unidade = firstNonBlank(asString(map.get("unidade")), asString(map.get("unidadeCodigo")), asString(map.get("unit")));
            String caixa = firstNonBlank(asString(map.get("caixa")), asString(map.get("caixaCodigo")), asString(map.get("lane")));
            String uf = firstNonBlank(asString(map.get("uf")), asString(map.get("estado")), asString(map.get("state")));
            String byInstitutionalCode = inferRegionByInstitutionalCode(firstNonBlank(caixa, unidade, orgao, tribunal));
            if (byInstitutionalCode != null) {
                return byInstitutionalCode;
            }
            return inferRegionByUf(uf);
        }
        return null;
    }

    private String inferRegionByAuthority(Authentication authentication) {
        Set<String> authorities = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority != null && authority.getAuthority() != null) {
                authorities.add(authority.getAuthority().toUpperCase(Locale.ROOT));
            }
        }
        if (authorities.stream().anyMatch(auth -> auth.contains("MINISTRO") || auth.contains("STJ") || auth.contains("STF") || auth.contains("TSE") || auth.contains("TST") || auth.contains("STM") || auth.contains("CNJ"))) {
            return "SUPERIOR";
        }
        return null;
    }

    private String mapReplicaByCaixa(String caixa) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || caixa == null || caixa.isBlank()) {
            return null;
        }
        String normalized = caixa.trim().toUpperCase(Locale.ROOT);
        String mapped = regionalSelection.getCaixaToReplica().get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String compact = normalized.replaceAll("[^A-Z0-9]", "");
        return regionalSelection.getCaixaToReplica().get(compact);
    }

    private String mapReplicaByUnidade(String unidade) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || unidade == null || unidade.isBlank()) {
            return null;
        }
        String normalized = unidade.trim().toUpperCase(Locale.ROOT);
        String mapped = regionalSelection.getUnidadeToReplica().get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String compact = normalized.replaceAll("[^A-Z0-9]", "");
        return regionalSelection.getUnidadeToReplica().get(compact);
    }

    private String mapReplicaByOrgao(String orgao) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || orgao == null || orgao.isBlank()) {
            return null;
        }
        String normalized = orgao.trim().toUpperCase(Locale.ROOT);
        String mapped = regionalSelection.getOrgaoToReplica().get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String compact = normalized.replaceAll("[^A-Z0-9]", "");
        return regionalSelection.getOrgaoToReplica().get(compact);
    }

    private String mapReplicaByTribunal(String tribunal) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || tribunal == null || tribunal.isBlank()) {
            return null;
        }
        String mapped = regionalSelection.getTribunalToReplica().get(tribunal.trim().toUpperCase(Locale.ROOT));
        if (mapped != null) {
            return mapped;
        }
        String normalized = tribunal.trim().toUpperCase(Locale.ROOT);
        String compact = normalized.replaceAll("[^A-Z0-9]", "");
        return regionalSelection.getTribunalToReplica().get(compact);
    }

    private String mapReplicaByUf(String uf) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = regionalSelection();
        if (regionalSelection == null || uf == null || uf.isBlank()) {
            return null;
        }
        return regionalSelection.getUfToReplica().get(uf.trim().toUpperCase(Locale.ROOT));
    }

    private String inferRegionByTribunal(String tribunal) {
        return inferRegionByInstitutionalCode(tribunal);
    }

    private String inferRegionByInstitutionalCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("STJ") || normalized.startsWith("STF") || normalized.startsWith("TSE")
                || normalized.startsWith("TST") || normalized.startsWith("STM") || normalized.startsWith("CNJ")
                || normalized.startsWith("PGR") || normalized.startsWith("MPF") || normalized.startsWith("DPU")
                || normalized.startsWith("AGU") || normalized.startsWith("PGFN")) {
            return "SUPERIOR";
        }
        String ufFromSuffix = extractUfFromTribunal(normalized);
        if (ufFromSuffix != null) {
            return inferRegionByUf(ufFromSuffix);
        }
        return null;
    }

    private String extractUfFromTribunal(String normalizedTribunal) {
        if (normalizedTribunal == null || normalizedTribunal.isBlank()) {
            return null;
        }
        String compact = normalizedTribunal.replaceAll("[^A-Z0-9]", "");
        if (compact.length() >= 2) {
            String suffix = compact.substring(compact.length() - 2);
            if (suffix.chars().allMatch(Character::isLetter)) {
                return suffix;
            }
        }
        return null;
    }

    private String inferRegionByUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        return switch (uf.trim().toUpperCase(Locale.ROOT)) {
            case "AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE" -> "NORDESTE";
            case "ES", "MG", "RJ", "SP" -> "SUDESTE";
            case "PR", "RS", "SC" -> "SUL";
            case "AC", "AM", "AP", "PA", "RO", "RR", "TO" -> "NORTE";
            case "DF", "GO", "MS", "MT" -> "CENTRO_OESTE";
            default -> null;
        };
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (PjbDataSourceRole.READ.name().equals(normalized) || PjbDataSourceRole.WRITE.name().equals(normalized)) {
            return normalized;
        }
        String lookup = normalized.startsWith(PREFIX) ? normalized : PREFIX + normalized;
        return regionalKeys.contains(lookup) ? lookup : null;
    }

    private ServletRequestAttributes servletRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes;
        }
        return null;
    }

    private PjbAdaptiveDataPlaneService.AdaptiveDecision adaptiveDecision() {
        return adaptiveDataPlaneContext == null ? null : adaptiveDataPlaneContext.current();
    }

    private PjbDataSourceRoutingProperties.RegionalSelection regionalSelection() {
        return properties == null ? null : properties.getRegionalSelection();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
