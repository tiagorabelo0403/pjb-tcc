package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalBindingApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRecertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalBindingApprovalApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalRecertificationApplicationService recertificationApplicationService;
    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;

    public InstitutionalBindingApprovalApplicationService(CurrentUserService currentUserService,
                                                          InstitutionalAffiliationStateRepository affiliationRepository,
                                                          InstitutionalNominationStateRepository nominationRepository,
                                                          InstitutionalAffiliationRequestStateRepository requestRepository,
                                                          InstitutionalRecertificationApplicationService recertificationApplicationService,
                                                          InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.recertificationApplicationService = Objects.requireNonNull(recertificationApplicationService);
        this.trustGovernanceOrchestrationApplicationService = Objects.requireNonNull(trustGovernanceOrchestrationApplicationService);
    }

    public InstitutionalBindingApproval avaliarAtual(String affiliationId, String nominationId) {
        Usuario user = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalNomination nomination = resolveNomination(user.getId(), affiliationId, nominationId, now);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        InstitutionalAffiliationRequest request = resolveRequest(affiliation);
        InstitutionalRecertificationCycle cycle = affiliation == null ? null : recertificationApplicationService.listar(affiliation.organizationScope() == null ? null : affiliation.organizationScope().name()).stream()
                .filter(item -> item.affiliationId().equals(affiliation.affiliationId()))
                .findFirst()
                .orElse(null);
        boolean affiliationActive = affiliation != null && affiliation.ativa();
        boolean nominationActive = nomination != null && nomination.ativaEm(now);
        boolean requiresDualAdministration = affiliationActive && affiliation.requerDuplaAprovacaoAdministrador();
        boolean dualAdministrationSatisfied = affiliationActive
                && (!requiresDualAdministration || activeAdministrators(affiliation.affiliationId(), now) >= 2);
        boolean recertificationDue = cycle != null && cycle.dueNow();
        boolean capacityBound = nomination != null && nomination.capacidades() != null && !nomination.capacidades().isEmpty();
        var trustGovernanceProfile = nomination == null ? null : trustGovernanceOrchestrationApplicationService.avaliarAtual(affiliationId, nomination.nominationId());
        boolean trustGovernanceSatisfied = trustGovernanceProfile != null && trustGovernanceProfile.fullyApproved();
        boolean homologated = affiliationActive
                && (request == null || request.materializedAffiliationId() == null || affiliation.affiliationId().equals(request.materializedAffiliationId()));
        ArrayList<String> findings = new ArrayList<>();
        if (!affiliationActive) {
            findings.add("afiliacao_institucional_inativa_ou_ausente");
        }
        if (!nominationActive) {
            findings.add("nomeacao_institucional_inativa_ou_ausente");
        }
        if (!dualAdministrationSatisfied) {
            findings.add("dupla_administracao_institucional_nao_satisfeita");
        }
        if (!capacityBound) {
            findings.add("capacidade_operacional_nao_amarada_a_nomeacao");
        }
        if (recertificationDue) {
            findings.add("recertificacao_periodica_pendente");
        }
        if (!trustGovernanceSatisfied) {
            findings.add("governanca_confianca_institucional_pendente");
        }
        boolean approved = affiliationActive && nominationActive && dualAdministrationSatisfied && capacityBound && !recertificationDue && trustGovernanceSatisfied;
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("pessoa_autenticada_com_vinculo_institucional_homologado");
        if (affiliation != null) {
            fundamentos.add("scope=" + (affiliation.organizationScope() == null ? "NAO_INFORMADO" : affiliation.organizationScope().name()));
            fundamentos.addAll(affiliation.fundamentos());
        }
        if (nomination != null) {
            fundamentos.add("lane=" + (nomination.accessLaneKind() == null ? "NAO_INFORMADA" : nomination.accessLaneKind().name()));
            fundamentos.add("papel=" + nomination.nominationRole().name());
        }
        if (cycle != null) {
            fundamentos.addAll(cycle.fundamentos());
        }
        if (trustGovernanceProfile != null) {
            fundamentos.addAll(trustGovernanceProfile.fundamentos());
        }
        return new InstitutionalBindingApproval(
                user.getId(),
                user.getNome(),
                affiliation == null ? null : affiliation.affiliationId(),
                nomination == null ? null : nomination.nominationId(),
                nomination == null ? null : nomination.unidadeCodigo(),
                nomination == null ? null : nomination.caixaCodigo(),
                affiliationActive,
                nominationActive,
                dualAdministrationSatisfied,
                recertificationDue,
                capacityBound,
                homologated,
                approved,
                List.copyOf(findings),
                fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList(),
                now);
    }

    private InstitutionalAffiliation resolveAffiliation(String affiliationId, InstitutionalNomination nomination) {
        String id = affiliationId == null || affiliationId.isBlank()
                ? nomination == null ? null : nomination.affiliationId()
                : affiliationId;
        return id == null ? null : affiliationRepository.findByAffiliationId(id).orElse(null);
    }

    private InstitutionalNomination resolveNomination(Long userId, String affiliationId, String nominationId, Instant now) {
        if (nominationId != null && !nominationId.isBlank()) {
            return nominationRepository.findByNominationId(nominationId).orElse(null);
        }
        return nominationRepository.findByNominatedUserId(userId).stream()
                .filter(item -> item.ativaEm(now))
                .filter(item -> affiliationId == null || affiliationId.isBlank() || affiliationId.equals(item.affiliationId()))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
    }

    private InstitutionalAffiliationRequest resolveRequest(InstitutionalAffiliation affiliation) {
        if (affiliation == null) {
            return null;
        }
        return requestRepository.findLatestByMaterializedAffiliationId(affiliation.affiliationId())
                .orElse(null);
    }

    private long activeAdministrators(String affiliationId, Instant now) {
        return nominationRepository.findByAffiliationId(affiliationId).stream()
                .filter(item -> item.ativaEm(now))
                .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                .count();
    }
}
