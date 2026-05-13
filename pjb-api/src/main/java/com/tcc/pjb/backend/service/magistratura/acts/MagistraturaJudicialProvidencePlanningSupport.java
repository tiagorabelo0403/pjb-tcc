package com.tcc.pjb.backend.service.magistratura.acts;

import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.containsAny;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.firstNonBlank;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.immutableList;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.join;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.label;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.nestedMap;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.normalize;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.normalizeSpaces;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.optionalDuration;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolveBlocking;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolvePriority;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.resolveWorkItemType;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.safeMap;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.stringValue;
import static com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationRules.tribunalFlow;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialProvidencePlanningSupport {

    private static final Duration MINIMUM_FUTURE_DUE = Duration.ofMinutes(15);

    private final SecretariatOperationalAssignmentService assignmentService;
    private final SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;

    public MagistraturaJudicialProvidencePlanningSupport(SecretariatOperationalAssignmentService assignmentService,
                                                         SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver) {
        this.assignmentService = Objects.requireNonNull(assignmentService);
        this.contactEnvelopeResolver = Objects.requireNonNull(contactEnvelopeResolver);
    }

    public List<MagistraturaJudicialProvidenceResponse> preview(Processo processo,
                                                                Usuario usuario,
                                                                MagistraturaJudicialActCode action,
                                                                MagistraturaJudicialActCommandRequest request,
                                                                SecretariatOperationalRoutingProfile profile) {
        List<ProvidencePlan> plans = buildPlans(processo, usuario, action, request, null, profile);
        List<MagistraturaJudicialProvidenceResponse> out = new ArrayList<>();
        for (ProvidencePlan plan : plans) {
            SecretariatOperationalAssignmentService.AssignmentSnapshot assignment = assignmentService.avaliar(processo, profile, plan.stageToken());
            Usuario primary = assignment == null ? null : assignment.primary();
            out.add(new MagistraturaJudicialProvidenceResponse(
                    plan.code(),
                    label(plan.code()),
                    plan.automatic(),
                    plan.stageToken(),
                    plan.targetInboxKey(),
                    plan.targetQueueCode(),
                    plan.targetPanelRoute(),
                    plan.dueAt(),
                    primary == null ? null : primary.getId(),
                    primary == null ? null : primary.getNome(),
                    primary == null ? null : primary.getEmail(),
                    plan.summary(),
                    contactEnvelopeResolver.participantSnapshots(processo),
                    List.copyOf(plan.reasons()),
                    List.copyOf(plan.warnings()),
                    enrichMetrics(plan.metrics(), processo, plan, assignment)
            ));
        }
        return List.copyOf(out);
    }

    List<ProvidencePlan> buildPlans(Processo processo,
                                    Usuario usuario,
                                    MagistraturaJudicialActCode action,
                                    MagistraturaJudicialActCommandRequest request,
                                    Map<String, Object> payload,
                                    SecretariatOperationalRoutingProfile profile) {
        LinkedHashSet<MagistraturaJudicialProvidenceCode> codes = new LinkedHashSet<>();
        codes.addAll(explicitProvidences(request));
        codes.addAll(actionProvidences(action));
        codes.addAll(textualProvidences(request));
        if (codes.isEmpty()) {
            codes.add(MagistraturaJudicialProvidenceCode.CUMPRIR_DETERMINACAO_CARTORIO);
        }
        List<ProvidencePlan> out = new ArrayList<>();
        for (MagistraturaJudicialProvidenceCode code : codes) {
            out.add(planFor(processo, usuario, action, code, request, payload, profile));
        }
        return List.copyOf(out);
    }

    private List<MagistraturaJudicialProvidenceCode> explicitProvidences(MagistraturaJudicialActCommandRequest request) {
        if (request == null || request.providencias() == null || request.providencias().isEmpty()) {
            return List.of();
        }
        LinkedHashSet<MagistraturaJudicialProvidenceCode> out = new LinkedHashSet<>();
        for (String raw : request.providencias()) {
            if (raw != null && !raw.isBlank()) {
                out.add(MagistraturaJudicialProvidenceCode.parse(raw));
            }
        }
        return List.copyOf(out);
    }

    private List<MagistraturaJudicialProvidenceCode> actionProvidences(MagistraturaJudicialActCode action) {
        return switch (action) {
            case DESIGNAR_AUDIENCIA -> List.of(
                    MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA,
                    MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES,
                    MagistraturaJudicialProvidenceCode.ORGANIZAR_CONCLUSAO
            );
            case NOMEACAO_PERITO -> List.of(
                    MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PERICIA,
                    MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES
            );
            case ORDEM_CUMPRIMENTO_OFICIAL -> List.of(MagistraturaJudicialProvidenceCode.EXPEDIR_ORDEM_CUMPRIMENTO);
            case SENTENCA, DECISAO_INTERLOCUTORIA, DECISAO_MONOCRATICA, ACORDAO, DECISAO_PLENARIA -> List.of(
                    MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO,
                    MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES
            );
            case VOTO_COLEGIADO, PEDIDO_VISTA, DESTAQUE, INCLUSAO_PAUTA, DESPACHO_RELATOR -> List.of(MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO);
            default -> List.of();
        };
    }

    private List<MagistraturaJudicialProvidenceCode> textualProvidences(MagistraturaJudicialActCommandRequest request) {
        if (request == null) {
            return List.of();
        }
        String corpus = join(
                request.conteudo(),
                request.fundamentacao(),
                request.dispositivo(),
                request.observacao(),
                request.relatorio(),
                request.voto(),
                request.decisao(),
                request.ementa(),
                request.tipo(),
                request.orgao(),
                request.votacao()
        );
        if (corpus.isBlank()) {
            return List.of();
        }
        String normalized = normalize(corpus);
        LinkedHashSet<MagistraturaJudicialProvidenceCode> out = new LinkedHashSet<>();
        if (containsAny(normalized, "audiencia", "conciliacao", "instrucao", "julgamento", "designo", "redesigne", "remarcar", "reagendar", "pauta")) {
            out.add(MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA);
        }
        if (containsAny(normalized,
                "intime", "intimacao", "intimar", "cite", "citacao", "citar", "dar ciencia", "de-se ciencia", "cientifique", "vista", "contrarrazoes", "contrarrazoes")) {
            out.add(MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES);
        }
        if (containsAny(normalized, "publique", "publicacao", "publicar", "acordao", "sentenca", "decisao", "disponibilizacao")) {
            out.add(MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO);
        }
        if (containsAny(normalized, "pericia", "perito", "quesitos", "assistente tecnico", "assistente tecnico", "nomeio")) {
            out.add(MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PERICIA);
        }
        if (containsAny(normalized,
                "mandado", "oficio", "carta precatoria", "carta de ordem", "requisicao", "comunicacao institucional", "oficial de justica", "central de mandados", "cumpra se", "cumprimento", "diligencia externa", "liminar", "tutela de urgencia", "tutela de evidencia", "medida cautelar", "bloqueio", "fornecimento", "obrigacao de fazer", "obrigacao de nao fazer")) {
            out.add(MagistraturaJudicialProvidenceCode.EXPEDIR_ORDEM_CUMPRIMENTO);
        }
        if (containsAny(normalized,
                "ministerio publico", "ministerio público", "defensoria", "procuradoria", "contadoria", "nucleo tecnico", "núcleo tecnico", "curadoria", "psicossocial")) {
            out.add(MagistraturaJudicialProvidenceCode.ABRIR_VISTA_TECNICA);
        }
        if (containsAny(normalized, "liquidacao", "calculo", "cálculo", "memoria", "contadoria", "rpv", "precatorio", "precatório", "conferencia de valores")) {
            out.add(MagistraturaJudicialProvidenceCode.CONTROLAR_CALCULO_LIQUIDACAO);
        }
        if (containsAny(normalized,
                "cumprimento de sentenca", "cumprimento de sentença", "execucao", "execução", "penhora", "avaliacao", "avaliação", "expropriacao", "expropriação", "embargos a execucao", "impugnacao")) {
            out.add(MagistraturaJudicialProvidenceCode.IMPULSIONAR_EXECUCAO);
        }
        if (containsAny(normalized,
                "emenda da inicial", "regularizacao", "regularização", "juntada de documentos", "correcao do polo", "correção do polo", "adequacao de rito", "adequação de rito", "especificacao de provas", "especificação de provas")) {
            out.add(MagistraturaJudicialProvidenceCode.SANEAR_PROCESSO);
        }
        if (containsAny(normalized,
                "apela", "agravo", "embargos de declaracao", "embargos de declaração", "recurso especial", "recurso extraordinario", "recurso extraordinário", "agravo interno", "admissibilidade", "colegiado", "sessao", "sessão", "sustentacao oral", "sustentação oral", "acordao", "baixa para origem")) {
            out.add(MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO);
        }
        if (containsAny(normalized,
                "incidente de desconsideracao", "incidente de desconsideração", "incidente de falsidade", "incompetencia", "incompetência", "suspeicao", "suspeição", "impedimento", "habilitacao", "habilitação", "substituicao processual", "substituição processual", "irdr", "cumprimento provisório", "cumprimento provisorio")) {
            out.add(MagistraturaJudicialProvidenceCode.PROCESSAR_INCIDENTE_PROCESSUAL);
        }
        if (containsAny(normalized, "conexao", "conexão", "continencia", "continência", "prevenção", "prevencao", "redistribuicao", "redistribuição")) {
            out.add(MagistraturaJudicialProvidenceCode.REDISTRIBUIR_OU_PREVENIR);
        }
        if (containsAny(normalized, "voltem conclusos", "conclusos", "retorno a conclusao", "retorno à conclusão")) {
            out.add(MagistraturaJudicialProvidenceCode.ORGANIZAR_CONCLUSAO);
        }
        if (out.isEmpty() && containsAny(normalized, "certifique", "junte", "juntada", "providencie", "secretaria")) {
            out.add(MagistraturaJudicialProvidenceCode.CUMPRIR_DETERMINACAO_CARTORIO);
        }
        return List.copyOf(out);
    }

    private ProvidencePlan planFor(Processo processo,
                                   Usuario usuario,
                                   MagistraturaJudicialActCode action,
                                   MagistraturaJudicialProvidenceCode code,
                                   MagistraturaJudicialActCommandRequest request,
                                   Map<String, Object> payload,
                                   SecretariatOperationalRoutingProfile profile) {
        Instant now = Instant.now();
        Instant hearingAt = resolveHearingAt(request, payload);
        MagistraturaAuthorityProjection authorityProjection = MagistraturaAuthorityProjection.resolve(processo, usuario, action, code, request, profile);
        MagistraturaOperationalUnitContext unitContext = MagistraturaOperationalUnitContext.resolve(processo, usuario, profile, authorityProjection);
        DeskTarget deskTarget = resolveDeskTarget(processo, action, code, request, profile, unitContext, authorityProjection);
        int priority = resolvePriority(code, request);
        Instant dueAt = adjustDueAt(now, computeDueAt(now, hearingAt, code, profile, request));
        List<String> dependencies = resolveDependencies(code, request, hearingAt);
        String institutionalOwner = resolveInstitutionalOwner(profile, code, deskTarget);
        String expectedReturn = expectedReturn(code, deskTarget.stageToken());
        String completionEvent = completionEvent(code, deskTarget.stageToken());
        String confirmationMode = confirmationMode(code);
        List<String> reasons = new ArrayList<>();
        reasons.add("Unidade alvo: " + institutionalOwner + '.');
        reasons.add("Vínculo de unidade: " + unitContext.authorityUnitBindingKey() + '.');
        reasons.add("Painel alvo: " + deskTarget.panelRoute() + '.');
        reasons.add("Fila derivada: " + deskTarget.queueCode() + '.');
        reasons.add("Célula operacional: " + deskTarget.cellCode() + '.');
        reasons.add("Ato da magistratura: " + action.name() + '.');
        reasons.add("Instância operacional: " + firstNonBlank(profile.instanciaAxis(), profile.organizationalPath(), profile.secretariatCode()) + '.');
        reasons.add("Autoridade julgadora: " + authorityProjection.authorityLabel() + " / " + authorityProjection.scope() + " / " + authorityProjection.judgmentAxis() + '.');
        reasons.add("Painel jurisdicional nativo: " + authorityProjection.panelRoute() + '.');
        reasons.add("Acesso institucional: " + authorityProjection.institutionalPanelCode() + " → " + authorityProjection.institutionalLandingPath() + '.');
        reasons.add("Unidade da autoridade: " + authorityProjection.authorityUnitCode() + " / " + authorityProjection.authorityUnitLabel() + '.');
        if (hearingAt != null && code == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA) {
            reasons.add("Audiência vinculada para " + hearingAt + '.');
        }
        List<String> warnings = new ArrayList<>();
        warnings.addAll(contactWarnings(processo, code));
        if (profile.secrecyAware()) {
            warnings.add("Aplicar trilha sigilosa na secretaria competente.");
        }
        if (Boolean.TRUE.equals(profile.conciliationPreferred()) && code == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA) {
            warnings.add("Conferir pauta conciliatória prioritária da unidade.");
        }
        if (code == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA && hearingAt == null) {
            warnings.add("Ato menciona audiência sem data ou hora consolidadas; a secretaria deve completar a agenda antes da reserva.");
        }
        if (code == MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PERICIA && request != null && request.peritoId() == null) {
            warnings.add("Nomeação pericial sem perito vinculado no comando; validar especialidade, aceite e honorários antes do despacho final de cumprimento.");
        }
        if (code == MagistraturaJudicialProvidenceCode.CONTROLAR_CALCULO_LIQUIDACAO && !containsAny(normalize(join(request == null ? null : request.conteudo(), request == null ? null : request.observacao())), "calculo", "liquidacao", "rpv", "precatorio", "contadoria")) {
            warnings.add("Validar base de cálculo e documentos financeiros antes de materializar a fila derivada.");
        }
        if (code == MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO && tribunalFlow(profile).isEmpty()) {
            warnings.add("Sem malha colegiada dedicada no roteamento; o plano reaproveitará a trilha operacional existente da unidade.");
        }
        if ((usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR || usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL || usuario.getTipoUsuario() == TipoUsuario.MINISTRO)
                && (code == MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO || code == MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO)
                && (request == null || request.orgao() == null || request.orgao().isBlank())) {
            warnings.add("Órgão julgador não informado explicitamente; a projeção jurisdicional usará o deskAxis/organizationalPath da unidade para evitar divergência de colegiado ou plenário.");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("tribunalCodigo", profile.tribunalCodigo());
        metrics.put("tipoJustica", profile.tipoJustica());
        metrics.put("ramoAxis", profile.ramoAxis());
        metrics.put("deskAxis", profile.deskAxis());
        metrics.put("instanciaAxis", profile.instanciaAxis());
        metrics.put("hearingAt", hearingAt);
        metrics.put("hearingDateBucket", hearingAt == null ? null : hearingAt.atOffset(ZoneOffset.UTC).toLocalDate().toString());
        metrics.put("dueAt", dueAt);
        metrics.put("dueDateBucket", dueAt == null ? null : dueAt.atOffset(ZoneOffset.UTC).toLocalDate().toString());
        metrics.put("actorTipo", usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name());
        metrics.put("processoNumero", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        metrics.put("automatic", true);
        metrics.put("unidadeAlvo", institutionalOwner);
        metrics.put("painelAlvo", deskTarget.panelRoute());
        metrics.put("responsavelInstitucional", institutionalOwner);
        metrics.put("authorityScope", authorityProjection.scope());
        metrics.put("authorityAxis", authorityProjection.authorityAxis());
        metrics.put("judgmentAxis", authorityProjection.judgmentAxis());
        metrics.put("authorityLabel", authorityProjection.authorityLabel());
        metrics.put("authorityPanelRoute", authorityProjection.panelRoute());
        metrics.put("authorityProcessMeshRoute", authorityProjection.processMeshRoute());
        metrics.put("authorityReturnRoute", authorityProjection.returnRoute());
        metrics.put("authorityOrgao", authorityProjection.orgaoLabel());
        metrics.put("authorityJusticeAxis", authorityProjection.justiceAxis());
        metrics.put("authorityTribunalAxis", authorityProjection.tribunalAxis());
        metrics.put("authorityClass", authorityProjection.authorityClass());
        metrics.put("authorityInstitutionalPanelCode", authorityProjection.institutionalPanelCode());
        metrics.put("authorityInstitutionalLandingPath", authorityProjection.institutionalLandingPath());
        metrics.put("authorityUnitCode", authorityProjection.authorityUnitCode());
        metrics.put("authorityUnitLabel", authorityProjection.authorityUnitLabel());
        metrics.put("authorityUnitBindingKey", authorityProjection.authorityUnitBindingKey());
        metrics.put("routingUnitCode", unitContext.unidadeCodigo());
        metrics.put("routingVaraLabel", unitContext.varaLabel());
        metrics.put("routingOrgaoLabel", unitContext.orgaoLabel());
        metrics.put("routingComarcaLabel", unitContext.comarcaLabel());
        metrics.put("routingSnapshotRoute", unitContext.snapshotRoute());
        metrics.put("routingUnitBindingKey", unitContext.authorityUnitBindingKey());
        metrics.put("routingCellCode", deskTarget.cellCode());
        metrics.put("routingStageBindingKey", deskTarget.bindingKey());
        metrics.put("prioridadeOperacional", priority);
        metrics.put("dependencias", dependencies);
        metrics.put("retornoEsperado", expectedReturn);
        metrics.put("eventoConclusao", completionEvent);
        metrics.put("modoConfirmacao", confirmationMode);
        metrics.put("collegiateSessionRoute", resolveCollegiateSessionRoute(processo, authorityProjection));
        metrics.put("collegiateAcordaoRoute", resolveCollegiateAcordaoRoute(processo, authorityProjection));
        metrics.put("collegiateOriginReturnRoute", resolveCollegiateOriginReturnRoute(authorityProjection));
        metrics.put("collegiateComplianceRoute", resolveCollegiateComplianceRoute(authorityProjection));
        metrics.put("workItemType", resolveWorkItemType(code).name());
        metrics.put("reuseOperationalMesh", true);
        metrics.put("inboxDerivado", deskTarget.inboxKey());
        metrics.put("queueDerivada", deskTarget.queueCode());
        metrics.put("stageFamily", deskTarget.stageToken());
        metrics.put("stageCellCode", deskTarget.cellCode());
        metrics.put("stageBindingKey", deskTarget.bindingKey());
        metrics.put("confirmationRequired", Boolean.TRUE);
        metrics.put("routeKey", profile.routeKey());
        metrics.put("organizationalPath", profile.organizationalPath());
        return new ProvidencePlan(
                code,
                true,
                deskTarget.stageToken(),
                deskTarget.inboxKey(),
                deskTarget.queueCode(),
                deskTarget.panelRoute(),
                dueAt,
                buildSummary(processo, action, code, request, payload, profile, dependencies, expectedReturn, completionEvent),
                List.copyOf(reasons),
                List.copyOf(warnings),
                safeMap(metrics),
                false,
                resolveWorkItemType(code),
                priority,
                resolveBlocking(code, processo),
                institutionalOwner,
                dependencies,
                expectedReturn,
                completionEvent,
                confirmationMode
        );
    }

    private String resolveCollegiateSessionRoute(Processo processo,
                                                 MagistraturaAuthorityProjection authorityProjection) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return OperationalApiRoutes.ministroPlenarioPauta(processo == null ? null : processo.getId());
        }
        if ("PLENARIO".equals(authorityProjection.judgmentAxis())) {
            return OperationalApiRoutes.desembargadorPlenarioRelator(null);
        }
        return OperationalApiRoutes.secretariatOperationalCollegiatePauta(processo == null ? null : processo.getId());
    }

    private String resolveCollegiateAcordaoRoute(Processo processo,
                                                 MagistraturaAuthorityProjection authorityProjection) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(processo == null ? null : processo.getId());
        }
        return OperationalApiRoutes.secretariatOperationalCollegiateAcordao(null);
    }

    private String resolveCollegiateOriginReturnRoute(MagistraturaAuthorityProjection authorityProjection) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        if ("SUPERIOR".equals(authorityProjection.scope())) {
            return authorityProjection.returnRoute();
        }
        return OperationalApiRoutes.secretariatOperationalCollegiateBaixa(null);
    }

    private String resolveCollegiateComplianceRoute(MagistraturaAuthorityProjection authorityProjection) {
        if (authorityProjection == null || "PRIMEIRO_GRAU".equals(authorityProjection.scope())) {
            return null;
        }
        return authorityProjection.processMeshRoute();
    }

    private Instant computeDueAt(Instant now,
                                 Instant hearingAt,
                                 MagistraturaJudicialProvidenceCode code,
                                 SecretariatOperationalRoutingProfile profile,
                                 MagistraturaJudicialActCommandRequest request) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> hearingAt == null
                    ? now.plus(optionalDuration(profile.audiencePreparationSla(), Duration.ofHours(12)))
                    : hearingAt.minus(optionalDuration(profile.audiencePreparationSla(), Duration.ofHours(48)));
            case EXPEDIR_INTIMACOES -> hearingAt == null
                    ? now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(8)))
                    : hearingAt.minus(Duration.ofHours(24));
            case PROVIDENCIAR_PUBLICACAO -> now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(10)));
            case CUMPRIR_DETERMINACAO_CARTORIO, SANEAR_PROCESSO, PROCESSAR_INCIDENTE_PROCESSUAL -> now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(12)));
            case PROVIDENCIAR_PERICIA -> now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(12)));
            case ABRIR_VISTA_TECNICA -> now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(12)));
            case EXPEDIR_ORDEM_CUMPRIMENTO, REDISTRIBUIR_OU_PREVENIR -> now.plus(optionalDuration(profile.receiptSla(), Duration.ofHours(2)));
            case CONTROLAR_CALCULO_LIQUIDACAO -> now.plus(optionalDuration(profile.executionQueueCode() == null ? null : profile.saneamentoSla(), Duration.ofHours(18)));
            case IMPULSIONAR_EXECUCAO -> now.plus(optionalDuration(profile.receiptSla(), Duration.ofHours(8)));
            case ORGANIZAR_CONCLUSAO -> now.plus(optionalDuration(profile.receiptSla(), Duration.ofHours(2)));
            case REMETER_COLEGIADO_OU_PLENARIO -> now.plus(optionalDuration(profile.saneamentoSla(), Duration.ofHours(3)));
        };
    }

    private Instant adjustDueAt(Instant now, Instant dueAt) {
        if (dueAt == null) {
            return now.plus(MINIMUM_FUTURE_DUE);
        }
        return dueAt.isBefore(now.minusSeconds(60)) ? now.plus(MINIMUM_FUTURE_DUE) : dueAt;
    }

    private List<String> resolveDependencies(MagistraturaJudicialProvidenceCode code,
                                             MagistraturaJudicialActCommandRequest request,
                                             Instant hearingAt) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> immutableList(
                    "partes mínimas vinculadas",
                    hearingAt == null ? "data e hora a consolidar" : "slot de pauta compatível",
                    "confirmação de sala física/virtual"
            );
            case EXPEDIR_INTIMACOES -> immutableList("destinatários identificados", "ato assinado", "meio de comunicação disponível");
            case PROVIDENCIAR_PUBLICACAO -> immutableList("texto final assinado", "canal de publicação definido", "marco temporal para prazo subsequente");
            case CUMPRIR_DETERMINACAO_CARTORIO -> immutableList("ato jurisdicional consolidado", "fila operacional existente disponível");
            case PROVIDENCIAR_PERICIA -> immutableList(
                    request != null && request.peritoId() != null ? "perito previamente vinculado" : "especialidade/perito a confirmar",
                    "quesitos e objeto da prova técnica",
                    "tratamento de honorários ou gratuidade"
            );
            case EXPEDIR_ORDEM_CUMPRIMENTO -> immutableList("unidade executora correta", "dados mínimos de diligência", "checkpoint de retorno configurado");
            case ORGANIZAR_CONCLUSAO -> immutableList("etapas mínimas cumpridas pela secretaria", "motivo da conclusão definido", "retorno habilitado ao gabinete");
            case REMETER_COLEGIADO_OU_PLENARIO -> immutableList("classe recursal ou ato colegiado identificado", "fila recursal competente", "evento de devolução ao órgão de origem parametrizado");
            case ABRIR_VISTA_TECNICA -> immutableList("órgão obrigatório identificado", "prazo de manifestação parametrizado", "bloqueio de avanço quando exigível");
            case CONTROLAR_CALCULO_LIQUIDACAO -> immutableList("base de cálculo íntegra", "documentos financeiros mínimos", "evento de retorno do cálculo pronto");
            case IMPULSIONAR_EXECUCAO -> immutableList("título ou comando executável", "etapa executiva identificada", "controle de retorno de constrição ou cumprimento");
            case SANEAR_PROCESSO -> immutableList("pendência saneadora tipificada", "prazo da parte ou da unidade", "regra de retorno por adimplemento ou descumprimento");
            case PROCESSAR_INCIDENTE_PROCESSUAL -> immutableList("incidente qualificado", "órgão competente confirmado", "efeito processual do incidente parametrizado");
            case REDISTRIBUIR_OU_PREVENIR -> immutableList("fundamento de competência/prevenção", "unidade de destino validada", "trilha auditável preservada");
        };
    }

    private String resolveInstitutionalOwner(SecretariatOperationalRoutingProfile profile,
                                             MagistraturaJudicialProvidenceCode code,
                                             DeskTarget target) {
        String secretariatCode = firstNonBlank(profile.secretariatCode(), profile.organizationalPath(), target.queueCode());
        return switch (code) {
            case EXPEDIR_ORDEM_CUMPRIMENTO -> firstNonBlank(target.queueCode(), secretariatCode, "CENTRAL_MANDADOS");
            case REMETER_COLEGIADO_OU_PLENARIO -> firstNonBlank(target.queueCode(), profile.organizationalPath(), secretariatCode);
            default -> firstNonBlank(secretariatCode, target.queueCode(), target.inboxKey());
        };
    }

    private String expectedReturn(MagistraturaJudicialProvidenceCode code, String stageToken) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> "confirmação de pauta, intimações e reserva material anexadas ao processo";
            case EXPEDIR_INTIMACOES -> "ciência/citação registrada e prazo subsequente habilitado";
            case PROVIDENCIAR_PUBLICACAO -> "data de disponibilização registrada e marco de contagem pronto";
            case CUMPRIR_DETERMINACAO_CARTORIO -> "cumprimento certificado e processo reposicionado na fila correta";
            case PROVIDENCIAR_PERICIA -> "aceite do perito, prazo do laudo e trilha de honorários controlados";
            case EXPEDIR_ORDEM_CUMPRIMENTO -> "retorno de diligência, mandado ou ofício incorporado ao processo";
            case ORGANIZAR_CONCLUSAO -> "processo devolvido à conclusão apenas após checklist mínimo";
            case REMETER_COLEGIADO_OU_PLENARIO -> "ato recursal encaminhado para " + stageToken.toLowerCase(java.util.Locale.ROOT) + " e pronto para baixa/retorno";
            case ABRIR_VISTA_TECNICA -> "manifestação do órgão técnico recebida ou prazo certificado";
            case CONTROLAR_CALCULO_LIQUIDACAO -> "cálculo homologável ou conferido disponível no processo";
            case IMPULSIONAR_EXECUCAO -> "ato executivo, constrição ou cumprimento materializado com retorno monitorado";
            case SANEAR_PROCESSO -> "regularização cumprida ou descumprimento certificado para nova decisão";
            case PROCESSAR_INCIDENTE_PROCESSUAL -> "incidente protocolado, processado e devolvido ao eixo competente";
            case REDISTRIBUIR_OU_PREVENIR -> "redistribuição/prevenção homologada e trilha auditável atualizada";
        };
    }

    private String completionEvent(MagistraturaJudicialProvidenceCode code, String stageToken) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> "AUDIENCIA_PREPARADA";
            case EXPEDIR_INTIMACOES -> "INTIMACAO_OU_CITACAO_CUMPRIDA";
            case PROVIDENCIAR_PUBLICACAO -> "PUBLICACAO_DISPONIBILIZADA";
            case CUMPRIR_DETERMINACAO_CARTORIO -> "DETERMINACAO_CARTORIO_CUMPRIDA";
            case PROVIDENCIAR_PERICIA -> "PERICIA_CONTROLADA";
            case EXPEDIR_ORDEM_CUMPRIMENTO -> "ORDEM_CUMPRIDA_OU_CERTIFICADA";
            case ORGANIZAR_CONCLUSAO -> "RETORNO_A_CONCLUSAO";
            case REMETER_COLEGIADO_OU_PLENARIO -> "FLUXO_" + stageToken + "_CONCLUIDO";
            case ABRIR_VISTA_TECNICA -> "VISTA_TECNICA_DEVOLVIDA";
            case CONTROLAR_CALCULO_LIQUIDACAO -> "CALCULO_PRONTO";
            case IMPULSIONAR_EXECUCAO -> "ETAPA_EXECUTIVA_CONFIRMADA";
            case SANEAR_PROCESSO -> "SANEAMENTO_PROCESSUAL_FECHADO";
            case PROCESSAR_INCIDENTE_PROCESSUAL -> "INCIDENTE_PROCESSUAL_TRATADO";
            case REDISTRIBUIR_OU_PREVENIR -> "REDISTRIBUICAO_AUDITADA";
        };
    }

    private String confirmationMode(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> "reserva_e_confirmacao_de_pauta";
            case EXPEDIR_ORDEM_CUMPRIMENTO, IMPULSIONAR_EXECUCAO -> "checkpoint_operacional_com_retorno";
            case PROVIDENCIAR_PERICIA -> "aceite_e_laudo";
            case PROVIDENCIAR_PUBLICACAO -> "disponibilizacao_oficial";
            default -> "certificacao_no_processo";
        };
    }

    private String buildSummary(Processo processo,
                                MagistraturaJudicialActCode action,
                                MagistraturaJudicialProvidenceCode code,
                                MagistraturaJudicialActCommandRequest request,
                                Map<String, Object> payload,
                                SecretariatOperationalRoutingProfile profile,
                                List<String> dependencies,
                                String expectedReturn,
                                String completionEvent) {
        List<String> parts = new ArrayList<>();
        parts.add("Processo: " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), String.valueOf(processo.getId())));
        parts.add("Ato jurisdicional: " + action.name());
        parts.add("Providência derivada: " + label(code));
        parts.add("Unidade: " + firstNonBlank(profile.secretariatCode(), profile.organizationalPath()));
        String coreText = firstNonBlank(
                request == null ? null : request.observacao(),
                request == null ? null : request.conteudo(),
                request == null ? null : request.dispositivo(),
                request == null ? null : request.fundamentacao(),
                payload == null ? null : stringValue(payload.get("tipo"))
        );
        if (coreText != null) {
            parts.add("Motivo: " + normalizeSpaces(coreText));
        }
        Instant hearingAt = resolveHearingAt(request, payload);
        if (hearingAt != null) {
            parts.add("Data/Hora: " + hearingAt.atOffset(ZoneOffset.of("-03:00")));
        }
        String local = request == null ? null : request.local();
        if (local != null && !local.isBlank()) {
            parts.add("Local: " + local.trim());
        }
        parts.add("Dependências: " + String.join(", ", dependencies));
        parts.add("Retorno esperado: " + expectedReturn);
        parts.add("Evento de conclusão: " + completionEvent);
        return String.join(" | ", parts);
    }

    private Instant resolveHearingAt(MagistraturaJudicialActCommandRequest request, Map<String, Object> payload) {
        if (request != null && request.dataHora() != null) {
            return request.dataHora();
        }
        if (payload != null) {
            Object raw = payload.get("dataHora");
            if (raw instanceof Instant instant) {
                return instant;
            }
        }
        return null;
    }

    private List<String> contactWarnings(Processo processo, MagistraturaJudicialProvidenceCode code) {
        if (!(code == MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES
                || code == MagistraturaJudicialProvidenceCode.ABRIR_VISTA_TECNICA
                || code == MagistraturaJudicialProvidenceCode.EXPEDIR_ORDEM_CUMPRIMENTO)) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        contactEnvelopeResolver.participantSnapshots(processo).forEach(participant -> {
            if (!Boolean.TRUE.equals(participant.get("contactReady"))) {
                warnings.add("Contato ou vinculação pendente para " + participant.get("role") + "; a secretaria deve complementar cadastro ou representação antes do cumprimento automático integral.");
            }
        });
        return List.copyOf(warnings);
    }

    private DeskTarget resolveDeskTarget(Processo processo,
                                         MagistraturaJudicialActCode action,
                                         MagistraturaJudicialProvidenceCode code,
                                         MagistraturaJudicialActCommandRequest request,
                                         SecretariatOperationalRoutingProfile profile,
                                         MagistraturaOperationalUnitContext unitContext,
                                         MagistraturaAuthorityProjection authorityProjection) {
        Map<String, Object> tribunalFlow = tribunalFlow(profile);
        Map<String, Object> queueCodes = nestedMap(tribunalFlow, "queueCodes");
        String defaultReceiptInbox = firstNonBlank(profile.receiptInboxKey(), profile.saneamentoInboxKey(), profile.executionInboxKey());
        String defaultReceiptQueue = firstNonBlank(profile.receiptQueueCode(), profile.saneamentoQueueCode(), profile.executionQueueCode());
        String defaultSaneamentoInbox = firstNonBlank(profile.saneamentoInboxKey(), profile.executionInboxKey(), profile.receiptInboxKey());
        String defaultSaneamentoQueue = firstNonBlank(profile.saneamentoQueueCode(), profile.executionQueueCode(), profile.receiptQueueCode());
        String defaultExecutionInbox = firstNonBlank(profile.executionInboxKey(), profile.saneamentoInboxKey(), profile.receiptInboxKey());
        String defaultExecutionQueue = firstNonBlank(profile.executionQueueCode(), profile.saneamentoQueueCode(), profile.receiptQueueCode());
        if (code == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA) {
            String inbox = firstNonBlank(profile.audienceInboxKey(), defaultExecutionInbox);
            String queue = firstNonBlank(profile.audienceQueueCode(), defaultExecutionQueue);
            String route = unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalSnapshot(), processo.getId(), "AUDIENCIA", inbox, queue);
            return deskTarget(unitContext, "AUDIENCIA", inbox, queue, route);
        }
        if (code == MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO) {
            String stage = resolveRecursalStage(action, request, queueCodes);
            String queue = switch (stage) {
                case "ADMISSIBILIDADE" -> firstNonBlank(stringValue(queueCodes.get("admissibilidade")), stringValue(queueCodes.get("gabineteRelator")), defaultExecutionQueue);
                case "PAUTA" -> firstNonBlank(stringValue(queueCodes.get("pauta")), stringValue(queueCodes.get("publicacaoPauta")), stringValue(queueCodes.get("sessao")), defaultExecutionQueue);
                case "ACORDAO" -> firstNonBlank(stringValue(queueCodes.get("acordao")), defaultExecutionQueue);
                case "EMBARGOS" -> firstNonBlank(stringValue(queueCodes.get("embargos")), profile.secretariatCode() + ":EMBARGOS", defaultExecutionQueue);
                default -> firstNonBlank(stringValue(queueCodes.get("sessao")), stringValue(queueCodes.get("gabineteRelator")), defaultExecutionQueue);
            };
            String route = switch (stage) {
                case "ADMISSIBILIDADE" -> unitContext.panelRoute("/api/v1/processual/recursal/admissibilidade", processo.getId(), stage, defaultExecutionInbox, queue);
                case "PAUTA" -> unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalCollegiatePauta(processo.getId()), processo.getId(), stage, defaultExecutionInbox, queue);
                case "ACORDAO" -> unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalCollegiateAcordao(processo.getId()), processo.getId(), stage, defaultExecutionInbox, queue);
                case "EMBARGOS" -> unitContext.panelRoute(OperationalApiRoutes.secretariatJulgamentoProcesso(processo.getId()), processo.getId(), stage, defaultExecutionInbox, queue);
                default -> unitContext.panelRoute(OperationalApiRoutes.secretariatJulgamentoProcesso(processo.getId()), processo.getId(), stage, defaultExecutionInbox, queue);
            };
            return deskTarget(unitContext, stage, defaultExecutionInbox, queue, route);
        }
        if (code == MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO) {
            String stage = action == MagistraturaJudicialActCode.ACORDAO || action == MagistraturaJudicialActCode.DECISAO_PLENARIA ? "ACORDAO" : "SANEAMENTO";
            String queue = "ACORDAO".equals(stage)
                    ? firstNonBlank(stringValue(queueCodes.get("acordao")), stringValue(queueCodes.get("publicacaoPauta")), defaultExecutionQueue)
                    : defaultSaneamentoQueue;
            String inbox = "ACORDAO".equals(stage) ? defaultExecutionInbox : defaultSaneamentoInbox;
            String route = "ACORDAO".equals(stage)
                    ? unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalCollegiateAcordao(processo.getId()), processo.getId(), stage, inbox, queue)
                    : unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalIntimacao(processo.getId()), processo.getId(), stage, inbox, queue);
            return deskTarget(unitContext, stage, inbox, queue, route);
        }
        if (code == MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES || code == MagistraturaJudicialProvidenceCode.ABRIR_VISTA_TECNICA) {
            return deskTarget(unitContext, "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue,
                    unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalIntimacao(processo.getId()), processo.getId(), "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.ORGANIZAR_CONCLUSAO) {
            return deskTarget(unitContext, "RECEBIMENTO", defaultReceiptInbox, defaultReceiptQueue,
                    unitContext.panelRoute(OperationalApiRoutes.secretariatOperationalConclusao(processo.getId()), processo.getId(), "RECEBIMENTO", defaultReceiptInbox, defaultReceiptQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.REDISTRIBUIR_OU_PREVENIR) {
            return deskTarget(unitContext, "RECEBIMENTO", defaultReceiptInbox, defaultReceiptQueue,
                    unitContext.panelRoute("/api/v1/secretaria/especializada/processos/" + processo.getId() + "/redistribuicao", processo.getId(), "RECEBIMENTO", defaultReceiptInbox, defaultReceiptQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.SANEAR_PROCESSO) {
            return deskTarget(unitContext, "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue,
                    unitContext.panelRoute("/api/v1/secretaria/especializada/processos/" + processo.getId() + "/checklist", processo.getId(), "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.PROCESSAR_INCIDENTE_PROCESSUAL) {
            return deskTarget(unitContext, "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue,
                    unitContext.panelRoute("/api/v1/secretaria/especializada/processos/" + processo.getId() + "/atos", processo.getId(), "SANEAMENTO", defaultSaneamentoInbox, defaultSaneamentoQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.CONTROLAR_CALCULO_LIQUIDACAO) {
            String route = processo.getRamoDireito() == RamoDireito.TRABALHISTA
                    ? OperationalApiRoutes.secretariatOperationalLabourExecucao(processo.getId())
                    : "/api/v1/processual/processos/" + processo.getId() + "/execucao-assistida";
            return deskTarget(unitContext, "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue,
                    unitContext.panelRoute(route, processo.getId(), "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.IMPULSIONAR_EXECUCAO) {
            String route = processo.getRamoDireito() == RamoDireito.TRABALHISTA
                    ? OperationalApiRoutes.secretariatOperationalLabourExecucao(processo.getId())
                    : "/api/v1/processual/processos/" + processo.getId() + "/execucao-assistida";
            return deskTarget(unitContext, "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue,
                    unitContext.panelRoute(route, processo.getId(), "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.EXPEDIR_ORDEM_CUMPRIMENTO) {
            return deskTarget(unitContext, "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue,
                    unitContext.panelRoute(OperationalApiRoutes.oficialJusticaNamedProcessWorkbench(processo.getId()), processo.getId(), "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue));
        }
        if (code == MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PERICIA) {
            return deskTarget(unitContext, "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue,
                    unitContext.panelRoute("/api/v1/secretaria/especializada/processos/" + processo.getId(), processo.getId(), "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue));
        }
        return deskTarget(unitContext, "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue,
                unitContext.panelRoute("/api/v1/secretaria/especializada/processos/" + processo.getId(), processo.getId(), "EXECUCAO", defaultExecutionInbox, defaultExecutionQueue));
    }

    private DeskTarget deskTarget(MagistraturaOperationalUnitContext unitContext,
                                  String stageToken,
                                  String inboxKey,
                                  String queueCode,
                                  String panelRoute) {
        return new DeskTarget(stageToken, inboxKey, queueCode, panelRoute, unitContext.stageCellCode(stageToken, queueCode), unitContext.stageBindingKey(stageToken, inboxKey, queueCode));
    }

    private String resolveRecursalStage(MagistraturaJudicialActCode action,
                                        MagistraturaJudicialActCommandRequest request,
                                        Map<String, Object> queueCodes) {
        String corpus = normalize(join(
                request == null ? null : request.tipo(),
                request == null ? null : request.observacao(),
                request == null ? null : request.conteudo(),
                request == null ? null : request.fundamentacao(),
                request == null ? null : request.ementa(),
                request == null ? null : request.votacao()
        ));
        if (containsAny(corpus, "embargo", "embargos")) {
            return "EMBARGOS";
        }
        if (containsAny(corpus, "admissibilidade", "contrarrazoes", "contrarrazoes", "recurso especial", "recurso extraordinario", "recurso extraordinário", "agravo interno")
                || queueCodes.containsKey("admissibilidade") && action == MagistraturaJudicialActCode.DESPACHO_RELATOR) {
            return "ADMISSIBILIDADE";
        }
        return switch (action) {
            case INCLUSAO_PAUTA -> "PAUTA";
            case ACORDAO, DECISAO_PLENARIA -> "ACORDAO";
            default -> "COLEGIADO";
        };
    }

    private Map<String, Object> enrichMetrics(Map<String, Object> metrics,
                                              Processo processo,
                                              ProvidencePlan plan,
                                              SecretariatOperationalAssignmentService.AssignmentSnapshot assignment) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(safeMap(metrics));
        out.put("panelRoute", plan.targetPanelRoute());
        out.put("stage", plan.stageToken());
        out.put("participantsCount", contactEnvelopeResolver.participantSnapshots(processo).size());
        Map<String, Object> contactEnvelope = contactEnvelopeResolver.buildEnvelope(processo);
        out.put("contactReadyCount", contactEnvelope.get("contactReadyCount"));
        out.put("contactMissingCount", contactEnvelope.get("contactMissingCount"));
        out.put("prioridadeOperacional", plan.priority());
        out.put("dependencias", plan.dependencies());
        out.put("retornoEsperado", plan.expectedReturn());
        out.put("eventoConclusao", plan.completionEvent());
        out.put("modoConfirmacao", plan.confirmationMode());
        out.put("responsavelInstitucional", plan.institutionalOwner());
        out.put("authorityScope", plan.metrics().get("authorityScope"));
        out.put("authorityAxis", plan.metrics().get("authorityAxis"));
        out.put("judgmentAxis", plan.metrics().get("judgmentAxis"));
        out.put("authorityPanelRoute", plan.metrics().get("authorityPanelRoute"));
        out.put("authorityReturnRoute", plan.metrics().get("authorityReturnRoute"));
        out.put("authorityOrgao", plan.metrics().get("authorityOrgao"));
        out.put("authorityUnitCode", plan.metrics().get("authorityUnitCode"));
        out.put("authorityUnitLabel", plan.metrics().get("authorityUnitLabel"));
        out.put("routingUnitCode", plan.metrics().get("routingUnitCode"));
        out.put("routingSnapshotRoute", plan.metrics().get("routingSnapshotRoute"));
        out.put("routingCellCode", plan.metrics().get("routingCellCode"));
        if (assignment != null && assignment.primary() != null) {
            out.put("responsavelNomeadoId", assignment.primary().getId());
            out.put("responsavelNomeado", assignment.primary().getNome());
            out.put("responsavelNomeadoEmail", assignment.primary().getEmail());
        }
        return safeMap(out);
    }
}
