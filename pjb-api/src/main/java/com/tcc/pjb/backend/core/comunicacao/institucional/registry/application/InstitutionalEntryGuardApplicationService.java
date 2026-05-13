package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustAssessmentApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalIdentityBaseProfileResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalEntryGuardSummary;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalEntryGuardApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver;
    private final InstitutionalTrustAssessmentApplicationService trustAssessmentApplicationService;

    public InstitutionalEntryGuardApplicationService(CurrentUserService currentUserService,
                                                     InstitutionalAffiliationStateRepository affiliationRepository,
                                                     InstitutionalNominationStateRepository nominationRepository,
                                                     InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                     InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver,
                                                     InstitutionalTrustAssessmentApplicationService trustAssessmentApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.identityBaseProfileResolver = Objects.requireNonNull(identityBaseProfileResolver);
        this.trustAssessmentApplicationService = Objects.requireNonNull(trustAssessmentApplicationService);
    }

    public InstitutionalEntryGuardSummary avaliarEntradaAtual() {
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        List<InstitutionalNomination> activeNominations = nominationRepository.findByNominatedUserId(usuario.getId()).stream()
                .filter(item -> item.ativaEm(now))
                .sorted(Comparator.comparing((InstitutionalNomination item) -> item.trustFloor() == null ? 0 : item.trustFloor().ordem()).reversed()
                        .thenComparing(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre() ? 1 : 0, Comparator.reverseOrder())
                        .thenComparing(InstitutionalNomination::updatedAt, Comparator.reverseOrder()))
                .toList();
        Map<String, InstitutionalAffiliation> affiliations = affiliationRepository.findByAffiliationIds(activeNominations.stream()
                        .map(InstitutionalNomination::affiliationId)
                        .toList()).stream()
                .filter(InstitutionalAffiliation::ativa)
                .collect(Collectors.toMap(InstitutionalAffiliation::affiliationId, Function.identity(), (left, right) -> right));
        activeNominations = activeNominations.stream()
                .filter(item -> affiliations.containsKey(item.affiliationId()))
                .toList();
        InstitutionalNomination primaryNomination = activeNominations.stream().findFirst().orElse(null);
        InstitutionalAffiliation primaryAffiliation = primaryNomination == null ? null : affiliations.get(primaryNomination.affiliationId());
        List<InstitutionalEntryContext> activeContexts = entryContextApplicationService.resolverContextosAtuais().stream()
                .filter(item -> primaryNomination == null
                        || (item.unidadeCodigo().equalsIgnoreCase(primaryNomination.unidadeCodigo())
                        && item.caixaCodigo().equalsIgnoreCase(primaryNomination.caixaCodigo())))
                .toList();
        InstitutionalTrustAssessment assessment = trustAssessmentApplicationService.avaliar(usuario, primaryAffiliation, primaryNomination);
        boolean identidadeAutenticada = usuario.getId() != null;
        boolean vinculoValido = primaryAffiliation != null && primaryAffiliation.ativa() && primaryNomination != null && primaryNomination.ativaEm(now);
        boolean contextoAtivo = !activeContexts.isEmpty();
        boolean autorizado = assessment.autorizado() && vinculoValido && contextoAtivo;
        return new InstitutionalEntryGuardSummary(
                usuario.getId(),
                usuario.getNome(),
                identityBaseProfileResolver.resolve(usuario),
                identidadeAutenticada,
                vinculoValido,
                contextoAtivo,
                autorizado,
                primaryAffiliation == null ? null : primaryAffiliation.affiliationId(),
                primaryNomination == null ? null : primaryNomination.nominationId(),
                assessment,
                activeContexts,
                trilhosAutenticacao(primaryAffiliation, primaryNomination),
                List.of("orgao", "unidade", "caixa", "papel", "capacidade", "plantao", "substituicao", "delegacao"),
                fundamentos(assessment, activeNominations, activeContexts, vinculoValido, contextoAtivo, autorizado),
                now
        );
    }

    private List<String> trilhosAutenticacao(InstitutionalAffiliation affiliation, InstitutionalNomination nomination) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("identidade_pessoal_forte");
        values.add("mfa_perfis_sensiveis");
        values.add("sso_institucional_quando_houver");
        values.add("trilha_forense_completa");
        if ((affiliation != null && affiliation.requerCertificadoICP()) || (nomination != null && nomination.requerCertificadoICP())) {
            values.add("certificado_qualificado_quando_o_ato_exigir");
        }
        if ((affiliation != null && affiliation.restringeCertificadoRedeInstitucional()) || (nomination != null && nomination.requerRedeInstitucional())) {
            values.add("rede_institucional_ou_autorizacao_remota");
        }
        return List.copyOf(values);
    }

    private List<String> fundamentos(InstitutionalTrustAssessment assessment,
                                     List<InstitutionalNomination> activeNominations,
                                     List<InstitutionalEntryContext> activeContexts,
                                     boolean vinculoValido,
                                     boolean contextoAtivo,
                                     boolean autorizado) {
        ArrayList<String> values = new ArrayList<>();
        values.add("pessoa_autenticada");
        values.add("modelo_responsabilidade=identidade_pessoal_raiz_com_contexto_institucional_delegado");
        values.add("nomeacoes_ativas=" + activeNominations.size());
        values.add("contextos_ativos=" + activeContexts.size());
        if (vinculoValido) {
            values.add("vinculo_institucional_valido");
        }
        if (contextoAtivo) {
            values.add("contexto_operacional_ativo");
        }
        values.add("autorizado=" + autorizado);
        values.addAll(assessment.reasons());
        return List.copyOf(values.stream().distinct().toList());
    }
}
