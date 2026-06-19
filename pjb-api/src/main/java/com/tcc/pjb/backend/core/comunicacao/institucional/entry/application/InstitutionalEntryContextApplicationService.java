package com.tcc.pjb.backend.core.comunicacao.institucional.entry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalOperationalCoverageRuleStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalEntryContextApplicationService {

    private final CurrentUserService currentUserService;
    private final VinculoUsuarioCaixaInstitucionalResolver vinculoResolver;
    private final InstitutionalInboxStateRepository inboxRepository;
    private final InstitutionalDelegationAssignmentStateRepository delegationRepository;
    private final InstitutionalOperationalCoverageRuleStateRepository coverageRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;

    public InstitutionalEntryContextApplicationService(CurrentUserService currentUserService,
                                                       VinculoUsuarioCaixaInstitucionalResolver vinculoResolver,
                                                       InstitutionalInboxStateRepository inboxRepository,
                                                       InstitutionalDelegationAssignmentStateRepository delegationRepository,
                                                       InstitutionalOperationalCoverageRuleStateRepository coverageRepository,
                                                       InstitutionalNominationStateRepository nominationRepository,
                                                       InstitutionalAffiliationStateRepository affiliationRepository,
                                                       InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver,
                                                       InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.vinculoResolver = Objects.requireNonNull(vinculoResolver);
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.delegationRepository = Objects.requireNonNull(delegationRepository);
        this.coverageRepository = Objects.requireNonNull(coverageRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.identityBaseProfileResolver = Objects.requireNonNull(identityBaseProfileResolver);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public InstitutionalEntrySummary resolverEntradaAtual() {
        Usuario usuario = currentUserService.getRequired();
        List<InstitutionalEntryContext> contextos = resolverContextos(usuario);
        InstitutionalEntryContext preferencial = contextos.stream().findFirst().orElse(null);
        InstitutionalIdentityBaseProfile identidadeBase = identityBaseProfileResolver.resolve(usuario);
        return new InstitutionalEntrySummary(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                identidadeBase,
                possuiAmbientePessoal(usuario),
                !contextos.isEmpty(),
                contextos,
                preferencial,
                Instant.now()
        );
    }

    public List<InstitutionalEntryContext> resolverContextosAtuais() {
        return resolverContextos(currentUserService.getRequired());
    }

    private List<InstitutionalEntryContext> resolverContextos(Usuario usuario) {
        Instant now = Instant.now();
        List<InstitutionalDelegationAssignment> delegacoes = delegationRepository.findByDelegadoUsuarioId(usuario.getId());
        List<InstitutionalOperationalCoverageRule> coberturas = coverageRepository.findByCoberturaUsuarioId(usuario.getId());
        List<InstitutionalNomination> nomeacoes = nominationRepository.findByNominatedUserId(usuario.getId());
        var affiliationIds = nomeacoes.stream()
                .map(InstitutionalNomination::affiliationId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<InstitutionalAffiliation> afiliacoes = affiliationRepository.findByAffiliationIds(affiliationIds);
        Map<String, InstitutionalAffiliation> afiliacoesPorId = afiliacoes.stream()
                .collect(java.util.stream.Collectors.toMap(InstitutionalAffiliation::affiliationId, java.util.function.Function.identity(), (left, right) -> right));
        Map<String, List<InstitutionalInboxItem>> inboxPorCaixa = new java.util.concurrent.ConcurrentHashMap<>();
        List<InstitutionalEntryContext> contextos = new ArrayList<>();
        for (DestinatarioInstitucionalKind kind : destinatariosElegiveis(usuario, nomeacoes, afiliacoesPorId)) {
            for (VinculoUsuarioCaixaInstitucional vinculo : vinculoResolver.resolver(usuario, kind, usuario.getUf(), usuario.getComarca())) {
                boolean delegacaoAtiva = delegacoes.stream().anyMatch(item -> item.unidadeCodigo().equalsIgnoreCase(vinculo.unidade().codigo())
                        && item.caixaCodigo().equalsIgnoreCase(vinculo.caixa().codigo())
                        && item.ativaEm(now));
                boolean coberturaAtiva = coberturas.stream().anyMatch(item -> item.unidadeCodigo().equalsIgnoreCase(vinculo.unidade().codigo())
                        && item.caixaCodigo().equalsIgnoreCase(vinculo.caixa().codigo())
                        && item.ativaEm(now));
                boolean substituicaoAtiva = vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.SUBSTITUTO || coberturaAtiva;
                boolean plantaoAtivo = vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.PLANTONISTA;
                InstitutionalNomination nomeacaoAtiva = nomeacoes.stream()
                        .filter(item -> item.ativaEm(now))
                        .filter(item -> item.unidadeCodigo().equalsIgnoreCase(vinculo.unidade().codigo()))
                        .filter(item -> item.caixaCodigo().equalsIgnoreCase(vinculo.caixa().codigo()))
                        .findFirst()
                        .orElse(null);
                String inboxKey = vinculo.unidade().codigo() + "|" + vinculo.caixa().codigo();
                List<InstitutionalInboxItem> inboxCaixa = inboxPorCaixa.computeIfAbsent(inboxKey,
                        ignored -> inboxRepository.findByUnidadeCodigoAndCaixaCodigo(vinculo.unidade().codigo(), vinculo.caixa().codigo()));
                long pendencias = inboxCaixa.stream().filter(item -> !item.status().isTerminal()).count();
                long semLeitura = inboxCaixa.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA).count();
                long urgentes = inboxCaixa.stream().filter(item -> item.bloqueiaFluxo() || (item.prazoCienciaEm() != null && item.prazoCienciaEm().isBefore(now.plusSeconds(6 * 3600)))).count();
                long atribuidas = inboxCaixa.stream().filter(item -> Objects.equals(item.atribuidoUsuarioId(), usuario.getId())).count();
                InstitutionalOrganizationBlueprint blueprint = resolveBlueprint(vinculo);
                InstitutionalAccessLaneBlueprint laneBlueprint = resolveLaneBlueprint(usuario, vinculo, blueprint);
                InstitutionalProcessProfile processProfile = nomeacaoAtiva != null
                        ? nomeacaoAtiva.processProfile()
                        : laneBlueprint != null && laneBlueprint.processProfile() != null
                            ? laneBlueprint.processProfile()
                            : resolverPerfilProcessual(usuario, vinculo);
                InstitutionalEntryLandingPanel painel = nomeacaoAtiva != null && nomeacaoAtiva.panelPreferencial() != null
                        ? nomeacaoAtiva.panelPreferencial()
                        : laneBlueprint != null && laneBlueprint.panel() != null
                            ? laneBlueprint.panel()
                            : resolverPainel(vinculo.funcaoOperacional(), vinculo.unidade().destinatarioKind(), processProfile);
                int prioridade = calcularPrioridade(vinculo, pendencias, semLeitura, urgentes, delegacaoAtiva, coberturaAtiva, plantaoAtivo);
                contextos.add(new InstitutionalEntryContext(
                        UUID.nameUUIDFromBytes((usuario.getId() + "|" + vinculo.unidade().codigo() + "|" + vinculo.caixa().codigo() + "|" + vinculo.funcaoOperacional().name()).getBytes()).toString(),
                        vinculo.unidade().destinatarioKind(),
                        vinculo.unidade().destinatarioKind().toOrganizacaoExtraJudicialKind(),
                        vinculo.unidade().sigla(),
                        vinculo.unidade().nomeOficial(),
                        vinculo.unidade().codigo(),
                        vinculo.unidade().unidade() == null ? vinculo.unidade().nomeOficial() : vinculo.unidade().unidade(),
                        vinculo.unidade().nucleo(),
                        vinculo.unidade().uf(),
                        vinculo.unidade().comarca(),
                        vinculo.caixa().codigo(),
                        vinculo.caixa().nomeExibicao(),
                        processProfile,
                        vinculo.funcaoOperacional(),
                        copyCapacidades(vinculo),
                        delegacaoAtiva,
                        substituicaoAtiva,
                        coberturaAtiva,
                        plantaoAtivo,
                        pendencias,
                        semLeitura,
                        urgentes,
                        atribuidas,
                        painel,
                        resolverLandingPath(vinculo, painel),
                        colorFor(vinculo.funcaoOperacional(), vinculo.unidade().destinatarioKind(), processProfile, urgentes, plantaoAtivo),
                        prioridade,
                        fundamentos(usuario, vinculo, nomeacaoAtiva, blueprint, laneBlueprint, delegacaoAtiva, coberturaAtiva, plantaoAtivo, pendencias, urgentes)
                ));
            }
        }
        return contextos.stream()
                .sorted(Comparator.comparingInt(InstitutionalEntryContext::prioridade).reversed()
                        .thenComparing(InstitutionalEntryContext::orgaoSigla)
                        .thenComparing(InstitutionalEntryContext::caixaCodigo))
                .toList();
    }

    private Set<DestinatarioInstitucionalKind> destinatariosElegiveis(Usuario usuario,
                                                                      List<InstitutionalNomination> nomeacoes,
                                                                      java.util.Map<String, InstitutionalAffiliation> afiliacoesPorId) {
        LinkedHashSet<DestinatarioInstitucionalKind> kinds = new LinkedHashSet<>();
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return Set.of();
        }
        if (tipo.isMinisterioPublico()) {
            kinds.add(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);
        }
        if (tipo.isDefensoriaPublica()) {
            kinds.add(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA);
        }
        if (tipo.isProcuradoria()) {
            kinds.add(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA);
            kinds.add(DestinatarioInstitucionalKind.FAZENDA_PUBLICA);
            switch (tipo) {
                case PROCURADORIA_ESTADUAL -> kinds.add(DestinatarioInstitucionalKind.PROCURADORIA_ESTADO);
                case PROCURADORIA_MUNICIPAL -> kinds.add(DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO);
                case PROCURADORIA_FEDERAL -> kinds.add(DestinatarioInstitucionalKind.AGU);
                default -> { }
            }
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.AGENTE_POLICIAL || tipo == TipoUsuario.ESCRIVAO_POLICIAL) {
            kinds.add(DestinatarioInstitucionalKind.DELEGACIA_POLICIA);
            kinds.add(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL);
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            kinds.add(DestinatarioInstitucionalKind.DELEGACIA_POLICIA);
            kinds.add(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL);
        }
        if (tipo.isPerito()) {
            kinds.add(DestinatarioInstitucionalKind.PERICIA_JUDICIAL);
            kinds.add(DestinatarioInstitucionalKind.PERITO_JUDICIAL);
            kinds.add(DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO);
        }
        if (tipo.isSaude()) {
            kinds.add(DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO);
        }
        if (tipo == TipoUsuario.PSICOLOGO_JUDICIAL) {
            kinds.add(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL);
        }
        if (tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL) {
            kinds.add(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL);
            kinds.add(DestinatarioInstitucionalKind.ASSISTENTE_SOCIAL_JUDICIAL);
        }
        if (tipo == TipoUsuario.CONTADOR_JUDICIAL || tipo.isServidorJudiciario()) {
            kinds.add(DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL);
        }
        if (tipo.isConciliacaoMediacao() || tipo.isServidorJudiciario()) {
            kinds.add(DestinatarioInstitucionalKind.CEJUSC);
        }
        if (tipo.isCartorioExtrajudicial()) {
            kinds.add(DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL);
        }
        if (tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario()) {
            kinds.add(DestinatarioInstitucionalKind.JUIZO_DEPRECADO);
            kinds.add(DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO);
        }
        Instant now = Instant.now();
        if (nomeacoes != null && afiliacoesPorId != null) {
            for (InstitutionalNomination nomeacao : nomeacoes) {
                if (nomeacao != null && nomeacao.ativaEm(now)) {
                    InstitutionalAffiliation afiliacao = afiliacoesPorId.get(nomeacao.affiliationId());
                    if (afiliacao != null && afiliacao.ativa()) {
                        kinds.add(afiliacao.destinatarioKind());
                    }
                }
            }
        }
        return kinds;
    }

    private InstitutionalProcessProfile resolverPerfilProcessual(Usuario usuario, VinculoUsuarioCaixaInstitucional vinculo) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return InstitutionalProcessProfile.PERFIL_HIBRIDO;
        }
        return switch (tipo) {
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> InstitutionalProcessProfile.PROMOTOR;
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> InstitutionalProcessProfile.DEFENSOR;
            case PROCURADOR, PROCURADORIA_ESTADUAL, PROCURADORIA_MUNICIPAL, PROCURADORIA_FEDERAL -> InstitutionalProcessProfile.PROCURADOR;
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL -> InstitutionalProcessProfile.DELEGADO;
            case PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA, PERITO_DIGITAL, PERITO_INSS, PERITO_MEDICO -> InstitutionalProcessProfile.PERITO_JUDICIAL;
            case MEDICO, HOSPITAL, UPA, CLINICA -> InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO;
            case PSICOLOGO_JUDICIAL -> InstitutionalProcessProfile.PSICOLOGO_JUDICIAL;
            case ASSISTENTE_SOCIAL_JUDICIAL -> InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL;
            case CONTADOR_JUDICIAL -> InstitutionalProcessProfile.CONTADOR_JUDICIAL;
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR, ASSISTENTE_TECNICO, ADMINISTRADOR_JUDICIAL, LEILOEIRO_JUDICIAL, CURADOR_ESPECIAL, CURADOR_AUSENTES, INVENTARIANTE -> InstitutionalProcessProfile.TECNICO_INSTITUCIONAL;
            case CONCILIADOR_CEJUSC -> InstitutionalProcessProfile.CONCILIADOR;
            case MEDIADOR, ARBITRO -> InstitutionalProcessProfile.MEDIADOR;
            case ASSESSOR_JUDICIAL, ASSESSOR_DESEMBARGADOR, ASSESSOR_MINISTRO -> InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL;
            case TABELIAO, REGISTRADOR_IMOVEIS, ESCREVENTE_CARTORIO -> InstitutionalProcessProfile.CARTORIO_EXTRAJUDICIAL;
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO, DESEMBARGADOR, DESEMBARGADOR_FEDERAL, MINISTRO -> InstitutionalProcessProfile.MAGISTRADO_COOPERANTE;
            case ADMINISTRADOR -> InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL;
            case SERVIDOR, SERVIDOR_FORUM -> vinculo.unidade().destinatarioKind() == DestinatarioInstitucionalKind.JUIZO_DEPRECADO || vinculo.unidade().destinatarioKind() == DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO
                    ? (vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE ? InstitutionalProcessProfile.DIRETOR_FORUM : InstitutionalProcessProfile.SECRETARIA_FORUM)
                    : vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE
                        ? InstitutionalProcessProfile.COORDENADOR_UNIDADE
                        : vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM
                            ? InstitutionalProcessProfile.SERVIDOR_TRIAGEM
                            : InstitutionalProcessProfile.TECNICO_INSTITUCIONAL;
            default -> vinculo.funcaoOperacional() == FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE
                    ? InstitutionalProcessProfile.COORDENADOR_UNIDADE
                    : InstitutionalProcessProfile.PERFIL_HIBRIDO;
        };
    }

    private InstitutionalEntryLandingPanel resolverPainel(FuncaoOperacionalInstitucional funcao,
                                                          DestinatarioInstitucionalKind kind,
                                                          InstitutionalProcessProfile processProfile) {
        if (processProfile == InstitutionalProcessProfile.DIRETOR_FORUM) {
            return InstitutionalEntryLandingPanel.PAINEL_DIRETORIA_FORUM;
        }
        if (processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM) {
            return InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM;
        }
        if (processProfile == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL || kind == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL || kind == DestinatarioInstitucionalKind.POLICIA_PENAL) {
            return InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL;
        }
        if (processProfile == InstitutionalProcessProfile.AGENDADOR_AUDIENCIA || processProfile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO || kind == DestinatarioInstitucionalKind.CEJUSC) {
            return InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO;
        }
        if (funcao == FuncaoOperacionalInstitucional.GESTOR_CAIXA || funcao == FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE) {
            return InstitutionalEntryLandingPanel.PAINEL_ORGAO;
        }
        if (funcao == FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM) {
            return InstitutionalEntryLandingPanel.PAINEL_TRIAGEM;
        }
        if (funcao == FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL || funcao == FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL) {
            return kind.isApoioTecnicoOuAuxiliar() ? InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO : InstitutionalEntryLandingPanel.PAINEL_CAIXA;
        }
        if (funcao.isFuncaoAssinantePreferencial()) {
            return InstitutionalEntryLandingPanel.PAINEL_TITULAR;
        }
        return InstitutionalEntryLandingPanel.PAINEL_UNIDADE;
    }

    private InstitutionalOrganizationBlueprint resolveBlueprint(VinculoUsuarioCaixaInstitucional vinculo) {
        InstitutionalOrganizationScope scope = blueprintCatalogApplicationService.inferScope(
                vinculo.unidade().destinatarioKind(),
                vinculo.unidade().codigo(),
                vinculo.unidade().sigla(),
                vinculo.unidade().nomeOficial());
        return blueprintCatalogApplicationService.resolve(scope, vinculo.unidade().destinatarioKind()).orElse(null);
    }

    private InstitutionalAccessLaneBlueprint resolveLaneBlueprint(Usuario usuario,
                                                                  VinculoUsuarioCaixaInstitucional vinculo,
                                                                  InstitutionalOrganizationBlueprint blueprint) {
        if (blueprint == null) {
            return null;
        }
        InstitutionalProcessProfile fallbackProfile = resolverPerfilProcessual(usuario, vinculo);
        return blueprintCatalogApplicationService.resolveLane(
                blueprint.scope(),
                inferLaneKind(vinculo.funcaoOperacional(), blueprint.scope()),
                inferNominationRole(vinculo.funcaoOperacional(), blueprint.scope()),
                vinculo.funcaoOperacional(),
                fallbackProfile
        ).orElse(null);
    }

    private InstitutionalNominationRole inferNominationRole(FuncaoOperacionalInstitucional funcao,
                                                            InstitutionalOrganizationScope scope) {
        if (funcao == null) {
            return null;
        }
        return switch (funcao) {
            case COORDENADOR_UNIDADE -> switch (scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope) {
                case FORUM -> InstitutionalNominationRole.DIRETORIA_FORUM;
                case DELEGACIA -> InstitutionalNominationRole.GESTOR_DELEGACIA;
                case UNIDADE_PRISIONAL -> InstitutionalNominationRole.DIRETOR_UNIDADE_PRISIONAL;
                case CEJUSC -> InstitutionalNominationRole.GESTOR_CEJUSC;
                case CONTADORIA -> InstitutionalNominationRole.GESTOR_CONTADORIA;
                case EQUIPE_PSICOSSOCIAL -> InstitutionalNominationRole.GESTOR_PSICOSSOCIAL;
                case CARTORIO_INTEGRADO -> InstitutionalNominationRole.GESTOR_CARTORIO_INTEGRADO;
                default -> InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL;
            };
            case GESTOR_CAIXA -> scope == InstitutionalOrganizationScope.FORUM || scope == InstitutionalOrganizationScope.SECRETARIA_UNIDADE_JUDICIARIA
                    ? InstitutionalNominationRole.SECRETARIA_FORUM
                    : scope == InstitutionalOrganizationScope.CENTRAL_AUDIENCIAS
                        ? InstitutionalNominationRole.AGENDADOR_AUDIENCIA
                        : InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL;
            case SERVIDOR_TRIAGEM -> scope == InstitutionalOrganizationScope.CENTRAL_AUDIENCIAS
                    ? InstitutionalNominationRole.AGENDADOR_AUDIENCIA
                    : InstitutionalNominationRole.TRIAGEM_ORGAO;
            case ASSESSOR_INSTITUCIONAL -> InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL;
            case APOIO_TECNICO_SETORIAL -> InstitutionalNominationRole.APOIO_TECNICO;
            case MEMBRO_TITULAR, SUBSTITUTO, PLANTONISTA -> InstitutionalNominationRole.TITULAR_INSTITUCIONAL;
        };
    }

    private com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind inferLaneKind(FuncaoOperacionalInstitucional funcao,
                                                                                              InstitutionalOrganizationScope scope) {
        if (funcao == null) {
            return null;
        }
        return switch (funcao) {
            case COORDENADOR_UNIDADE -> switch (scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope) {
                case FORUM -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.DIRETORIA;
                case UNIDADE_PRISIONAL -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.DIRECAO_PRISIONAL;
                case DELEGACIA, POLICIA_PENAL, CEJUSC, CONTADORIA, EQUIPE_PSICOSSOCIAL, CARTORIO_INTEGRADO, COOPERACAO_JUDICIAL_EXTERNA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA;
                default -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA;
            };
            case GESTOR_CAIXA -> switch (scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope) {
                case CENTRAL_AUDIENCIAS -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.AGENDAMENTO_AUDIENCIA;
                case CENTRAL_MANDADOS -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.CENTRAL_MANDADOS;
                case DELEGACIA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.CARTORIO_POLICIAL;
                case CONTADORIA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.CONTADORIA;
                default -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.SECRETARIA;
            };
            case SERVIDOR_TRIAGEM -> switch (scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope) {
                case CENTRAL_AUDIENCIAS -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.AGENDAMENTO_AUDIENCIA;
                case POLICIA_PENAL, UNIDADE_PRISIONAL -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.CUSTODIA;
                default -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.TRIAGEM;
            };
            case ASSESSOR_INSTITUCIONAL -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.ASSESSORIA;
            case APOIO_TECNICO_SETORIAL -> switch (scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope) {
                case CONTADORIA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.CONTADORIA;
                case EQUIPE_PSICOSSOCIAL -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.PSICOSSOCIAL;
                case COOPERACAO_JUDICIAL_EXTERNA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.COOPERACAO;
                default -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.APOIO_TECNICO;
            };
            case MEMBRO_TITULAR, SUBSTITUTO, PLANTONISTA -> com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind.TITULAR;
        };
    }

    private String resolverLandingPath(VinculoUsuarioCaixaInstitucional vinculo,
                                       InstitutionalEntryLandingPanel painel) {
        String unidade = vinculo.unidade().codigo();
        String caixa = vinculo.caixa().codigo();
        return switch (painel) {
            case PAINEL_ORGAO, PAINEL_UNIDADE, PAINEL_TITULAR, PAINEL_ADMINISTRATIVO, PAINEL_APOIO_TECNICO, PAINEL_DIRETORIA_FORUM ->
                    InstitutionalApiRoutes.painelExecutivoComUnidade(unidade);
            case PAINEL_SECRETARIA_FORUM ->
                    OperationalApiRoutes.secretariatOperationalSnapshot(unidade);
            case PAINEL_AUDIENCIAS_CONCILIACAO ->
                    "/api/v1/conciliacao/operacional/processos/0/agendamento?unidadeCodigo=" + unidade;
            case PAINEL_TRIAGEM, PAINEL_CAIXA ->
                    InstitutionalApiRoutes.inbox(unidade, caixa);
            case PAINEL_DELEGACIA ->
                    InstitutionalApiRoutes.painelExecutivoComUnidadeEScope(unidade, "DELEGACIA");
            case PAINEL_CUSTODIA_PRISIONAL ->
                    InstitutionalApiRoutes.painelExecutivoComUnidadeEScope(unidade, "UNIDADE_PRISIONAL");
            case PAINEL_TECNICO_JUDICIAL ->
                    InstitutionalApiRoutes.painelExecutivoComUnidadeEScope(unidade, "APOIO_TECNICO");
            case PAINEL_COOPERACAO_JUDICIAL ->
                    InstitutionalApiRoutes.painelExecutivoComUnidadeEScope(unidade, "COOPERACAO");
        };
    }

    private int calcularPrioridade(VinculoUsuarioCaixaInstitucional vinculo,
                                   long pendencias,
                                   long semLeitura,
                                   long urgentes,
                                   boolean delegacaoAtiva,
                                   boolean coberturaAtiva,
                                   boolean plantaoAtivo) {
        int prioridade = switch (vinculo.funcaoOperacional()) {
            case PLANTONISTA -> 100;
            case SUBSTITUTO -> 92;
            case MEMBRO_TITULAR -> 88;
            case COORDENADOR_UNIDADE, GESTOR_CAIXA -> 80;
            case ASSESSOR_INSTITUCIONAL -> 68;
            case SERVIDOR_TRIAGEM -> 64;
            case APOIO_TECNICO_SETORIAL -> 60;
        };
        prioridade += (int) Math.min(18L, urgentes * 3L);
        prioridade += (int) Math.min(12L, semLeitura * 2L);
        prioridade += (int) Math.min(8L, pendencias / 3L);
        if (delegacaoAtiva) prioridade += 9;
        if (coberturaAtiva) prioridade += 8;
        if (plantaoAtivo) prioridade += 12;
        return prioridade;
    }

    private String colorFor(FuncaoOperacionalInstitucional funcao,
                            DestinatarioInstitucionalKind kind,
                            InstitutionalProcessProfile processProfile,
                            long urgentes,
                            boolean plantaoAtivo) {
        if (plantaoAtivo) {
            return "#B91C1C";
        }
        if (urgentes > 0) {
            return "#D97706";
        }
        if (processProfile == InstitutionalProcessProfile.DIRETOR_FORUM) {
            return "#7C3AED";
        }
        if (processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM) {
            return "#0F766E";
        }
        if (processProfile == InstitutionalProcessProfile.AGENDADOR_AUDIENCIA || processProfile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO) {
            return "#2563EB";
        }
        if (kind.isInstituicaoEssencialJustica()) {
            return "#2563EB";
        }
        if (kind.isApoioTecnicoOuAuxiliar()) {
            return "#059669";
        }
        return switch (funcao) {
            case GESTOR_CAIXA, COORDENADOR_UNIDADE -> "#7C3AED";
            case SERVIDOR_TRIAGEM -> "#0F766E";
            default -> "#475569";
        };
    }

    private List<String> fundamentos(Usuario usuario,
                                     VinculoUsuarioCaixaInstitucional vinculo,
                                     InstitutionalNomination nomeacaoAtiva,
                                     InstitutionalOrganizationBlueprint blueprint,
                                     InstitutionalAccessLaneBlueprint laneBlueprint,
                                     boolean delegacaoAtiva,
                                     boolean coberturaAtiva,
                                     boolean plantaoAtivo,
                                     long pendencias,
                                     long urgentes) {
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("tipo_usuario=" + usuario.getTipoUsuario().name());
        fundamentos.add("orgao=" + vinculo.unidade().destinatarioKind().name());
        fundamentos.add("unidade=" + vinculo.unidade().codigo());
        fundamentos.add("caixa=" + vinculo.caixa().codigo());
        fundamentos.add("funcao=" + vinculo.funcaoOperacional().name());
        if (nomeacaoAtiva != null) {
            fundamentos.add("nomeacao=" + nomeacaoAtiva.nominationRole().name());
            fundamentos.add("trust_floor=" + (nomeacaoAtiva.trustFloor() == null ? "NAO_INFORMADO" : nomeacaoAtiva.trustFloor().name()));
        } else {
            if (blueprint != null) fundamentos.add("blueprint_scope=" + blueprint.scope().name());
            if (laneBlueprint != null) {
                fundamentos.add("blueprint_lane=" + laneBlueprint.laneKind().name());
                fundamentos.add("blueprint_role=" + laneBlueprint.nominationRole().name());
            }
        }
        if (delegacaoAtiva) fundamentos.add("delegacao_ativa");
        if (coberturaAtiva) fundamentos.add("cobertura_ativa");
        if (plantaoAtivo) fundamentos.add("plantao_ativo");
        if (pendencias > 0) fundamentos.add("pendencias=" + pendencias);
        if (urgentes > 0) fundamentos.add("urgencias=" + urgentes);
        return fundamentos;
    }

    private boolean possuiAmbientePessoal(Usuario usuario) {
        return identityBaseProfileResolver.resolve(usuario).possuiFluxoDireto();
    }

    private java.util.Set<com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional> copyCapacidades(VinculoUsuarioCaixaInstitucional vinculo) {
        return vinculo.capacidades() == null || vinculo.capacidades().isEmpty()
                ? java.util.EnumSet.noneOf(com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.class)
                : java.util.EnumSet.copyOf(vinculo.capacidades());
    }
}
