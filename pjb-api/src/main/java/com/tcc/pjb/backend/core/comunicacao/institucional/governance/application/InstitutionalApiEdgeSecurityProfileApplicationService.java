package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalWorkloadIdentityPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalApiEdgeSecurityProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalApiEdgeSecurityProfileApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService;
    private final InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService;

    public InstitutionalApiEdgeSecurityProfileApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                 InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService,
                                                                 InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.workloadIdentityPlanApplicationService = Objects.requireNonNull(workloadIdentityPlanApplicationService);
        this.integrationCredentialApplicationService = Objects.requireNonNull(integrationCredentialApplicationService);
    }

    public InstitutionalApiEdgeSecurityProfile avaliar(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
        InstitutionalWorkloadIdentityPlan workloadPlan = workloadIdentityPlanApplicationService.avaliar(affiliationId);
        List<InstitutionalIntegrationCredential> activeCredentials = integrationCredentialApplicationService.list(affiliationId).stream()
                .filter(item -> item.ativaEm(Instant.now()))
                .sorted(Comparator.comparing(InstitutionalIntegrationCredential::issuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        boolean fapi2Required = affiliation != null && affiliation.ativa();
        boolean messageSigningRequired = activeCredentials.stream().anyMatch(InstitutionalIntegrationCredential::requiresPayloadSignature) || !activeCredentials.isEmpty();
        boolean senderConstrainedTokensRequired = fapi2Required;
        boolean privateKeyJwtRequired = fapi2Required;
        boolean parRequired = fapi2Required;
        boolean pkceRequired = fapi2Required;
        boolean mtlsRequired = true;
        boolean backendTlsPolicyRequired = true;
        boolean spiffeBindingRequired = workloadPlan.enabled();
        ArrayList<String> findings = new ArrayList<>();
        if (affiliation == null) {
            findings.add("afiliacao_nao_localizada_para_perfil_api");
        }
        if (affiliation != null && !affiliation.ativa()) {
            findings.add("afiliacao_nao_homologada_para_fapi2_completo");
        }
        if (activeCredentials.isEmpty()) {
            findings.add("credencial_de_integracao_ativa_ainda_nao_emitida");
        }
        if (!workloadPlan.enabled()) {
            findings.add("workload_identity_ainda_nao_homologado");
        }
        int rotationDays = activeCredentials.stream().mapToInt(InstitutionalIntegrationCredential::credentialRotationDays).min().orElse(30);
        List<String> families = activeCredentials.stream().flatMap(item -> item.integrationFamilies().stream()).filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
        List<String> workloadBindings = workloadPlan.workloads().stream().map(item -> item.spiffeId() + "@" + item.serviceAccount()).toList();
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("perfil_api_institucional_alinhado_a_fapi2_security_profile");
        fundamentos.add("sender_constrained_tokens_com_mutual_tls_e_private_key_jwt");
        fundamentos.add("gateway_api_com_backend_tls_policy_e_workload_identity_spiffe");
        fundamentos.add("rotacao_curta_de_credencial_para_integracoes_sensiveis");
        if (!families.isEmpty()) {
            fundamentos.add("familias_integracao=" + String.join(",", families));
        }
        return new InstitutionalApiEdgeSecurityProfile(
                affiliationId,
                affiliation == null ? "PJB" : affiliation.orgaoSigla(),
                affiliation == null ? "Perfil institucional de segurança de API" : affiliation.orgaoNome(),
                workloadPlan.trustDomain(),
                "pjb-sovereign-gateway",
                normalizeHost(affiliation == null ? "pjb" : affiliation.orgaoSigla()) + ".api.pjb.jus.br",
                true,
                fapi2Required,
                messageSigningRequired,
                senderConstrainedTokensRequired,
                privateKeyJwtRequired,
                parRequired,
                pkceRequired,
                mtlsRequired,
                backendTlsPolicyRequired,
                spiffeBindingRequired,
                false,
                rotationDays,
                workloadBindings,
                families,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private String normalizeHost(String value) {
        return value == null ? "pjb" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]", "-");
    }
}
