package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceAggregate;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCompetenceEnvelope;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralContextVector;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralNextBestAct;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalProceduralCoherenceApplicationService {

    private final InstitutionalProcessWorkspaceApplicationService workspaceApplicationService;
    private final InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService;

    public InstitutionalProceduralCoherenceApplicationService(InstitutionalProcessWorkspaceApplicationService workspaceApplicationService,
                                                             InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService) {
        this.workspaceApplicationService = Objects.requireNonNull(workspaceApplicationService);
        this.accessProfileCatalogApplicationService = Objects.requireNonNull(accessProfileCatalogApplicationService);
    }

    public InstitutionalProceduralCoherenceAggregate detalhar(String profileCode,
                                                              Long processoId,
                                                              String rito,
                                                              String fase,
                                                              String status,
                                                              String ramo) {
        InstitutionalProcessWorkspace workspace = workspaceApplicationService.detalharPerfil(profileCode, processoId, rito, fase, status, ramo);
        InstitutionalAccessProfileCatalogEntry entry = loadEntry(profileCode);
        InstitutionalProceduralContextVector context = buildContext(workspace, entry);
        InstitutionalProceduralCompetenceEnvelope competence = buildCompetenceEnvelope(workspace, entry, context);
        List<InstitutionalProceduralActEvaluation> evaluations = workspace.actions().stream()
                .map(action -> evaluateAction(workspace, entry, context, competence, action))
                .sorted(Comparator.comparingInt(InstitutionalProceduralActEvaluation::coherenceScore).reversed()
                        .thenComparing(InstitutionalProceduralActEvaluation::actionTitle))
                .toList();
        List<InstitutionalProceduralCoherenceFinding> aggregateFindings = buildAggregateFindings(workspace, context, competence, evaluations);
        List<InstitutionalProceduralNextBestAct> nextBestActs = buildNextBestActs(context, evaluations);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(workspace.fundamentos());
        fundamentos.addAll(competence.fundamentos());
        aggregateFindings.forEach(finding -> fundamentos.addAll(finding.fundamentos()));
        return new InstitutionalProceduralCoherenceAggregate(
                context,
                competence,
                aggregateFindings,
                evaluations,
                nextBestActs,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    public InstitutionalProceduralActEvaluation avaliarAto(String profileCode,
                                                           String actionCode,
                                                           Long processoId,
                                                           String rito,
                                                           String fase,
                                                           String status,
                                                           String ramo) {
        InstitutionalProcessWorkspace workspace = workspaceApplicationService.detalharPerfil(profileCode, processoId, rito, fase, status, ramo);
        InstitutionalAccessProfileCatalogEntry entry = loadEntry(profileCode);
        InstitutionalProceduralContextVector context = buildContext(workspace, entry);
        InstitutionalProceduralCompetenceEnvelope competence = buildCompetenceEnvelope(workspace, entry, context);
        InstitutionalProcessActionSpec action = workspace.actions().stream()
                .filter(item -> item.code().equalsIgnoreCase(actionCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ato processual não habilitado para o perfil: " + actionCode));
        return evaluateAction(workspace, entry, context, competence, action);
    }

    public InstitutionalProceduralCoherenceDiagnosticReport diagnosticar(Long processoId,
                                                                         String rito,
                                                                         String fase,
                                                                         String status,
                                                                         String ramo) {
        ArrayList<InstitutionalProceduralCoherenceFinding> findings = new ArrayList<>();
        accessProfileCatalogApplicationService.listarPerfis().stream()
                .filter(entry -> entry.entryMode() == InstitutionalEntryMode.INSTITUCIONAL_AFILIADO)
                .map(entry -> detalhar(entry.codigo(), processoId, rito, fase, status, ramo))
                .forEach(aggregate -> findings.addAll(aggregate.aggregateFindings().stream()
                        .map(finding -> new InstitutionalProceduralCoherenceFinding(
                                finding.code(),
                                finding.severity(),
                                finding.blocking(),
                                aggregate.context().profileCode() + ": " + finding.message(),
                                prependEvidence(aggregate.context().profileCode(), finding.evidences()),
                                finding.fundamentos()
                        )).toList()));
        long blocking = findings.stream().filter(InstitutionalProceduralCoherenceFinding::blocking).count();
        return new InstitutionalProceduralCoherenceDiagnosticReport(
                findings.isEmpty(),
                findings.size(),
                blocking,
                findings.stream().sorted(Comparator.comparing((InstitutionalProceduralCoherenceFinding finding) -> finding.severity().weight()).reversed()
                        .thenComparing(InstitutionalProceduralCoherenceFinding::code)).toList(),
                List.of(
                        "A coerência processual valida competência material, fase, rito, status, segregação de função e força de assinatura.",
                        "A identidade base continua pessoal; a atuação processual depende de vínculo institucional, papel, caixa e contexto ativos.",
                        "O próximo melhor ato processual precisa respeitar recursos, embargos, execução, urgência e custódia sem mistura de trilhas."
                ),
                Instant.now()
        );
    }

    private InstitutionalAccessProfileCatalogEntry loadEntry(String profileCode) {
        return accessProfileCatalogApplicationService.listarPerfis().stream()
                .filter(item -> item.codigo().equalsIgnoreCase(profileCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perfil institucional não encontrado: " + profileCode));
    }

    private InstitutionalProceduralContextVector buildContext(InstitutionalProcessWorkspace workspace,
                                                              InstitutionalAccessProfileCatalogEntry entry) {
        boolean custodial = normalize(workspace.ramoDireito()).contains("PENAL")
                && (containsAnyCode(workspace.actions(), "CUSTODIA", "APRESENTACAO") || containsAnyText(workspace.tabs(), "custod") || containsAnyText(workspace.quickFilters(), "custod"));
        boolean technical = isTechnical(entry.processProfile()) || containsAnyCode(workspace.actions(), "LAUDO", "ESTUDO", "COMPLEMENTACAO");
        boolean governance = isGovernance(entry) || containsAnyCode(workspace.actions(), "GERIR_", "HOMOLOGAR_", "GOVERNANCA");
        return new InstitutionalProceduralContextVector(
                workspace.profileCode(),
                workspace.displayName(),
                workspace.panel(),
                workspace.processProfile(),
                workspace.trustFloor(),
                safe(workspace.ritoProcessual()),
                safe(workspace.faseProcessual()),
                safe(workspace.statusProcessual()),
                safe(workspace.ramoDireito()),
                containsAnyText(workspace.tabs(), "recurso") || normalize(workspace.faseProcessual()).contains("RECURSAL") || normalize(workspace.statusProcessual()).contains("RECURSO"),
                containsAnyText(workspace.tabs(), "embarg") || normalize(workspace.statusProcessual()).contains("EMBARG"),
                containsAnyText(workspace.tabs(), "execu") || normalize(workspace.faseProcessual()).contains("EXECU") || normalize(workspace.statusProcessual()).contains("CUMPRIMENTO"),
                containsAnyText(workspace.tabs(), "urg") || containsAnyText(workspace.quickFilters(), "urg") || containsAnyCode(workspace.actions(), "URGENTE", "CUSTODIA"),
                custodial,
                technical,
                governance,
                List.of(
                        "O vetor de contexto concentra rito, fase, status, ramo, urgência, custódia, governança e trilha habilitada.",
                        "O workspace do perfil foi usado como base do bounded context processual-institucional."
                )
        );
    }

    private InstitutionalProceduralCompetenceEnvelope buildCompetenceEnvelope(InstitutionalProcessWorkspace workspace,
                                                                              InstitutionalAccessProfileCatalogEntry entry,
                                                                              InstitutionalProceduralContextVector context) {
        String eixoMaterial = !context.ramoDireito().isBlank() ? context.ramoDireito() : inferMaterialAxis(context.ritoProcessual());
        String eixoProcedimental = !context.ritoProcessual().isBlank() ? context.ritoProcessual() : inferProceduralAxis(context.ramoDireito());
        String eixoFasico = !context.faseProcessual().isBlank() ? context.faseProcessual() : inferFasicAxis(context);
        String eixoAtuacao = switch (entry.processProfile()) {
            case PROMOTOR, DEFENSOR, PROCURADOR -> "MANIFESTACAO_TITULAR";
            case SERVIDOR_TRIAGEM, ASSESSOR_INSTITUCIONAL, ANALISTA_INSTITUCIONAL, TECNICO_INSTITUCIONAL -> "SUPORTE_QUALIFICADO";
            case POLICIAL_PENAL, GESTOR_UNIDADE_PRISIONAL, OPERADOR_CUSTODIA_PRISIONAL -> "CUSTODIA_E_CUMPRIMENTO_MATERIAL";
            case CONCILIADOR, MEDIADOR, AGENDADOR_AUDIENCIA, AGENDADOR_CONCILIACAO -> "AUTOCOMPOSICAO_E_PAUTA";
            default -> isGovernance(entry) ? "GOVERNANCA_INSTITUCIONAL" : "ATUACAO_ESPECIALIZADA";
        };
        boolean exigeAssinaturaForte = workspace.actions().stream().anyMatch(InstitutionalProcessActionSpec::requiresCertificate);
        boolean exigeSegregacaoTitular = entry.nominationRole() != null && entry.nominationRole().name().contains("ASSESSORIA")
                || entry.processProfile() == InstitutionalProcessProfile.SERVIDOR_TRIAGEM
                || entry.processProfile() == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL
                || entry.processProfile() == InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL;
        return new InstitutionalProceduralCompetenceEnvelope(
                eixoMaterial,
                eixoProcedimental,
                eixoFasico,
                eixoAtuacao,
                exigeAssinaturaForte,
                exigeSegregacaoTitular,
                true,
                List.of(
                        "Competência material nasce do ramo/rito efetivo; competência funcional nasce do perfil institucional homologado.",
                        "Atos posteriores a arquivamento, baixa ou trânsito não podem modificar o fluxo ordinário sem trilha excepcional.",
                        "Titularidade, assessoria, triagem, custódia e governança permanecem segregadas para reduzir erro estrutural."
                )
        );
    }

    private InstitutionalProceduralActEvaluation evaluateAction(InstitutionalProcessWorkspace workspace,
                                                                InstitutionalAccessProfileCatalogEntry entry,
                                                                InstitutionalProceduralContextVector context,
                                                                InstitutionalProceduralCompetenceEnvelope competence,
                                                                InstitutionalProcessActionSpec action) {
        ArrayList<InstitutionalProceduralCoherenceFinding> findings = new ArrayList<>();
        LinkedHashSet<String> guards = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(action.fundamentos());
        int score = 100;
        String normalizedCode = normalize(action.code());
        String normalizedTitle = normalize(action.title());

        if (action.requiresCertificate() || competence.exigeAssinaturaForte()) {
            guards.add("identidade_forte");
            guards.add("trilha_forense");
            fundamentos.add("Atos com assinatura, petição ou prova material exigem identidade forte e trilha reforçada.");
        }
        if (action.requiresCertificate()) {
            guards.add("certificado_ou_step_up");
            score += 10;
        }
        if (action.requiresTitularApproval() || competence.exigeSegregacaoTitular()) {
            guards.add("segregacao_titular");
        }
        if (action.modifiesFlow()) {
            guards.add("controle_de_fluxo");
        }
        if (isClosedStatus(context.statusProcessual()) && action.modifiesFlow()) {
            findings.add(finding("STATUS_ENCERRADO_BLOQUEIA_MODIFICACAO", InstitutionalRiskSeverity.CRITICA, true,
                    "Ato modificador de fluxo em processo encerrado, arquivado, baixado ou transitado.",
                    List.of("status=" + context.statusProcessual(), "action=" + action.code()),
                    List.of("Fluxo ordinário não pode ser reaberto por ato comum após encerramento.")));
            score -= 90;
        }
        if (matchesAny(normalizedCode, normalizedTitle, "RECURSO", "CONTRARRAZ", "RECORRER") && !context.recursal()) {
            findings.add(finding("ATO_RECURSAL_FORA_DA_TRILHA", InstitutionalRiskSeverity.ALTA, false,
                    "Ato recursal fora de trilha recursal ativa.",
                    List.of("fase=" + context.faseProcessual(), "status=" + context.statusProcessual()),
                    List.of("Recursos e contrarrazões devem operar em fase/aba recursal.")));
            score -= 35;
            guards.add("revisao_recursal");
        }
        if (matchesAny(normalizedCode, normalizedTitle, "EMBARG") && !context.embargos()) {
            findings.add(finding("ATO_EMBARGOS_FORA_DA_TRILHA", InstitutionalRiskSeverity.MEDIA, false,
                    "Ato de embargos sem trilha de embargos destacada no processo.",
                    List.of("status=" + context.statusProcessual(), "tabs=" + String.join(",", workspace.tabs())),
                    List.of("Embargos não devem ficar misturados ao fluxo ordinário.")));
            score -= 25;
            guards.add("revisao_embargos");
        }
        if (matchesAny(normalizedCode, normalizedTitle, "EXECUCAO", "CUMPRIMENTO", "PENHORA", "CALCULO") && !context.execucao()) {
            findings.add(finding("ATO_EXECUTORIO_FORA_DA_TRILHA", InstitutionalRiskSeverity.MEDIA, false,
                    "Ato de execução/cumprimento fora de trilha executória principal.",
                    List.of("fase=" + context.faseProcessual(), "action=" + action.code()),
                    List.of("Execução e cumprimento possuem separadores próprios e controles de materialidade.")));
            score -= 20;
            guards.add("validacao_executorio");
        }
        if (matchesAny(normalizedCode, normalizedTitle, "CUSTODIA", "APRESENTACAO") && !context.custodial()) {
            findings.add(finding("ATO_CUSTODIA_SEM_CONTEXTO", InstitutionalRiskSeverity.ALTA, false,
                    "Ato de custódia ou apresentação sem contexto penal/custodial ativo.",
                    List.of("ramo=" + context.ramoDireito(), "action=" + action.code()),
                    List.of("Custódia, escolta, apresentação e confirmação material exigem trilha penal específica.")));
            score -= 40;
            guards.add("confirmacao_unidade_custodiante");
        }
        if (matchesAny(normalizedCode, normalizedTitle, "URGENTE", "LIMINAR") && !context.urgente()) {
            findings.add(finding("ATO_URGENTE_SEM_MARCADOR", InstitutionalRiskSeverity.MEDIA, false,
                    "Ato urgente sem marcador de urgência máximo no contexto atual.",
                    List.of("tabs=" + String.join(",", workspace.tabs())),
                    List.of("Urgência precisa de lane destacada ou etiqueta processual compatível.")));
            score -= 15;
            guards.add("revalidacao_prioridade");
        }
        if (matchesAny(normalizedCode, normalizedTitle, "PARECER") && entry.processProfile() != InstitutionalProcessProfile.PROMOTOR) {
            findings.add(finding("PARECER_FORA_DO_PERFIL_TITULAR", InstitutionalRiskSeverity.ALTA, true,
                    "Emissão de parecer ministerial fora do perfil de promotoria.",
                    List.of("processProfile=" + entry.processProfile().name(), "action=" + action.code()),
                    List.of("Parecer ministerial permanece sob titularidade adequada.")));
            score -= 80;
        }
        if (matchesAny(normalizedCode, normalizedTitle, "DEFESA", "ASSISTIDO") && entry.processProfile() != InstitutionalProcessProfile.DEFENSOR) {
            findings.add(finding("DEFESA_FORA_DO_PERFIL_DEFENSORIAL", InstitutionalRiskSeverity.ALTA, true,
                    "Ato defensivo vinculado a perfil não defensorial.",
                    List.of("processProfile=" + entry.processProfile().name()),
                    List.of("Defesa técnica do assistido exige perfil defensorial ou delegação material específica.")));
            score -= 80;
        }
        if (matchesAny(normalizedCode, normalizedTitle, "ACORDO_PUBLICO") && entry.processProfile() != InstitutionalProcessProfile.PROCURADOR) {
            findings.add(finding("ACORDO_PUBLICO_FORA_DA_PROCURADORIA", InstitutionalRiskSeverity.ALTA, true,
                    "Negociação de acordo público fora da procuradoria.",
                    List.of("processProfile=" + entry.processProfile().name()),
                    List.of("Acordo público depende da trilha fazendária e de autorização própria do ente.")));
            score -= 85;
        }
        if (matchesAny(normalizedCode, normalizedTitle, "LAUDO", "ESTUDO", "COMPLEMENTACAO") && !context.technical()) {
            findings.add(finding("ATO_TECNICO_FORA_DA_TRILHA", InstitutionalRiskSeverity.ALTA, true,
                    "Produção técnica fora de perfil ou trilha técnica.",
                    List.of("processProfile=" + entry.processProfile().name(), "action=" + action.code()),
                    List.of("Laudos, estudos e complementações permanecem em bounded context técnico.")));
            score -= 75;
        }
        if (matchesAny(normalizedCode, normalizedTitle, "GERIR_", "HOMOLOGAR_", "GOVERNANCA") && !context.governance()) {
            findings.add(finding("ATO_GOVERNANCA_FORA_DA_TRILHA", InstitutionalRiskSeverity.ALTA, true,
                    "Ato de governança fora de perfil gestor ou contexto governamental.",
                    List.of("processProfile=" + entry.processProfile().name()),
                    List.of("Gestão de lotação, guarda e governança institucional não se mistura com mérito.")));
            score -= 80;
        }
        if (action.requiresTitularApproval() && isSupportProfile(entry.processProfile())) {
            findings.add(finding("ATO_EXIGE_REFERENDO_TITULAR", InstitutionalRiskSeverity.MEDIA, false,
                    "O ato depende de referendo ou homologação por titular/gestor competente.",
                    List.of("action=" + action.code(), "processProfile=" + entry.processProfile().name()),
                    List.of("Assessoria, triagem e apoio técnico não concluem sozinhos atos com exigência de aprovação titular.")));
            score -= 20;
        }
        if (!action.fasesPreferenciais().isEmpty() && !action.fasesPreferenciais().contains(context.faseProcessual())) {
            findings.add(finding("FASE_NAO_PREFERENCIAL", InstitutionalRiskSeverity.BAIXA, false,
                    "O ato está disponível, mas a fase atual não é a preferencial.",
                    List.of("fase=" + context.faseProcessual(), "preferenciais=" + String.join(",", action.fasesPreferenciais())),
                    List.of("A matriz permite o ato, mas a esteira processual recomenda fase preferencial diferente.")));
            score -= 8;
        }
        if (!action.ritosPreferenciais().isEmpty() && !action.ritosPreferenciais().contains(context.ritoProcessual())) {
            findings.add(finding("RITO_NAO_PREFERENCIAL", InstitutionalRiskSeverity.BAIXA, false,
                    "O ato está disponível, mas o rito atual não é o rito preferencial mais forte.",
                    List.of("rito=" + context.ritoProcessual(), "preferenciais=" + String.join(",", action.ritosPreferenciais())),
                    List.of("O motor de coerência preserva flexibilidade, mas sinaliza o melhor encaixe por rito.")));
            score -= 8;
        }

        boolean blocking = findings.stream().anyMatch(InstitutionalProceduralCoherenceFinding::blocking);
        fundamentos.addAll(competence.fundamentos());
        findings.forEach(finding -> fundamentos.addAll(finding.fundamentos()));
        String decision;
        if (blocking) {
            decision = "BLOQUEADO_POR_COERENCIA_PROCESSUAL";
        } else if (score >= 95) {
            decision = "LIBERADO_COM_FORTE_ADERENCIA";
        } else if (score >= 70) {
            decision = "LIBERADO_COM_GUARDAS";
        } else {
            decision = "LIBERADO_COM_ALERTAS";
        }
        return new InstitutionalProceduralActEvaluation(
                action.code(),
                action.title(),
                !blocking,
                blocking,
                Math.max(score, 0),
                decision,
                List.copyOf(guards),
                findings.stream().sorted(Comparator.comparing((InstitutionalProceduralCoherenceFinding finding) -> finding.severity().weight()).reversed()
                        .thenComparing(InstitutionalProceduralCoherenceFinding::code)).toList(),
                List.copyOf(fundamentos)
        );
    }

    private List<InstitutionalProceduralCoherenceFinding> buildAggregateFindings(InstitutionalProcessWorkspace workspace,
                                                                                  InstitutionalProceduralContextVector context,
                                                                                  InstitutionalProceduralCompetenceEnvelope competence,
                                                                                  List<InstitutionalProceduralActEvaluation> evaluations) {
        ArrayList<InstitutionalProceduralCoherenceFinding> findings = new ArrayList<>();
        if (evaluations.stream().noneMatch(InstitutionalProceduralActEvaluation::allowed)) {
            findings.add(finding("PERFIL_SEM_ATO_COERENTE", InstitutionalRiskSeverity.CRITICA, true,
                    "O perfil não possui qualquer ato coerente liberado para o contexto processual atual.",
                    List.of("profileCode=" + workspace.profileCode(), "fase=" + context.faseProcessual()),
                    List.of("Painel, perfil, rito e fase precisam convergir para ao menos um ato processual operacional.")));
        }
        if (competence.eixoAtuacao().equals("MANIFESTACAO_TITULAR")
                && evaluations.stream().noneMatch(item -> item.allowed() && matchesAny(normalize(item.actionCode()), normalize(item.actionTitle()), "PARECER", "DEFESA", "INFORMACOES", "ASSINAR", "PETICIONAR"))) {
            findings.add(finding("TRILHA_TITULAR_SEM_ATO_FINAL", InstitutionalRiskSeverity.ALTA, true,
                    "Perfil titular sem ato final de manifestação, assinatura ou petição coerente.",
                    List.of("profile=" + workspace.profileCode(), "actions=" + evaluations.size()),
                    List.of("Promotoria, defensoria e procuradoria precisam manter trilha final de manifestação.")));
        }
        if (context.recursal() && workspace.tabs().stream().noneMatch(tab -> normalize(tab).contains("RECURSO"))) {
            findings.add(finding("TRILHA_RECURSAL_SEM_ABA", InstitutionalRiskSeverity.ALTA, false,
                    "Contexto recursal sem aba recursal explícita.",
                    List.of("tabs=" + String.join(",", workspace.tabs())),
                    List.of("O processo recursal precisa manter aba visual destacada para recursos e contrarrazões.")));
        }
        if (context.embargos() && evaluations.stream().noneMatch(item -> item.allowed() && normalize(item.actionCode()).contains("EMBARG"))) {
            findings.add(finding("EMBARGOS_SEM_ATO_COMPATIVEL", InstitutionalRiskSeverity.ALTA, false,
                    "Embargos ativos sem ato compatível liberado para o perfil.",
                    List.of("status=" + context.statusProcessual(), "profile=" + workspace.profileCode()),
                    List.of("A esteira de embargos precisa ficar disponível quando o contexto realmente ingressou nessa trilha.")));
        }
        if (context.custodial() && evaluations.stream().noneMatch(item -> item.allowed() && matchesAny(normalize(item.actionCode()), normalize(item.actionTitle()), "CUSTODIA", "APRESENTACAO", "CERTIDAO"))) {
            findings.add(finding("CUSTODIA_SEM_PROVA_MATERIAL", InstitutionalRiskSeverity.CRITICA, true,
                    "Fluxo custodial sem ato de confirmação, apresentação ou prova material coerente.",
                    List.of("profile=" + workspace.profileCode(), "ramo=" + context.ramoDireito()),
                    List.of("Custódia exige rastreabilidade material e ato próprio de confirmação ou certidão.")));
        }
        if (context.governance() && evaluations.stream().noneMatch(item -> item.allowed() && matchesAny(normalize(item.actionCode()), normalize(item.actionTitle()), "GERIR_", "HOMOLOGAR_", "GOVERNANCA"))) {
            findings.add(finding("GOVERNANCA_SEM_ATO_COMPATIVEL", InstitutionalRiskSeverity.ALTA, false,
                    "Perfil de governança sem ato próprio de lotação, guarda ou governança institucional.",
                    List.of("profile=" + workspace.profileCode()),
                    List.of("Governança não pode se limitar a abas visuais; precisa existir ato compatível.")));
        }
        if (isClosedStatus(context.statusProcessual()) && evaluations.stream().anyMatch(item -> item.allowed() && normalize(item.actionCode()).contains("REDISTRIBUIR"))) {
            findings.add(finding("REDISTRIBUICAO_POS_ENCERRAMENTO", InstitutionalRiskSeverity.MEDIA, false,
                    "Redistribuição interna ainda disponível em processo encerrado; exige revisão de contexto operacional.",
                    List.of("status=" + context.statusProcessual()),
                    List.of("Encerramento forte deve reduzir atos de circulação interna ao mínimo justificável.")));
        }
        return findings.stream().sorted(Comparator.comparing((InstitutionalProceduralCoherenceFinding finding) -> finding.severity().weight()).reversed()
                .thenComparing(InstitutionalProceduralCoherenceFinding::code)).toList();
    }

    private List<InstitutionalProceduralNextBestAct> buildNextBestActs(InstitutionalProceduralContextVector context,
                                                                       List<InstitutionalProceduralActEvaluation> evaluations) {
        return evaluations.stream()
                .filter(InstitutionalProceduralActEvaluation::allowed)
                .map(evaluation -> toNextBestAct(context, evaluation))
                .sorted(Comparator.comparingInt(InstitutionalProceduralNextBestAct::priorityScore).reversed()
                        .thenComparing(InstitutionalProceduralNextBestAct::actionTitle))
                .limit(6)
                .toList();
    }

    private InstitutionalProceduralNextBestAct toNextBestAct(InstitutionalProceduralContextVector context,
                                                             InstitutionalProceduralActEvaluation evaluation) {
        int priority = evaluation.coherenceScore();
        String normalizedCode = normalize(evaluation.actionCode());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(evaluation.fundamentos());
        if (context.urgente() && matchesAny(normalizedCode, normalizedCode, "URGENTE", "CUSTODIA", "ESCALAR", "APRESENTACAO")) {
            priority += 40;
            fundamentos.add("Urgência, custódia e escala prioritária sobem para o topo do próximo melhor ato.");
        }
        if (context.recursal() && matchesAny(normalizedCode, normalizedCode, "RECURSO", "CONTRARRAZ", "EMBARG")) {
            priority += 25;
            fundamentos.add("Trilha recursal ativa prioriza recurso, contrarrazões e atos integrativos.");
        }
        if (context.execucao() && matchesAny(normalizedCode, normalizedCode, "EXECUCAO", "CUMPRIMENTO", "CERTIDAO", "CALCULO")) {
            priority += 20;
            fundamentos.add("Execução e cumprimento exigem prova material, cálculo e ordenação própria.");
        }
        if (context.custodial() && matchesAny(normalizedCode, normalizedCode, "CUSTODIA", "APRESENTACAO", "CERTIDAO")) {
            priority += 35;
            fundamentos.add("Contexto custodial privilegia prova material, apresentação e confirmação imediata.");
        }
        if (!context.recursal() && !context.execucao() && matchesAny(normalizedCode, normalizedCode, "RECEBER", "CIENCIA", "TRIAGEM", "ANALISE", "MINUTA", "PARECER", "DEFESA", "INFORMACOES")) {
            priority += 15;
            fundamentos.add("Na trilha ordinária o motor privilegia entrada, ciência, análise, minuta e manifestação final.");
        }
        String rationale;
        if (matchesAny(normalizedCode, normalizedCode, "RECEBER", "CIENCIA")) {
            rationale = "Primeiro consolida a entrada institucional, ciência e marco interno do prazo.";
        } else if (matchesAny(normalizedCode, normalizedCode, "ANALISE", "CLASSIFICAR", "MINUTA")) {
            rationale = "Depois organiza rito, fase, urgência e prepara o fluxo sem romper segregação de função.";
        } else if (matchesAny(normalizedCode, normalizedCode, "PARECER", "DEFESA", "INFORMACOES", "PETICIONAR", "ASSINAR")) {
            rationale = "Em seguida pratica o ato de manifestação compatível com legitimidade institucional e fase vigente.";
        } else if (matchesAny(normalizedCode, normalizedCode, "RECURSO", "CONTRARRAZ", "EMBARG")) {
            rationale = "Na trilha recursal o próximo melhor ato protege prazo, adequação e separação entre recurso principal e integrativo.";
        } else if (matchesAny(normalizedCode, normalizedCode, "CUSTODIA", "APRESENTACAO", "CERTIDAO")) {
            rationale = "No fluxo custodial o sistema exige confirmação material e trilha probatória imediata.";
        } else {
            rationale = "O ato foi ranqueado pelo motor unificado considerando fase, rito, status, perfil institucional e guardas obrigatórias.";
        }
        return new InstitutionalProceduralNextBestAct(
                evaluation.actionCode(),
                evaluation.actionTitle(),
                Math.max(priority, 0),
                rationale,
                evaluation.mandatoryGuards(),
                List.copyOf(fundamentos)
        );
    }

    private List<String> prependEvidence(String profileCode, List<String> evidences) {
        ArrayList<String> out = new ArrayList<>();
        out.add("profileCode=" + profileCode);
        if (evidences != null) {
            out.addAll(evidences);
        }
        return List.copyOf(out);
    }

    private InstitutionalProceduralCoherenceFinding finding(String code,
                                                            InstitutionalRiskSeverity severity,
                                                            boolean blocking,
                                                            String message,
                                                            List<String> evidences,
                                                            List<String> fundamentos) {
        return new InstitutionalProceduralCoherenceFinding(code, severity, blocking, message, evidences, fundamentos);
    }

    private boolean containsAnyCode(List<InstitutionalProcessActionSpec> actions, String... fragments) {
        for (InstitutionalProcessActionSpec action : actions) {
            String token = normalize(action.code());
            for (String fragment : fragments) {
                if (token.contains(normalize(fragment))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAnyText(List<String> values, String fragment) {
        String normalized = normalize(fragment);
        return values.stream().map(this::normalize).anyMatch(item -> item.contains(normalized));
    }

    private boolean matchesAny(String left, String right, String... fragments) {
        for (String fragment : fragments) {
            String normalized = normalize(fragment);
            if (left.contains(normalized) || right.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGovernance(InstitutionalAccessProfileCatalogEntry entry) {
        return entry.nominationRole() != null && entry.nominationRole().isGestaoMestre()
                || entry.processProfile() == InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL
                || entry.processProfile() == InstitutionalProcessProfile.COORDENADOR_UNIDADE
                || entry.processProfile() == InstitutionalProcessProfile.DIRETOR_FORUM
                || entry.processProfile() == InstitutionalProcessProfile.SECRETARIA_FORUM;
    }

    private boolean isSupportProfile(InstitutionalProcessProfile profile) {
        return profile == InstitutionalProcessProfile.SERVIDOR_TRIAGEM
                || profile == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL
                || profile == InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL
                || profile == InstitutionalProcessProfile.TECNICO_INSTITUCIONAL;
    }

    private boolean isTechnical(InstitutionalProcessProfile profile) {
        return profile == InstitutionalProcessProfile.PERITO_JUDICIAL
                || profile == InstitutionalProcessProfile.PSICOLOGO_JUDICIAL
                || profile == InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL
                || profile == InstitutionalProcessProfile.CONTADOR_JUDICIAL
                || profile == InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO;
    }

    private boolean isClosedStatus(String status) {
        String normalized = normalize(status);
        return normalized.contains("ARQUIV")
                || normalized.contains("BAIX")
                || normalized.contains("TRANSITO")
                || normalized.contains("JULGADO");
    }

    private String inferMaterialAxis(String rito) {
        String normalized = normalize(rito);
        if (normalized.contains("PENAL")) return "PENAL";
        if (normalized.contains("TRIBUT") || normalized.contains("FISCAL") || normalized.contains("FAZENDA")) return "TRIBUTARIO_FAZENDARIO";
        if (normalized.contains("TRABALH")) return "TRABALHISTA";
        if (normalized.contains("PREVIDENCIARIO")) return "PREVIDENCIARIO";
        if (normalized.contains("MILITAR")) return "MILITAR";
        if (normalized.contains("ELEITORAL")) return "ELEITORAL";
        if (normalized.contains("INFANCIA")) return "INFANCIA_E_JUVENTUDE";
        return "CIVIL_E_PUBLICO";
    }

    private String inferProceduralAxis(String ramo) {
        String normalized = normalize(ramo);
        if (normalized.contains("PENAL")) return "PROCEDIMENTO_PENAL_COMUM";
        if (normalized.contains("TRIBUT") || normalized.contains("FAZENDA")) return "EXECUCAO_FISCAL";
        if (normalized.contains("TRABALH")) return "TRABALHISTA_ORDINARIO";
        return "COMUM_ORDINARIO";
    }

    private String inferFasicAxis(InstitutionalProceduralContextVector context) {
        if (context.recursal()) return "RECURSAL";
        if (context.execucao()) return "EXECUCAO";
        if (context.custodial()) return "AUDIENCIA_CUSTODIA";
        return "CONHECIMENTO";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
