package com.tcc.pjb.backend.core.actor;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Component
public class ActorAssignmentEngine {

    private static final Duration BATCH_ASSIGNMENT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration BATCH_AWAIT_GRACE = Duration.ofMillis(250);

    private final PjbExecutionOrchestrator executionOrchestrator;

    
    public enum ActorCategory { INTERNO, EXTERNO }

    
    public record WorkItemSpec(
            String id,
            String code,
            WorkItemType type,
            String title,
            String description,
            TipoUsuario actorRole,
            ActorCategory actorCategory,
            int slotAdvogado,
            FaseProcessual fase,
            boolean blocking,
            boolean required,
            int slaDays,
            WorkItemStatus initialStatus,
            List<String> legalBases,
            Instant deadline,
            Map<String, Object> metadata
    ) {
        public WorkItemSpec {
            type = type == null ? WorkItemType.OUTRO : type;
            actorRole = actorRole == null ? TipoUsuario.SERVIDOR_FORUM : actorRole;
            actorCategory = actorCategory == null ? ActorCategory.INTERNO : actorCategory;
            fase = fase == null ? FaseProcessual.CONHECIMENTO : fase;
            initialStatus = initialStatus == null ? WorkItemStatus.PENDENTE : initialStatus;
            legalBases = legalBases == null ? List.of() : List.copyOf(legalBases);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        }

        public boolean isExternal() { return actorCategory == ActorCategory.EXTERNO; }
        public boolean isAdvogado() { return actorRole == TipoUsuario.ADVOGADO; }
        public boolean isMP()       { return actorRole == TipoUsuario.MEMBRO_MINISTERIO_PUBLICO; }
        public boolean isDefensor() { return actorRole == TipoUsuario.DEFENSOR_PUBLICO; }
    }

    
    public record AssignmentResult(
            Long processoId,
            RitoProcessual rito,
            FaseProcessual fase,
            List<WorkItemSpec> workItems,
            List<String> diagnostics,
            boolean hasExternalItems,
            int totalBlocking,
            Instant generatedAt
    ) {
        public AssignmentResult(Long processoId, RitoProcessual rito, FaseProcessual fase, List<WorkItemSpec> workItems, List<String> diagnostics, java.util.Map<String, Object> metadata, boolean hasExternalItems) {
            this(processoId, rito, fase, workItems, diagnostics, hasExternalItems, 0, java.time.Instant.now());
        }

        public List<WorkItemSpec> advogadoItems() {
            return workItems.stream().filter(WorkItemSpec::isAdvogado).toList();
        }
        public List<WorkItemSpec> mpItems() {
            return workItems.stream().filter(WorkItemSpec::isMP).toList();
        }
        public List<WorkItemSpec> internos() {
            return workItems.stream().filter(w -> !w.isExternal()).toList();
        }
    }

    
    public record AssignmentContext(
            Long processoId,
            RitoProcessual rito,
            FaseProcessual fase,
            int qtdAdvogados,
            boolean temMP,
            boolean temDefensorPublico,
            boolean temProcuradoria,
            boolean temAssistenteAcusacao,
            boolean recurso,
            boolean emenda,
            List<String> extraRoles,
            Map<String, Object> extras
    ) {
        public AssignmentContext {
            extraRoles = extraRoles == null ? List.of() : List.copyOf(extraRoles);
            extras = extras == null ? Map.of() : Map.copyOf(extras);
        }

        public static AssignmentContext of(Long processoId, RitoProcessual rito, FaseProcessual fase) {
            return new AssignmentContext(processoId, rito, fase, 1, false, false, false, false, false, false,
                    List.of(), Map.of());
        }
    }

    
    public ActorAssignmentEngine(PjbExecutionOrchestrator executionOrchestrator) {
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    public AssignmentResult assign(AssignmentContext ctx) {
        Objects.requireNonNull(ctx, "AssignmentContext");
        Objects.requireNonNull(ctx.rito(), "rito");
        Objects.requireNonNull(ctx.fase(), "fase");

        DefinitionSnapshot snapshot = ProceduralCatalogSupport.snapshot(ctx.rito());
        RitoStage stage = findStage(snapshot, ctx.fase());

        List<WorkItemSpec> specs = new ArrayList<>(32);
        List<String> diagnostics = new ArrayList<>(8);

        if (stage == null) {
            diagnostics.add("AVISO: Fase '" + ctx.fase().name() + "' não mapeada no catálogo para o rito '" +
                    ctx.rito().name() + "'. Nenhum work item gerado.");
            return result(ctx, specs, diagnostics);
        }

        List<WorkTemplate> templates = stage.getWork();
        if (templates == null || templates.isEmpty()) {
            diagnostics.add("AVISO: Stage '" + ctx.fase().name() + "' sem work templates definidos para rito '" +
                    ctx.rito().name() + "'.");
            return result(ctx, specs, diagnostics);
        }

        for (WorkTemplate t : templates) {
            if (t == null || t.getCode() == null) continue;
            TipoUsuario role = resolveRole(t.getActorRole(), diagnostics);
            ActorCategory category = categorize(role);
            int slotAdv = resolveSlot(t, ctx, role);
            Instant deadline = computeDeadline(t.getSlaDays());

            WorkItemSpec spec = new WorkItemSpec(
                    UUID.randomUUID().toString(),
                    t.getCode(),
                    resolveType(t.getType()),
                    t.getTitle() != null ? t.getTitle() : humanize(t.getCode()),
                    t.getDescription() != null ? t.getDescription() : t.getTitle(),
                    role,
                    category,
                    slotAdv,
                    ctx.fase(),
                    Boolean.TRUE.equals(t.getBlocking()),
                    Boolean.TRUE.equals(t.getBlocking()),
                    t.getSlaDays() != null ? t.getSlaDays() : defaultSla(role),
                    WorkItemStatus.PENDENTE,
                    t.getLegalBases() != null ? List.copyOf(t.getLegalBases()) : List.of(),
                    deadline,
                    buildMetadata(t, ctx)
            );
            specs.add(spec);
        }

        enrichWithMpIfRequired(ctx, snapshot, stage, specs, diagnostics);

        ensureAdvogadoProtocolo(ctx, specs, diagnostics);

        return result(ctx, specs, diagnostics);
    }

    
    public List<AssignmentResult> assignBatch(List<AssignmentContext> contexts) {
        Objects.requireNonNull(contexts, "contexts");
        List<CompletableFuture<AssignmentResult>> futures = new ArrayList<>(contexts.size());
        for (AssignmentContext ctx : contexts) {
            CompletableFuture<AssignmentResult> future = executionOrchestrator
                    .supply(PjbExecutionDescriptor.burst("actor-assignment.batch", BATCH_ASSIGNMENT_TIMEOUT), () -> assign(ctx))
                    .completeOnTimeout(timeoutResult(ctx), BATCH_ASSIGNMENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> failedResult(ctx, ex));
            futures.add(future);
        }
        awaitBatch(futures);
        List<AssignmentResult> results = new ArrayList<>(futures.size());
        for (int index = 0; index < futures.size(); index++) {
            AssignmentContext ctx = contexts.get(index);
            results.add(futures.get(index).getNow(timeoutResult(ctx)));
        }
        return List.copyOf(results);
    }

    private void awaitBatch(List<CompletableFuture<AssignmentResult>> futures) {
        if (futures.isEmpty()) {
            return;
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.orTimeout(BATCH_ASSIGNMENT_TIMEOUT.plus(BATCH_AWAIT_GRACE).toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (RuntimeException e) {
            cancelPending(futures);
        }
    }

    private void cancelPending(List<CompletableFuture<AssignmentResult>> futures) {
        futures.stream()
                .filter(future -> !future.isDone())
                .forEach(future -> future.cancel(true));
    }

    
    private AssignmentResult timeoutResult(AssignmentContext ctx) {
        return result(ctx, List.of(), List.of("Atribuição em lote degradada por timeout controlado."));
    }

    private AssignmentResult failedResult(AssignmentContext ctx, Throwable ex) {
        String message = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Falha assíncrona durante a atribuição em lote."
                : "Falha assíncrona durante a atribuição em lote: " + ex.getMessage();
        return result(ctx, List.of(), List.of(message));
    }

    public WorkItemSpec emendaDocumental(Long processoId, RitoProcessual rito, FaseProcessual fase,
                                          int slotAdvogado) {
        return new WorkItemSpec(
                UUID.randomUUID().toString(),
                "EMENDA_DOCUMENTAL_ADV_" + slotAdvogado,
                WorkItemType.JUNTADA,
                "Emenda ou complementação documental",
                "Cumprir determinação de emenda, saneamento ou complementação de documentos " +
                "exigidos pelo cartório ou pelo juízo.",
                TipoUsuario.ADVOGADO,
                ActorCategory.EXTERNO,
                slotAdvogado,
                fase,
                true,
                true,
                5,
                WorkItemStatus.PENDENTE,
                List.of(),
                computeDeadline(5),
                metadata(processoId, rito, null, "EMENDA")
        );
    }

    
    public WorkItemSpec recursalAdvogado(Long processoId, RitoProcessual rito, int slotAdvogado) {
        return new WorkItemSpec(
                UUID.randomUUID().toString(),
                "RECURSO_ADV_" + slotAdvogado,
                WorkItemType.RECURSO,
                "Interposição de recurso cabível",
                "Interpor o recurso cabível com regularidade formal (tempestividade, preparo, motivação).",
                TipoUsuario.ADVOGADO,
                ActorCategory.EXTERNO,
                slotAdvogado,
                FaseProcessual.RECURSAL,
                true,
                true,
                5,
                WorkItemStatus.PENDENTE,
                List.of(),
                computeDeadline(15),
                metadata(processoId, rito, null, "RECURSO")
        );
    }

    
    public WorkItemSpec contrarrazoesAdvogado(Long processoId, RitoProcessual rito, int slotAdvogado) {
        return new WorkItemSpec(
                UUID.randomUUID().toString(),
                "CONTRARRAZOES_ADV_" + slotAdvogado,
                WorkItemType.MANIFESTACAO,
                "Apresentação de contrarrazões recursais",
                "Apresentar contrarrazões ou resposta ao recurso interposto pela parte contrária.",
                TipoUsuario.ADVOGADO,
                ActorCategory.EXTERNO,
                slotAdvogado,
                FaseProcessual.RECURSAL,
                false,
                false,
                15,
                WorkItemStatus.PENDENTE,
                List.of(),
                computeDeadline(15),
                metadata(processoId, rito, null, "CONTRARRAZOES")
        );
    }

    
    public List<String> diagnoseExternalCoverage(RitoProcessual rito, List<WorkItemSpec> items) {
        List<String> problems = new ArrayList<>();
        DefinitionSnapshot snapshot = ProceduralCatalogSupport.snapshot(rito);
        boolean hasExternalParty = snapshot.parties().stream().anyMatch(PartyRoleSpec::external);
        if (!hasExternalParty) return problems;

        boolean hasAdvogado = items.stream().anyMatch(WorkItemSpec::isAdvogado);
        boolean hasMP = items.stream().anyMatch(WorkItemSpec::isMP);

        if (!hasAdvogado) {
            problems.add("ACTOR_GAP: Rito '" + rito.name() + "' tem partes externas mas nenhum " +
                    "work item atribuído a ADVOGADO. O painel do advogado ficará vazio.");
        }
        boolean ritoExigeMP = rito.isMilitar() || rito.isEleitoral()
                || rito.name().startsWith("PENAL");
        if (ritoExigeMP && !hasMP) {
            problems.add("ACTOR_GAP: Rito '" + rito.name() + "' exige participação do MP mas " +
                    "nenhum work item foi atribuído a MEMBRO_MINISTERIO_PUBLICO.");
        }
        return problems;
    }

    
    public Map<String, Object> coverageReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        int totalProblems = 0;
        for (RitoProcessual rito : ProceduralCatalogSupport.catalogDrivenRitos()) {
            AssignmentContext ctx = AssignmentContext.of(0L, rito, FaseProcessual.CONHECIMENTO);
            AssignmentResult result = assign(ctx);
            List<String> diag = diagnoseExternalCoverage(rito, result.workItems());
            totalProblems += diag.size();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rito", rito.name());
            row.put("hasAdvogadoItem", result.workItems().stream().anyMatch(WorkItemSpec::isAdvogado));
            row.put("hasMpItem", result.workItems().stream().anyMatch(WorkItemSpec::isMP));
            row.put("totalItems", result.workItems().size());
            row.put("externalItems", result.workItems().stream().filter(WorkItemSpec::isExternal).count());
            row.put("problems", diag);
            rows.add(row);
        }
        report.put("totalRitos", ProceduralCatalogSupport.catalogDrivenRitos().size());
        report.put("totalProblemas", totalProblems);
        report.put("ritos", rows);
        return report;
    }


    private Map<String, Object> metadata(Long processoId, RitoProcessual rito, FaseProcessual fase, String tipo) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (processoId != null) {
            out.put("processoId", processoId);
        }
        if (rito != null) {
            out.put("rito", rito.name());
        }
        if (fase != null) {
            out.put("fase", fase.name());
        }
        if (tipo != null && !tipo.isBlank()) {
            out.put("tipo", tipo);
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private void enrichWithMpIfRequired(AssignmentContext ctx, DefinitionSnapshot snapshot,
                                         RitoStage stage, List<WorkItemSpec> specs,
                                         List<String> diagnostics) {
        boolean mpExternoNoPolo = snapshot.parties().stream()
                .anyMatch(p -> p.code().startsWith("MINISTERIO_PUBLICO") && p.external());
        boolean mpJaTemTask = specs.stream().anyMatch(WorkItemSpec::isMP);
        if (!mpExternoNoPolo || mpJaTemTask) return;
        if (!ctx.temMP()) {
            diagnostics.add("INFO: MP não presente no contexto — task de MP não criada. " +
                    "Se o MP atuar como custos legis, adicione temMP=true.");
            return;
        }
        specs.add(new WorkItemSpec(
                UUID.randomUUID().toString(),
                "MANIFESTACAO_MP_" + ctx.rito().name(),
                WorkItemType.MANIFESTACAO,
                "Manifestação do Ministério Público",
                "Manifestação ministerial quando o MP atuar como parte ou custos legis.",
                TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                ActorCategory.EXTERNO,
                1,
                ctx.fase(),
                false,
                false,
                5,
                WorkItemStatus.PENDENTE,
                List.of(),
                computeDeadline(5),
                metadata(null, ctx.rito(), ctx.fase(), null)
        ));
        diagnostics.add("INFO: Work item de MP gerado automaticamente pois MP está no polo.");
    }

    private void ensureAdvogadoProtocolo(AssignmentContext ctx, List<WorkItemSpec> specs,
                                          List<String> diagnostics) {
        boolean jaTemProtocolo = specs.stream()
                .anyMatch(s -> s.isAdvogado()
                        && (s.type() == WorkItemType.MANIFESTACAO || s.type() == WorkItemType.RECURSO));
        if (!jaTemProtocolo && ctx.fase() == FaseProcessual.CONHECIMENTO) {
            diagnostics.add("WORKFLOW_BLUEPRINT_INCOMPLETE: nenhum work item de protocolo para ADVOGADO encontrado no catálogo do rito "
                    + ctx.rito().name() + ".");
        }
    }

    private RitoStage findStage(DefinitionSnapshot snapshot, FaseProcessual fase) {
        return snapshot.stages().stream()
                .filter(s -> fase.name().equalsIgnoreCase(s.getFase()))
                .findFirst().orElse(null);
    }

    private TipoUsuario resolveRole(String raw, List<String> diagnostics) {
        if (raw == null || raw.isBlank()) return TipoUsuario.SERVIDOR_FORUM;
        try {
            return TipoUsuario.valueOf(raw.toUpperCase(Locale.ROOT).strip());
        } catch (IllegalArgumentException e) {
            diagnostics.add("WARN: actorRole '" + raw + "' não reconhecido. Usando SERVIDOR_FORUM.");
            return TipoUsuario.SERVIDOR_FORUM;
        }
    }

    private ActorCategory categorize(TipoUsuario role) {
        return switch (role) {
            case ADVOGADO, MEMBRO_MINISTERIO_PUBLICO, DEFENSOR_PUBLICO,
                 PROCURADORIA_FEDERAL, PROCURADORIA_ESTADUAL, PROCURADORIA_MUNICIPAL,
                 CIDADAO -> ActorCategory.EXTERNO;
            default -> ActorCategory.INTERNO;
        };
    }

    private int resolveSlot(WorkTemplate t, AssignmentContext ctx, TipoUsuario role) {
        if (role != TipoUsuario.ADVOGADO) return 1;
        Integer priority = t.getPriority();
        if (priority != null && priority > 1 && ctx.qtdAdvogados() >= priority) return priority;
        return 1;
    }

    private WorkItemType resolveType(String raw) {
        if (raw == null) return WorkItemType.OUTRO;
        try { return WorkItemType.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return WorkItemType.OUTRO; }
    }

    private int defaultSla(TipoUsuario role) {
        return switch (role) {
            case MAGISTRADO, JUIZ, DESEMBARGADOR, MINISTRO -> 5;
            case ADVOGADO, DEFENSOR_PUBLICO -> 15;
            case MEMBRO_MINISTERIO_PUBLICO -> 10;
            default -> 3;
        };
    }

    private Instant computeDeadline(int slaDays) {
        return Instant.now().plus(Duration.ofDays(Math.max(1, slaDays)));
    }

    private Map<String, Object> buildMetadata(WorkTemplate t, AssignmentContext ctx) {
        Map<String, Object> m = new LinkedHashMap<>(8);
        m.put("rito", ctx.rito().name());
        m.put("fase", ctx.fase().name());
        m.put("processoId", ctx.processoId());
        m.put("legalBases", t.getLegalBases() != null ? t.getLegalBases() : List.of());
        return safeCopy(m);
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (input == null || input.isEmpty()) return copy;
        input.forEach((k, v) -> { if (k != null) copy.put(k, v); });
        return copy;
    }

    private AssignmentResult result(AssignmentContext ctx,
                                     List<WorkItemSpec> specs,
                                     List<String> diagnostics) {
        return new AssignmentResult(
                ctx.processoId(),
                ctx.rito(),
                ctx.fase(),
                List.copyOf(specs),
                List.copyOf(diagnostics),
                specs.stream().anyMatch(WorkItemSpec::isExternal),
                (int) specs.stream().filter(WorkItemSpec::blocking).count(),
                Instant.now()
        );
    }

    private String humanize(String token) {
        return token == null ? "" : java.util.Arrays.stream(token.split("_"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                .reduce((a, b) -> a + " " + b).orElse(token);
    }
}
