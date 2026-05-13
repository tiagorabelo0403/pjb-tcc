package com.tcc.pjb.backend.service.juiz.decision;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoDecisaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoRejeicaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizOrdemCumprimentoOficialRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class GabineteOficialRetornoTriagemService {

    private static final String SUGGESTED_TEMPLATE_SUFFIX = ":ORDEM_JUDICIAL_SUGERIDA_AO_GABINETE";
    private static final String SUGGESTED_JSON_MARKER = "PJB_SUGESTAO_ORDEM_JSON[[";
    private static final String DECISION_MARKER = "PJB_GABINETE_DECISAO_OFICIAL[[";
    private static final String MARKER_END = "]]";
    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);

    private final WorkItemRepository workItemRepository;
    private final PerfilDashboardContextFactory contextFactory;
    private final PjbAuthorizationService authorizationService;
    private final JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final PainelServiceCommons commons;
    private final PjbTimeService timeService;
    private final ObjectMapper objectMapper;

    public GabineteOficialRetornoTriagemService(WorkItemRepository workItemRepository,
                                                PerfilDashboardContextFactory contextFactory,
                                                PjbAuthorizationService authorizationService,
                                                JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService,
                                                OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                PainelServiceCommons commons,
                                                PjbTimeService timeService,
                                                ObjectMapper objectMapper) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.juizOficialCumprimentoOrderService = Objects.requireNonNull(juizOficialCumprimentoOrderService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.commons = Objects.requireNonNull(commons);
        this.timeService = Objects.requireNonNull(timeService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sugestaoConsolidada(Long gabineteWorkItemId) {
        WorkItem item = resolveSuggestionItem(gabineteWorkItemId);
        Usuario juiz = requireJudge();
        DecisionState state = decisionState(item);
        Map<String, Object> suggestedPayload = resolveSuggestedPayload(item);
        Optional<WorkItem> latestOfficialWork = workItemRepository.findLatestLinkedOfficialByProcesso(item.getProcessoId(), OFFICIAL_ROLES);
        Usuario official = latestOfficialWork.map(WorkItem::getAssignedUser).orElse(null);
        LinkedHashMap<String, Object> minuta = buildMinutaDecisoria(item, suggestedPayload);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SUGESTAO_GABINETE_OFICIAL_CONSOLIDADA");
        out.put("generatedAt", timeService.nowUtc());
        out.put("gabineteWorkItemId", item.getId());
        out.put("processoId", item.getProcessoId());
        out.put("processoNumero", contextEnvelopeService.processNumber(item.getProcesso()));
        out.put("queueCode", item.getQueueCode());
        out.put("inboxKey", item.getInboxKey());
        out.put("prioridadeJudicial", suggestedPayload.get("prioridade"));
        out.put("state", state.name());
        out.put("stateLabel", state.label());
        out.put("bloqueadoParaNovaDecisao", state.isTerminal());
        out.put("conflitoComOrdemPosterior", hasPosteriorJudicialOrder(item, latestOfficialWork.orElse(null)));
        out.put("oficialResponsavel", official != null ? contextEnvelopeService.oficialEnvelope(official, null) : officialFallback(item, suggestedPayload));
        out.put("sugestaoOrdem", safeCopy(new LinkedHashMap<>(suggestedPayload)));
        out.put("minutaDecisoria", safeCopy(minuta));
        out.put("usuarioMagistrado", judgeMap(juiz));
        LinkedHashMap<String, Object> paths = new LinkedHashMap<>();
        paths.put("aprovarMinuta", OperationalApiRoutes.judgeOfficialReturnApproveMinuta(item.getId()));
        paths.put("aprovarReexpedicao", OperationalApiRoutes.judgeOfficialReturnApproveReexpedicao(item.getId()));
        paths.put("rejeitar", OperationalApiRoutes.judgeOfficialReturnReject(item.getId()));
        paths.put("ordemDiretaJuiz", OperationalApiRoutes.judgeGabineteDecisoes() + "/processos/" + item.getProcessoId() + "/ordem-cumprimento-oficial");
        out.put("paths", safeCopy(paths));
        out.put("auditHash", Hashes.sha256HexPrefix(item.getId() + "|CONSOLIDADA|" + state.name(), 32));
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> aprovarMinuta(Long gabineteWorkItemId, JuizGabineteOficialRetornoDecisaoRequest request) {
        WorkItem item = resolveSuggestionItem(gabineteWorkItemId);
        Usuario juiz = requireJudge();
        guardTerminalState(item, "minuta já decidida no gabinete");
        guardPosteriorJudicialOrder(item);
        Map<String, Object> payload = mergeSuggestedPayload(item, request);
        WorkItem audit = createDecisionAuditItem(item, payload, "MINUTA_APROVADA", buildApprovalTitle(item, false), buildApprovalDescription(item, juiz, payload, false));
        markSuggestionItemDecided(item, "MINUTA_APROVADA", request != null ? request.observacaoResolvida() : null, request == null || request.concluirItemOrigemResolvido(true));
        publishHistory(item, juiz, "MINUTA_DECISORIA_OFICIAL_APROVADA", audit.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "MINUTA_DECISORIA_APROVADA");
        out.put("gabineteWorkItemId", item.getId());
        out.put("processoId", item.getProcessoId());
        out.put("processoNumero", contextEnvelopeService.processNumber(item.getProcesso()));
        out.put("auditWorkItemId", audit.getId());
        out.put("sugestaoAplicada", safeCopy(new LinkedHashMap<>(payload)));
        out.put("auditHash", Hashes.sha256HexPrefix(item.getId() + "|MINUTA_APROVADA|" + audit.getId(), 32));
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> aprovarEReexpedir(Long gabineteWorkItemId, JuizGabineteOficialRetornoDecisaoRequest request) {
        WorkItem item = resolveSuggestionItem(gabineteWorkItemId);
        Usuario juiz = requireJudge();
        guardTerminalState(item, "reexpedição já decidida no gabinete");
        guardPosteriorJudicialOrder(item);
        Map<String, Object> payload = mergeSuggestedPayload(item, request);
        JuizOrdemCumprimentoOficialRequest ordemRequest = toJudgeOrderRequest(payload);
        Map<String, Object> order = juizOficialCumprimentoOrderService.ordenarCumprimento(item.getProcessoId(), ordemRequest);
        WorkItem audit = createDecisionAuditItem(item, payload, "REEXPEDICAO_APROVADA", buildApprovalTitle(item, true), buildApprovalDescription(item, juiz, payload, true));
        markSuggestionItemDecided(item, "REEXPEDICAO_APROVADA", request != null ? request.observacaoResolvida() : null, request == null || request.concluirItemOrigemResolvido(true));
        publishHistory(item, juiz, "REEXPEDICAO_OFICIAL_APROVADA", audit.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REEXPEDICAO_OFICIAL_APROVADA");
        out.put("gabineteWorkItemId", item.getId());
        out.put("processoId", item.getProcessoId());
        out.put("processoNumero", contextEnvelopeService.processNumber(item.getProcesso()));
        out.put("auditWorkItemId", audit.getId());
        out.put("ordemJudicial", order);
        out.put("sugestaoAplicada", safeCopy(new LinkedHashMap<>(payload)));
        out.put("auditHash", Hashes.sha256HexPrefix(item.getId() + "|REEXPEDICAO_APROVADA|" + audit.getId(), 32));
        return safeCopy(out);
    }

    @Transactional
    public Map<String, Object> rejeitarSugestao(Long gabineteWorkItemId, JuizGabineteOficialRetornoRejeicaoRequest request) {
        WorkItem item = resolveSuggestionItem(gabineteWorkItemId);
        Usuario juiz = requireJudge();
        guardTerminalState(item, "sugestão já rejeitada ou decidida");
        String fundamento = request == null
                ? "Sugestão cartorária rejeitada pelo gabinete após reanálise do retorno do Oficial."
                : request.fundamentoRejeicaoResolvido("Sugestão cartorária rejeitada pelo gabinete após reanálise do retorno do Oficial.");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("fundamento", fundamento);
        putIfNotBlank(payload, "observacao", request != null ? request.observacaoResolvida() : null);
        WorkItem audit = createDecisionAuditItem(item, payload, "SUGESTAO_REJEITADA", buildRejectionTitle(item), buildRejectionDescription(item, juiz, payload));
        markSuggestionItemDecided(item, "SUGESTAO_REJEITADA", request != null ? request.observacaoResolvida() : null, request == null || request.concluirItemOrigemResolvido(true));
        publishHistory(item, juiz, "SUGESTAO_OFICIAL_REJEITADA", audit.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SUGESTAO_GABINETE_REJEITADA");
        out.put("gabineteWorkItemId", item.getId());
        out.put("processoId", item.getProcessoId());
        out.put("processoNumero", contextEnvelopeService.processNumber(item.getProcesso()));
        out.put("auditWorkItemId", audit.getId());
        out.put("fundamentoRejeicao", fundamento);
        out.put("auditHash", Hashes.sha256HexPrefix(item.getId() + "|SUGESTAO_REJEITADA|" + audit.getId(), 32));
        return safeCopy(out);
    }

    private WorkItem resolveSuggestionItem(Long gabineteWorkItemId) {
        WorkItem item = workItemRepository.findById(gabineteWorkItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sugestão de retorno do oficial não encontrada no gabinete"));
        if (item.getTemplateCode() == null || !item.getTemplateCode().contains(SUGGESTED_TEMPLATE_SUFFIX)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "work item informado não corresponde à sugestão de ordem ao gabinete");
        }
        if (item.getProcesso() == null || item.getProcessoId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "sugestão de retorno do oficial sem processo vinculado");
        }
        return item;
    }

    private Usuario requireJudge() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario juiz = ctx.usuario();
        authorizationService.requireRole(juiz, "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL", "ROLE_JUIZ_TRABALHISTA", "ROLE_JUIZ_ELEITORAL", "ROLE_JUIZ_MILITAR");
        return juiz;
    }

    private void guardTerminalState(WorkItem item, String message) {
        DecisionState state = decisionState(item);
        if (state.isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void guardPosteriorJudicialOrder(WorkItem item) {
        WorkItem latest = workItemRepository.findLatestLinkedOfficialByProcesso(item.getProcessoId(), OFFICIAL_ROLES).orElse(null);
        if (hasPosteriorJudicialOrder(item, latest)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe ordem judicial posterior para o Oficial neste processo");
        }
    }

    private boolean hasPosteriorJudicialOrder(WorkItem item, WorkItem latestOfficial) {
        if (latestOfficial == null || latestOfficial.getTitulo() == null) {
            return false;
        }
        String title = latestOfficial.getTitulo().trim().toUpperCase(Locale.ROOT);
        Instant officialInstant = latestOfficial.getCreatedAt() != null ? latestOfficial.getCreatedAt() : latestOfficial.getUpdatedAt();
        Instant itemInstant = item.getCreatedAt() != null ? item.getCreatedAt() : item.getUpdatedAt();
        return title.contains("ORDEM JUDICIAL") && officialInstant != null && itemInstant != null && officialInstant.isAfter(itemInstant);
    }

    private Map<String, Object> resolveSuggestedPayload(WorkItem item) {
        String json = extractBetween(firstNonBlank(item.getBaseLegal(), item.getDescricao()), SUGGESTED_JSON_MARKER, MARKER_END);
        if (json != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
                if (!parsed.isEmpty()) {
                    return normalizePayload(parsed, item);
                }
            } catch (Exception ignored) {
            }
        }
        return normalizePayload(fallbackPayload(item), item);
    }

    private Map<String, Object> mergeSuggestedPayload(WorkItem item, JuizGabineteOficialRetornoDecisaoRequest request) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>(resolveSuggestedPayload(item));
        Long fallbackOfficialId = asLong(payload.get("oficialId"));
        String fallbackFundamento = stringOf(payload.get("fundamento"));
        String fallbackConteudo = stringOf(payload.get("conteudoOperacional"));
        String fallbackTipo = stringOf(payload.get("tipoCumprimento"));
        int fallbackPrioridade = asInt(payload.get("prioridade"), 1);
        Instant fallbackDueAt = asInstant(payload.get("dueAt"));
        String fallbackJanela = stringOf(payload.get("janelaTerritorial"));
        String fallbackBairro = stringOf(payload.get("bairroPreferencial"));
        String fallbackMicro = stringOf(payload.get("microterritorio"));
        boolean fallbackCiencia = asBoolean(payload.get("cienciaObrigatoria"), true);
        boolean fallbackOriginal = asBoolean(payload.get("exigirOficioOriginalNoEncerramento"), true);
        if (request != null) {
            payload.put("oficialId", request.oficialIdResolvido(fallbackOfficialId));
            payload.put("fundamento", request.fundamentoResolvido(fallbackFundamento));
            payload.put("conteudoOperacional", request.conteudoOperacionalResolvido(fallbackConteudo));
            payload.put("tipoCumprimento", request.tipoCumprimentoResolvido(fallbackTipo));
            payload.put("prioridade", request.prioridadeResolvida(fallbackPrioridade));
            payload.put("dueAt", request.dueAtResolvido(fallbackDueAt));
            payload.put("janelaTerritorial", request.janelaTerritorialResolvida(fallbackJanela));
            payload.put("bairroPreferencial", request.bairroPreferencialResolvido(fallbackBairro));
            payload.put("microterritorio", request.microterritorioResolvida(fallbackMicro));
            payload.put("cienciaObrigatoria", request.cienciaObrigatoriaResolvida(fallbackCiencia));
            payload.put("exigirOficioOriginalNoEncerramento", request.exigirOficioOriginalNoEncerramentoResolvido(fallbackOriginal));
            putIfNotBlank(payload, "observacao", request.observacaoResolvida());
        }
        return safeCopy(payload);
    }

    private Map<String, Object> normalizePayload(Map<String, Object> parsed, WorkItem item) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        Optional<WorkItem> latestOfficialWork = workItemRepository.findLatestLinkedOfficialByProcesso(item.getProcessoId(), OFFICIAL_ROLES);
        Long defaultOfficialId = latestOfficialWork.map(WorkItem::getAssignedUser).filter(Objects::nonNull).map(Usuario::getId).orElse(null);
        payload.put("oficialId", asLong(parsed.getOrDefault("oficialId", defaultOfficialId)));
        payload.put("fundamento", firstNonBlank(stringOf(parsed.get("fundamento")), "Cumprimento judicial sugerido após retorno classificado do Oficial."));
        payload.put("conteudoOperacional", firstNonBlank(stringOf(parsed.get("conteudoOperacional")), "Reexpedir determinação judicial ao Oficial de Justiça com ajuste operacional."));
        payload.put("tipoCumprimento", firstNonBlank(stringOf(parsed.get("tipoCumprimento")), "CUMPRIMENTO_JUDICIAL"));
        payload.put("prioridade", asInt(parsed.get("prioridade"), 1));
        payload.put("dueAt", asInstant(parsed.get("dueAt")));
        putIfNotBlank(payload, "janelaTerritorial", stringOf(parsed.get("janelaTerritorial")));
        putIfNotBlank(payload, "bairroPreferencial", stringOf(parsed.get("bairroPreferencial")));
        putIfNotBlank(payload, "microterritorio", stringOf(parsed.get("microterritorio")));
        payload.put("cienciaObrigatoria", asBoolean(parsed.get("cienciaObrigatoria"), true));
        payload.put("exigirOficioOriginalNoEncerramento", asBoolean(parsed.get("exigirOficioOriginalNoEncerramento"), true));
        putIfNotBlank(payload, "observacao", stringOf(parsed.get("observacao")));
        return safeCopy(payload);
    }

    private Map<String, Object> fallbackPayload(WorkItem item) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        Optional<WorkItem> latestOfficialWork = workItemRepository.findLatestLinkedOfficialByProcesso(item.getProcessoId(), OFFICIAL_ROLES);
        Usuario official = latestOfficialWork.map(WorkItem::getAssignedUser).orElse(null);
        if (official != null) {
            payload.put("oficialId", official.getId());
        }
        payload.put("fundamento", firstNonBlank(extractToken(item.getDescricao(), "Fundamento sugerido: ", '.'), "Cumprimento judicial sugerido após retorno classificado do Oficial."));
        payload.put("conteudoOperacional", firstNonBlank(extractToken(item.getDescricao(), "Conteúdo operacional sugerido: ", '.'), "Reexpedir determinação judicial ao Oficial de Justiça com ajuste operacional."));
        payload.put("tipoCumprimento", "CUMPRIMENTO_JUDICIAL");
        payload.put("prioridade", asInt(extractToken(item.getDescricao(), "Prioridade sugerida: ", '.'), 1));
        putIfNotBlank(payload, "janelaTerritorial", extractToken(item.getDescricao(), "Janela territorial sugerida: ", '.'));
        payload.put("cienciaObrigatoria", Boolean.TRUE);
        payload.put("exigirOficioOriginalNoEncerramento", Boolean.TRUE);
        return payload;
    }

    private LinkedHashMap<String, Object> buildMinutaDecisoria(WorkItem item, Map<String, Object> payload) {
        LinkedHashMap<String, Object> minuta = new LinkedHashMap<>();
        String processoNumero = contextEnvelopeService.processNumber(item.getProcesso());
        String fundamento = stringOf(payload.get("fundamento"));
        String conteudo = stringOf(payload.get("conteudoOperacional"));
        String janela = stringOf(payload.get("janelaTerritorial"));
        int prioridade = asInt(payload.get("prioridade"), 1);
        minuta.put("ementa", "Retorno classificado do Oficial de Justiça com necessidade de deliberação judicial no processo " + processoNumero + '.');
        minuta.put("fundamento", fundamento);
        minuta.put("dispositivo", "Aprovo a triagem do retorno do Oficial e determino a adoção da providência judicial compatível com o cenário classificado.");
        minuta.put("determinacaoOperacional", conteudo);
        minuta.put("janelaTerritorial", janela);
        minuta.put("prioridadeJudicial", prioridade);
        minuta.put("filaGabinete", item.getQueueCode());
        minuta.put("inboxGabinete", item.getInboxKey());
        minuta.put("travaOficioOriginal", asBoolean(payload.get("exigirOficioOriginalNoEncerramento"), true));
        minuta.put("cienciaObrigatoria", asBoolean(payload.get("cienciaObrigatoria"), true));
        return minuta;
    }

    private Map<String, Object> officialFallback(WorkItem item, Map<String, Object> payload) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        Long officialId = asLong(payload.get("oficialId"));
        if (officialId != null) {
            out.put("usuarioId", officialId);
        }
        putIfNotBlank(out, "nome", extractToken(item.getDescricao(), "Oficial sugerido: ", '.'));
        return safeCopy(out);
    }

    private Map<String, Object> judgeMap(Usuario juiz) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("usuarioId", juiz.getId());
        out.put("nome", firstNonBlank(juiz.getNome(), "MAGISTRADO"));
        out.put("perfil", juiz.getTipoUsuario() != null ? juiz.getTipoUsuario().name() : "JUIZ");
        return safeCopy(out);
    }

    private JuizOrdemCumprimentoOficialRequest toJudgeOrderRequest(Map<String, Object> payload) {
        return new JuizOrdemCumprimentoOficialRequest(
                asLong(payload.get("oficialId")),
                stringOf(payload.get("fundamento")),
                stringOf(payload.get("conteudoOperacional")),
                stringOf(payload.get("tipoCumprimento")),
                asInt(payload.get("prioridade"), 1),
                asInstant(payload.get("dueAt")),
                stringOf(payload.get("janelaTerritorial")),
                stringOf(payload.get("bairroPreferencial")),
                stringOf(payload.get("microterritorio")),
                asBoolean(payload.get("cienciaObrigatoria"), true),
                asBoolean(payload.get("exigirOficioOriginalNoEncerramento"), true),
                stringOf(payload.get("observacao"))
        );
    }

    private WorkItem createDecisionAuditItem(WorkItem source,
                                             Map<String, Object> payload,
                                             String decisionAxis,
                                             String title,
                                             String description) {
        String templateCode = "GABINETE:RETORNO_OFICIAL_DECISAO:" + source.getId() + ':' + decisionAxis;
        WorkItem audit = workItemRepository.findLatestByProcessoIdAndTemplateCode(source.getProcessoId(), templateCode)
                .orElseGet(() -> WorkItem.builder().processo(source.getProcesso()).templateCode(templateCode).build());
        audit.setFaseOrigem(source.getProcesso().getFaseAtual());
        audit.setType(WorkItemType.DECISAO);
        audit.setTitulo(title);
        audit.setDescricao(description);
        audit.setQueueCode(source.getQueueCode());
        audit.setInboxKey(source.getInboxKey());
        audit.setAssignedRole(source.getAssignedRole());
        audit.setAssignedUser(null);
        audit.setStatus(WorkItemStatus.CONCLUIDO);
        audit.setPrioridade(asInt(payload.get("prioridade"), 1));
        audit.setBlocking(false);
        audit.setDueAt(timeService.nowUtc());
        audit.setUf(firstNonBlank(source.getUf(), source.getProcesso().getUf()));
        audit.setComarca(firstNonBlank(source.getComarca(), source.getProcesso().getComarca()));
        audit.setBaseLegal(buildDecisionAuditBaseLegal(payload, decisionAxis));
        audit.setSemInteresse(false);
        return workItemRepository.save(audit);
    }

    private void markSuggestionItemDecided(WorkItem item, String decisionAxis, String observation, boolean concludeItem) {
        String decisionPayload = DECISION_MARKER + decisionAxis + MARKER_END;
        item.setBaseLegal(appendSection(item.getBaseLegal(), decisionPayload));
        item.setDescricao(appendSection(item.getDescricao(), buildDecisionSummary(decisionAxis, observation)));
        item.setBlocking(false);
        if (concludeItem) {
            item.setStatus(WorkItemStatus.CONCLUIDO);
        }
        workItemRepository.save(item);
    }

    private String buildDecisionAuditBaseLegal(Map<String, Object> payload, String decisionAxis) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Decisão do gabinete sobre retorno classificado do Oficial de Justiça.");
        parts.add(DECISION_MARKER + decisionAxis + MARKER_END);
        try {
            parts.add(SUGGESTED_JSON_MARKER + objectMapper.writeValueAsString(payload) + MARKER_END);
        } catch (Exception ex) {
            parts.add("Payload decisório indisponível.");
        }
        return trim(String.join(" ", parts), 3900);
    }

    private String buildDecisionSummary(String decisionAxis, String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Decisão do gabinete aplicada ao retorno do Oficial: " + decisionAxis + '.');
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação: " + observation + '.');
        }
        return trim(String.join(" ", parts), 1900);
    }

    private String buildApprovalTitle(WorkItem item, boolean reexpedicao) {
        return trim((reexpedicao ? "Reexpedição aprovada" : "Minuta aprovada") + " — retorno do Oficial — " + contextEnvelopeService.processNumber(item.getProcesso()), 220);
    }

    private String buildApprovalDescription(WorkItem item, Usuario juiz, Map<String, Object> payload, boolean reexpedicao) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add(reexpedicao
                ? "O gabinete aprovou a reexpedição judicial ao Oficial de Justiça."
                : "O gabinete aprovou a minuta decisória referente ao retorno do Oficial de Justiça.");
        parts.add("Magistrado: " + firstNonBlank(juiz.getNome(), "MAGISTRADO") + '.');
        parts.add("Processo: " + contextEnvelopeService.processNumber(item.getProcesso()) + '.');
        parts.add("Fundamento: " + firstNonBlank(stringOf(payload.get("fundamento")), "Fundamento não informado") + '.');
        parts.add("Determinação operacional: " + firstNonBlank(stringOf(payload.get("conteudoOperacional")), "Sem determinação operacional") + '.');
        if (payload.get("janelaTerritorial") != null) {
            parts.add("Janela territorial: " + payload.get("janelaTerritorial") + '.');
        }
        parts.add("Prioridade judicial: " + asInt(payload.get("prioridade"), 1) + '.');
        return trim(String.join(" ", parts), 2600);
    }

    private String buildRejectionTitle(WorkItem item) {
        return trim("Sugestão rejeitada — retorno do Oficial — " + contextEnvelopeService.processNumber(item.getProcesso()), 220);
    }

    private String buildRejectionDescription(WorkItem item, Usuario juiz, Map<String, Object> payload) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("O gabinete rejeitou a sugestão cartorária derivada do retorno do Oficial de Justiça.");
        parts.add("Magistrado: " + firstNonBlank(juiz.getNome(), "MAGISTRADO") + '.');
        parts.add("Processo: " + contextEnvelopeService.processNumber(item.getProcesso()) + '.');
        parts.add("Fundamento da rejeição: " + firstNonBlank(stringOf(payload.get("fundamento")), "Fundamento não informado") + '.');
        if (payload.get("observacao") != null) {
            parts.add("Observação: " + payload.get("observacao") + '.');
        }
        return trim(String.join(" ", parts), 2200);
    }

    private void publishHistory(WorkItem item, Usuario juiz, String event, Long auditWorkItemId) {
        Processo processo = item.getProcesso();
        String message = event + " no processo " + contextEnvelopeService.processNumber(processo) + " com work item de auditoria " + auditWorkItemId + '.';
        commons.publishUserHistory(juiz, "JUIZ", event, message, processo, auditWorkItemId);
    }

    private DecisionState decisionState(WorkItem item) {
        String marker = extractBetween(firstNonBlank(item.getBaseLegal(), item.getDescricao()), DECISION_MARKER, MARKER_END);
        return DecisionState.fromMarker(marker);
    }

    private String appendSection(String base, String append) {
        if (append == null || append.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return append;
        }
        return trim(base + ' ' + append, 4000);
    }

    private String extractBetween(String source, String startMarker, String endMarker) {
        if (source == null || source.isBlank()) {
            return null;
        }
        int start = source.indexOf(startMarker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + startMarker.length();
        int end = source.indexOf(endMarker, valueStart);
        if (end < 0) {
            return null;
        }
        String candidate = source.substring(valueStart, end).trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private String extractToken(String source, String prefix, char endChar) {
        if (source == null || source.isBlank() || prefix == null || prefix.isBlank()) {
            return null;
        }
        int start = source.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int valueStart = start + prefix.length();
        int end = source.indexOf(endChar, valueStart);
        if (end < 0) {
            end = source.length();
        }
        String candidate = source.substring(valueStart, end).trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String normalized = value.trim();
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private int asInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return Math.max(0, Math.min(number.intValue(), 5));
        }
        try {
            return Math.max(0, Math.min(Integer.parseInt(String.valueOf(value).trim()), 5));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return switch (String.valueOf(value).trim().toUpperCase(Locale.ROOT)) {
            case "TRUE", "SIM", "1" -> true;
            case "FALSE", "NAO", "NÃO", "0" -> false;
            default -> fallback;
        };
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        try {
            return Instant.parse(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (target != null && key != null && value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 1)).trim() + '…';
    }

    private Map<String, Object> safeCopy(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    return;
                }
                if (value instanceof Map<?, ?> nested) {
                    LinkedHashMap<String, Object> nestedCopy = new LinkedHashMap<>();
                    nested.forEach((nestedKey, nestedValue) -> {
                        if (nestedKey != null && nestedValue != null) {
                            nestedCopy.put(String.valueOf(nestedKey), nestedValue);
                        }
                    });
                    out.put(key, nestedCopy.isEmpty() ? Map.of() : Map.copyOf(nestedCopy));
                } else if (value instanceof List<?> list) {
                    out.put(key, List.copyOf(list));
                } else {
                    out.put(key, value);
                }
            });
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private enum DecisionState {
        ABERTA("Sugestão aberta no gabinete", false),
        MINUTA_APROVADA("Minuta aprovada", true),
        REEXPEDICAO_APROVADA("Reexpedição aprovada", true),
        SUGESTAO_REJEITADA("Sugestão rejeitada", true);

        private final String label;
        private final boolean terminal;

        DecisionState(String label, boolean terminal) {
            this.label = label;
            this.terminal = terminal;
        }

        static DecisionState fromMarker(String marker) {
            if (marker == null || marker.isBlank()) {
                return ABERTA;
            }
            for (DecisionState state : values()) {
                if (state.name().equalsIgnoreCase(marker.trim())) {
                    return state;
                }
            }
            return ABERTA;
        }

        public String label() {
            return label;
        }

        public boolean isTerminal() {
            return terminal;
        }
    }
}
