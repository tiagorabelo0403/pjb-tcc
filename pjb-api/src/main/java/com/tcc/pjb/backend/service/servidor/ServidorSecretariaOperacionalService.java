package com.tcc.pjb.backend.service.servidor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
@Service
public class ServidorSecretariaOperacionalService {
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final CalendarInstitutionalBridgeService institutionalBridgeService;
private final ServidorSecretariaAtosService atosService;
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
CalendarInstitutionalBridgeService institutionalBridgeService,
ServidorSecretariaAtosService atosService,
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
this.institutionalBridgeService = institutionalBridgeService;
this.atosService = atosService;
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

public Map<String, Object> realizarJuntada(Long processoId, String tipoDocumento,
String descricao, String origem) {
return atosService.realizarJuntada(processoId, tipoDocumento, descricao, origem);
}

public Map<String, Object> expedicaoIntimacao(Long processoId, String destinatario,
String conteudo, String prazo, Long oficialId, Boolean reativarOficial,
String origemOperacional, String fundamentoOperacional,
String observacaoOperacional, Boolean manterRetornoForumAberto) {
return atosService.expedicaoIntimacao(processoId, destinatario, conteudo, prazo, oficialId,
reativarOficial, origemOperacional, fundamentoOperacional, observacaoOperacional, manterRetornoForumAberto);
}

public Map<String, Object> expedirMandadoCitacao(Long processoId, Long oficialId, String enderecoCitacao, String observacaoOperacional) {
return atosService.expedirMandadoCitacao(processoId, oficialId, enderecoCitacao, observacaoOperacional);
}

public Map<String, Object> conclusaoParaDespacho(Long processoId, String motivoConclusa) {
return atosService.conclusaoParaDespacho(processoId, motivoConclusa);
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
}
