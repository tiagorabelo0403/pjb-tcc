package com.tcc.pjb.backend.service.perito;
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
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoLaudoRequest;
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
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
@Service
public class PeritoOperacionalEnhancedService {
private static final EnumSet<TipoUsuario> PERITOS = EnumSet.of(
TipoUsuario.PERITO, TipoUsuario.PERITO_CRIMINAL, TipoUsuario.PERITO_AMBIENTAL,
TipoUsuario.PERITO_CONTABIL, TipoUsuario.PERITO_ENGENHARIA,
TipoUsuario.PERITO_DIGITAL, TipoUsuario.PERITO_INSS, TipoUsuario.PERITO_MEDICO,
TipoUsuario.PSICOLOGO_JUDICIAL, TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL,
TipoUsuario.ASSISTENTE_TECNICO
);
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final ProcessoLifecycleMachine lifecycleMachine;
private final InstitutionalActorRoutingService institutionalActorRoutingService;
private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
public PeritoOperacionalEnhancedService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService,
ProcessoLifecycleMachine lifecycleMachine,
InstitutionalActorRoutingService institutionalActorRoutingService,
InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
this.lifecycleMachine = lifecycleMachine;
this.institutionalActorRoutingService = institutionalActorRoutingService;
this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
}
public PeritoSnapshot bootstrapPainel() {
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRoleAny(usuario, PERITOS.stream()
.map(t -> "ROLE_" + t.name()).toArray(String[]::new));
List<WorkItem> inbox = commons.inboxHibrido(usuario, 40);
List<String> nomeacoesPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "NOMEACAO", "ACEITE_NOMEACAO"))
.limit(10).map(commons::resumo).toList();
List<String> laudosPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "LAUDO", "PERICIA", "EXAME"))
.limit(15).map(commons::resumo).toList();
List<String> quesitosResponder = inbox.stream()
.filter(i -> commons.titleContains(i, "QUESITO", "ESCLARECIMENTO"))
.limit(10).map(commons::resumo).toList();
int honorariosPendentes = (int) inbox.stream()
.filter(i -> commons.titleContains(i, "HONORARIO", "PAGAMENTO")).count();
int prazosUrgentes = (int) inbox.stream()
.filter(i -> i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(72, ChronoUnit.HOURS))).count();
return new PeritoSnapshot(
ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(),
resolveEspecialidade(usuario), nomeacoesPendentes,
laudosPendentes, quesitosResponder,
honorariosPendentes, prazosUrgentes,
ctx.prazoRadar(), ctx.sessionRisk()
);
}
@Transactional
public Map<String, Object> aceitarNomeacao(Long processoId) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
InstitutionalActorRoutingService.InstitutionalRoute aceiteRoute = institutionalActorRoutingService.secretaryReceipt(processoId, "ACEITE_PERITO");
WorkItem aceite = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("ACEITE_NOMEACAO:" + processoId + ":" + usuario.getId())
.type(WorkItemType.PETICAO)
.titulo("Aceite de Nomeação Pericial — " + processo.getNumeroProcesso())
.descricao("Perito " + usuario.getNome() + " aceita a nomeação.")
.queueCode(aceiteRoute.queueCode())
.inboxKey(aceiteRoute.inboxKey())
.assignedRole(aceiteRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
.build();
workItemRepository.save(aceite);
return Map.of("status", "NOMEACAO_ACEITA", "processoId", processoId,
"workItemId", aceite.getId());
}
@Transactional
public Map<String, Object> apresentarLaudo(Long processoId, PeritoLaudoRequest request) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
String tipoLaudo = request == null ? null : request.tipoLaudo();
String conclusao = request == null ? null : request.conclusao();
String metodologia = request == null ? null : request.metodologia();
String dedupKey = UUID.nameUUIDFromBytes(
("LAUDO:" + tipoLaudo + ":" + processoId + ":" + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute laudoRoute = institutionalActorRoutingService.secretaryReceipt(processoId, "JUNTADA_LAUDO");
WorkItem laudo = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.LAUDO)
.titulo("Laudo " + tipoLaudo + " — " + processo.getNumeroProcesso())
.descricao("METODOLOGIA: " + metodologia + " | CONCLUSÃO: " + conclusao)
.queueCode(laudoRoute.queueCode())
.inboxKey(laudoRoute.inboxKey())
.assignedRole(laudoRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
.build();
workItemRepository.save(laudo);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.APRESENTAR_LAUDO);
processoRepository.save(processo);
commons.publishUserHistory(usuario, "PERITO", "LAUDO_APRESENTADO",
"Laudo " + tipoLaudo + " apresentado.", processo, processoId);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "LAUDO_PROTOCOLADO");
out.put("tipo", tipoLaudo);
out.put("processoId", processoId);
out.put("workItemId", laudo.getId());
out.put("dedupKey", dedupKey);
out.putAll(institutionalMultimediaWorkspaceService.enrich(
new InstitutionalMultimediaWorkspaceService.ResolveRequest(
"PERICIA",
"LAUDO_PERICIAL",
processoId,
usuario.getTipoUsuario(),
request,
request != null && request.prepararPacoteProtocoloResolvido(),
false,
true
)
));
return out;
}
@Transactional
public Map<String, Object> responderQuesitos(Long processoId, Map<Integer, String> respostas) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
StringBuilder conteudo = new StringBuilder();
respostas.forEach((n, r) -> conteudo.append("Quesito ").append(n).append(": ").append(r).append("\n"));
InstitutionalActorRoutingService.InstitutionalRoute quesitoRoute = institutionalActorRoutingService.secretaryReceipt(processoId, "JUNTADA_QUESITOS");
WorkItem quesito = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("QUESITOS:" + processoId + ":" + usuario.getId())
.type(WorkItemType.PETICAO)
.titulo("Resposta a Quesitos — " + processo.getNumeroProcesso())
.descricao(conteudo.toString())
.queueCode(quesitoRoute.queueCode())
.inboxKey(quesitoRoute.inboxKey())
.assignedRole(quesitoRoute.assignedRole())
.status(WorkItemStatus.CONCLUIDO)
.prioridade(2)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
.build();
workItemRepository.save(quesito);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.RESPONDER_QUESITOS);
processoRepository.save(processo);
return Map.of("status", "QUESITOS_RESPONDIDOS", "processoId", processoId,
"total", respostas.size(), "workItemId", quesito.getId());
}
@Transactional
public Map<String, Object> solicitarHonorarios(Long processoId, double valor, String justificativa) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
InstitutionalActorRoutingService.InstitutionalRoute honorarioRoute = institutionalActorRoutingService.gabineteDecision(processoId, "HONORARIOS_PERICIAIS");
WorkItem honorario = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("HONORARIO:" + processoId + ":" + usuario.getId())
.type(WorkItemType.PETICAO)
.titulo("Solicitação de Honorários Periciais — " + processo.getNumeroProcesso())
.descricao("Valor: R$ " + String.format("%.2f", valor) + " | " + justificativa)
.queueCode(honorarioRoute.queueCode())
.inboxKey(honorarioRoute.inboxKey())
.assignedRole(honorarioRoute.assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(3)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal("Art. 465, §3º CPC — Honorários Periciais")
.dueAt(Instant.now().plus(5, ChronoUnit.DAYS))
.build();
workItemRepository.save(honorario);
return Map.of("status", "HONORARIOS_SOLICITADOS", "valor", valor,
"processoId", processoId, "workItemId", honorario.getId());
}
private String resolveEspecialidade(Usuario usuario) {

return switch (usuario.getTipoUsuario()) {
case PERITO_CRIMINAL -> "PERICIA_CRIMINAL";
case PERITO_CONTABIL -> "PERICIA_CONTABIL";
case PERITO_MEDICO -> "PERICIA_MEDICA";
case PERITO_DIGITAL -> "PERICIA_DIGITAL";
case PERITO_ENGENHARIA -> "PERICIA_ENGENHARIA";
case PSICOLOGO_JUDICIAL -> "PSICOLOGIA_JUDICIAL";
case ASSISTENTE_SOCIAL_JUDICIAL -> "SERVICO_SOCIAL_JUDICIAL";
default -> "PERICIA_GERAL";
};
}
}
