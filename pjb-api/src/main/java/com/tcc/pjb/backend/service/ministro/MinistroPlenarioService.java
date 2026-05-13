package com.tcc.pjb.backend.service.ministro;
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
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
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
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
@Service
public class MinistroPlenarioService {
private static final EnumSet<TipoUsuario> MINISTROS = EnumSet.of(TipoUsuario.MINISTRO);
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final ProcessoLifecycleMachine lifecycleMachine;
private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
private final InstitutionalActorRoutingService institutionalActorRoutingService;
private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;
public MinistroPlenarioService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService,
ProcessoLifecycleMachine lifecycleMachine,
InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
InstitutionalActorRoutingService institutionalActorRoutingService,
RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
this.lifecycleMachine = lifecycleMachine;
this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
this.institutionalActorRoutingService = institutionalActorRoutingService;
this.recursalQualifiedDocumentMaterializerService = recursalQualifiedDocumentMaterializerService;
}
public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
return institutionalActorTopologyMeshService.snapshot(processoId);
}
public PlenarioSnapshot bootstrapPlenario() {
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
if (!MINISTROS.contains(usuario.getTipoUsuario())) {
throw new AccessDeniedPjbException("Acesso exclusivo a ministros de tribunal superior.");
}
List<WorkItem> inbox = commons.inboxHibrido(usuario, 80);
List<String> recursosTurmaPlenario = inbox.stream()
.filter(i -> commons.titleContains(i, "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO",
"RECLAMACAO", "EMBARGOS_DIVERGENCIA", "ACAO_ORIGINARIA"))
.limit(25).map(commons::resumo).toList();
List<String> questoesOrdensExpediente = inbox.stream()
.filter(i -> commons.titleContains(i, "QUESTAO_ORDEM", "PEDIDO_DESTAQUE",

"PEDIDO_PREFERENCIA", "PAUTA_PLENARIO"))
.limit(15).map(commons::resumo).toList();
List<String> monocraticosPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "MONOCRATICO", "INDIVIDUAL", "RELATORIO"))
.limit(30).map(commons::resumo).toList();
List<String> embargosDeclaracaoPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "EMBARGOS_DECLARACAO", "ESCLARECIMENTO"))
.limit(15).map(commons::resumo).toList();
int adesPlenario = (int) inbox.stream()
.filter(i -> commons.titleContains(i, "PLENARIO", "TURMA")).count();
int recursosUrgentes = (int) inbox.stream()
.filter(i -> i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(72, ChronoUnit.HOURS)))
.count();
boolean stepUpRequerido = ctx.sessionRisk() != null;
return new PlenarioSnapshot(
ctx.generatedAt(),
ctx.perfilAtivo(),
ctx.tratamento(),
resolveCorte(usuario),
resolveOrgao(usuario),
recursosTurmaPlenario,
questoesOrdensExpediente,
monocraticosPendentes,
embargosDeclaracaoPendentes,
adesPlenario,
recursosUrgentes,
stepUpRequerido,
ctx.prazoRadar(),
ctx.sessionRisk()
);
}
@Transactional
public Map<String, Object> proferirDecisaoMonocratica(Long processoId, String relatorio,
String fundamentacao, String dispositivo) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_MINISTRO");
String dedupKey = UUID.nameUUIDFromBytes(
("MINISTRO_MONO:" + processoId + ":" + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute publicationRoute = institutionalActorRoutingService.superiorCourt(processoId, "DECISAO_MONOCRATICA", true);
WorkItem mono = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.SENTENCA)
.titulo("Decisão Monocrática — Min. " + usuario.getNome() + " — " + processo.getNumeroProcesso())
.descricao("RELATÓRIO: " + relatorio + " | DISPOSITIVO: " + dispositivo)
.queueCode(publicationRoute.queueCode())
.inboxKey(publicationRoute.inboxKey())
.assignedRole(publicationRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(0)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal(fundamentacao)
.dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
.build();
workItemRepository.save(mono);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.PROFERIR_SENTENCA);
processoRepository.save(processo);
commons.publishUserHistory(usuario, "MINISTRO", "DECISAO_MONOCRATICA_PROFERIDA",
"Decisão monocrática proferida.", processo, processoId);
Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarDecisaoMonocratica(
processoId,
mono.getTitulo(),
relatorio,
fundamentacao,
dispositivo,
resolveOrgao(usuario),
"ULTIMA_INSTANCIA"
);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "DECISAO_MONOCRATICA_PROFERIDA");
out.put("processoId", processoId);
out.put("workItemId", mono.getId());
out.put("dedupKey", dedupKey);
out.put("encaminhadoPara", publicationRoute.inboxKey());
out.put("documentoFormalAssinado", documentoFormalAssinado);
out.put("assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"));
out.put("validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana"));
return out;
}

@Transactional
public Map<String, Object> incluirPautaPlenario(Long processoId, Instant dataSessao, String orgao) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_MINISTRO");
InstitutionalActorRoutingService.InstitutionalRoute pautaRoute = institutionalActorRoutingService.superiorCourt(processoId, "PAUTA_" + orgao, false);
WorkItem pauta = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("PAUTA_PLENARIO:" + processoId + ":" + dataSessao.toEpochMilli())
.type(WorkItemType.AUDIENCIA)
.titulo("Inclusão em Pauta — " + orgao + " — " + processo.getNumeroProcesso())
.descricao("Órgão: " + orgao + " | Data: " + dataSessao)
.queueCode(pautaRoute.queueCode())
.inboxKey(pautaRoute.inboxKey())
.assignedRole(pautaRoute.assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(0)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(dataSessao)
.build();
workItemRepository.save(pauta);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.DESIGNAR_AUDIENCIA);
processoRepository.save(processo);
Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPauta(
processoId,
pauta.getTitulo(),
"Partes, advogados, procuradores e interessados habilitados",
"Inclusão em pauta de julgamento no órgão " + orgao + " em " + dataSessao + '.',
dataSessao.toString(),
resolveOrgao(usuario),
"ULTIMA_INSTANCIA"
);
return Map.of(
"status", "INCLUIDO_PAUTA",
"orgao", orgao,
"dataSessao", dataSessao,
"workItemId", pauta.getId(),
"documentoFormalAssinado", documentoFormalAssinado,
"assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"),
"validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana")
);
}
@Transactional
public Map<String, Object> registrarDecisaoPlenaria(Long processoId, String votacao,
String ementa, String dispositivo) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRole(usuario, "ROLE_MINISTRO");
String dedupKey = UUID.nameUUIDFromBytes(
("PLENARIO:" + processoId + ":" + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute publicationRoute = institutionalActorRoutingService.superiorCourt(processoId, "ACORDAO_PLENARIO", true);
WorkItem acordao = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.SENTENCA)
.titulo("Acórdão Plenário — " + processo.getNumeroProcesso() + " — " + votacao)
.descricao("EMENTA: " + ementa + " | DISPOSITIVO: " + dispositivo)
.queueCode(publicationRoute.queueCode())
.inboxKey(publicationRoute.inboxKey())
.assignedRole(publicationRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(0)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
.build();
workItemRepository.save(acordao);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.LAVRAR_ACORDAO);
processoRepository.save(processo);
commons.publishUserHistory(usuario, "MINISTRO", "DECISAO_PLENARIA_REGISTRADA",
"Decisão plenária registrada e encaminhada para publicação.", processo, processoId);
Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
processoId,
acordao.getTitulo(),
ementa,
"Votação: " + votacao,
dispositivo,
resolveOrgao(usuario),
"ULTIMA_INSTANCIA",
"ACORDAO_PLENARIO"
);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "ACORDAO_PLENARIO_REGISTRADO");
out.put("votacao", votacao);
out.put("processoId", processoId);
out.put("workItemId", acordao.getId());
out.put("documentoFormalAssinado", documentoFormalAssinado);
out.put("assinaturaQualificada", documentoFormalAssinado.get("assinaturaQualificada"));
out.put("validacaoSoberana", documentoFormalAssinado.get("validacaoSoberana"));
return out;
}
private String resolveCorte(Usuario usuario) {
return switch (usuario.getTipoUsuario()) {
case MINISTRO -> "SUPERIOR_TRIBUNAL";
default -> "TRIBUNAL_SUPERIOR_DESCONHECIDO";
};
}

private String resolveOrgao(Usuario usuario) {
return "PLENARIO_" + (usuario.getUf() == null ? "NACIONAL" : usuario.getUf().toUpperCase());
}
}
