package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalFourLevelAccessSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalCaseSummary;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOperationalClosureApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalEntryGuardApplicationService entryGuardApplicationService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;

    public InstitutionalOperationalClosureApplicationService(CurrentUserService currentUserService,
                                                             InstitutionalAffiliationStateRepository affiliationRepository,
                                                             InstitutionalNominationStateRepository nominationRepository,
                                                             InstitutionalEntryGuardApplicationService entryGuardApplicationService,
                                                             InstitutionalEntryContextApplicationService entryContextApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.entryGuardApplicationService = Objects.requireNonNull(entryGuardApplicationService);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
    }

    public InstitutionalFourLevelAccessSummary resolverQuatroNiveisAtual(String affiliationId) {
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        var guard = entryGuardApplicationService.avaliarEntradaAtual();
        InstitutionalAffiliation affiliation = selecionarAfiliacao(affiliationId, guard.affiliationId());
        InstitutionalNomination nomination = selecionarNomeacao(usuario, affiliation, now);
        InstitutionalEntryContext context = selecionarContexto(entryContextApplicationService.resolverContextosAtuais(), nomination);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("nivel_1_cadastro_institucional");
        fundamentos.add("nivel_2_estrutura_orgao_unidade_caixa_capacidade");
        fundamentos.add("nivel_3_pessoa_autenticada_vinculada_ao_orgao");
        fundamentos.add("nivel_4_contexto_operacional_ativo");
        fundamentos.add("modelo_texto_orgao_nao_loga_sozinho");
        if (guard.identityBaseProfile() != null) {
            fundamentos.add("identidade_base=" + guard.identityBaseProfile().identityCode());
        }
        fundamentos.addAll(guard.fundamentos());
        if (context != null) {
            fundamentos.addAll(context.fundamentosEntrada());
        }
        return new InstitutionalFourLevelAccessSummary(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                guard.identityBaseProfile() == null ? null : guard.identityBaseProfile().identityCode(),
                affiliation == null ? null : affiliation.affiliationId(),
                affiliation == null ? null : affiliation.destinatarioKind(),
                affiliation == null ? null : affiliation.organizationScope(),
                affiliation == null ? null : affiliation.orgaoSigla(),
                affiliation == null ? null : affiliation.orgaoNome(),
                context != null ? context.unidadeCodigo() : nomination == null ? affiliation == null ? null : affiliation.unidadeCodigo() : nomination.unidadeCodigo(),
                context != null ? context.unidadeNome() : affiliation == null ? null : affiliation.unidadeNome(),
                context != null ? context.caixaCodigo() : nomination == null ? null : nomination.caixaCodigo(),
                context != null ? context.caixaNome() : nomination == null ? null : nomination.caixaCodigo(),
                nomination == null ? null : nomination.nominationRole(),
                context != null ? context.funcaoOperacional() : nomination == null ? null : nomination.funcaoOperacional(),
                context != null ? context.processProfile() : nomination == null ? null : nomination.processProfile(),
                context != null ? context.capacidades() : nomination == null ? Set.of() : nomination.capacidades(),
                context != null ? context.landingPanel() : nomination == null ? null : nomination.panelPreferencial(),
                affiliation != null,
                nomination != null,
                nomination != null,
                context != null,
                context != null && context.plantaoAtivo(),
                context != null && context.substituicaoAtiva(),
                context != null && context.delegacaoAtiva(),
                guard.autorizado() && context != null && nomination != null && affiliation != null,
                List.copyOf(fundamentos),
                now
        );
    }

    public List<InstitutionalOperationalCaseSummary> listarCasosOperacionais(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("Afiliação institucional não encontrada."));
        Instant now = Instant.now();
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationId(affiliationId).stream()
                .filter(item -> item.ativaEm(now))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        ArrayList<InstitutionalOperationalCaseSummary> cases = new ArrayList<>();
        DestinatarioInstitucionalKind kind = affiliation.destinatarioKind();
        if (isMinisterioPublico(kind)) {
            cases.add(buildMinisterioPublicoCase(affiliation, nominations, now));
        }
        if (isDefensoria(kind)) {
            cases.add(buildDefensoriaCase(affiliation, nominations, now));
        }
        if (isCustodia(kind)) {
            cases.add(buildCustodiaCase(affiliation, nominations, now));
        }
        if (cases.isEmpty()) {
            cases.add(buildGenericCase(affiliation, nominations, now));
        }
        return List.copyOf(cases);
    }

    private InstitutionalAffiliation selecionarAfiliacao(String requestedAffiliationId, String guardedAffiliationId) {
        String effectiveId = firstNonBlank(requestedAffiliationId, guardedAffiliationId);
        if (effectiveId == null) {
            return null;
        }
        return affiliationRepository.findByAffiliationId(effectiveId).orElse(null);
    }

    private InstitutionalNomination selecionarNomeacao(Usuario usuario, InstitutionalAffiliation affiliation, Instant now) {
        if (usuario == null || usuario.getId() == null) {
            return null;
        }
        return nominationRepository.findByNominatedUserId(usuario.getId()).stream()
                .filter(item -> item.ativaEm(now))
                .filter(item -> affiliation == null || item.affiliationId().equals(affiliation.affiliationId()))
                .sorted(Comparator.comparing((InstitutionalNomination item) -> item.nominationRole() != null && item.nominationRole().isGestaoMestre() ? 1 : 0, Comparator.reverseOrder())
                        .thenComparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .findFirst()
                .orElse(null);
    }

    private InstitutionalEntryContext selecionarContexto(List<InstitutionalEntryContext> contexts, InstitutionalNomination nomination) {
        if (contexts == null || contexts.isEmpty()) {
            return null;
        }
        if (nomination == null) {
            return contexts.getFirst();
        }
        return contexts.stream()
                .filter(item -> item.unidadeCodigo().equalsIgnoreCase(nomination.unidadeCodigo()))
                .filter(item -> item.caixaCodigo().equalsIgnoreCase(nomination.caixaCodigo()))
                .findFirst()
                .orElse(contexts.getFirst());
    }

    private InstitutionalOperationalCaseSummary buildMinisterioPublicoCase(InstitutionalAffiliation affiliation,
                                                                           List<InstitutionalNomination> nominations,
                                                                           Instant now) {
        InstitutionalNomination triagem = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM
                        || item.nominationRole() == InstitutionalNominationRole.TRIAGEM_ORGAO);
        InstitutionalNomination minuta = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.PREPARAR_MINUTA)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL
                        || item.nominationRole() == InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL);
        InstitutionalNomination titular = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.MEMBRO_TITULAR
                        || item.nominationRole() == InstitutionalNominationRole.TITULAR_INSTITUCIONAL);
        InstitutionalNomination primary = firstPresent(triagem, minuta, titular, nominations.isEmpty() ? null : nominations.getFirst());
        return new InstitutionalOperationalCaseSummary(
                "MP_VISTA_OBRIGATORIA",
                "Vista obrigatória ao Ministério Público",
                affiliation.destinatarioKind(),
                affiliation.orgaoSigla(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                primary == null ? null : primary.caixaCodigo(),
                primary == null ? null : laneName(primary),
                triagem != null || minuta != null || titular != null,
                triagem != null,
                minuta != null,
                titular != null,
                titular != null,
                false,
                false,
                true,
                titular == null ? null : titular.panelPreferencial(),
                List.of(
                        "identifica_destinatario_juridico_mp",
                        "resolve_unidade_competente_promotoria",
                        "entrega_na_caixa_institucional",
                        "permite_triagem_por_servidor_autorizado=" + (triagem != null),
                        "permite_minuta_por_assessor=" + (minuta != null),
                        "exige_manifestacao_do_titular=" + (titular != null),
                        "nomeacoes_ativas=" + nominations.size(),
                        "timestamp=" + now),
                now
        );
    }

    private InstitutionalOperationalCaseSummary buildDefensoriaCase(InstitutionalAffiliation affiliation,
                                                                    List<InstitutionalNomination> nominations,
                                                                    Instant now) {
        InstitutionalNomination recepcao = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM);
        InstitutionalNomination defensor = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.MEMBRO_TITULAR
                        || item.nominationRole() == InstitutionalNominationRole.TITULAR_INSTITUCIONAL);
        InstitutionalNomination assessor = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.PREPARAR_MINUTA)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL);
        InstitutionalNomination primary = firstPresent(recepcao, assessor, defensor, nominations.isEmpty() ? null : nominations.getFirst());
        return new InstitutionalOperationalCaseSummary(
                "DEFENSORIA_REU_SEM_ADVOGADO",
                "Atuação defensorial para réu sem advogado",
                affiliation.destinatarioKind(),
                affiliation.orgaoSigla(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                primary == null ? null : primary.caixaCodigo(),
                primary == null ? null : laneName(primary),
                recepcao != null || defensor != null || assessor != null,
                recepcao != null,
                assessor != null,
                defensor != null,
                defensor != null,
                false,
                false,
                true,
                defensor == null ? null : defensor.panelPreferencial(),
                List.of(
                        "identifica_destinatario_defensoria",
                        "resolve_nucleo_competente",
                        "entrega_na_caixa_institucional",
                        "servidor_recebe=" + (recepcao != null),
                        "defensor_atua=" + (defensor != null),
                        "peticao_retorna_aos_autos=" + (defensor != null),
                        "nomeacoes_ativas=" + nominations.size(),
                        "timestamp=" + now),
                now
        );
    }

    private InstitutionalOperationalCaseSummary buildCustodiaCase(InstitutionalAffiliation affiliation,
                                                                  List<InstitutionalNomination> nominations,
                                                                  Instant now) {
        InstitutionalNomination custodia = chooseNomination(nominations,
                item -> hasAny(item, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA)
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.GESTOR_CAIXA);
        InstitutionalNomination diretor = chooseNomination(nominations,
                item -> item.nominationRole() == InstitutionalNominationRole.DIRETOR_UNIDADE_PRISIONAL
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE
                        || item.funcaoOperacional() == FuncaoOperacionalInstitucional.MEMBRO_TITULAR);
        InstitutionalNomination primary = firstPresent(custodia, diretor, nominations.isEmpty() ? null : nominations.getFirst());
        return new InstitutionalOperationalCaseSummary(
                "CUSTODIA_AUDIENCIA_REU_PRESO",
                "Audiência de réu preso com entrega à unidade custodiante",
                affiliation.destinatarioKind(),
                affiliation.orgaoSigla(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                primary == null ? null : primary.caixaCodigo(),
                primary == null ? null : laneName(primary),
                custodia != null || diretor != null,
                false,
                false,
                false,
                false,
                diretor != null || custodia != null,
                diretor != null || custodia != null,
                true,
                primary == null ? null : primary.panelPreferencial(),
                List.of(
                        "identifica_destinatario_unidade_custodiante",
                        "resolve_unidade_prisional_competente",
                        "entrega_na_caixa_de_custodia_ou_apresentacao",
                        "confirmacao_de_custodia=" + (diretor != null || custodia != null),
                        "registro_de_cumprimento=" + (diretor != null || custodia != null),
                        "nomeacoes_ativas=" + nominations.size(),
                        "timestamp=" + now),
                now
        );
    }

    private InstitutionalOperationalCaseSummary buildGenericCase(InstitutionalAffiliation affiliation,
                                                                 List<InstitutionalNomination> nominations,
                                                                 Instant now) {
        InstitutionalNomination primary = nominations.isEmpty() ? null : nominations.getFirst();
        boolean receives = hasAny(primary, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA);
        boolean drafts = hasAny(primary, CapacidadeCaixaInstitucional.PREPARAR_MINUTA);
        boolean signs = hasAny(primary, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO);
        return new InstitutionalOperationalCaseSummary(
                "OPERACAO_INSTITUCIONAL_PADRAO",
                "Operação institucional padrão do órgão vinculado",
                affiliation.destinatarioKind(),
                affiliation.orgaoSigla(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                primary == null ? null : primary.caixaCodigo(),
                primary == null ? null : laneName(primary),
                receives || drafts || signs,
                receives,
                drafts,
                signs,
                signs,
                false,
                false,
                signs,
                primary == null ? null : primary.panelPreferencial(),
                List.of(
                        "destinatario_juridico_resolvido",
                        "orgao_unidade_caixa_capacidade_materializados",
                        "nomeacoes_ativas=" + nominations.size(),
                        "timestamp=" + now),
                now
        );
    }

    private InstitutionalNomination chooseNomination(List<InstitutionalNomination> nominations,
                                                     java.util.function.Predicate<InstitutionalNomination> predicate) {
        return nominations.stream().filter(predicate).findFirst().orElse(null);
    }

    private boolean hasAny(InstitutionalNomination nomination, CapacidadeCaixaInstitucional... capacities) {
        if (nomination == null || nomination.capacidades() == null || nomination.capacidades().isEmpty() || capacities == null) {
            return false;
        }
        for (CapacidadeCaixaInstitucional capacity : capacities) {
            if (nomination.capacidades().contains(capacity)) {
                return true;
            }
        }
        return false;
    }

    private InstitutionalNomination firstPresent(InstitutionalNomination... values) {
        if (values == null) {
            return null;
        }
        for (InstitutionalNomination value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String laneName(InstitutionalNomination nomination) {
        if (nomination == null) {
            return null;
        }
        return Optional.ofNullable(nomination.accessLaneKind()).map(Enum::name).orElse(nomination.caixaCodigo());
    }

    private boolean isMinisterioPublico(DestinatarioInstitucionalKind kind) {
        return kind == DestinatarioInstitucionalKind.MINISTERIO_PUBLICO;
    }

    private boolean isDefensoria(DestinatarioInstitucionalKind kind) {
        return kind == DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA;
    }

    private boolean isCustodia(DestinatarioInstitucionalKind kind) {
        return kind == DestinatarioInstitucionalKind.POLICIA_PENAL
                || kind == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL;
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
}
