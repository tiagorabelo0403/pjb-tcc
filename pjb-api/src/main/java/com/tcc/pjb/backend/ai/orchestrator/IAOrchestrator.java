package com.tcc.pjb.backend.ai.orchestrator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionTimedOutException;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.skills.IASkill;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public final class IAOrchestrator {

    private final List<IASkill> skills;
    private final Semaphore parallelism;
    private final long globalTimeoutMs;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public IAOrchestrator(List<IASkill> skills, Environment env, PjbExecutionOrchestrator executionOrchestrator) {
        this.skills = List.copyOf(Objects.requireNonNull(skills, "skills"));
        Objects.requireNonNull(env, "env");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        int maxParallel = Integer.parseInt(env.getProperty("pjb.ai.orchestrator.maxParallel", "64"));
        if (maxParallel < 1 || maxParallel > 10_000) {
            throw new IllegalArgumentException("maxParallel out of range");
        }
        this.parallelism = new Semaphore(maxParallel, true);
        long gt = Long.parseLong(env.getProperty("pjb.ai.orchestrator.globalTimeoutMs", "3500"));
        if (gt < 50 || gt > 300_000) {
            throw new IllegalArgumentException("globalTimeoutMs out of range");
        }
        this.globalTimeoutMs = gt;
    }

    public IAResponse processar(IARequest request) {
        Objects.requireNonNull(request, "request");
        Map<String, Object> baseContext = Map.of(
                "requestId", request.getRequestId(),
                "origem", request.getOrigem(),
                "acao", request.getAcao()
        );
        List<IASkill> eligible = skills.stream()
                .filter(skill -> safeSupports(skill, request))
                .toList();
        if (eligible.isEmpty()) {
            return IAResponse.builder()
                    .origem("IA_ORCHESTRATOR")
                    .status(IAResponse.StatusIA.INDETERMINADO)
                    .texto("Nenhuma skill compatível com a ação solicitada.")
                    .confianca(0.4)
                    .dataGeracao(Instant.now())
                    .metadado("acao", request.getAcao())
                    .build();
        }
        Instant deadline = Instant.now().plusMillis(globalTimeoutMs);
        List<SkillRun> runs = new ArrayList<>(eligible.size());
        try {
            List<CompletableFuture<SkillRun>> tasks = new ArrayList<>(eligible.size());
            for (int i = 0; i < eligible.size(); i++) {
                IASkill skill = eligible.get(i);
                int order = i;
                CompletableFuture<SkillRun> task = executionOrchestrator
                        .supply(PjbExecutionDescriptor.burst("ai.orchestrator.skill." + safeSkillName(skill), Duration.ofMillis(globalTimeoutMs)),
                                () -> runSkill(order, skill, request, baseContext))
                        .exceptionally(ex -> {
                            Throwable cause = unwrap(ex);
                            if (cause instanceof PjbExecutionTimedOutException) {
                                return SkillRun.timeout(order, safeSkillName(skill), globalTimeoutMs);
                            }
                            return SkillRun.failed(order, safeSkillName(skill), cause, globalTimeoutMs);
                        });
                tasks.add(task);
            }
            for (CompletableFuture<SkillRun> task : tasks) {
                long remainingMs = Math.max(1L, Duration.between(Instant.now(), deadline).toMillis());
                try {
                    runs.add(task.get(remainingMs, TimeUnit.MILLISECONDS));
                } catch (TimeoutException ex) {
                    task.cancel(true);
                    runs.add(SkillRun.timeout(-1, "GLOBAL_TIMEOUT", globalTimeoutMs));
                }
            }
        } catch (Exception ex) {
            runs.add(SkillRun.failed(-1, "SCOPE", unwrap(ex), globalTimeoutMs));
        }
        return consolidate(request, runs);
    }

    private SkillRun runSkill(int order, IASkill skill, IARequest request, Map<String, Object> baseContext) {
        long start = System.nanoTime();
        boolean acquired = false;
        try {
            parallelism.acquire();
            acquired = true;
            Map<String, Object> ctx = Collections.unmodifiableMap(baseContext);
            IAResponse response = skill.executar(request, ctx);
            long dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return SkillRun.success(order, safeSkillName(skill), response, dur);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            long dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return SkillRun.failed(order, safeSkillName(skill), ex, dur);
        } catch (Exception ex) {
            long dur = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return SkillRun.failed(order, safeSkillName(skill), ex, dur);
        } finally {
            if (acquired) {
                parallelism.release();
            }
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String safeSkillName(IASkill skill) {
        try {
            return skill == null ? "NULL" : String.valueOf(skill.getNome());
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    private static boolean safeSupports(IASkill skill, IARequest request) {
        try {
            return skill != null && skill.suporta(request);
        } catch (Exception ex) {
            return false;
        }
    }

    private IAResponse consolidate(IARequest request, List<SkillRun> runs) {
        IAResponse consolidated = IAResponse.builder()
                .origem("IA_ORCHESTRATOR")
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(1.0)
                .dataGeracao(Instant.now())
                .build();
        List<SkillRun> ordered = runs.stream()
                .sorted(Comparator.comparingInt(SkillRun::order).thenComparing(SkillRun::skillName))
                .toList();
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        for (SkillRun run : ordered) {
            diagnostics.put(run.skillName(), run.diagnostics());
            IAResponse partial = run.response();
            if (partial == null) {
                if (run.timedOut()) {
                    consolidated = consolidated
                            .adicionarAlerta("Timeout na skill: " + run.skillName())
                            .toBuilder()
                            .status(resolveStatus(consolidated.getStatus(), IAResponse.StatusIA.ALERTA))
                            .confianca(Math.min(Optional.ofNullable(consolidated.getConfianca()).orElse(1.0), 0.55))
                            .build();
                } else {
                    consolidated = consolidated
                            .adicionarAlerta("Falha na skill: " + run.skillName())
                            .toBuilder()
                            .status(resolveStatus(consolidated.getStatus(), IAResponse.StatusIA.ALERTA))
                            .confianca(Math.min(Optional.ofNullable(consolidated.getConfianca()).orElse(1.0), 0.45))
                            .build();
                }
                continue;
            }
            consolidated = merge(consolidated, partial);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("acao", request.getAcao());
        meta.put("diagnostics", diagnostics);
        return consolidated.toBuilder()
                .metadado("orchestrator", meta)
                .build();
    }

    private static IAResponse merge(IAResponse base, IAResponse partial) {
        IAResponse.StatusIA status = resolveStatus(base.getStatus(), partial.getStatus());
        Double confidence = minNonNull(base.getConfianca(), partial.getConfianca());
        IAResponse.IAResponseBuilder builder = base.toBuilder()
                .status(status)
                .confianca(confidence);
        if (partial.getTexto() != null && !partial.getTexto().isBlank()) {
            String current = Optional.ofNullable(base.getTexto()).orElse("");
            String addition = partial.getTexto();
            builder.texto(current.isBlank() ? addition : current + "\n\n" + addition);
        }
        if (partial.getAlertas() != null && !partial.getAlertas().isEmpty()) {
            for (String alerta : partial.getAlertas()) {
                builder.adicionarAlerta(alerta);
            }
        }
        if (partial.getMetadados() != null && !partial.getMetadados().isEmpty()) {
            for (Map.Entry<String, Object> entry : partial.getMetadados().entrySet()) {
                builder.metadado(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    private static IAResponse.StatusIA resolveStatus(IAResponse.StatusIA current, IAResponse.StatusIA incoming) {
        if (current == null) {
            return incoming == null ? IAResponse.StatusIA.SUCESSO : incoming;
        }
        if (incoming == null) {
            return current;
        }
        if (current == IAResponse.StatusIA.ERRO || incoming == IAResponse.StatusIA.ERRO) {
            return IAResponse.StatusIA.ERRO;
        }
        if (current == IAResponse.StatusIA.ALERTA || incoming == IAResponse.StatusIA.ALERTA) {
            return IAResponse.StatusIA.ALERTA;
        }
        if (current == IAResponse.StatusIA.INDETERMINADO || incoming == IAResponse.StatusIA.INDETERMINADO) {
            return IAResponse.StatusIA.INDETERMINADO;
        }
        return IAResponse.StatusIA.SUCESSO;
    }

    private static Double minNonNull(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.min(a, b);
    }

    private record SkillRun(int order,
                            String skillName,
                            IAResponse response,
                            boolean timedOut,
                            long elapsedMs,
                            Throwable error) {

        private static SkillRun success(int order, String skillName, IAResponse response, long elapsedMs) {
            return new SkillRun(order, skillName, response, false, elapsedMs, null);
        }

        private static SkillRun failed(int order, String skillName, Throwable error, long elapsedMs) {
            return new SkillRun(order, skillName, null, false, elapsedMs, error);
        }

        private static SkillRun timeout(int order, String skillName, long elapsedMs) {
            return new SkillRun(order, skillName, null, true, elapsedMs, null);
        }

        private Map<String, Object> diagnostics() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("timedOut", timedOut);
            out.put("elapsedMs", elapsedMs);
            if (error != null) {
                out.put("errorType", error.getClass().getName());
                out.put("errorMessage", error.getMessage());
            }
            return out;
        }
    }
}
