package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalIntegrationSecurityPolicyApplicationService {

    private static final Duration GLOBAL_CACHE_TTL = Duration.ofSeconds(20);

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final AtomicReference<CachedPolicies> globalCache = new AtomicReference<>();
    private final ConcurrentHashMap<String, CachedPolicies> scopedCache = new ConcurrentHashMap<>();

    public InstitutionalIntegrationSecurityPolicyApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                   InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public List<InstitutionalIntegrationSecurityPolicy> listar(String scopeCode, String affiliationId) {
        InstitutionalOrganizationScope scope = InstitutionalOrganizationScope.fromTexto(scopeCode);
        String normalizedAffiliationId = normalize(affiliationId);
        if (normalizedAffiliationId != null) {
            return computePolicies(scopeCode, scope, normalizedAffiliationId, Instant.now());
        }
        if (scope == null) {
            CachedPolicies cache = globalCache.get();
            if (isFresh(cache)) {
                return cache.policies();
            }
            List<InstitutionalIntegrationSecurityPolicy> policies = computePolicies(scopeCode, null, null, Instant.now());
            globalCache.set(new CachedPolicies(policies, Instant.now().plus(GLOBAL_CACHE_TTL)));
            return policies;
        }
        String cacheKey = scope.name();
        CachedPolicies cache = scopedCache.get(cacheKey);
        if (isFresh(cache)) {
            return cache.policies();
        }
        List<InstitutionalIntegrationSecurityPolicy> policies = computePolicies(scopeCode, scope, null, Instant.now());
        scopedCache.put(cacheKey, new CachedPolicies(policies, Instant.now().plus(GLOBAL_CACHE_TTL)));
        return policies;
    }

    private List<InstitutionalIntegrationSecurityPolicy> computePolicies(String scopeCode,
                                                                         InstitutionalOrganizationScope scope,
                                                                         String affiliationId,
                                                                         Instant now) {
        List<InstitutionalAffiliation> affiliations = resolveAffiliations(scope, affiliationId);
        if (!affiliations.isEmpty()) {
            return affiliations.stream().map(item -> fromAffiliation(item, now)).toList();
        }
        List<InstitutionalOrganizationBlueprint> blueprints = scope == null
                ? blueprintCatalogApplicationService.listar()
                : blueprintCatalogApplicationService.findByScope(scope).stream().toList();
        return blueprints.stream().map(item -> fromBlueprint(item, now)).toList();
    }

    private List<InstitutionalAffiliation> resolveAffiliations(InstitutionalOrganizationScope scope, String affiliationId) {
        if (affiliationId != null) {
            return affiliationRepository.findByAffiliationId(affiliationId)
                    .stream()
                    .filter(item -> scope == null || item.organizationScope() == scope)
                    .toList();
        }
        return scope == null
                ? affiliationRepository.findAll()
                : affiliationRepository.findByOrganizationScope(scope);
    }

    private InstitutionalIntegrationSecurityPolicy fromAffiliation(InstitutionalAffiliation affiliation, Instant now) {
        LinkedHashSet<String> controls = baseControls();
        InstitutionalTrustLevel trustLevel = affiliation.trustFloor();
        if (trustLevel != null && trustLevel.ordem() >= InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO.ordem()) {
            controls.add("MUTUAL_TLS");
            controls.add("ASSINATURA_DE_PAYLOAD");
            controls.add("CARIMBO_DE_TEMPO");
        }
        if (affiliation.requerCertificadoICP()) {
            controls.add("CERTIFICADO_ICP_BRASIL_POR_INSTITUICAO");
        }
        if (affiliation.restringeCertificadoRedeInstitucional()) {
            controls.add("ALLOWLIST_DE_ORIGEM");
            controls.add("REDE_INSTITUCIONAL_OU_AUTORIZACAO_REMOTA");
        }
        if (affiliation.permiteUsoRemotoComAutorizacao()) {
            controls.add("AUTORIZACAO_REMOTA_VERSIONADA");
        }
        return new InstitutionalIntegrationSecurityPolicy(
                affiliation.affiliationId(),
                "AFILIACAO_ATIVA",
                affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation.orgaoNome() + " / " + affiliation.unidadeNome(),
                trustLevel == null ? null : trustLevel.name(),
                sanitize(affiliation.canaisHabilitados()),
                sanitize(affiliation.conveniosIntegracoes()),
                controls.contains("MUTUAL_TLS"),
                controls.contains("ASSINATURA_DE_PAYLOAD"),
                controls.contains("ALLOWLIST_DE_ORIGEM"),
                true,
                affiliation.requerDuplaAprovacaoAdministrador(),
                rotationDays(trustLevel),
                List.copyOf(controls),
                appendFundamentos(affiliation.fundamentos(),
                        "integracao_por_credencial_institucional",
                        "segredo_rotacionavel",
                        "revogacao_imediata_de_credencial_comprometida"),
                now
        );
    }

    private InstitutionalIntegrationSecurityPolicy fromBlueprint(InstitutionalOrganizationBlueprint blueprint, Instant now) {
        LinkedHashSet<String> controls = baseControls();
        if (blueprint.requerCertificadoICP() || (blueprint.trustFloorPadrao() != null && blueprint.trustFloorPadrao().ordem() >= InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO.ordem())) {
            controls.add("MUTUAL_TLS");
            controls.add("ASSINATURA_DE_PAYLOAD");
        }
        if (blueprint.restringeCertificadoRedeInstitucional()) {
            controls.add("ALLOWLIST_DE_ORIGEM");
        }
        if (blueprint.permiteUsoRemotoComAutorizacao()) {
            controls.add("AUTORIZACAO_REMOTA_VERSIONADA");
        }
        return new InstitutionalIntegrationSecurityPolicy(
                "BLUEPRINT::" + blueprint.codigo(),
                "BASELINE_SCOPE",
                blueprint.scope().name(),
                blueprint.nomeExibicao(),
                blueprint.trustFloorPadrao() == null ? null : blueprint.trustFloorPadrao().name(),
                List.of(),
                List.of(),
                controls.contains("MUTUAL_TLS"),
                controls.contains("ASSINATURA_DE_PAYLOAD"),
                controls.contains("ALLOWLIST_DE_ORIGEM"),
                true,
                blueprint.requerDuplaAprovacaoAdministrador(),
                rotationDays(blueprint.trustFloorPadrao()),
                List.copyOf(controls),
                appendFundamentos(blueprint.fundamentos(),
                        "baseline_de_integracao_sistema_sistema",
                        "idempotencia_obrigatoria",
                        "correlacao_de_requisicoes"),
                now
        );
    }


    private boolean isFresh(CachedPolicies cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LinkedHashSet<String> baseControls() {
        LinkedHashSet<String> controls = new LinkedHashSet<>();
        controls.add("CREDENCIAL_PROPRIA_POR_INSTITUICAO");
        controls.add("SEGREDO_ROTACIONAVEL");
        controls.add("IDEMPOTENCIA");
        controls.add("CORRELACAO_DE_REQUISICOES");
        controls.add("TRILHA_FORENSE_POR_CHAMADA");
        controls.add("REVOGACAO_IMEDIATA");
        return controls;
    }

    private int rotationDays(InstitutionalTrustLevel trustLevel) {
        if (trustLevel == null) {
            return 60;
        }
        return switch (trustLevel) {
            case NIVEL_1_IDENTIDADE_FEDERADA -> 90;
            case NIVEL_2_NOMEACAO_ATIVA -> 60;
            case NIVEL_3_CERTIFICADO_QUALIFICADO -> 45;
            case NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO -> 30;
        };
    }

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private List<String> appendFundamentos(List<String> fundamentos, String... extras) {
        ArrayList<String> out = new ArrayList<>();
        if (fundamentos != null) {
            out.addAll(fundamentos);
        }
        if (extras != null) {
            for (String extra : extras) {
                if (extra != null && !extra.isBlank()) {
                    out.add(extra.trim());
                }
            }
        }
        return List.copyOf(out.stream().distinct().toList());
    }

    private record CachedPolicies(List<InstitutionalIntegrationSecurityPolicy> policies, Instant expiresAt) { }
}
