package com.tcc.pjb.backend.service.servidor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.ForumOfficialReturnReactivationRequest;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processo.ProcessoSlaJudicialService;
import com.tcc.pjb.backend.service.forum.ForumOfficialReturnOperationalService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
@Service
public class ServidorSecretariaOperacionalService {
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final ProcessoLifecycleMachine lifecycleMachine;
private final ProcessoSlaJudicialService processoSlaJudicialService;
private final InstitutionalActorRoutingService institutionalActorRoutingService;
private final ForumOfficialReturnOperationalService forumOfficialReturnOperationalService;
private final CalendarInstitutionalBridgeService institutionalBridgeService;
private final OfficialDocumentTemplateService officialDocumentTemplateService;
private final PainelSharedExperienceService sharedExperienceService;
private final PainelSignalReflectionService signalReflectionService;
private final PainelNativeCollectionCompositionService collectionCompositionService;
private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
public ServidorSecretariaOperacionalService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService,
ProcessoLifecycleMachine lifecycleMachine,
ProcessoSlaJudicialService processoSlaJudicialService,
InstitutionalActorRoutingService institutionalActorRoutingService,
ForumOfficialReturnOperationalService forumOfficialReturnOperationalService,
CalendarInstitutionalBridgeService institutionalBridgeService,
OfficialDocumentTemplateService officialDocumentTemplateService,
PainelSharedExperienceService sharedExperienceService,
PainelSignalReflectionService signalReflectionService,
PainelNativeCollectionCompositionService collectionCompositionService,
PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                       PainelExecutionSurfaceCompositionService executionSurfaceCompositionService) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
this.lifecycleMachine = lifecycleMachine;
this.processoSlaJudicialService = processoSlaJudicialService;
this.institutionalActorRoutingService = institutionalActorRoutingService;
this.forumOfficialReturnOperationalService = forumOfficialReturnOperationalService;
this.institutionalBridgeService = institutionalBridgeService;
this.officialDocumentTemplateService = officialDocumentTemplateService;
this.sharedExperienceService = sharedExperienceService;
this.signalReflectionService = signalReflectionService;
this.collectionCompositionService = collectionCompositionService;
this.actionSurfaceCompositionService = actionSurfaceCompositionService;
this.executionSurfaceCompositionService = executionSurfaceCompositionService;
}
public SecretariaSnapshot bootstrapSecretaria() {
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
List<WorkItem> inbox = commons.inboxHibrido(usuario, 100);
List<String> juntadasPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "JUNTADA", "PROTOCOLO", "PETICAO_JUNTADA"))
.limit(30).map(commons::resumo).toList();
List<String> intimacoesExpedir = inbox.stream()
.filter(i -> commons.titleContains(i, "INTIMAR", "EXPEDIR_INTIMACAO",
"PUBLICACAO_DJ"))
.limit(20).map(commons::resumo).toList();
List<String> mandadosExpedir = inbox.stream()
.filter(i -> commons.titleContains(i, "MANDADO", "EXPEDIR_MANDADO"))
.limit(20).map(commons::resumo).toList();
List<String> conclusosPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "CONCLUSO", "CONCLUSAO_PARA_DESPACHO"))
.limit(20).map(commons::resumo).toList();
long totalFila = processoRepository.findByComarcaAndUf(
usuario.getComarca(), usuario.getUf(), PageRequest.of(0, 1)).getTotalElements();
int prazosVencendo24h = (int) inbox.stream()
.filter(i -> i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))).count();
Map<String, Object> sharedExperience = sharedExperienceService.snapshot("SECRETARIA");
Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("SECRETARIA", sharedExperience, inbox.size(), prazosVencendo24h, "COORDENACAO_CARTORARIA");
Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("SECRETARIA", operationalSignals);
juntadasPendentes = collectionCompositionService.composeList("SECRETARIA", "JUNTADAS_PENDENTES", juntadasPendentes, operationalSignals, nativeComposition);
intimacoesExpedir = collectionCompositionService.composeList("SECRETARIA", "INTIMACOES_EXPEDIR", intimacoesExpedir, operationalSignals, nativeComposition);
mandadosExpedir = collectionCompositionService.composeList("SECRETARIA", "MANDADOS_EXPEDIR", mandadosExpedir, operationalSignals, nativeComposition);
conclusosPendentes = collectionCompositionService.composeList("SECRETARIA", "CONCLUSOS_PENDENTES", conclusosPendentes, operationalSignals, nativeComposition);
Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("SECRETARIA", operationalSignals, nativeComposition, Map.of(
"juntadasPendentes", juntadasPendentes,
"intimacoesExpedir", intimacoesExpedir,
"mandadosExpedir", mandadosExpedir,
"conclusosPendentes", conclusosPendentes
));
Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("SECRETARIA", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("SECRETARIA", operationalSignals, nativeComposition, collectionComposition, actionSurface);
CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridgeForUser(usuario, java.time.LocalDate.now(java.time.ZoneOffset.UTC), java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(14), null);
var institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
return new SecretariaSnapshot(
ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(),
resolveVara(usuario), totalFila, juntadasPendentes,
intimacoesExpedir, mandadosExpedir, conclusosPendentes,
prazosVencendo24h, ctx.prazoRadar(), ctx.sessionRisk(), institutionalFocus, institutionalBridge, operationalSignals, nativeComposition, collectionComposition, actionSurface, executionSurface, sharedExperience
);
}

@Transactional
public Map<String, Object> realizarJuntada(Long processoId, String tipoDocumento,
String descricao, String origem) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
String dedupKey = UUID.nameUUIDFromBytes(
("JUNTADA:" + tipoDocumento + ":" + processoId + ":" + Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute juntadaRoute = institutionalActorRoutingService.gabineteReview(processoId, "CIENCIA_JUNTADA");
WorkItem juntada = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.JUNTADA)
.titulo("Juntada de " + tipoDocumento + " — " + processo.getNumeroProcesso())
.descricao(descricao + " | Origem: " + origem)
.queueCode(juntadaRoute.queueCode())
.inboxKey(juntadaRoute.inboxKey())
.assignedRole(juntadaRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(2)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(1, ChronoUnit.HOURS))
.build();
workItemRepository.save(juntada);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.REALIZAR_JUNTADA);
processoRepository.save(processo);
commons.publishUserHistory(usuario, "SERVIDOR", "JUNTADA_REALIZADA",
tipoDocumento + " juntado aos autos.", processo, processoId);
LinkedHashMap<String, Object> response = new LinkedHashMap<>();
response.put("status", "JUNTADA_REALIZADA");
response.put("tipo", safeText(tipoDocumento));
response.put("processoId", processoId);
response.put("workItemId", juntada.getId());
response.put("dedupKey", dedupKey);
return Map.copyOf(response);
}
@Transactional
public Map<String, Object> expedicaoIntimacao(Long processoId, String destinatario,
String conteudo, String prazo, Long oficialId, Boolean reativarOficial,
String origemOperacional, String fundamentoOperacional,
String observacaoOperacional, Boolean manterRetornoForumAberto) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
String normalizedDestinatario = destinatario == null ? "DESTINATARIO" : destinatario.trim().toUpperCase().replace(' ', '_');
String templateCode = UUID.nameUUIDFromBytes(("INTIMACAO:" + processoId + ':' + normalizedDestinatario + ':' + (prazo == null ? "PADRAO" : prazo.trim().toUpperCase()) + ':' + (conteudo == null ? "" : conteudo.trim())).getBytes(StandardCharsets.UTF_8)).toString();
var sla = processoSlaJudicialService.snapshot(processo);
Instant dueAt = prazo == null || prazo.isBlank() ? sla.dueAtInitialCommunication() : resolverPrazo(prazo);
WorkItem intimacao = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode, WorkItemStatus.CANCELADO)
.orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
intimacao.setFaseOrigem(processo.getFaseAtual());
intimacao.setType(WorkItemType.INTIMACAO);
intimacao.setTitulo("Intimação — " + destinatario + " — " + processo.getNumeroProcesso());
intimacao.setDescricao(conteudo);
intimacao.setQueueCode("SECRETARIA_INTIMACOES");
intimacao.setInboxKey("PORTAL_INTIMACAO_" + normalizedDestinatario);
intimacao.setAssignedRole(TipoUsuario.ADVOGADO);
intimacao.setStatus(WorkItemStatus.PENDENTE);
intimacao.setPrioridade(1);
intimacao.setUf(usuario.getUf());
intimacao.setComarca(usuario.getComarca());
intimacao.setDueAt(dueAt);
workItemRepository.save(intimacao);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.EXPEDIR_INTIMACAO);
processoRepository.save(processo);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "INTIMACAO_EXPEDIDA");
out.put("destinatario", destinatario);
out.put("prazo", prazo);
out.put("dueAt", dueAt);
out.put("workItemId", intimacao.getId());
out.put("slaCitacaoDiasUteis", sla.prazoCitacaoDiasUteis());
boolean officialTarget = shouldTriggerOfficialReactivation(destinatario, oficialId, reativarOficial);
out.put("destinadaAoOficial", officialTarget);
OfficialDocumentTemplateRenderResponse intimacaoFormal = renderFormalIntimation(processoId, processo, destinatario, conteudo, prazo);
out.put("documentoFormalAssinado", summarizeRenderedDocument(intimacaoFormal));
if (officialTarget) {
ForumOfficialReturnReactivationRequest reactivationRequest = new ForumOfficialReturnReactivationRequest(
        oficialId,
        normalizeOrigin(origemOperacional),
        normalizeText(fundamentoOperacional),
        normalizeText(observacaoOperacional),
        dueAt,
        manterRetornoForumAberto
);
boolean strictOfficialMode = Boolean.TRUE.equals(reativarOficial) || oficialId != null;
try {
Map<String, Object> automatic = forumOfficialReturnOperationalService.reativarPorExpedicaoAutomatica(processo, reactivationRequest, destinatario, conteudo);
out.put("reativacaoOficial", automatic);
out.put("processoReapareceNoPainelDoOficial", Boolean.TRUE);
out.put("botaoCientePath", "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao");
} catch (RuntimeException ex) {
out.put("reativacaoOficialFalhou", ex.getMessage());
out.put("processoReapareceNoPainelDoOficial", Boolean.FALSE);
if (strictOfficialMode) {
throw ex;
}
}
}
return out;
}

private OfficialDocumentTemplateRenderResponse renderFormalIntimation(Long processoId, Processo processo, String destinatario, String conteudo, String prazo) {
return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
processoId,
TemplateDocumentoOficial.INTIMACAO_FORMAL,
"Intimação formal — " + processo.getNumeroProcesso(),
Map.of(
"destinatario", normalizeText(destinatario) == null ? "DESTINATARIO_NAO_INFORMADO" : normalizeText(destinatario),
"conteudoIntimacao", normalizeText(conteudo) == null ? "CONTEUDO_NAO_INFORMADO" : normalizeText(conteudo),
"prazoResposta", normalizeText(prazo) == null ? "PRAZO_CONFORME_EXPEDIENTE" : normalizeText(prazo)
),
Boolean.TRUE,
Boolean.TRUE
));
}

private Map<String, Object> summarizeRenderedDocument(OfficialDocumentTemplateRenderResponse render) {
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("documentoId", render.documentoId());
out.put("template", render.template().name());
out.put("tituloDocumento", render.tituloDocumento());
out.put("hashSha256", render.hashSha256());
out.put("assinaturaQualificada", render.assinaturaQualificada());
out.put("validacaoSoberana", render.validacaoSoberana());
out.put("selado", render.selado());
return Collections.unmodifiableMap(out);
}

private boolean shouldTriggerOfficialReactivation(String destinatario, Long oficialId, Boolean reativarOficial) {
if (Boolean.TRUE.equals(reativarOficial) || oficialId != null) {
return true;
}
if (destinatario == null || destinatario.isBlank()) {
return false;
}
String normalized = destinatario.trim().toUpperCase();
return normalized.contains("OFICIAL") || normalized.contains("MANDADO") || normalized.contains("CUMPRIMENTO");
}

private String normalizeOrigin(String origemOperacional) {
if (origemOperacional == null || origemOperacional.isBlank()) {
return "SECRETARIA";
}
return origemOperacional.trim().toUpperCase();
}

private String normalizeText(String value) {
if (value == null) {
return null;
}
String normalized = value.trim();
return normalized.isEmpty() ? null : normalized;
}

@Transactional
public Map<String, Object> expedirMandadoCitacao(Long processoId, Long oficialId, String enderecoCitacao, String observacaoOperacional) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario servidor = ctx.usuario();
authorizationService.requireRole(servidor, "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
Instant dueAt = Instant.now().plus(15, ChronoUnit.DAYS);
ForumOfficialReturnReactivationRequest reactivationRequest = new ForumOfficialReturnReactivationRequest(
        oficialId,
        "SECRETARIA_MANDADO_CITACAO",
        "Mandado de citação expedido pela secretaria após admissão da petição inicial.",
        normalizeText(observacaoOperacional),
        dueAt,
        Boolean.FALSE
);
Map<String, Object> automatic = forumOfficialReturnOperationalService.reativarPorExpedicaoAutomatica(
        processo, reactivationRequest, "OFICIAL DE JUSTIÇA", enderecoCitacao);
WorkItem officialItem = resolveOfficialItem(automatic);
officialItem.setType(WorkItemType.DILIGENCIA);
officialItem.setTitulo("Mandado de Citação — " + processo.getNumeroProcesso());
officialItem.setDescricao("Cumprir citação da parte ré no endereço informado: " + enderecoCitacao
        + (observacaoOperacional == null || observacaoOperacional.isBlank() ? "" : " Observação: " + observacaoOperacional));
officialItem.setDueAt(dueAt);
officialItem.setPrioridade(1);
officialItem = workItemRepository.save(officialItem);
OfficialDocumentTemplateRenderResponse mandadoFormal = officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
        processoId,
        TemplateDocumentoOficial.MANDADO,
        "Mandado de citação — " + processo.getNumeroProcesso(),
        Map.of(
                "qualificacaoPartes", firstNonBlank(processo.getParteReuNome(), "PARTE_RE_NAO_IDENTIFICADA") + " — " + enderecoCitacao,
                "ordemJudicial", "Cite-se a parte ré para responder no prazo legal, nos termos da admissão da petição inicial.",
                "prazoCumprimento", String.valueOf(dueAt)
        ),
        Boolean.TRUE,
        Boolean.TRUE
));
lifecycleMachine.apply(processo, ProcessoLifecycleAction.EXPEDIR_INTIMACAO);
processoRepository.save(processo);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "MANDADO_CITACAO_EXPEDIDO");
out.put("processoId", processoId);
out.put("workItemId", officialItem.getId());
out.put("dueAt", dueAt);
out.put("enderecoCitacao", enderecoCitacao);
out.put("mandadoFormalAssinado", summarizeRenderedDocument(mandadoFormal));
out.put("reativacaoOficial", automatic.get("reativacao"));
out.put("processoReapareceNoPainelDoOficial", Boolean.TRUE);
return out;
}

private WorkItem resolveOfficialItem(Map<String, Object> automatic) {
Object nested = automatic == null ? null : automatic.get("reativacao");
Object workItemId = nested instanceof Map<?, ?> map ? map.get("workItemId") : null;
Long id = asLong(workItemId);
if (id == null) {
throw new RecursoNaoEncontradoException("Não foi possível resolver o WorkItem do oficial após a expedição do mandado.");
}
return workItemRepository.findById(id)
.orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem do oficial", id));
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
} catch (NumberFormatException ex) {
return null;
}
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

@Transactional
public Map<String, Object> conclusaoParaDespacho(Long processoId, String motivoConclusa) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
String templateCode = "CONCLUSAO:DESPACHO_INICIAL:" + processoId;
WorkItem concluso = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode, WorkItemStatus.CANCELADO)
.orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
var sla = processoSlaJudicialService.snapshot(processo);
InstitutionalActorRoutingService.InstitutionalRoute conclusoRoute = institutionalActorRoutingService.gabineteDecision(processoId, "DESPACHO_INICIAL");
concluso.setFaseOrigem(processo.getFaseAtual());
concluso.setType(WorkItemType.DESPACHO);
concluso.setTitulo("Concluso para Despacho — " + processo.getNumeroProcesso());
concluso.setDescricao("Motivo: " + motivoConclusa);
concluso.setQueueCode(conclusoRoute.queueCode());
concluso.setInboxKey(conclusoRoute.inboxKey());
concluso.setAssignedRole(conclusoRoute.assignedRole());
concluso.setStatus(WorkItemStatus.PENDENTE);
concluso.setPrioridade(1);
concluso.setUf(usuario.getUf());
concluso.setComarca(usuario.getComarca());
concluso.setDueAt(sla.dueAtInitialConclusion());
workItemRepository.save(concluso);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.CONCLUIR_PARA_DESPACHO);
processoRepository.save(processo);
LinkedHashMap<String, Object> response = new LinkedHashMap<>();
response.put("status", "CONCLUSO_PARA_DESPACHO");
response.put("processoId", processoId);
response.put("workItemId", concluso.getId());
response.put("slaDespachoDiasUteis", sla.prazoDespachoInicialDiasUteis());
return Map.copyOf(response);
}
    @PjbTransactionalBudget(operation = "servidor.secretaria.saneamento-bulk-fila", maxMillis = 5000)
@Transactional
public Map<String, Object> saneamentoBulkFila(String queueCode, int limite) {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
List<WorkItem> vencidos = workItemRepository
.findByQueueCodeAndStatusAndDueAtBefore(
queueCode, WorkItemStatus.PENDENTE, Instant.now(),
PageRequest.of(0, Math.min(limite, 200))).getContent();
vencidos.forEach(item -> item.setStatus(WorkItemStatus.EXPIRADO));
workItemRepository.saveAll(vencidos);
LinkedHashMap<String, Object> response = new LinkedHashMap<>();
response.put("status", "SANEAMENTO_CONCLUIDO");
response.put("fila", safeText(queueCode));
response.put("itensProcessados", vencidos.size());
return Map.copyOf(response);
}
private String resolveVara(Usuario usuario) {
return "VARA_" + (usuario.getComarca() == null ? "CENTRAL" : usuario.getComarca().toUpperCase());
}

private String safeText(String value) {
if (value == null || value.isBlank()) {
return "N/A";
}
return value.trim();
}
private Instant resolverPrazo(String prazo) {
return switch (prazo == null ? "15D" : prazo.toUpperCase()) {
case "5D" -> Instant.now().plus(5, ChronoUnit.DAYS);
case "10D" -> Instant.now().plus(10, ChronoUnit.DAYS);
case "15D" -> Instant.now().plus(15, ChronoUnit.DAYS);
case "30D" -> Instant.now().plus(30, ChronoUnit.DAYS);
default -> Instant.now().plus(15, ChronoUnit.DAYS);
};
}
}
