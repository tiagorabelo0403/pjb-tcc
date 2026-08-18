package com.tcc.pjb.backend.service.admin;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
@Service
public class AdministradorNacionalGovernanceService {
private final PerfilDashboardContextFactory contextFactory;
private final PainelServiceCommons commons;
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PjbAuthorizationService authorizationService;
public AdministradorNacionalGovernanceService(PerfilDashboardContextFactory contextFactory,
PainelServiceCommons commons,
ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PjbAuthorizationService authorizationService) {
this.contextFactory = contextFactory;
this.commons = commons;
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.authorizationService = authorizationService;
}
public AdminGovernanceSnapshot bootstrapGovernance() {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long totalProcessosNacionais = processoRepository.count();
long workItemsPendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
long workItemsExpirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
long workItemsConcluidos = workItemRepository.countByStatus(WorkItemStatus.CONCLUIDO);
long workItemsUltimaHora = workItemRepository
.countByCreatedAtAfter(Instant.now().minus(1, ChronoUnit.HOURS));
double taxaExpiracaoPercent = workItemsPendentes == 0 ? 0.0
: (workItemsExpirados * 100.0) / (workItemsPendentes + workItemsExpirados);
List<String> filasComBacklog = workItemRepository.findFilasComMaisDe(50).stream()
.limit(20).toList();
List<String> alertasAtivos = resolveAlertas(workItemsExpirados, taxaExpiracaoPercent, filasComBacklog);
return new AdminGovernanceSnapshot(
ctx.generatedAt(), "ADMINISTRADOR", "Sistema",
totalProcessosNacionais, workItemsPendentes,
workItemsExpirados, workItemsConcluidos, workItemsUltimaHora,
taxaExpiracaoPercent, filasComBacklog, alertasAtivos,
ctx.sessionRisk()
);
}

public Map<String, Object> metricasPorTribunal(String uf) {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long total = processoRepository.countByUf(uf);
long ativos = processoRepository.countByUfAndFaseAtivaNotArchived(uf);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("uf", uf);
out.put("totalProcessos", total);
out.put("processosAtivos", ativos);
out.put("workItemsPendentes",
workItemRepository.countByUfAndStatus(uf, WorkItemStatus.PENDENTE));
out.put("workItemsExpirados",
workItemRepository.countByUfAndStatus(uf, WorkItemStatus.EXPIRADO));
out.put("geradoEm", Instant.now());
return out;
}
public Map<String, Object> metricasPorComarca(String uf, String comarca) {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("uf", uf);
out.put("comarca", comarca);
out.put("totalProcessos",
processoRepository.countByUfAndComarca(uf, comarca));
out.put("workItemsPendentes",
workItemRepository.countByUfAndComarcaAndStatus(uf, comarca, WorkItemStatus.PENDENTE));
out.put("geradoEm", Instant.now());
return out;
}
    @PjbTransactionalBudget(operation = "admin.governance.reconciliacao-global", maxMillis = 8000)
@Transactional
public Map<String, Object> executarReconciliacaoGlobal() {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
List<com.tcc.pjb.backend.model.entity.workflow.WorkItem> expirados = workItemRepository
.findByStatusAndDueAtBefore(WorkItemStatus.PENDENTE, Instant.now(),
PageRequest.of(0, 500))
.getContent().stream()
.peek(item -> item.setStatus(WorkItemStatus.EXPIRADO))
.toList();
workItemRepository.saveAll(expirados);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", "RECONCILIACAO_EXECUTADA");
out.put("itensExpirados", expirados.size());
out.put("ids", expirados.stream().map(com.tcc.pjb.backend.model.entity.workflow.WorkItem::getId).toList());
out.put("executadoEm", Instant.now());
out.put("operador", ctx.usuario().getId());
return out;
}
@Transactional
public Map<String, Object> ativarModoEmergencia(String motivo, String responsavel) {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
commons.publishUserHistory(ctx.usuario(), "ADMIN", "MODO_EMERGENCIA_ATIVADO",
"Emergência: " + motivo + " | Responsável: " + responsavel, null, null);
return Map.of("status", "MODO_EMERGENCIA_ATIVO", "motivo", motivo,
"responsavel", responsavel, "ativadoEm", Instant.now());
}
public Map<String, Object> healthCheckNacional() {
PerfilDashboardContext ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long pendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
long expirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
double taxaExpiracao = pendentes == 0 ? 0.0 : (expirados * 100.0) / (pendentes + expirados);
String nivel = taxaExpiracao < 5 ? "SAUDAVEL" : taxaExpiracao < 15 ? "ATENCAO" : "CRITICO";
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("status", nivel);

out.put("workItemsPendentes", pendentes);
out.put("workItemsExpirados", expirados);
out.put("taxaExpiracaoPercent", String.format("%.2f", taxaExpiracao));
out.put("totalProcessos", processoRepository.count());
out.put("timestamp", Instant.now());
return out;
}
private List<String> resolveAlertas(long expirados, double taxa, List<String> backlog) {
List<String> alertas = new java.util.ArrayList<>();
if (expirados > 1000) alertas.add("ALERTA: " + expirados + " work items expirados no sistema.");
if (taxa > 15) alertas.add("CRITICO: Taxa de expiração " + String.format("%.1f", taxa) + "% acima do limite.");
if (!backlog.isEmpty()) alertas.add("BACKLOG: " + backlog.size() + " filas com acúmulo crítico.");
return alertas;
}
}
