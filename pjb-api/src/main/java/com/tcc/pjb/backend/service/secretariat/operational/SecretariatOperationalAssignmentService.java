package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;


@Service
public class SecretariatOperationalAssignmentService {

    private final UsuarioRepository usuarioRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService projectionService;

    public SecretariatOperationalAssignmentService(UsuarioRepository usuarioRepository,
                                                   WorkItemRepository workItemRepository,
                                                   SecretariatQueueProjectionService projectionService) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
    }

    @Transactional(readOnly = true)
    public AssignmentSnapshot avaliar(Processo processo,
                                      SecretariatOperationalRoutingProfile routing,
                                      String stage) {
        StageRoute stageRoute = resolveStageRoute(routing, stage);
        List<WorkItem> targetItems = locateTargetItems(processo.getId(), stageRoute);
        List<Usuario> candidates = resolveCandidates(processo, routing, stageRoute);
        Usuario primary = candidates.isEmpty() ? null : candidates.getFirst();
        Usuario backup = candidates.size() > 1 ? candidates.get(1) : null;
        List<CandidateLoad> candidateLoads = candidates.stream().map(candidate -> buildCandidateLoad(processo, routing, stageRoute, candidate)).toList();
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Distribuição interna calculada sobre a própria malha de work items e inbox institucional da secretaria.");
        fundamentos.add("Stage analisado: " + stageRoute.stageToken() + " no inbox " + stageRoute.inboxKey() + " e queue " + stageRoute.queueCode() + '.');
        fundamentos.add("Célula operacional sugerida: " + stageRoute.cellCode() + '.');
        if (primary != null) {
            fundamentos.add("Servidor recomendado: " + primary.getNome() + " (#" + primary.getId() + ").");
        }
        if (backup != null) {
            fundamentos.add("Servidor de retaguarda: " + backup.getNome() + " (#" + backup.getId() + ").");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("candidateCount", candidates.size());
        metrics.put("targetItemCount", targetItems.size());
        metrics.put("stage", stageRoute.stageToken());
        metrics.put("cellCode", stageRoute.cellCode());
        metrics.put("queueCode", stageRoute.queueCode());
        metrics.put("inboxKey", stageRoute.inboxKey());
        metrics.put("processoId", processo.getId());
        metrics.put("secretariatCode", routing.secretariatCode());
        return new AssignmentSnapshot(stageRoute.stageToken(), stageRoute.cellCode(), targetItems.stream().map(WorkItem::getId).toList(), primary, backup, candidateLoads, List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    @PjbTransactionalBudget(operation = "secretariat.operational.atribuir", maxMillis = 3000)
    @Transactional
    public AssignmentSnapshot atribuir(Processo processo,
                                       SecretariatOperationalRoutingProfile routing,
                                       String stage) {
        AssignmentSnapshot snapshot = avaliar(processo, routing, stage);
        if (snapshot.primary() == null || snapshot.targetWorkItemIds().isEmpty()) {
            return snapshot;
        }
        Usuario selected = snapshot.primary();
        StageRoute stageRoute = resolveStageRoute(routing, stage);
        List<WorkItem> items = locateTargetItems(processo.getId(), stageRoute);
        for (WorkItem item : items) {
            item.setAssignedUser(selected);
            item.setAssignedRole(selected.getTipoUsuario() == null ? TipoUsuario.SERVIDOR_FORUM : selected.getTipoUsuario());
            if (item.getStatus() == null) {
                item.setStatus(WorkItemStatus.PENDENTE);
            }
            if (item.getDueAt() == null) {
                item.setDueAt(Instant.now().plus(stageRoute.defaultDueHours(), java.time.temporal.ChronoUnit.HOURS));
            }
            projectionService.upsert(item, item.getPrioridade() == null ? 50 : Math.max(50, 120 - item.getPrioridade() * 10),
                    List.of(stageRoute.stageToken(), routing.secretariatCode(), stageRoute.cellCode(), "ASSIGNED"));
        }
        workItemRepository.saveAll(items);
        return avaliar(processo, routing, stage);
    }

    private List<WorkItem> locateTargetItems(Long processoId, StageRoute stageRoute) {
        List<WorkItem> items = workItemRepository.findAllByProcesso(processoId).stream()
                .filter(item -> item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO)
                .filter(item -> same(item.getInboxKey(), stageRoute.inboxKey()))
                .filter(item -> same(item.getQueueCode(), stageRoute.queueCode()) || item.getQueueCode() == null || item.getQueueCode().isBlank())
                .sorted(Comparator.comparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (!items.isEmpty()) {
            return items;
        }
        return workItemRepository.findAllByProcesso(processoId).stream()
                .filter(item -> item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO)
                .sorted(Comparator.comparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(1)
                .toList();
    }

    private List<Usuario> resolveCandidates(Processo processo,
                                            SecretariatOperationalRoutingProfile routing,
                                            StageRoute stageRoute) {
        String comarca = processo.getComarca() == null || processo.getComarca().isBlank()
                ? stringValue(routing.metadata().get("comarca"))
                : processo.getComarca();
        List<Usuario> exact = isBlank(comarca) ? List.of() : usuarioRepository.findByComarcaAndAtivoTrue(comarca);
        List<Usuario> base = new ArrayList<>();
        base.addAll(exact);
        if (base.isEmpty()) {
            base.addAll(usuarioRepository.findByTipoUsuario(TipoUsuario.SERVIDOR_FORUM));
            base.addAll(usuarioRepository.findByTipoUsuario(TipoUsuario.SERVIDOR));
        }
        return base.stream()
                .filter(Usuario::isServidorJudiciario)
                .filter(Usuario::isAtivo)
                .filter(candidate -> territorialMatch(candidate, processo))
                .map(candidate -> buildCandidateLoad(processo, routing, stageRoute, candidate))
                .sorted(Comparator.comparingInt(CandidateLoad::rankingScore).reversed().thenComparingInt(CandidateLoad::activeItems).thenComparingInt(CandidateLoad::overdueItems).thenComparing(CandidateLoad::nome))
                .map(CandidateLoad::usuario)
                .limit(8)
                .toList();
    }

    private CandidateLoad buildCandidateLoad(Processo processo,
                                             SecretariatOperationalRoutingProfile routing,
                                             StageRoute stageRoute,
                                             Usuario candidate) {
        long activeLong = workItemRepository.inboxByUser(candidate.getId(), PageRequest.of(0, 1)).getTotalElements();
        long overdueLong = workItemRepository.countOverdueByAssignedUser(candidate.getId(), Instant.now());
        long dueSoonLong = workItemRepository.countDueByAssignedUser(candidate.getId(), Instant.now().plusSeconds(stageRoute.defaultDueHours() * 3600L));
        int specialty = specialtyScore(candidate, routing, processo, stageRoute);
        int territorial = territorialScore(candidate, processo);
        int pressurePenalty = Math.toIntExact(Math.min(200, activeLong * 2 + overdueLong * 5 + dueSoonLong * 3));
        int ranking = Math.max(0, specialty + territorial + 300 - pressurePenalty);
        List<String> reasons = new ArrayList<>();
        reasons.add("Carga ativa: " + activeLong);
        reasons.add("Atrasos: " + overdueLong);
        reasons.add("Vencendo no horizonte do stage: " + dueSoonLong);
        if (specialty > 0) {
            reasons.add("Afinidade por especialidade, lane ou célula da secretaria.");
        }
        if (territorial >= 40) {
            reasons.add("Correspondência territorial exata com a comarca da secretaria.");
        }
        return new CandidateLoad(candidate, candidate.getId(), candidate.getNome(), stageRoute.cellCode(), Math.toIntExact(activeLong), Math.toIntExact(overdueLong), Math.toIntExact(dueSoonLong), ranking, List.copyOf(reasons));
    }

    private int specialtyScore(Usuario candidate,
                               SecretariatOperationalRoutingProfile routing,
                               Processo processo,
                               StageRoute stageRoute) {
        int score = 0;
        String unidadeCodigo = routing.metadata() == null ? null : stringValue(routing.metadata().get("unidadeJudiciariaCodigo"));
        String vara = routing.metadata() == null ? null : stringValue(routing.metadata().get("vara"));
        if (candidate.possuiEspecialidade(routing.ramoAxis())) {
            score += 120;
        }
        if (candidate.possuiEspecialidade(routing.regimeAxis())) {
            score += 80;
        }
        if (candidate.possuiEspecialidade(stageRoute.stageToken())) {
            score += 70;
        }
        if (candidate.possuiEspecialidade(stageRoute.cellCode())) {
            score += 60;
        }
        if (candidate.possuiEspecialidade(routing.secretariatCode())) {
            score += 160;
        }
        if (candidate.possuiEspecialidade(routing.tribunalCodigo())) {
            score += 90;
        }
        if (unidadeCodigo != null && candidate.possuiEspecialidade(unidadeCodigo)) {
            score += 180;
        }
        if (vara != null && candidate.possuiEspecialidade(vara)) {
            score += 130;
        }
        score += profileAffinityScore(candidate, routing, stageRoute, unidadeCodigo, vara);
        if (processo.getClasseProcessual() != null && candidate.possuiEspecialidade(processo.getClasseProcessual())) {
            score += 40;
        }
        if (routing.secrecyAware() && candidate.possuiEspecialidade("SIGILO")) {
            score += 100;
        }
        return score;
    }

    private int profileAffinityScore(Usuario candidate,
                                     SecretariatOperationalRoutingProfile routing,
                                     StageRoute stageRoute,
                                     String unidadeCodigo,
                                     String vara) {
        String perfil = candidate.getPerfil();
        int score = 0;
        if (containsToken(perfil, routing.secretariatCode())) {
            score += 140;
        }
        if (containsToken(perfil, routing.tribunalCodigo())) {
            score += 80;
        }
        if (containsToken(perfil, unidadeCodigo)) {
            score += 170;
        }
        if (containsToken(perfil, vara)) {
            score += 120;
        }
        if (containsToken(perfil, routing.organizationalPath())) {
            score += 110;
        }
        if (containsToken(perfil, stageRoute.cellCode())) {
            score += 100;
        }
        if (containsToken(perfil, stageRoute.queueCode())) {
            score += 70;
        }
        return score;
    }

    private int territorialScore(Usuario candidate, Processo processo) {
        int score = 0;
        if (same(candidate.getComarca(), processo.getComarca())) {
            score += 80;
        }
        if (same(candidate.getUf(), processo.getUf())) {
            score += 30;
        }
        return score;
    }


    private boolean containsToken(String source, String token) {
        if (isBlank(source) || isBlank(token)) {
            return false;
        }
        return normalize(source).contains(normalize(token));
    }

    private boolean territorialMatch(Usuario candidate, Processo processo) {
        return same(candidate.getComarca(), processo.getComarca()) || same(candidate.getUf(), processo.getUf()) || isBlank(processo.getComarca());
    }

    private StageRoute resolveStageRoute(SecretariatOperationalRoutingProfile routing, String stage) {
        String token = normalizeStage(stage);
        java.util.Map<String, Object> tribunalFlow = tribunalFlow(routing);
        java.util.Map<String, Object> queueCodes = nestedMap(tribunalFlow, "queueCodes");
        return switch (token) {
            case "RECEBIMENTO" -> new StageRoute(token, routing.receiptInboxKey(), routing.receiptQueueCode(), routing.secretariatCode() + "_RECEBIMENTO", Math.max(1L, routing.receiptSla().toHours()));
            case "SANEAMENTO" -> new StageRoute(token, routing.saneamentoInboxKey(), routing.saneamentoQueueCode(), routing.secretariatCode() + "_SANEAMENTO", Math.max(2L, routing.saneamentoSla().toHours()));
            case "AUDIENCIA" -> new StageRoute(token, routing.audienceInboxKey(), routing.audienceQueueCode(), routing.secretariatCode() + "_PAUTA", Math.max(1L, routing.audiencePreparationSla().toHours()));
            case "PAUTA" -> new StageRoute(token, routing.executionInboxKey(), firstNonBlank(stringValue(queueCodes.get("pauta")), stringValue(queueCodes.get("publicacaoPauta")), routing.audienceQueueCode(), routing.executionQueueCode()), routing.secretariatCode() + "_PAUTA_COLEGIADA", 12L);
            case "COLEGIADO" -> new StageRoute(token, routing.executionInboxKey(), firstNonBlank(stringValue(queueCodes.get("sessao")), stringValue(queueCodes.get("gabineteRelator")), routing.executionQueueCode()), routing.secretariatCode() + "_COLEGIADO", 12L);
            case "ACORDAO" -> new StageRoute(token, routing.executionInboxKey(), firstNonBlank(stringValue(queueCodes.get("acordao")), routing.executionQueueCode()), routing.secretariatCode() + "_ACORDAO", 18L);
            case "EMBARGOS" -> new StageRoute(token, routing.executionInboxKey(), firstNonBlank(stringValue(queueCodes.get("embargos")), routing.secretariatCode() + ":EMBARGOS", routing.executionQueueCode()), routing.secretariatCode() + "_EMBARGOS", 18L);
            default -> new StageRoute("EXECUCAO", routing.executionInboxKey(), routing.executionQueueCode(), routing.secretariatCode() + "_EXECUCAO", 8L);
        };
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "RECEBIMENTO";
        }
        String normalized = stage.trim().toUpperCase(Locale.ROOT).replace('Ç', 'C');
        if (normalized.startsWith("SANE")) {
            return "SANEAMENTO";
        }
        if (normalized.startsWith("AUD")) {
            return "AUDIENCIA";
        }
        if (normalized.startsWith("PAUTA")) {
            return "PAUTA";
        }
        if (normalized.startsWith("COLEG") || normalized.startsWith("SESSA") || normalized.startsWith("RELAT")) {
            return "COLEGIADO";
        }
        if (normalized.startsWith("ACOR")) {
            return "ACORDAO";
        }
        if (normalized.startsWith("EMBAR")) {
            return "EMBARGOS";
        }
        if (normalized.startsWith("EXEC")) {
            return "EXECUCAO";
        }
        return "RECEBIMENTO";
    }

    private static boolean same(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }


    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> tribunalFlow(SecretariatOperationalRoutingProfile routing) {
        if (routing == null || routing.metadata() == null) {
            return java.util.Map.of();
        }
        Object raw = routing.metadata().get("tribunalFlow");
        return raw instanceof java.util.Map<?, ?> map ? (java.util.Map<String, Object>) map : java.util.Map.of();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> nestedMap(java.util.Map<String, Object> source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return java.util.Map.of();
        }
        Object raw = source.get(key);
        return raw instanceof java.util.Map<?, ?> map ? (java.util.Map<String, Object>) map : java.util.Map.of();
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

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    public record AssignmentSnapshot(
            String stage,
            String cellCode,
            List<Long> targetWorkItemIds,
            Usuario primary,
            Usuario backup,
            List<CandidateLoad> candidates,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record CandidateLoad(
            Usuario usuario,
            Long usuarioId,
            String nome,
            String cellCode,
            int activeItems,
            int overdueItems,
            int dueSoonItems,
            int rankingScore,
            List<String> reasons
    ) {
    }

    private record StageRoute(
            String stageToken,
            String inboxKey,
            String queueCode,
            String cellCode,
            long defaultDueHours
    ) {
    }
}
