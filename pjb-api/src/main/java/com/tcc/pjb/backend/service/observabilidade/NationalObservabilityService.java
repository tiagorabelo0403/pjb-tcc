package com.tcc.pjb.backend.service.observabilidade;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
@Service
public class NationalObservabilityService {
private static final List<String> UFS_BR = List.of(
"AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
"PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"
);
private final ProcessoRepository processoRepository;
private final WorkItemRepository workItemRepository;
private final PerfilDashboardContextFactory contextFactory;
private final PjbAuthorizationService authorizationService;
public NationalObservabilityService(ProcessoRepository processoRepository,
WorkItemRepository workItemRepository,
PerfilDashboardContextFactory contextFactory,
PjbAuthorizationService authorizationService) {
this.processoRepository = processoRepository;
this.workItemRepository = workItemRepository;
this.contextFactory = contextFactory;
this.authorizationService = authorizationService;
}
public NationalDashboard nationalDashboard() {
var ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long totalProcessos = processoRepository.count();
long pendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
long expirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
long concluidos = workItemRepository.countByStatus(WorkItemStatus.CONCLUIDO);
long throughputUltimaHora = workItemRepository
.countByCreatedAtAfter(Instant.now().minus(1, ChronoUnit.HOURS));
long throughputUltimas24h = workItemRepository
.countByCreatedAtAfter(Instant.now().minus(24, ChronoUnit.HOURS));
double taxaExpiracao = (pendentes + expirados) == 0 ? 0.0
: (expirados * 100.0) / (pendentes + expirados);
String nivelSaude = taxaExpiracao < 5 ? "VERDE"
: taxaExpiracao < 15 ? "AMARELO" : "VERMELHO";
List<MetricaTribunal> metricasPorUf = UFS_BR.stream()
.map(uf -> new MetricaTribunal(uf,
processoRepository.countByUf(uf),
workItemRepository.countByUfAndStatus(uf, WorkItemStatus.PENDENTE),
workItemRepository.countByUfAndStatus(uf, WorkItemStatus.EXPIRADO)))
.filter(m -> m.totalProcessos() > 0)
.toList();
List<String> filasBacklog = workItemRepository.findFilasComMaisDe(100)
.stream().limit(10).toList();

List<AlertaOperacional> alertas = resolveAlertas(expirados, taxaExpiracao, filasBacklog);
return new NationalDashboard(
Instant.now(), totalProcessos, pendentes, expirados, concluidos,
throughputUltimaHora, throughputUltimas24h, taxaExpiracao,
nivelSaude, metricasPorUf, filasBacklog, alertas
);
}
public Map<String, Object> slaReport() {
var ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long totalPendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
long vencidosMenos24h = workItemRepository.countByStatusAndDueAtBetween(
WorkItemStatus.PENDENTE,
Instant.now().minus(24, ChronoUnit.HOURS),
Instant.now());
long vencidos24a72h = workItemRepository.countByStatusAndDueAtBetween(
WorkItemStatus.PENDENTE,
Instant.now().minus(72, ChronoUnit.HOURS),
Instant.now().minus(24, ChronoUnit.HOURS));
long vencidosMaisde72h = workItemRepository
.countByStatusAndDueAtBefore(WorkItemStatus.PENDENTE,
Instant.now().minus(72, ChronoUnit.HOURS));
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("totalPendentes", totalPendentes);
out.put("dentroDoSla", totalPendentes - vencidosMenos24h - vencidos24a72h - vencidosMaisde72h);
out.put("vencidosMenos24h", vencidosMenos24h);
out.put("vencidos24a72h", vencidos24a72h);
out.put("vencidosMaisde72h", vencidosMaisde72h);
out.put("geradoEm", Instant.now());
return out;
}
public Map<String, Object> runbookStatus() {
var ctx = contextFactory.build();
authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_ADMINISTRADOR");
long expirados = workItemRepository.countByStatus(WorkItemStatus.EXPIRADO);
long pendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
List<String> backlog = workItemRepository.findFilasComMaisDe(50).stream().limit(5).toList();
double taxa = (pendentes + expirados) == 0 ? 0 : (expirados * 100.0) / (pendentes + expirados);
LinkedHashMap<String, Object> out = new LinkedHashMap<>();
out.put("sistemaStatus", taxa < 5 ? "OPERACIONAL" : taxa < 15 ? "DEGRADADO" : "CRITICO");
out.put("acoesSugeridas", resolveRunbook(taxa, expirados, backlog));
out.put("geradoEm", Instant.now());
return out;
}
private List<AlertaOperacional> resolveAlertas(long expirados, double taxa, List<String> backlog) {
List<AlertaOperacional> alertas = new java.util.ArrayList<>();
if (taxa > 20) alertas.add(new AlertaOperacional("CRITICO",
"Taxa de expiração crítica: " + String.format("%.1f", taxa) + "%",
"Executar reconciliação imediata em ROLE_ADMIN → executarReconciliacaoGlobal"));
else if (taxa > 10) alertas.add(new AlertaOperacional("ATENCAO",
"Taxa de expiração elevada: " + String.format("%.1f", taxa) + "%",
"Monitorar e planejar reconciliação"));
if (expirados > 5000) alertas.add(new AlertaOperacional("CRITICO",
"Volume crítico de work items expirados: " + expirados,
"Escalar para equipe de operações"));
backlog.forEach(f -> alertas.add(new AlertaOperacional("ATENCAO",
"Backlog na fila: " + f, "Verificar processadores da fila " + f)));
return alertas;
}
private List<String> resolveRunbook(double taxa, long expirados, List<String> backlog) {
List<String> acoes = new java.util.ArrayList<>();
if (taxa > 10) acoes.add("Executar POST /admin/reconciliacao-global");
if (expirados > 1000) acoes.add("Analisar filas com maior volume de expiração");
if (!backlog.isEmpty()) acoes.add("Verificar consumers das filas: " + backlog);
if (acoes.isEmpty()) acoes.add("Sistema operacional — nenhuma ação necessária");
return acoes;
}
}
