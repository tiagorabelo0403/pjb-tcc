package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.infrastructure.InstitutionalRootAdministratorApprovalStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalRootAdministratorApprovalApplicationService {

    private final InstitutionalRootAdministratorApprovalStateRepository repository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final CurrentUserService currentUserService;

    public InstitutionalRootAdministratorApprovalApplicationService(InstitutionalRootAdministratorApprovalStateRepository repository,
                                                                   InstitutionalAffiliationStateRepository affiliationRepository,
                                                                   CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public InstitutionalRootAdministratorApproval consolidar(String affiliationId) {
        InstitutionalAffiliation affiliation = loadAffiliation(affiliationId);
        return repository.findLatestByAffiliationId(affiliationId).orElseGet(() -> pendingEnvelope(affiliationId, affiliation, affiliation == null ? null : affiliation.representanteUsuarioId(), null));
    }

    public InstitutionalRootAdministratorApproval decidir(String affiliationId,
                                                          Long candidateUserId,
                                                          String candidateUserName,
                                                          String approvalSource,
                                                          boolean approved,
                                                          List<String> fundamentos) {
        InstitutionalAffiliation affiliation = loadAffiliation(affiliationId);
        Usuario actor = currentUserService.getRequired();
        InstitutionalRootAdministratorApproval current = repository.findLatestByAffiliationId(affiliationId)
                .orElseGet(() -> pendingEnvelope(affiliationId, affiliation, candidateUserId, candidateUserName));
        Long resolvedCandidateUserId = current.candidateUserId() != null ? current.candidateUserId() : candidateUserId;
        String resolvedCandidateUserName = current.candidateUserName() != null ? current.candidateUserName() : candidateUserName;
        InstitutionalRootAdministratorApproval normalized = resolvedCandidateUserId == null && current.candidateUserId() == null
                ? pendingEnvelope(affiliationId, affiliation, affiliation == null ? null : affiliation.representanteUsuarioId(), resolvedCandidateUserName)
                : new InstitutionalRootAdministratorApproval(
                        current.approvalId(),
                        current.affiliationId(),
                        resolvedCandidateUserId,
                        resolvedCandidateUserName,
                        current.institutionActorUserId(),
                        current.institutionActorName(),
                        current.institutionApproved(),
                        current.institutionApprovedAt(),
                        current.pjbActorUserId(),
                        current.pjbActorName(),
                        current.pjbApproved(),
                        current.pjbApprovedAt(),
                        current.requiresDualApproval(),
                        current.approved(),
                        current.rejected(),
                        current.findings(),
                        current.fundamentos(),
                        current.createdAt(),
                        current.updatedAt(),
                        current.hashIntegridade());
        return repository.save(normalized.decidir(approvalSource, actor.getId(), actor.getNome(), approved, mergeFundamentos(affiliation, approvalSource, fundamentos), Instant.now()));
    }

    public boolean isSatisfied(String affiliationId) {
        InstitutionalAffiliation affiliation = loadAffiliation(affiliationId);
        if (affiliation == null || !affiliation.requerDuplaAprovacaoAdministrador()) {
            return true;
        }
        return repository.findLatestByAffiliationId(affiliationId).map(InstitutionalRootAdministratorApproval::approved).orElse(false);
    }

    private InstitutionalAffiliation loadAffiliation(String affiliationId) {
        return affiliationId == null || affiliationId.isBlank() ? null : affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
    }

    private InstitutionalRootAdministratorApproval pendingEnvelope(String requestedAffiliationId,
                                                                   InstitutionalAffiliation affiliation,
                                                                   Long candidateUserId,
                                                                   String candidateUserName) {
        ArrayList<String> findings = new ArrayList<>();
        if (affiliation == null) {
            findings.add("afiliacao_inexistente");
        } else if (affiliation.requerDuplaAprovacaoAdministrador()) {
            findings.add("aprovacao_institucional_pendente");
            findings.add("aprovacao_pjb_pendente");
        } else {
            findings.add("aprovacao_admin_raiz_unilateral_suficiente_para_este_orgao");
        }
        return new InstitutionalRootAdministratorApproval(
                UUID.randomUUID().toString(),
                affiliation == null ? requestedAffiliationId : affiliation.affiliationId(),
                candidateUserId,
                candidateUserName,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                null,
                affiliation != null && affiliation.requerDuplaAprovacaoAdministrador(),
                false,
                false,
                List.copyOf(findings),
                mergeFundamentos(affiliation, null, List.of()),
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private List<String> mergeFundamentos(InstitutionalAffiliation affiliation, String approvalSource, List<String> fundamentos) {
        ArrayList<String> out = new ArrayList<>();
        out.add("administrador_raiz_institucional_exige_quatro_olhos_para_orgao_sensivel");
        if (affiliation != null) {
            out.add("afiliacao=" + affiliation.affiliationId());
            out.add("orgao=" + affiliation.orgaoSigla());
            out.add("dupla_aprovacao=" + affiliation.requerDuplaAprovacaoAdministrador());
        }
        if (approvalSource != null && !approvalSource.isBlank()) {
            out.add("fonte_aprovacao=" + approvalSource.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (fundamentos != null && !fundamentos.isEmpty()) {
            out.addAll(fundamentos);
        }
        return List.copyOf(out);
    }
}
