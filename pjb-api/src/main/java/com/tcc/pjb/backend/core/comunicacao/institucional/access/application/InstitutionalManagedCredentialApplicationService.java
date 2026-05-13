package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.infrastructure.InstitutionalManagedCredentialStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalManagedCredentialApplicationService {

    private final InstitutionalManagedCredentialStateRepository repository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService;

    public InstitutionalManagedCredentialApplicationService(InstitutionalManagedCredentialStateRepository repository,
                                                           InstitutionalAffiliationStateRepository affiliationRepository,
                                                           InstitutionalNominationStateRepository nominationRepository,
                                                           InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService) {
        this.repository = Objects.requireNonNull(repository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.rootAdministratorApprovalApplicationService = Objects.requireNonNull(rootAdministratorApprovalApplicationService);
    }

    public InstitutionalManagedCredential emitir(String affiliationId,
                                                 String nominationId,
                                                 Long nominatedUserId,
                                                 String displayName,
                                                 String laneCode,
                                                 List<String> allowedNetworks,
                                                 Integer rotationWindowDays,
                                                 List<String> fundamentos) {
        InstitutionalAffiliation affiliation = loadAffiliation(affiliationId);
        InstitutionalNomination nomination = resolveNomination(affiliationId, nominationId, nominatedUserId);
        ArrayList<String> findings = new ArrayList<>();
        boolean rootApprovalSatisfied = rootAdministratorApprovalApplicationService.isSatisfied(affiliationId);
        boolean sensitive = nomination != null && isSensitiveNomination(nomination);
        boolean allowsManagedLogin = !sensitive;
        boolean govBrBindingConfirmed = nomination != null && nomination.ativaEm(Instant.now());
        if (affiliation == null) {
            findings.add("afiliacao_inexistente");
        }
        if (nomination == null) {
            findings.add("nomeacao_nao_localizada");
        }
        if (sensitive) {
            findings.add("faixa_assinante_ou_peticionante_nao_pode_receber_login_gerenciado");
        }
        if (!rootApprovalSatisfied) {
            findings.add("aprovacao_admin_raiz_pendente");
        }
        if (!govBrBindingConfirmed) {
            findings.add("vinculo_govbr_pessoal_pendente");
        }
        String status = affiliation == null || nomination == null
                ? "INCONSISTENTE"
                : !rootApprovalSatisfied
                    ? "PENDENTE_APROVACAO_ADMIN_RAIZ"
                    : !allowsManagedLogin
                        ? "BLOQUEADA_ASSINANTE"
                        : !govBrBindingConfirmed
                            ? "PENDENTE_VINCULO_GOVBR"
                            : "ATIVA";
        InstitutionalManagedCredential credential = new InstitutionalManagedCredential(
                UUID.randomUUID().toString(),
                affiliationId,
                nomination == null ? nominationId : nomination.nominationId(),
                nomination == null ? nominatedUserId : nomination.nominatedUserId(),
                nomination == null ? null : nomination.nominatedUserName(),
                buildManagedUsername(affiliation, nomination, laneCode),
                displayName == null || displayName.isBlank() ? nomination == null ? "Credencial institucional" : nomination.nominatedUserName() : displayName.trim(),
                laneCode == null || laneCode.isBlank() ? nomination == null || nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name() : laneCode.trim(),
                sensitive,
                allowsManagedLogin,
                true,
                govBrBindingConfirmed,
                status,
                rotationWindowDays == null ? 90 : rotationWindowDays,
                allowedNetworks,
                List.copyOf(findings),
                buildFundamentos(affiliation, nomination, fundamentos, rootApprovalSatisfied),
                Instant.now(),
                Instant.now(),
                null
        );
        return repository.save(credential);
    }

    public InstitutionalManagedCredential revogar(String credentialId, List<String> fundamentos) {
        InstitutionalManagedCredential current = repository.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial gerenciada não localizada."));
        return repository.save(current.withStatus("REVOGADA", current.findings(), fundamentos, Instant.now()));
    }

    public List<InstitutionalManagedCredential> listar(String affiliationId) {
        return repository.findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalManagedCredential::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private InstitutionalAffiliation loadAffiliation(String affiliationId) {
        return affiliationId == null || affiliationId.isBlank() ? null : affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
    }

    private InstitutionalNomination resolveNomination(String affiliationId, String nominationId, Long userId) {
        if (nominationId != null && !nominationId.isBlank()) {
            return nominationRepository.findByNominationId(nominationId).orElse(null);
        }
        if (userId == null) {
            return null;
        }
        return nominationRepository.findByNominatedUserId(userId).stream()
                .filter(item -> affiliationId == null || affiliationId.isBlank() || affiliationId.equals(item.affiliationId()))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private boolean isSensitiveNomination(InstitutionalNomination nomination) {
        if (nomination.requerCertificadoICP()) {
            return true;
        }
        if (nomination.funcaoOperacional() != null && nomination.funcaoOperacional().isFuncaoAssinantePreferencial()) {
            return true;
        }
        return nomination.capacidades() != null && nomination.capacidades().stream().anyMatch(cap -> cap.isAtoDeAssinaturaOuManifestacao() || cap.isAtoDeCiencia());
    }

    private String buildManagedUsername(InstitutionalAffiliation affiliation,
                                        InstitutionalNomination nomination,
                                        String laneCode) {
        String sigla = affiliation == null || affiliation.orgaoSigla() == null ? "PJB" : affiliation.orgaoSigla().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        String lane = laneCode == null || laneCode.isBlank()
                ? nomination == null || nomination.accessLaneKind() == null ? "lane" : nomination.accessLaneKind().name().toLowerCase(Locale.ROOT)
                : laneCode.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        String user = nomination == null || nomination.nominatedUserId() == null ? "usr" : String.valueOf(nomination.nominatedUserId());
        return sigla + "." + lane + "." + user;
    }

    private List<String> buildFundamentos(InstitutionalAffiliation affiliation,
                                          InstitutionalNomination nomination,
                                          List<String> fundamentos,
                                          boolean rootApprovalSatisfied) {
        ArrayList<String> out = new ArrayList<>();
        out.add("login_institucional_gerenciado_so_e_permitido_para_rotina_nao_assinante");
        out.add("identidade_pessoal_govbr_permanece_raiz_para_trilha_de_responsabilizacao");
        out.add("aprovacao_admin_raiz=" + rootApprovalSatisfied);
        if (affiliation != null) {
            out.add("afiliacao=" + affiliation.affiliationId());
            out.add("orgao=" + affiliation.orgaoSigla());
        }
        if (nomination != null && nomination.accessLaneKind() != null) {
            out.add("lane=" + nomination.accessLaneKind().name());
        }
        if (fundamentos != null && !fundamentos.isEmpty()) {
            out.addAll(fundamentos);
        }
        return List.copyOf(out);
    }
}
