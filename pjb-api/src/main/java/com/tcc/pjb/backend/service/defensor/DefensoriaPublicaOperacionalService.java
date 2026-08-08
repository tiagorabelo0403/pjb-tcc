package com.tcc.pjb.backend.service.defensor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
@Service
public class DefensoriaPublicaOperacionalService {
private static final EnumSet<TipoUsuario> DEFENSORIA = EnumSet.of(
TipoUsuario.DEFENSOR_PUBLICO, TipoUsuario.DEFENSOR_PUBLICO_FEDERAL
);
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
private final InstitutionalActorRoutingService institutionalActorRoutingService;
private final PainelSharedExperienceService sharedExperienceService;
private final PainelSignalReflectionService signalReflectionService;
private final PainelNativeCollectionCompositionService collectionCompositionService;
private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
private final MovimentacaoProcessualRegistrar movimentacaoRegistrar;
public DefensoriaPublicaOperacionalService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService,
InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
InstitutionalActorRoutingService institutionalActorRoutingService,
PainelSharedExperienceService sharedExperienceService,
PainelSignalReflectionService signalReflectionService,
PainelNativeCollectionCompositionService collectionCompositionService,
PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                       PainelExecutionSurfaceCompositionService executionSurfaceCompositionService,
                                       MovimentacaoProcessualRegistrar movimentacaoRegistrar) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
this.institutionalActorRoutingService = institutionalActorRoutingService;
this.sharedExperienceService = sharedExperienceService;
this.signalReflectionService = signalReflectionService;
this.collectionCompositionService = collectionCompositionService;
this.actionSurfaceCompositionService = actionSurfaceCompositionService;
this.executionSurfaceCompositionService = executionSurfaceCompositionService;
this.movimentacaoRegistrar = movimentacaoRegistrar;
}
public DefensoriaSnapshot bootstrapPainel() {
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
List<WorkItem> inbox = commons.inboxHibrido(usuario, 60);
List<String> assistidosPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "ASSISTIDO", "HIPOSSUFICIENTE",
"NECESSITADO", "GRATUIDADE"))
.limit(20).map(commons::resumo).toList();
List<String> peticoesPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "PETICAO", "DEFESA", "CONTRARRAZOES",
"RESPOSTA", "ALEGACOES"))
.limit(20).map(commons::resumo).toList();

List<String> audienciasPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "AUDIENCIA", "INSTRUCAO", "JULGAMENTO"))
.limit(15).map(commons::resumo).toList();
List<String> recursosUrgentes = inbox.stream()
.filter(i -> commons.titleContains(i, "RECURSO", "HABEAS", "MANDADO_SEGURANCA")
&& i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS)))
.limit(10).map(commons::resumo).toList();
int prazosVencendo24h = (int) inbox.stream()
.filter(i -> i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS)))
.count();
int presos = (int) inbox.stream()
.filter(i -> commons.titleContains(i, "PRESO", "DETENTO", "CUSTODIADO",
"FLAGRANTE", "PRISAO"))
.count();
Map<String, Object> sharedExperience = sharedExperienceService.snapshot("DEFENSOR_PUBLICO");
Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("DEFENSOR_PUBLICO", sharedExperience, peticoesPendentes.size() + recursosUrgentes.size(), prazosVencendo24h, "DEFENSORIA_OPERACIONAL");
Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("DEFENSOR_PUBLICO", operationalSignals);
assistidosPendentes = collectionCompositionService.composeList("DEFENSOR_PUBLICO", "ASSISTIDOS_PENDENTES", assistidosPendentes, operationalSignals, nativeComposition);
peticoesPendentes = collectionCompositionService.composeList("DEFENSOR_PUBLICO", "PETICOES_PENDENTES", peticoesPendentes, operationalSignals, nativeComposition);
audienciasPendentes = collectionCompositionService.composeList("DEFENSOR_PUBLICO", "AUDIENCIAS_PENDENTES", audienciasPendentes, operationalSignals, nativeComposition);
recursosUrgentes = collectionCompositionService.composeList("DEFENSOR_PUBLICO", "RECURSOS_URGENTES", recursosUrgentes, operationalSignals, nativeComposition);
Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, Map.of(
"assistidosPendentes", assistidosPendentes,
"peticoesPendentes", peticoesPendentes,
"audienciasPendentes", audienciasPendentes,
"recursosUrgentes", recursosUrgentes
));
Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, collectionComposition, actionSurface);
return new DefensoriaSnapshot(
ctx.generatedAt(),
ctx.perfilAtivo(),
ctx.tratamento(),
resolveNucleo(usuario),
assistidosPendentes,
peticoesPendentes,
audienciasPendentes,
recursosUrgentes,
prazosVencendo24h,
presos,
ctx.prazoRadar(),
ctx.sessionRisk(),
operationalSignals,
nativeComposition,
collectionComposition,
actionSurface,
executionSurface,
sharedExperience
);
}

public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
return institutionalActorTopologyMeshService.snapshot(processoId);
}
@Transactional
public Map<String, Object> apresentarDefesa(Long processoId, String defesa,
String fundamentacao) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_DEFENSOR_PUBLICO",
"ROLE_DEFENSOR_PUBLICO_FEDERAL");
String dedupKey = UUID.nameUUIDFromBytes(
("DEFESA:" + processoId + ":" + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
WorkItem defesaItem = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.PETICAO)
.titulo("Defesa — Defensoria Pública — " + processo.getNumeroProcesso())
.descricao(defesa)
.queueCode(institutionalActorRoutingService.secretaryExecution(processoId, "DEFESA_DEFENSORIA").queueCode())
.inboxKey(institutionalActorRoutingService.secretaryExecution(processoId, "DEFESA_DEFENSORIA").inboxKey())
.assignedRole(institutionalActorRoutingService.secretaryExecution(processoId, "DEFESA_DEFENSORIA").assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal(fundamentacao)
.dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
.build();
workItemRepository.save(defesaItem);
commons.publishUserHistory(usuario, "DEFENSOR", "DEFESA_APRESENTADA",
"Defesa apresentada pela Defensoria Pública.", processo, processoId);
movimentacaoRegistrar.registrar(processo, usuario, processo.getFaseAtual(), "Defesa da Defensoria Pública apresentada.");
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "DEFESA_PROTOCOLADA");
out.put("processoId", processoId);
out.put("workItemId", defesaItem.getId());
out.put("dedupKey", dedupKey);
return out;
}
@Transactional

public Map<String, Object> impetrarHabeasCorpus(Long processoId, String impetrante,
String paciente, String fundamentacao) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_DEFENSOR_PUBLICO",
"ROLE_DEFENSOR_PUBLICO_FEDERAL");
String dedupKey = UUID.nameUUIDFromBytes(
("HC:" + processoId + ":" + paciente).getBytes(StandardCharsets.UTF_8)).toString();
WorkItem hcItem = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.PETICAO)
.titulo("Habeas Corpus — Paciente: " + paciente)
.descricao("Impetrante: " + impetrante + " | Paciente: " + paciente)
.queueCode(institutionalActorRoutingService.gabineteDecision(processoId, "HABEAS_CORPUS").queueCode())
.inboxKey(institutionalActorRoutingService.gabineteDecision(processoId, "HABEAS_CORPUS").inboxKey())
.assignedRole(institutionalActorRoutingService.gabineteDecision(processoId, "HABEAS_CORPUS").assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(0)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal(fundamentacao)
.dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
.build();
workItemRepository.save(hcItem);
commons.publishUserHistory(usuario, "DEFENSOR", "HC_IMPETRADO",
"Habeas Corpus impetrado pela Defensoria.", processo, processoId);
movimentacaoRegistrar.registrar(processo, usuario, processo.getFaseAtual(), "Habeas Corpus impetrado pela Defensoria Pública.");
LinkedHashMap<String, Object> response = new LinkedHashMap<>();
response.put("status", "HC_IMPETRADO");
response.put("paciente", paciente == null || paciente.isBlank() ? "PACIENTE_NAO_INFORMADO" : paciente.trim());
response.put("workItemId", hcItem.getId());
response.put("dedupKey", dedupKey);
response.put("encaminhadoPara", institutionalActorRoutingService.gabineteDecision(processoId, "HABEAS_CORPUS").inboxKey());
return Map.copyOf(response);
}
@Transactional
public Map<String, Object> solicitarAssistenciaJudiciariaGratuita(Long processoId,
String renda,
String justificativa) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_DEFENSOR_PUBLICO",
"ROLE_DEFENSOR_PUBLICO_FEDERAL");
return criarSolicitacaoAjg(processo, usuario, renda, justificativa,
"Assistência judiciária gratuita solicitada pela Defensoria Pública.");
}

@Transactional
public Map<String, Object> solicitarAssistenciaJudiciariaGratuitaComoParte(Long processoId,
String renda,
String justificativa) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
if (usuario.getTipoUsuario() != com.tcc.pjb.backend.model.entity.enums.TipoUsuario.CIDADAO) {
throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException("Apenas o próprio cidadão parte do processo pode solicitar AJG diretamente.");
}
String cpf = usuario.getCpf();
if (cpf == null || cpf.isBlank() || !com.tcc.pjb.backend.core.security.ProcessoPartyCpfLinkPolicy.vinculado(cpf, processo)) {
throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException("Cidadão não é parte do processo.");
}
return criarSolicitacaoAjg(processo, usuario, renda, justificativa,
"Assistência judiciária gratuita solicitada pela própria parte (jus postulandi).");
}

private Map<String, Object> criarSolicitacaoAjg(Processo processo, Usuario usuario, String renda, String justificativa, String descricaoMovimentacao) {
Long processoId = processo.getId();
WorkItem ajgItem = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("AJG:" + processoId + ":" + usuario.getId())
.type(WorkItemType.PETICAO)
.titulo("Pedido de AJG — " + processo.getNumeroProcesso())
.descricao("Renda declarada: " + renda + " | Justificativa: " + justificativa)
.queueCode(institutionalActorRoutingService.gabineteDecision(processoId, "ASSISTENCIA_JUDICIARIA_GRATUITA").queueCode())
.inboxKey(institutionalActorRoutingService.gabineteDecision(processoId, "ASSISTENCIA_JUDICIARIA_GRATUITA").inboxKey())
.assignedRole(institutionalActorRoutingService.gabineteDecision(processoId, "ASSISTENCIA_JUDICIARIA_GRATUITA").assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal("Art. 98 CPC — Gratuidade da Justiça")
.dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
.build();
ajgItem = workItemRepository.save(ajgItem);
movimentacaoRegistrar.registrar(processo, usuario, processo.getFaseAtual(), descricaoMovimentacao);
LinkedHashMap<String, Object> response = new LinkedHashMap<>();
response.put("status", "AJG_SOLICITADA");
response.put("processoId", processoId);
response.put("workItemId", ajgItem.getId());
return Map.copyOf(response);
}
public List<Map<String, Object>> listarAssistidosSemAdvogado() {
return commons.inboxHibrido(contextFactory.build().usuario(), 30).stream()
.filter(i -> commons.titleContains(i, "SEM_ADVOGADO", "SEM_DEFESA", "HIPOSSUFICIENTE"))
.map(commons::mapWorkItem)
.toList();
}
private String resolveNucleo(Usuario usuario) {
return switch (usuario.getTipoUsuario()) {
case DEFENSOR_PUBLICO_FEDERAL -> "DPDF_" + (usuario.getUf() == null ? "BR" : usuario.getUf().toUpperCase());
default -> "DPE_" + (usuario.getUf() == null ? "BR" : usuario.getUf().toUpperCase());
};
}
}
