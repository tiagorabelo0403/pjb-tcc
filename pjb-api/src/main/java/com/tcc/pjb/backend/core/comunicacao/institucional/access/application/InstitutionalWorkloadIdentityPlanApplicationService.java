package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityBinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalWorkloadIdentityPlanApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;

    public InstitutionalWorkloadIdentityPlanApplicationService(InstitutionalAffiliationStateRepository affiliationRepository) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
    }

    public InstitutionalWorkloadIdentityPlan avaliar(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
        String orgaoSigla = affiliation == null || affiliation.orgaoSigla() == null || affiliation.orgaoSigla().isBlank() ? "PJB" : affiliation.orgaoSigla();
        String namespace = "pjb";
        String trustDomain = "spiffe://pjb.jus.br/" + normalize(orgaoSigla);
        ArrayList<InstitutionalWorkloadIdentityBinding> workloads = new ArrayList<>();
        workloads.add(binding(trustDomain, namespace, "api", "API institucional", "pjb-backend", "pjb-api", "processos:read processos:write institucional:dispatch"));
        workloads.add(binding(trustDomain, namespace, "worker", "Worker soberano", "pjb-backend", "pjb-worker", "institutional:queue institutional:workflow kafka:consume"));
        workloads.add(binding(trustDomain, namespace, "scheduler", "Scheduler operacional", "pjb-backend", "pjb-scheduler", "jobs:governed scheduling:critical"));
        workloads.add(binding(trustDomain, namespace, "db-edge-rw", "Borda de banco escrita", "pjb-db-edge-rw", "database-edge-rw", "postgres:rw"));
        workloads.add(binding(trustDomain, namespace, "db-edge-ro", "Borda de banco leitura", "pjb-db-edge-ro", "database-edge-ro", "postgres:ro"));
        ArrayList<String> findings = new ArrayList<>();
        if (affiliation == null) {
            findings.add("afiliacao_nao_localizada_para_plano_de_identidade_workload");
        }
        if (affiliation != null && !affiliation.ativa()) {
            findings.add("afiliacao_ainda_nao_homologada_para_plano_completo_de_workload_identity");
        }
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("workload_identity_governada_por_trust_domain_spiffe");
        fundamentos.add("mtls_obrigatorio_entre_api_worker_scheduler_e_borda_de_banco");
        fundamentos.add("tokens_projetados_de_serviceaccount_e_egress_segmentado_por_workload");
        if (affiliation != null) {
            fundamentos.add("orgao=" + affiliation.orgaoSigla());
            fundamentos.add("unidade=" + affiliation.unidadeCodigo());
        }
        return new InstitutionalWorkloadIdentityPlan(
                affiliationId,
                affiliation == null ? orgaoSigla : affiliation.orgaoSigla(),
                affiliation == null ? "Plano institucional de workload identity" : affiliation.orgaoNome(),
                trustDomain,
                namespace,
                affiliation != null && affiliation.ativa(),
                true,
                true,
                workloads,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private InstitutionalWorkloadIdentityBinding binding(String trustDomain,
                                                         String namespace,
                                                         String code,
                                                         String displayName,
                                                         String serviceAccount,
                                                         String audience,
                                                         String egress) {
        return new InstitutionalWorkloadIdentityBinding(
                code,
                displayName,
                trustDomain + "/workload/" + code,
                serviceAccount,
                namespace,
                audience,
                true,
                true,
                List.of(egress),
                List.of("workload=" + code, "namespace=" + namespace));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.-]", "-");
    }
}
