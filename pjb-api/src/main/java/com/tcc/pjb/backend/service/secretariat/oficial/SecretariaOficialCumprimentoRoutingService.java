package com.tcc.pjb.backend.service.secretariat.oficial;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoMaterializacaoRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoReclassificacaoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInboxAccessService;
import com.tcc.pjb.backend.service.servidor.ServidorSecretariaOperacionalService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecretariaOficialCumprimentoRoutingService {

    private static final List<WorkItemStatus> ACTIVE_STATUSES = List.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);
    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);
    private static final String TEMPLATE_PREFIX = "SECRETARIA:RETORNO_CUMPRIMENTO_OFICIAL:";
    private static final String NEXT_TEMPLATE_PREFIX = "SECRETARIA:PROVIDENCIA_CUMPRIMENTO_OFICIAL:";
    private static final String MATERIALIZATION_TEMPLATE_PREFIX = "SECRETARIA:ATO_SUBSEQUENTE_OFICIAL:";
    private static final String MATERIALIZED_MARKER = "ATO_SUBSEQUENTE_MATERIALIZADO:";

    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService routingService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final SecretariatInboxAccessService inboxAccessService;
    private final CurrentUserService currentUserService;
    private final PjbTimeService timeService;
    private final ServidorSecretariaOperacionalService operacionalService;
    private final SecretariatOfficialActsDrawerService officialActsDrawerService;

    public SecretariaOficialCumprimentoRoutingService(WorkItemRepository workItemRepository,
                                                      InstitutionalActorRoutingService routingService,
                                                      OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                      SecretariatInboxAccessService inboxAccessService,
                                                      CurrentUserService currentUserService,
                                                      PjbTimeService timeService,
                                                      ServidorSecretariaOperacionalService operacionalService,
                                                      SecretariatOfficialActsDrawerService officialActsDrawerService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.routingService = Objects.requireNonNull(routingService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.inboxAccessService = Objects.requireNonNull(inboxAccessService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.timeService = Objects.requireNonNull(timeService);
        this.operacionalService = Objects.requireNonNull(operacionalService);
        this.officialActsDrawerService = Objects.requireNonNull(officialActsDrawerService);
    }

    @Transactional
    public Map<String, Object> registrarRecebimentoAutomatico(Processo processo,
                                                              WorkItem mandado,
                                                              Usuario oficial,
                                                              DiligenciaEncerramentoTipo outcome,
                                                              String checklistDigest,
                                                              String bundleReference,
                                                              boolean cienciaConfirmada,
                                                              boolean oficioOriginalEmitido,
                                                              Instant prazoOperacional) {
        Processo safeProcesso = Objects.requireNonNull(processo, "processo_requerido");
        WorkItem safeMandado = Objects.requireNonNull(mandado, "mandado_requerido");
        Usuario safeOficial = Objects.requireNonNull(oficial, "oficial_requerido");
        DiligenciaEncerramentoTipo safeOutcome = outcome == null ? DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO : outcome;
        Classification classification = Classification.fromOutcome(safeOutcome);
        InstitutionalActorRoutingService.InstitutionalRoute route = routeForOutcome(safeProcesso.getId(), classification);
        String templateCode = TEMPLATE_PREFIX + safeMandado.getId() + ':' + classification.name();
        WorkItem intake = workItemRepository.findLatestByProcessoIdAndTemplateCode(safeProcesso.getId(), templateCode)
                .orElseGet(() -> WorkItem.builder().processo(safeProcesso).templateCode(templateCode).build());
        intake.setFaseOrigem(safeProcesso.getFaseAtual());
        intake.setType(WorkItemType.CERTIDAO);
        intake.setTitulo(buildReceiptTitle(safeProcesso, safeOficial, classification));
        intake.setDescricao(buildReceiptDescription(safeProcesso, safeMandado, safeOficial, classification, checklistDigest, bundleReference, cienciaConfirmada, oficioOriginalEmitido));
        intake.setQueueCode(route.queueCode());
        intake.setInboxKey(route.inboxKey());
        intake.setAssignedRole(route.assignedRole());
        intake.setAssignedUser(null);
        intake.setStatus(WorkItemStatus.PENDENTE);
        intake.setPrioridade(classification.priority());
        intake.setBlocking(false);
        intake.setDueAt(resolveDeskDueAt(prazoOperacional, safeMandado.getDueAt(), classification));
        intake.setUf(firstNonBlank(safeProcesso.getUf(), safeOficial.getUf()));
        intake.setComarca(firstNonBlank(safeProcesso.getComarca(), safeOficial.getComarca()));
        intake.setBaseLegal(buildReceiptBaseLegal(safeProcesso, safeOficial, classification, checklistDigest, bundleReference, route));
        intake.setSemInteresse(false);
        WorkItem saved = workItemRepository.save(intake);

        WorkItem nextProvidence = materializeNextProvidence(saved, classification, null);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RETORNO_CUMPRIMENTO_OFICIAL_CLASSIFICADO");
        out.put("classificacao", classification.name());
        out.put("compartimento", classification.bucket());
        out.put("deskWorkItemId", saved.getId());
        out.put("queueCode", saved.getQueueCode());
        out.put("inboxKey", saved.getInboxKey());
        out.put("processoId", safeProcesso.getId());
        out.put("processoNumero", contextEnvelopeService.processNumber(safeProcesso));
        out.put("oficialContexto", contextEnvelopeService.processEnvelope(safeOficial, safeProcesso, safeMandado, null, null));
        out.put("nextProvidence", nextProvidenceMap(nextProvidence, classification));
        out.put("deskPath", OperationalApiRoutes.secretariatOperationalOfficialClosures(saved.getInboxKey()));
        out.put("proximaProvidenciaPath", OperationalApiRoutes.secretariatOperationalOfficialClosureNextProvidence(saved.getId()));
        out.put("materializarAtoPath", OperationalApiRoutes.secretariatOperationalOfficialClosureMaterializeAct(saved.getId()));
        out.put("auditHash", Hashes.sha256HexPrefix(templateCode + '|' + saved.getId() + '|' + nextProvidence.getId(), 32));
        return safeCopy(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> inbox(String inboxKey, int limit) {
        String normalizedInbox = inboxAccessService.requireAccess(inboxKey);
        int safeLimit = Math.max(1, Math.min(limit, 80));
        Usuario operador = currentUserService.getRequired();
        List<WorkItem> items = workItemRepository.findSecretariatOfficialOutcomeItemsByInbox(normalizedInbox, ACTIVE_STATUSES, PageRequest.of(0, safeLimit));
        List<Map<String, Object>> rows = items.stream().map(this::toDeskRow).toList();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", timeService.nowUtc());
        out.put("mode", "SECRETARIAT_OFFICIAL_CLOSURE_INBOX_V1");
        out.put("inboxKey", normalizedInbox);
        out.put("total", workItemRepository.countSecretariatOfficialOutcomeItemsByInbox(normalizedInbox, ACTIVE_STATUSES));
        out.put("visible", rows.size());
        LinkedHashMap<String, Object> operadorMap = new LinkedHashMap<>();
        operadorMap.put("usuarioId", operador.getId());
        operadorMap.put("usuarioNome", firstNonBlank(operador.getNome(), "USUARIO_NAO_IDENTIFICADO"));
        operadorMap.put("perfil", operador.getTipoUsuario() != null ? operador.getTipoUsuario().name() : "SERVIDOR_FORUM");
        out.put("usuarioOperador", safeCopy(operadorMap));
        out.put("items", rows);
        out.put("reclassificacaoPathTemplate", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_RECLASSIFY);
        out.put("proximaProvidenciaPathTemplate", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_NEXT_PROVIDENCE);
        out.put("materializarAtoPathTemplate", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_MATERIALIZE_ACT);
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> reclassificar(Long deskWorkItemId, SecretariaOficialCumprimentoReclassificacaoRequest request) {
        WorkItem desk = resolveDeskForOperationalFollowup(deskWorkItemId);
        if (isDeskMaterialized(desk)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "o retorno do oficial já teve ato subsequente materializado e não pode reabrir a próxima providência automaticamente");
        }
        Classification current = classificationOf(desk);
        Classification override = Classification.fromExternal(request == null ? null : request.classificacaoResolvida()).orElse(current);
        if (override != current) {
            InstitutionalActorRoutingService.InstitutionalRoute route = routeForOutcome(desk.getProcessoId(), override);
            desk.setTemplateCode(TEMPLATE_PREFIX + sourceMandadoToken(desk.getTemplateCode()) + ':' + override.name());
            desk.setTitulo(rewriteReceiptTitle(desk.getTitulo(), override));
            desk.setDescricao(rewriteDescriptionClassificacao(desk.getDescricao(), override));
            desk.setQueueCode(route.queueCode());
            desk.setInboxKey(route.inboxKey());
            desk.setPrioridade(request != null && request.manterPrioridadeOriginalResolvido() ? desk.getPrioridade() : override.priority());
            desk.setBaseLegal(rewriteBaseLegal(desk.getBaseLegal(), override, request == null ? null : request.observacaoResolvida()));
        } else if (request != null && request.observacaoResolvida() != null) {
            desk.setBaseLegal(rewriteBaseLegal(desk.getBaseLegal(), override, request.observacaoResolvida()));
        }
        desk.setStatus(request != null && request.concluirDeskOriginalResolvido() ? WorkItemStatus.CONCLUIDO : WorkItemStatus.EM_EXECUCAO);
        WorkItem savedDesk = workItemRepository.save(desk);
        WorkItem nextProvidence = materializeNextProvidence(savedDesk, override, request == null ? null : request.observacaoResolvida());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RETORNO_OFICIAL_RECLASSIFICADO");
        out.put("deskWorkItemId", savedDesk.getId());
        out.put("classificacaoAnterior", current.name());
        out.put("classificacaoAtual", override.name());
        out.put("queueCode", savedDesk.getQueueCode());
        out.put("inboxKey", savedDesk.getInboxKey());
        out.put("nextProvidence", nextProvidenceMap(nextProvidence, override));
        out.put("auditHash", Hashes.sha256HexPrefix(savedDesk.getId() + "|RECLASSIFICACAO|" + override.name() + '|' + timeService.nowUtc().toEpochMilli(), 32));
        return safeCopy(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> proximaProvidencia(Long deskWorkItemId) {
        WorkItem desk = resolveDeskForOperationalFollowup(deskWorkItemId);
        Classification classification = classificationOf(desk);
        Processo processo = Objects.requireNonNull(desk.getProcesso(), "processo_requerido");
        Optional<WorkItem> currentNextProvidence = findExistingNextProvidence(desk, classification);
        Optional<WorkItem> latestOfficialItem = workItemRepository.findLatestLinkedOfficialByProcesso(processo.getId(), OFFICIAL_ROLES);
        Usuario latestOfficial = latestOfficialItem.map(WorkItem::getAssignedUser).orElse(null);
        Map<String, Object> suggestedOrder = buildSuggestedJudicialOrder(processo, desk, latestOfficial, classification);
        boolean materialized = isDeskMaterialized(desk);
        MaterializationAct materializedAct = materializedActOf(desk).orElse(null);
        ArrayList<Map<String, Object>> acts = new ArrayList<>();
        for (MaterializationAct act : MaterializationAct.values()) {
            acts.add(buildMaterializableActCard(act, desk, classification, materialized, materializedAct, suggestedOrder));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PROXIMA_PROVIDENCIA_OFICIAL_AVALIADA");
        out.put("mode", "SECRETARIAT_OFFICIAL_NEXT_PROVIDENCE_V1");
        out.put("deskWorkItemId", desk.getId());
        out.put("processoId", processo.getId());
        out.put("processoNumero", contextEnvelopeService.processNumber(processo));
        out.put("classificacao", classification.name());
        out.put("compartimento", classification.bucket());
        out.put("compartimentoLabel", classification.bucketLabel());
        out.put("recomendacaoPrincipal", classification.primaryAct().name());
        out.put("recomendacaoPrincipalLabel", classification.primaryAct().label());
        out.put("atoJaMaterializado", materialized);
        if (materializedAct != null) {
            out.put("atoMaterializado", materializedAct.name());
            out.put("atoMaterializadoLabel", materializedAct.label());
        }
        currentNextProvidence.ifPresent(workItem -> out.put("nextProvidence", nextProvidenceMap(workItem, classification)));
        out.put("oficialVinculado", officialResolutionMap(desk, latestOfficial));
        out.put("ordemJudicialSugerida", suggestedOrder);
        out.put("gavetaPrevista", officialActsDrawerService.previewReservation(desk, latestOfficial, classification.primaryAct().name()));
        out.put("atosMaterializaveis", List.copyOf(acts));
        LinkedHashMap<String, Object> paths = new LinkedHashMap<>();
        paths.put("reclassificar", OperationalApiRoutes.secretariatOperationalOfficialClosureReclassify(desk.getId()));
        paths.put("proximaProvidencia", OperationalApiRoutes.secretariatOperationalOfficialClosureNextProvidence(desk.getId()));
        paths.put("materializarAto", OperationalApiRoutes.secretariatOperationalOfficialClosureMaterializeAct(desk.getId()));
        out.put("paths", safeCopy(paths));
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> materializarAto(Long deskWorkItemId, SecretariaOficialCumprimentoMaterializacaoRequest request) {
        WorkItem desk = resolveDeskForOperationalFollowup(deskWorkItemId);
        Classification classification = classificationOf(desk);
        Processo processo = Objects.requireNonNull(desk.getProcesso(), "processo_requerido");
        if (isDeskMaterialized(desk)) {
            LinkedHashMap<String, Object> already = new LinkedHashMap<>();
            already.put("status", "ATO_SUBSEQUENTE_JA_MATERIALIZADO");
            already.put("deskWorkItemId", desk.getId());
            already.put("processoId", processo.getId());
            already.put("processoNumero", contextEnvelopeService.processNumber(processo));
            materializedActOf(desk).ifPresent(act -> {
                already.put("ato", act.name());
                already.put("atoLabel", act.label());
            });
            already.put("auditHash", Hashes.sha256HexPrefix(desk.getId() + "|ALREADY|" + stringOf(materializedActOf(desk).map(Enum::name).orElse(null)), 32));
            return safeCopy(already);
        }
        WorkItem nextProvidence = findExistingNextProvidence(desk, classification)
                .orElseGet(() -> materializeNextProvidence(desk, classification, request == null ? null : request.observacaoResolvida()));
        Optional<WorkItem> latestOfficialItem = workItemRepository.findLatestLinkedOfficialByProcesso(processo.getId(), OFFICIAL_ROLES);
        Usuario latestOfficial = latestOfficialItem.map(WorkItem::getAssignedUser).orElse(null);
        MaterializationAct recommended = classification.primaryAct();
        MaterializationAct act = MaterializationAct.fromExternal(request == null ? null : request.atoResolvido(recommended.name())).orElse(recommended);
        Map<String, Object> suggestedOrder = buildSuggestedJudicialOrder(processo, desk, latestOfficial, classification);
        Map<String, Object> operation = switch (act) {
            case JUNTADA_FINAL_PROCESSUAL -> materializeFinalJuntada(processo, desk, classification, request);
            case NOVA_EXPEDICAO_AO_OFICIAL -> materializeNovaExpedicaoAoOficial(processo, desk, classification, latestOfficial, request);
            case CONCLUSAO_AUTOMATICA_AO_GABINETE -> materializeConclusaoAutomaticaAoGabinete(processo, desk, classification, request);
            case ORDEM_JUDICIAL_SUGERIDA_AO_GABINETE -> materializeSuggestedJudicialOrderToGabinete(processo, desk, classification, latestOfficial, request, suggestedOrder);
        };
        Map<String, Object> finalization = finalizeMaterialization(desk, nextProvidence, act, request == null ? null : request.observacaoResolvida(), operation);
        Map<String, Object> drawerReservation = officialActsDrawerService.registerMaterializedAct(desk, latestOfficial, act.name(), operation, request == null ? null : request.observacaoResolvida());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ATO_SUBSEQUENTE_MATERIALIZADO");
        out.put("deskWorkItemId", desk.getId());
        out.put("processoId", processo.getId());
        out.put("processoNumero", contextEnvelopeService.processNumber(processo));
        out.put("classificacao", classification.name());
        out.put("recomendacaoPrincipal", recommended.name());
        out.put("ato", act.name());
        out.put("atoLabel", act.label());
        out.put("operacao", operation);
        out.put("finalizacao", finalization);
        out.put("gavetaSecretaria", drawerReservation);
        out.put("ordemJudicialSugerida", suggestedOrder);
        out.put("auditHash", Hashes.sha256HexPrefix(desk.getId() + "|" + act.name() + '|' + timeService.nowUtc().toEpochMilli(), 32));
        return safeCopy(out);
    }

    private WorkItem resolveDeskForOperationalFollowup(Long deskWorkItemId) {
        WorkItem desk = workItemRepository.findById(deskWorkItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "retorno classificado do oficial não encontrado"));
        validateDeskItem(desk);
        inboxAccessService.requireAccess(desk.getInboxKey());
        return desk;
    }

    private Optional<WorkItem> findExistingNextProvidence(WorkItem desk, Classification classification) {
        return workItemRepository.findLatestByProcessoIdAndTemplateCode(desk.getProcessoId(), nextProvidenceTemplateCode(desk, classification));
    }

    private String nextProvidenceTemplateCode(WorkItem desk, Classification classification) {
        return NEXT_TEMPLATE_PREFIX + sourceMandadoToken(desk.getTemplateCode()) + ':' + classification.name();
    }

    private String materializationTemplateCode(WorkItem desk, MaterializationAct act) {
        return MATERIALIZATION_TEMPLATE_PREFIX + sourceMandadoToken(desk.getTemplateCode()) + ':' + act.name();
    }

    private Map<String, Object> materializeFinalJuntada(Processo processo,
                                                        WorkItem desk,
                                                        Classification classification,
                                                        SecretariaOficialCumprimentoMaterializacaoRequest request) {
        String tipoDocumento = request == null ? "CERTIDAO_CUMPRIMENTO_OFICIAL_FINAL" : request.tipoDocumentoResolvido("CERTIDAO_CUMPRIMENTO_OFICIAL_FINAL");
        String descricao = request == null
                ? buildFinalJuntadaDescription(desk, classification, null)
                : request.descricaoResolvida(buildFinalJuntadaDescription(desk, classification, request.observacaoResolvida()));
        String origem = request == null ? "SECRETARIA_ATO_SUBSEQUENTE_OFICIAL" : request.origemResolvida("SECRETARIA_ATO_SUBSEQUENTE_OFICIAL");
        Map<String, Object> response = operacionalService.realizarJuntada(processo.getId(), tipoDocumento, descricao, origem);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("atoExecutado", MaterializationAct.JUNTADA_FINAL_PROCESSUAL.name());
        out.put("tipoDocumento", tipoDocumento);
        out.put("origem", origem);
        out.put("resultado", response);
        return safeCopy(out);
    }

    private Map<String, Object> materializeNovaExpedicaoAoOficial(Processo processo,
                                                                  WorkItem desk,
                                                                  Classification classification,
                                                                  Usuario latestOfficial,
                                                                  SecretariaOficialCumprimentoMaterializacaoRequest request) {
        Long officialId = request == null ? latestOfficialId(latestOfficial) : request.oficialIdResolvido(latestOfficialId(latestOfficial));
        String observation = request == null ? null : request.observacaoResolvida();
        String fundamento = request == null
                ? buildReexpeditionFundamento(desk, classification, latestOfficial, null)
                : request.fundamentoResolvido(buildReexpeditionFundamento(desk, classification, latestOfficial, observation));
        String conteudo = request == null
                ? buildReexpeditionConteudo(desk, classification, latestOfficial, null)
                : request.conteudoOperacionalResolvido(buildReexpeditionConteudo(desk, classification, latestOfficial, observation));
        String prazo = request == null ? classification.defaultExpeditionPrazo() : request.prazoResolvido(classification.defaultExpeditionPrazo());
        Map<String, Object> response = operacionalService.expedicaoIntimacao(
                processo.getId(),
                "OFICIAL DE JUSTIÇA",
                conteudo,
                prazo,
                officialId,
                officialId != null,
                request == null ? "SECRETARIA_ATO_SUBSEQUENTE_OFICIAL" : request.origemResolvida("SECRETARIA_ATO_SUBSEQUENTE_OFICIAL"),
                fundamento,
                observation,
                request != null && request.manterRetornoForumAbertoResolvido(false)
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("atoExecutado", MaterializationAct.NOVA_EXPEDICAO_AO_OFICIAL.name());
        out.put("oficialIdResolvido", officialId);
        putIfNotBlank(out, "oficialNomeResolvido", latestOfficial != null ? latestOfficial.getNome() : extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        out.put("prazo", prazo);
        out.put("resultado", response);
        return safeCopy(out);
    }

    private Map<String, Object> materializeConclusaoAutomaticaAoGabinete(Processo processo,
                                                                         WorkItem desk,
                                                                         Classification classification,
                                                                         SecretariaOficialCumprimentoMaterializacaoRequest request) {
        String motivo = request == null
                ? buildAutomaticConclusionReason(desk, classification, null)
                : request.descricaoResolvida(buildAutomaticConclusionReason(desk, classification, request.observacaoResolvida()));
        Map<String, Object> response = operacionalService.conclusaoParaDespacho(processo.getId(), motivo);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("atoExecutado", MaterializationAct.CONCLUSAO_AUTOMATICA_AO_GABINETE.name());
        out.put("motivoConclusao", motivo);
        out.put("resultado", response);
        return safeCopy(out);
    }

    private Map<String, Object> materializeSuggestedJudicialOrderToGabinete(Processo processo,
                                                                            WorkItem desk,
                                                                            Classification classification,
                                                                            Usuario latestOfficial,
                                                                            SecretariaOficialCumprimentoMaterializacaoRequest request,
                                                                            Map<String, Object> suggestedOrder) {
        MaterializationAct act = MaterializationAct.ORDEM_JUDICIAL_SUGERIDA_AO_GABINETE;
        InstitutionalActorRoutingService.InstitutionalRoute route = routingService.gabineteDecision(processo.getId(), "ORDEM_JUDICIAL_SUGERIDA_OFICIAL");
        String templateCode = materializationTemplateCode(desk, act);
        WorkItem gabItem = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        gabItem.setFaseOrigem(processo.getFaseAtual());
        gabItem.setType(WorkItemType.DECISAO);
        gabItem.setTitulo(buildSuggestedOrderTitle(processo, classification));
        gabItem.setDescricao(buildSuggestedOrderDescription(processo, desk, classification, latestOfficial, request, suggestedOrder));
        gabItem.setQueueCode(route.queueCode());
        gabItem.setInboxKey(route.inboxKey());
        gabItem.setAssignedRole(route.assignedRole());
        gabItem.setAssignedUser(null);
        gabItem.setStatus(WorkItemStatus.PENDENTE);
        gabItem.setPrioridade(resolveSuggestedOrderPriority(classification, request));
        gabItem.setBlocking(true);
        gabItem.setDueAt(resolveSuggestedOrderDueAt(desk, request, classification));
        gabItem.setUf(firstNonBlank(processo.getUf(), desk.getUf()));
        gabItem.setComarca(firstNonBlank(processo.getComarca(), desk.getComarca()));
        gabItem.setBaseLegal(buildSuggestedOrderBaseLegal(processo, desk, classification, latestOfficial, request, route));
        gabItem.setSemInteresse(false);
        WorkItem saved = workItemRepository.save(gabItem);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("atoExecutado", act.name());
        out.put("workItemId", saved.getId());
        out.put("queueCode", saved.getQueueCode());
        out.put("inboxKey", saved.getInboxKey());
        out.put("assignedRole", saved.getAssignedRole() != null ? saved.getAssignedRole().name() : null);
        out.put("dueAt", saved.getDueAt());
        out.put("judgeEndpoint", judgeOrderEndpoint(processo.getId()));
        out.put("requestBodySugerido", safeCopy(new LinkedHashMap<>(requestBodyFromSuggestedOrder(suggestedOrder))));
        return safeCopy(out);
    }

    private Map<String, Object> finalizeMaterialization(WorkItem desk,
                                                        WorkItem nextProvidence,
                                                        MaterializationAct act,
                                                        String observation,
                                                        Map<String, Object> operation) {
        Instant now = timeService.nowUtc();
        String auditHash = Hashes.sha256HexPrefix(desk.getId() + "|MATERIALIZACAO|" + act.name() + '|' + now.toEpochMilli(), 32);
        desk.setStatus(WorkItemStatus.CONCLUIDO);
        desk.setSemInteresse(false);
        desk.setBaseLegal(appendMaterializationTrace(desk.getBaseLegal(), act, observation, auditHash));
        desk.setDescricao(appendMaterializationSummary(desk.getDescricao(), act, observation));
        workItemRepository.save(desk);
        if (nextProvidence != null && nextProvidence.getStatus() != WorkItemStatus.CANCELADO) {
            nextProvidence.setStatus(WorkItemStatus.CONCLUIDO);
            nextProvidence.setSemInteresse(false);
            nextProvidence.setBaseLegal(appendMaterializationTrace(nextProvidence.getBaseLegal(), act, observation, auditHash));
            nextProvidence.setDescricao(appendMaterializationSummary(nextProvidence.getDescricao(), act, observation));
            workItemRepository.save(nextProvidence);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("deskStatusFinal", desk.getStatus() != null ? desk.getStatus().name() : null);
        out.put("nextProvidenceStatusFinal", nextProvidence != null && nextProvidence.getStatus() != null ? nextProvidence.getStatus().name() : null);
        out.put("atoMaterializado", act.name());
        out.put("auditHash", auditHash);
        out.put("materializadoEm", now);
        out.put("resultadoOperacional", operation);
        return safeCopy(out);
    }

    private Map<String, Object> buildMaterializableActCard(MaterializationAct act,
                                                           WorkItem desk,
                                                           Classification classification,
                                                           boolean materialized,
                                                           MaterializationAct materializedAct,
                                                           Map<String, Object> suggestedOrder) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("ato", act.name());
        out.put("label", act.label());
        out.put("categoria", act.category());
        out.put("recomendado", act == classification.primaryAct());
        out.put("disponivel", !materialized);
        if (materializedAct != null) {
            out.put("materializadoNesteDesk", act == materializedAct);
        }
        out.put("materializarPath", OperationalApiRoutes.secretariatOperationalOfficialClosureMaterializeAct(desk.getId()));
        out.put("requestDefault", buildActDefaultPayload(act, desk, classification, suggestedOrder));
        return safeCopy(out);
    }

    private Map<String, Object> buildActDefaultPayload(MaterializationAct act,
                                                       WorkItem desk,
                                                       Classification classification,
                                                       Map<String, Object> suggestedOrder) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("ato", act.name());
        switch (act) {
            case JUNTADA_FINAL_PROCESSUAL -> {
                payload.put("tipoDocumento", "CERTIDAO_CUMPRIMENTO_OFICIAL_FINAL");
                payload.put("descricao", buildFinalJuntadaDescription(desk, classification, null));
                payload.put("origem", "SECRETARIA_ATO_SUBSEQUENTE_OFICIAL");
            }
            case NOVA_EXPEDICAO_AO_OFICIAL -> {
                payload.put("fundamento", buildReexpeditionFundamento(desk, classification, null, null));
                payload.put("conteudoOperacional", buildReexpeditionConteudo(desk, classification, null, null));
                payload.put("prazo", classification.defaultExpeditionPrazo());
                payload.put("origem", "SECRETARIA_ATO_SUBSEQUENTE_OFICIAL");
            }
            case CONCLUSAO_AUTOMATICA_AO_GABINETE -> payload.put("descricao", buildAutomaticConclusionReason(desk, classification, null));
            case ORDEM_JUDICIAL_SUGERIDA_AO_GABINETE -> payload.putAll(requestBodyFromSuggestedOrder(suggestedOrder));
        }
        return safeCopy(payload);
    }

    private Map<String, Object> buildSuggestedJudicialOrder(Processo processo,
                                                            WorkItem desk,
                                                            Usuario latestOfficial,
                                                            Classification classification) {
        LinkedHashMap<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("oficialId", latestOfficialId(latestOfficial));
        requestBody.put("fundamento", buildSuggestedJudicialFundamento(desk, classification, latestOfficial));
        requestBody.put("conteudoOperacional", buildSuggestedJudicialConteudo(desk, classification, latestOfficial));
        requestBody.put("tipoCumprimento", classification.defaultJudicialOrderType());
        requestBody.put("prioridade", classification.defaultJudicialPriority());
        putIfNotBlank(requestBody, "janelaTerritorial", buildSuggestedTerritoryWindow(processo, desk));
        requestBody.put("cienciaObrigatoria", Boolean.TRUE);
        requestBody.put("exigirOficioOriginalNoEncerramento", Boolean.TRUE);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("endpoint", judgeOrderEndpoint(processo.getId()));
        out.put("requestBody", safeCopy(requestBody));
        out.put("oficial", officialResolutionMap(desk, latestOfficial));
        out.put("fundamento", requestBody.get("fundamento"));
        out.put("conteudoOperacional", requestBody.get("conteudoOperacional"));
        out.put("prioridade", requestBody.get("prioridade"));
        out.put("janelaTerritorial", requestBody.get("janelaTerritorial"));
        out.put("cienciaObrigatoria", requestBody.get("cienciaObrigatoria"));
        out.put("travaOficioOriginalNoEncerramento", requestBody.get("exigirOficioOriginalNoEncerramento"));
        return safeCopy(out);
    }

    private Map<String, Object> requestBodyFromSuggestedOrder(Map<String, Object> suggestedOrder) {
        if (suggestedOrder == null) {
            return Map.of();
        }
        Object requestBody = suggestedOrder.get("requestBody");
        if (requestBody instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(String.valueOf(key), value);
                }
            });
            return safeCopy(out);
        }
        return Map.of();
    }

    private Long latestOfficialId(Usuario latestOfficial) {
        return latestOfficial != null ? latestOfficial.getId() : null;
    }

    private int resolveSuggestedOrderPriority(Classification classification,
                                              SecretariaOficialCumprimentoMaterializacaoRequest request) {
        return request == null ? classification.defaultJudicialPriority() : request.prioridadeResolvida(classification.defaultJudicialPriority());
    }

    private Instant resolveSuggestedOrderDueAt(WorkItem desk,
                                               SecretariaOficialCumprimentoMaterializacaoRequest request,
                                               Classification classification) {
        Instant fallback = classification == Classification.FRUSTRADO
                ? timeService.nowUtc().plusSeconds(6 * 3600)
                : resolveNextProvidenceDueAt(desk.getDueAt(), classification);
        return request == null ? fallback : request.dueAtResolvido(fallback);
    }

    private String buildFinalJuntadaDescription(WorkItem desk, Classification classification, String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Juntada final processual materializada a partir do retorno classificado do cumprimento do Oficial de Justiça.");
        parts.add("Compartimento de origem: " + classification.bucketLabel() + '.');
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Resumo do retorno: " + trim(desk.getDescricao(), 1000));
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação operacional: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1800);
    }

    private String buildReexpeditionFundamento(WorkItem desk,
                                               Classification classification,
                                               Usuario latestOfficial,
                                               String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Nova expedição ao Oficial de Justiça derivada do retorno classificado do cumprimento.");
        parts.add("Compartimento: " + classification.bucketLabel() + '.');
        String oficialResolvido = firstNonBlank(latestOfficial != null ? latestOfficial.getNome() : null, extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        if (oficialResolvido != null) {
            parts.add("Oficial resolvido: " + oficialResolvido + '.');
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação cartorária: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1400);
    }

    private String buildReexpeditionConteudo(WorkItem desk,
                                             Classification classification,
                                             Usuario latestOfficial,
                                             String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Reexpedir diligência ao Oficial de Justiça com base no retorno classificado pela secretaria.");
        parts.add("Classificação de origem: " + classification.bucketLabel() + '.');
        if (latestOfficial != null && latestOfficial.getNome() != null) {
            parts.add("Oficial sugerido: " + latestOfficial.getNome() + '.');
        }
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Resumo operacional do retorno: " + trim(desk.getDescricao(), 1000));
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação complementar: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1800);
    }

    private String buildAutomaticConclusionReason(WorkItem desk,
                                                  Classification classification,
                                                  String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Conclusão automática ao gabinete a partir do retorno classificado do Oficial de Justiça.");
        parts.add("Compartimento: " + classification.bucketLabel() + '.');
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Resumo do retorno: " + trim(desk.getDescricao(), 900));
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação da secretaria: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1500);
    }

    private String buildSuggestedJudicialFundamento(WorkItem desk,
                                                    Classification classification,
                                                    Usuario latestOfficial) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Retorno classificado do Oficial de Justiça sugere ordem judicial subsequente no mesmo processo.");
        parts.add("Compartimento: " + classification.bucketLabel() + '.');
        String oficialVinculado = firstNonBlank(latestOfficial != null ? latestOfficial.getNome() : null, extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        if (oficialVinculado != null) {
            parts.add("Oficial vinculado: " + oficialVinculado + '.');
        }
        return trim(String.join(" ", parts), 1200);
    }

    private String buildSuggestedJudicialConteudo(WorkItem desk,
                                                  Classification classification,
                                                  Usuario latestOfficial) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Avaliar nova ordem judicial de cumprimento ao Oficial de Justiça com base no retorno classificado pela secretaria.");
        parts.add("Classe operacional: " + classification.bucketLabel() + '.');
        if (latestOfficial != null && latestOfficial.getNome() != null) {
            parts.add("Oficial sugerido: " + latestOfficial.getNome() + '.');
        }
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Resumo do retorno classificado: " + trim(desk.getDescricao(), 900));
        }
        return trim(String.join(" ", parts), 1600);
    }

    private String buildSuggestedTerritoryWindow(Processo processo, WorkItem desk) {
        return firstNonBlank(
                joinNonBlank(" — ", processo != null ? processo.getComarca() : null, processo != null ? processo.getUf() : null),
                joinNonBlank(" — ", extractToken(desk.getDescricao(), "Cidade: ", '.'), firstNonBlank(processo != null ? processo.getUf() : null, desk.getUf())),
                processo != null ? processo.getVara() : null,
                extractToken(desk.getDescricao(), "Vara: ", '.')
        );
    }

    private String buildSuggestedOrderTitle(Processo processo, Classification classification) {
        return trim("Ordem judicial sugerida ao gabinete — " + classification.bucketLabel() + " — " + contextEnvelopeService.processNumber(processo), 220);
    }

    private String buildSuggestedOrderDescription(Processo processo,
                                                  WorkItem desk,
                                                  Classification classification,
                                                  Usuario latestOfficial,
                                                  SecretariaOficialCumprimentoMaterializacaoRequest request,
                                                  Map<String, Object> suggestedOrder) {
        Map<String, Object> requestBody = requestBodyFromSuggestedOrder(suggestedOrder);
        ArrayList<String> parts = new ArrayList<>();
        parts.add("A secretaria/cartório materializou uma ordem judicial sugerida ao gabinete a partir do retorno classificado do Oficial de Justiça.");
        parts.add("Processo: " + contextEnvelopeService.processNumber(processo) + '.');
        parts.add("Compartimento de origem: " + classification.bucketLabel() + '.');
        parts.add("Endpoint do juiz: " + judgeOrderEndpoint(processo.getId()) + '.');
        String oficialSugerido = firstNonBlank(latestOfficial != null ? latestOfficial.getNome() : null, extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        if (oficialSugerido != null) {
            parts.add("Oficial sugerido: " + oficialSugerido + '.');
        }
        String fundamentoSugerido = stringOf(requestBody.get("fundamento"));
        if (fundamentoSugerido != null) {
            parts.add("Fundamento sugerido: " + fundamentoSugerido + '.');
        }
        String conteudoSugerido = stringOf(requestBody.get("conteudoOperacional"));
        if (conteudoSugerido != null) {
            parts.add("Conteúdo operacional sugerido: " + conteudoSugerido + '.');
        }
        String janelaSugerida = stringOf(requestBody.get("janelaTerritorial"));
        if (janelaSugerida != null) {
            parts.add("Janela territorial sugerida: " + janelaSugerida + '.');
        }
        if (requestBody.get("prioridade") != null) {
            parts.add("Prioridade sugerida: " + requestBody.get("prioridade") + '.');
        }
        if (request != null && request.observacaoResolvida() != null) {
            parts.add("Observação da secretaria: " + request.observacaoResolvida() + '.');
        }
        return trim(String.join(" ", parts), 1900);
    }

    private String buildSuggestedOrderBaseLegal(Processo processo,
                                                WorkItem desk,
                                                Classification classification,
                                                Usuario latestOfficial,
                                                SecretariaOficialCumprimentoMaterializacaoRequest request,
                                                InstitutionalActorRoutingService.InstitutionalRoute route) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Sugestão de ordem judicial materializada ao gabinete com base na classificação do retorno do Oficial.");
        parts.add("Compartimento: " + classification.bucketLabel() + '.');
        parts.add("Rota institucional: " + firstNonBlank(route.routeAxis(), classification.name()) + '.');
        String oficialVinculadoBase = firstNonBlank(latestOfficial != null ? latestOfficial.getNome() : null, extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        if (oficialVinculadoBase != null) {
            parts.add("Oficial vinculado: " + oficialVinculadoBase + '.');
        }
        String janelaTerritorial = buildSuggestedTerritoryWindow(processo, desk);
        if (janelaTerritorial != null) {
            parts.add("Janela territorial sugerida: " + janelaTerritorial + '.');
        }
        if (request != null && request.observacaoResolvida() != null) {
            parts.add("Observação cartorária: " + request.observacaoResolvida() + '.');
        }
        return trim(String.join(" ", parts), 1500);
    }

    private String judgeOrderEndpoint(Long processoId) {
        return "/api/v1/juiz/gabinete-decisoes/processos/" + processoId + "/ordem-cumprimento-oficial";
    }

    private String appendMaterializationTrace(String baseLegal,
                                              MaterializationAct act,
                                              String observation,
                                              String auditHash) {
        ArrayList<String> parts = new ArrayList<>();
        if (baseLegal != null && !baseLegal.isBlank()) {
            parts.add(baseLegal);
        }
        parts.add(MATERIALIZED_MARKER + ' ' + act.name() + '.');
        parts.add("Hash de auditoria: " + auditHash + '.');
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação de materialização: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1800);
    }

    private String appendMaterializationSummary(String description,
                                                MaterializationAct act,
                                                String observation) {
        ArrayList<String> parts = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            parts.add(description);
        }
        parts.add("Ato subsequente materializado: " + act.label() + '.');
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1900);
    }

    private Optional<MaterializationAct> materializedActOf(WorkItem desk) {
        String candidate = firstNonBlank(desk != null ? desk.getBaseLegal() : null, desk != null ? desk.getDescricao() : null);
        if (candidate == null || !candidate.contains(MATERIALIZED_MARKER)) {
            return Optional.empty();
        }
        String token = extractToken(candidate, MATERIALIZED_MARKER + ' ', '.');
        return MaterializationAct.fromExternal(token);
    }

    private boolean isDeskMaterialized(WorkItem desk) {
        return materializedActOf(desk).isPresent();
    }

    private Map<String, Object> officialResolutionMap(WorkItem desk, Usuario latestOfficial) {
        if (latestOfficial != null) {
            return contextEnvelopeService.oficialEnvelope(latestOfficial, null);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfNotBlank(out, "nome", extractToken(desk.getDescricao(), "Oficial responsável: ", '.'));
        putIfNotBlank(out, "uf", desk.getUf());
        putIfNotBlank(out, "comarca", desk.getComarca());
        return safeCopy(out);
    }

    private WorkItem materializeNextProvidence(WorkItem desk, Classification classification, String observation) {
        Processo processo = Objects.requireNonNull(desk.getProcesso(), "processo_requerido");
        InstitutionalActorRoutingService.InstitutionalRoute route = routeForNextProvidence(processo.getId(), classification);
        String templateCode = nextProvidenceTemplateCode(desk, classification);
        String sourceToken = sourceMandadoToken(desk.getTemplateCode());
        workItemRepository.findAllByProcesso(processo.getId()).stream()
                .filter(item -> item.getTemplateCode() != null && item.getTemplateCode().startsWith(NEXT_TEMPLATE_PREFIX + sourceToken + ':'))
                .filter(item -> !templateCode.equals(item.getTemplateCode()))
                .filter(item -> item.getStatus() != WorkItemStatus.CANCELADO)
                .forEach(item -> {
                    item.setSemInteresse(true);
                    item.setStatus(WorkItemStatus.CONCLUIDO);
                    workItemRepository.save(item);
                });
        WorkItem next = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        next.setFaseOrigem(processo.getFaseAtual());
        next.setType(classification.nextType());
        next.setTitulo(buildProvidenceTitle(processo, classification));
        next.setDescricao(buildProvidenceDescription(desk, classification, observation));
        next.setQueueCode(route.queueCode());
        next.setInboxKey(route.inboxKey());
        next.setAssignedRole(route.assignedRole());
        next.setAssignedUser(null);
        next.setStatus(WorkItemStatus.PENDENTE);
        next.setPrioridade(classification.priority());
        next.setBlocking(classification.blocking());
        next.setDueAt(resolveNextProvidenceDueAt(desk.getDueAt(), classification));
        next.setUf(desk.getUf());
        next.setComarca(desk.getComarca());
        next.setBaseLegal(buildProvidenceBaseLegal(desk, classification, observation, route));
        next.setSemInteresse(false);
        return workItemRepository.save(next);
    }

    private InstitutionalActorRoutingService.InstitutionalRoute routeForOutcome(Long processoId, Classification classification) {
        return switch (classification) {
            case POSITIVO -> routingService.secretaryReceipt(processoId, "RETORNO_CUMPRIMENTO_OFICIAL_POSITIVO");
            case PARCIAL -> routingService.secretarySaneamento(processoId, "RETORNO_CUMPRIMENTO_OFICIAL_PARCIAL");
            case FRUSTRADO -> routingService.secretaryExecution(processoId, "RETORNO_CUMPRIMENTO_OFICIAL_FRUSTRADO");
        };
    }

    private InstitutionalActorRoutingService.InstitutionalRoute routeForNextProvidence(Long processoId, Classification classification) {
        return switch (classification) {
            case POSITIVO -> routingService.secretaryReceipt(processoId, "JUNTADA_RESULTADO_CUMPRIMENTO_OFICIAL");
            case PARCIAL -> routingService.secretarySaneamento(processoId, "SANEAMENTO_RESULTADO_CUMPRIMENTO_OFICIAL");
            case FRUSTRADO -> routingService.secretaryExecution(processoId, "REANALISE_RESULTADO_CUMPRIMENTO_OFICIAL");
        };
    }

    private Map<String, Object> toDeskRow(WorkItem desk) {
        Processo processo = desk.getProcesso();
        String oficialNome = extractToken(desk.getDescricao(), "Oficial responsável: ", '.');
        String vara = firstNonBlank(processo != null ? processo.getVara() : null, extractToken(desk.getDescricao(), "Vara: ", '.'));
        String cidade = firstNonBlank(processo != null ? processo.getComarca() : null, extractToken(desk.getDescricao(), "Cidade: ", '.'));
        String tribunal = firstNonBlank(processo != null ? processo.getTribunal() : null, extractToken(desk.getDescricao(), "Tribunal: ", '.'));
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("deskWorkItemId", desk.getId());
        row.put("processoId", desk.getProcessoId());
        row.put("processoNumero", processo != null ? contextEnvelopeService.processNumber(processo) : "PROCESSO_NAO_IDENTIFICADO");
        row.put("titulo", desk.getTitulo());
        row.put("classificacao", classificationOf(desk).name());
        row.put("compartimento", classificationOf(desk).bucket());
        row.put("status", desk.getStatus() != null ? desk.getStatus().name() : null);
        row.put("prazo", desk.getDueAt());
        row.put("vara", vara);
        row.put("forum", contextEnvelopeService.resolveForum(processo, contextEnvelopeService.resolveEsfera(null, processo, TipoUsuario.SERVIDOR_FORUM), cidade));
        row.put("cidade", cidade);
        row.put("uf", firstNonBlank(processo != null ? processo.getUf() : null, desk.getUf()));
        row.put("tribunal", tribunal);
        row.put("oficialNome", oficialNome);
        row.put("queueCode", desk.getQueueCode());
        row.put("inboxKey", desk.getInboxKey());
        row.put("reclassificarPath", OperationalApiRoutes.secretariatOperationalOfficialClosureReclassify(desk.getId()));
        row.put("proximaProvidenciaPath", OperationalApiRoutes.secretariatOperationalOfficialClosureNextProvidence(desk.getId()));
        row.put("materializarAtoPath", OperationalApiRoutes.secretariatOperationalOfficialClosureMaterializeAct(desk.getId()));
        row.put("gavetaPrevista", officialActsDrawerService.previewReservation(desk, null, classificationOf(desk).primaryAct().name()));
        if (isDeskMaterialized(desk)) {
            materializedActOf(desk).ifPresent(act -> row.put("atoMaterializado", act.name()));
        }
        return safeCopy(row);
    }

    private void validateDeskItem(WorkItem desk) {
        if (desk.getTemplateCode() == null || !desk.getTemplateCode().startsWith(TEMPLATE_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "work item informado não pertence à recepção classificada de cumprimento do oficial");
        }
    }

    private Classification classificationOf(WorkItem desk) {
        return Classification.fromTemplate(desk.getTemplateCode()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "classificação do retorno do oficial não pôde ser inferida"));
    }

    private String buildReceiptTitle(Processo processo, Usuario oficial, Classification classification) {
        return trim("Retorno do Cumprimento do Oficial — " + classification.bucketLabel() + " — " + contextEnvelopeService.processNumber(processo) + " — " + firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO"), 220);
    }

    private String buildReceiptDescription(Processo processo,
                                           WorkItem mandado,
                                           Usuario oficial,
                                           Classification classification,
                                           String checklistDigest,
                                           String bundleReference,
                                           boolean cienciaConfirmada,
                                           boolean oficioOriginalEmitido) {
        ArrayList<String> parts = new ArrayList<>();
        Map<String, Object> envelope = contextEnvelopeService.processEnvelope(oficial, processo, mandado, null, null);
        parts.add("Resultado do cumprimento recebido automaticamente da trilha soberana do Oficial de Justiça.");
        parts.add("Classificação automática: " + classification.bucketLabel() + '.');
        parts.add("Processo: " + contextEnvelopeService.processNumber(processo) + '.');
        parts.add("Oficial responsável: " + firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO") + '.');
        parts.add("Vara: " + firstNonBlank(stringOf(envelope.get("vara")), "VARA_NAO_IDENTIFICADA") + '.');
        parts.add("Fórum: " + firstNonBlank(stringOf(envelope.get("forum")), "FORUM_NAO_IDENTIFICADO") + '.');
        parts.add("Cidade: " + firstNonBlank(stringOf(envelope.get("cidade")), "CIDADE_NAO_IDENTIFICADA") + '.');
        parts.add("Tribunal: " + firstNonBlank(stringOf(envelope.get("tribunal")), "TRIBUNAL_NAO_IDENTIFICADO") + '.');
        parts.add("Região judicial: " + firstNonBlank(stringOf(envelope.get("regiaoJudicial")), "REGIAO_NAO_IDENTIFICADA") + '.');
        parts.add("Próxima providência sugerida: " + classification.nextProvidenceLabel() + '.');
        if (checklistDigest != null) {
            parts.add("Checklist digest: " + checklistDigest + '.');
        }
        if (bundleReference != null) {
            parts.add("Bundle operacional: " + bundleReference + '.');
        }
        parts.add("Ciência confirmada: " + (cienciaConfirmada ? "SIM" : "NAO") + '.');
        parts.add("Ofício original direto emitido: " + (oficioOriginalEmitido ? "SIM" : "NAO") + '.');
        return trim(String.join(" ", parts), 1900);
    }

    private String buildReceiptBaseLegal(Processo processo,
                                         Usuario oficial,
                                         Classification classification,
                                         String checklistDigest,
                                         String bundleReference,
                                         InstitutionalActorRoutingService.InstitutionalRoute route) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Recepção cartorária do resultado do cumprimento do Oficial com classificação automática e abertura da próxima providência topológica.");
        parts.add("Compartimento: " + classification.bucketLabel() + '.');
        parts.add("Rota institucional: " + firstNonBlank(route.routeAxis(), classification.name()) + '.');
        parts.add("Oficial: " + firstNonBlank(oficial.getNome(), "OFICIAL_NAO_IDENTIFICADO") + '.');
        if (processo.getStatusProcesso() != null) {
            parts.add("Status processual atual: " + processo.getStatusProcesso().name() + '.');
        }
        if (checklistDigest != null) {
            parts.add("Checklist digest: " + checklistDigest + '.');
        }
        if (bundleReference != null) {
            parts.add("Bundle: " + bundleReference + '.');
        }
        return trim(String.join(" ", parts), 1400);
    }

    private String buildProvidenceTitle(Processo processo, Classification classification) {
        return trim(classification.nextProvidenceLabel() + " — " + contextEnvelopeService.processNumber(processo), 220);
    }

    private String buildProvidenceDescription(WorkItem desk, Classification classification, String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Providência aberta automaticamente a partir do resultado classificado do cumprimento do Oficial de Justiça.");
        parts.add("Compartimento de origem: " + classification.bucketLabel() + '.');
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Resumo do retorno: " + trim(desk.getDescricao(), 1100));
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação cartorária: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1900);
    }

    private String buildProvidenceBaseLegal(WorkItem desk,
                                            Classification classification,
                                            String observation,
                                            InstitutionalActorRoutingService.InstitutionalRoute route) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Próxima providência aberta automaticamente pela secretaria/cartório a partir do encerramento soberano do Oficial.");
        parts.add("Classificação: " + classification.bucketLabel() + '.');
        parts.add("Rota institucional: " + firstNonBlank(route.routeAxis(), classification.name()) + '.');
        if (desk.getBaseLegal() != null && !desk.getBaseLegal().isBlank()) {
            parts.add(trim(desk.getBaseLegal(), 700));
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação complementar: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1400);
    }

    private Map<String, Object> nextProvidenceMap(WorkItem nextProvidence, Classification classification) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("workItemId", nextProvidence.getId());
        map.put("classificacao", classification.name());
        map.put("label", classification.nextProvidenceLabel());
        map.put("queueCode", nextProvidence.getQueueCode());
        map.put("inboxKey", nextProvidence.getInboxKey());
        map.put("dueAt", nextProvidence.getDueAt());
        map.put("assignedRole", nextProvidence.getAssignedRole() != null ? nextProvidence.getAssignedRole().name() : null);
        return safeCopy(map);
    }

    private Instant resolveDeskDueAt(Instant prazoOperacional, Instant mandadoDueAt, Classification classification) {
        if (prazoOperacional != null) {
            return prazoOperacional;
        }
        if (mandadoDueAt != null) {
            return mandadoDueAt;
        }
        return switch (classification) {
            case POSITIVO -> timeService.nowUtc().plusSeconds(24 * 3600);
            case PARCIAL -> timeService.nowUtc().plusSeconds(18 * 3600);
            case FRUSTRADO -> timeService.nowUtc().plusSeconds(8 * 3600);
        };
    }

    private Instant resolveNextProvidenceDueAt(Instant deskDueAt, Classification classification) {
        if (deskDueAt != null) {
            return deskDueAt;
        }
        return switch (classification) {
            case POSITIVO -> timeService.nowUtc().plusSeconds(24 * 3600);
            case PARCIAL -> timeService.nowUtc().plusSeconds(12 * 3600);
            case FRUSTRADO -> timeService.nowUtc().plusSeconds(6 * 3600);
        };
    }

    private String rewriteReceiptTitle(String title, Classification override) {
        String base = title == null ? "Retorno do Cumprimento do Oficial" : title.replace("POSITIVE", "");
        return trim(base.replaceFirst("—\\s*[^—]+\\s*—", "— " + override.bucketLabel() + " —"), 220);
    }

    private String rewriteDescriptionClassificacao(String description, Classification override) {
        if (description == null || description.isBlank()) {
            return "Classificação automática: " + override.bucketLabel() + '.';
        }
        return trim(description.replaceFirst("Classificação automática:\\s*[^.]+\\.", "Classificação automática: " + override.bucketLabel() + '.'), 1900);
    }

    private String rewriteBaseLegal(String baseLegal, Classification override, String observation) {
        ArrayList<String> parts = new ArrayList<>();
        if (baseLegal != null && !baseLegal.isBlank()) {
            parts.add(baseLegal.replaceFirst("Compartimento:\\s*[^.]+\\.", "Compartimento: " + override.bucketLabel() + '.'));
        } else {
            parts.add("Compartimento: " + override.bucketLabel() + '.');
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação de reclassificação: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1400);
    }

    private String extractToken(String text, String marker, char terminal) {
        if (text == null || marker == null || marker.isBlank()) {
            return null;
        }
        int start = text.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int contentStart = start + marker.length();
        int end = text.indexOf(terminal, contentStart);
        if (end < 0) {
            end = text.length();
        }
        return trim(text.substring(contentStart, end), 120);
    }

    private String sourceMandadoToken(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return "SEM_MANDADO";
        }
        String[] parts = templateCode.split(":");
        if (parts.length < 3) {
            return "SEM_MANDADO";
        }
        return parts[2];
    }

    private String trim(String text, int max) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 1)).trim() + '…';
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

    private String joinNonBlank(String separator, String... values) {
        ArrayList<String> tokens = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    tokens.add(value.trim());
                }
            }
        }
        return tokens.isEmpty() ? null : String.join(separator == null ? " " : separator, tokens);
    }

    private String stringOf(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private void putIfNotBlank(ArrayList<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (map != null && key != null && !key.isBlank() && value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}
