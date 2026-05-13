package com.tcc.pjb.backend.service.conciliacao;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
@Service
public class ConciliadorMediadorEnhancedService {
private static final EnumSet<TipoUsuario> CONCILIACAO = EnumSet.of(
TipoUsuario.CONCILIADOR_CEJUSC, TipoUsuario.MEDIADOR, TipoUsuario.ARBITRO
);
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
private final ProcessoLifecycleMachine lifecycleMachine;
private final InstitutionalActorRoutingService institutionalActorRoutingService;
private final OfficialDocumentTemplateService officialDocumentTemplateService;
public ConciliadorMediadorEnhancedService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService,
ProcessoLifecycleMachine lifecycleMachine,
InstitutionalActorRoutingService institutionalActorRoutingService,
OfficialDocumentTemplateService officialDocumentTemplateService) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
this.lifecycleMachine = lifecycleMachine;
this.institutionalActorRoutingService = institutionalActorRoutingService;
this.officialDocumentTemplateService = officialDocumentTemplateService;
}
public ConciliacaoSnapshot bootstrapPainel() {
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRoleAny(usuario, "ROLE_CONCILIADOR_CEJUSC",
"ROLE_MEDIADOR", "ROLE_ARBITRO");
List<WorkItem> inbox = commons.inboxHibrido(usuario, 40);
List<String> sessoesPendentes = inbox.stream()
.filter(i -> commons.titleContains(i, "SESSAO", "AUDIENCIA_CONCILIACAO",
"AUDIENCIA_MEDIACAO", "SESSAO_ARBITRAGEM"))
.limit(15).map(commons::resumo).toList();
List<String> acordosPendentesHomologacao = inbox.stream()
.filter(i -> commons.titleContains(i, "ACORDO", "HOMOLOGACAO", "TERMO"))
.limit(10).map(commons::resumo).toList();
List<String> novosCasosDesignados = inbox.stream()
.filter(i -> commons.titleContains(i, "DESIGNACAO", "NOVO_CASO", "DISTRIBUICAO"))
.limit(10).map(commons::resumo).toList();
int sessoesHoje = (int) inbox.stream()
.filter(i -> i.getDueAt() != null
&& i.getDueAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))).count();
int taxaAcordoSimulada = calcularTaxaAcordo(inbox);
return new ConciliacaoSnapshot(
ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(),
resolveCejusc(usuario), resolvePerfil(usuario),
sessoesPendentes, acordosPendentesHomologacao,
novosCasosDesignados, sessoesHoje, taxaAcordoSimulada,
ctx.prazoRadar(), ctx.sessionRisk()
);
}
@Transactional
public Map<String, Object> registrarResultadoSessao(Long processoId, String resultado,
String observacoes, boolean acordoFirmado) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRoleAny(usuario, "ROLE_CONCILIADOR_CEJUSC",
"ROLE_MEDIADOR", "ROLE_ARBITRO");
String dedupKey = UUID.nameUUIDFromBytes(
("SESSAO_RESULTADO:" + processoId + ":" + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute resultadoRoute = acordoFirmado
? institutionalActorRoutingService.gabineteDecision(processoId, "HOMOLOGACAO_ACORDO")
: institutionalActorRoutingService.secretarySaneamento(processoId, "CERTIFICACAO_SEM_ACORDO");
WorkItem resultadoItem = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(acordoFirmado ? WorkItemType.ACORDO : WorkItemType.PETICAO)
.titulo("Resultado de Sessão — " + resultado + " — " + processo.getNumeroProcesso())
.descricao(observacoes)
.queueCode(resultadoRoute.queueCode())
.inboxKey(resultadoRoute.inboxKey())
.assignedRole(resultadoRoute.assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(acordoFirmado ? 1 : 3)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(Instant.now().plus(acordoFirmado ? 24 : 48, ChronoUnit.HOURS))
.build();
workItemRepository.save(resultadoItem);
if (acordoFirmado) {
    lifecycleMachine.apply(processo, ProcessoLifecycleAction.REGISTRAR_ACORDO);
    processoRepository.save(processo);
}
commons.publishUserHistory(usuario, resolvePerfil(usuario), "SESSAO_REALIZADA",
"Sessão realizada. Resultado: " + resultado, processo, processoId);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", acordoFirmado ? "ACORDO_FIRMADO_AGUARDANDO_HOMOLOGACAO" : "SESSAO_INFRUTIFERA");
out.put("resultado", resultado);
out.put("processoId", processoId);
out.put("workItemId", resultadoItem.getId());
out.put("dedupKey", dedupKey);
if (!acordoFirmado) {
out.put("documentoFormalAssinado", summarizeRenderedDocument(renderResultadoSessaoSemAcordo(processo, usuario, resultado, observacoes)));
}
return Collections.unmodifiableMap(out);
}
@Transactional
public Map<String, Object> agendarSessao(Long processoId, Instant dataHora,
String sala, String modalidade) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRoleAny(usuario, "ROLE_CONCILIADOR_CEJUSC",
"ROLE_MEDIADOR", "ROLE_ARBITRO");
InstitutionalActorRoutingService.InstitutionalRoute agendaRoute = institutionalActorRoutingService.secretaryAudience(processoId, "AGENDAMENTO_SESSAO_" + resolvePerfil(usuario));
WorkItem sessao = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode("SESSAO:" + processoId + ":" + dataHora.toEpochMilli())
.type(WorkItemType.AUDIENCIA)
.titulo("Sessão " + resolvePerfil(usuario) + " — " + modalidade
+ " — " + processo.getNumeroProcesso())
.descricao("Sala: " + sala + " | Modalidade: " + modalidade)
.queueCode(agendaRoute.queueCode())
.inboxKey(agendaRoute.inboxKey())
.assignedRole(agendaRoute.assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.dueAt(dataHora)
.build();
workItemRepository.save(sessao);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.DESIGNAR_AUDIENCIA);
processoRepository.save(processo);
return Map.of("status", "SESSAO_AGENDADA", "dataHora", dataHora,
"sala", sala, "modalidade", modalidade, "workItemId", sessao.getId());
}
@Transactional
public Map<String, Object> lavrarTermoAcordo(Long processoId, String clausulas,
List<String> partes, double valor) {
Processo processo = processoRepository.findById(processoId)
.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
PerfilDashboardContext ctx = contextFactory.build();
Usuario usuario = ctx.usuario();
authorizationService.requireRoleAny(usuario, "ROLE_CONCILIADOR_CEJUSC",
"ROLE_MEDIADOR", "ROLE_ARBITRO");
String dedupKey = UUID.nameUUIDFromBytes(
("TERMO_ACORDO:" + processoId).getBytes(StandardCharsets.UTF_8)).toString();
InstitutionalActorRoutingService.InstitutionalRoute termoRoute = institutionalActorRoutingService.gabineteDecision(processoId, "HOMOLOGACAO_ACORDO");
WorkItem termo = WorkItem.builder()
.processo(processo)
.faseOrigem(processo.getFaseAtual())
.templateCode(dedupKey)
.type(WorkItemType.ACORDO)
.titulo("Termo de Acordo — R$ " + String.format("%.2f", valor)
+ " — " + processo.getNumeroProcesso())
.descricao("Partes: " + normalizePartes(partes) + "\nCláusulas: " + defaultText(clausulas, "Cláusulas não informadas"))
.queueCode(termoRoute.queueCode())
.inboxKey(termoRoute.inboxKey())
.assignedRole(termoRoute.assignedRole())
.status(WorkItemStatus.PENDENTE)
.prioridade(1)
.uf(usuario.getUf())
.comarca(usuario.getComarca())
.baseLegal("Art. 334 §11 CPC — Homologação de Acordo")
.dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
.build();
workItemRepository.save(termo);
lifecycleMachine.apply(processo, ProcessoLifecycleAction.REGISTRAR_ACORDO);
processoRepository.save(processo);
OfficialDocumentTemplateRenderResponse termoFormal = renderTermoAcordo(processo, usuario, partes, clausulas, valor);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "TERMO_LAVRADO_AGUARDANDO_HOMOLOGACAO");
out.put("valor", valor);
out.put("processoId", processoId);
out.put("workItemId", termo.getId());
out.put("dedupKey", dedupKey);
out.put("documentoFormalAssinado", summarizeRenderedDocument(termoFormal));
return Collections.unmodifiableMap(out);
}
private OfficialDocumentTemplateRenderResponse renderResultadoSessaoSemAcordo(Processo processo, Usuario usuario, String resultado, String observacoes) {
return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
processo.getId(),
TemplateDocumentoOficial.CERTIDAO,
"Certidão de sessão sem acordo — " + processo.getNumeroProcesso(),
Map.of(
"fatoCertificado", defaultText(resultado, "Sessão realizada sem acordo homologável"),
"responsavelCertificacao", defaultText(usuario.getNome(), "AUTORIDADE_CONSENSUAL"),
"observacoesSessao", defaultText(observacoes, "Sem observações adicionais")
),
Boolean.TRUE,
Boolean.TRUE
));
}

private OfficialDocumentTemplateRenderResponse renderTermoAcordo(Processo processo, Usuario usuario, List<String> partes, String clausulas, double valor) {
return officialDocumentTemplateService.renderizar(new OfficialDocumentTemplateRenderRequest(
processo.getId(),
TemplateDocumentoOficial.TERMO_ACORDO,
"Termo de acordo — " + processo.getNumeroProcesso(),
Map.of(
"partesEnvolvidas", normalizePartes(partes),
"clausulasAcordo", defaultText(clausulas, "Cláusulas não informadas"),
"valorAcordado", String.format(java.util.Locale.ROOT, "%.2f", valor),
"perfilCondutor", resolvePerfil(usuario),
"ceJuscOuCamara", resolveCejusc(usuario)
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

private String defaultText(String value, String fallback) {
return value == null || value.isBlank() ? fallback : value.trim();
}

private String normalizePartes(List<String> partes) {
if (partes == null || partes.isEmpty()) {
return "PARTES_NAO_INFORMADAS";
}
return partes.stream()
.filter(java.util.Objects::nonNull)
.map(String::trim)
.filter(v -> !v.isBlank())
.distinct()
.reduce((a, b) -> a + ", " + b)
.orElse("PARTES_NAO_INFORMADAS");
}

private String resolveCejusc(Usuario usuario) {
return "CEJUSC_" + (usuario.getComarca() == null ? "CENTRAL" : usuario.getComarca().toUpperCase());
}
private String resolvePerfil(Usuario usuario) {
return switch (usuario.getTipoUsuario()) {
case CONCILIADOR_CEJUSC -> "CONCILIACAO";
case MEDIADOR -> "MEDIACAO";
case ARBITRO -> "ARBITRAGEM";
default -> "RESOLUCAO_CONSENSUAL";
};
}
private int calcularTaxaAcordo(List<WorkItem> inbox) {
long total = inbox.stream()
.filter(i -> commons.titleContains(i, "SESSAO", "CONCILIACAO", "MEDIACAO")).count();
long acordos = inbox.stream()
.filter(i -> commons.titleContains(i, "ACORDO", "HOMOLOGACAO")).count();
return total == 0 ? 0 : (int) ((acordos * 100) / total);
}
}
