package com.tcc.pjb.backend.ai.agentic.core;

import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.agents.common.SynthesizerAgent;
import com.tcc.pjb.backend.ai.scope.MateriaDecision;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Locale;
import com.tcc.pjb.backend.core.util.PayloadMaps;

@Service
public class AgenticOrchestrator {

    private final List<Agent> agents;
    private final SynthesizerAgent synthesizer;
    private final AgenticRequestRouter router;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public AgenticOrchestrator(List<Agent> agents, SynthesizerAgent synthesizer, AgenticRequestRouter router,
                               PjbExecutionOrchestrator executionOrchestrator) {
        this.agents = PayloadMaps.copyListWithoutNulls(agents);
        this.synthesizer = synthesizer;
        this.router = router;
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    public AgentResult run(AgenticRunRequest request) {
        AgenticRoutingDecision decision = router.route(request);

        List<Agent> selected = selectAgents(decision, request);
        List<AgentResult> results = executeAgents(selected, request);

        SynthesizerAgent.SynthesisResult synthesis = synthesizer.synthesize(decision, request, results);

        double confidence = aggregateConfidence(results, decision);
        List<String> actions = aggregateActions(results);

        boolean needsHuman = needsHumanApproval(request, decision, confidence, actions);

        AgentResult out = new AgentResult();
        out.setAgent("AgenticOrchestrator");
        out.setConfidence(confidence);
        out.setHumanReviewRequired(needsHuman);
        out.setData(Map.of(
                "domain", decision.domain().name(),
                "materia", decision.materia().materia().name(),
                "materiaConfidence", decision.materia().confidence(),
                "sigilo", decision.sigilo().nivel().name(),
                "sigiloScore", decision.sigilo().score(),
                "sigiloSignals", decision.sigilo().signals().stream().map(Enum::name).toList(),
                "report", synthesis.report(),
                "explainability", synthesis.explainability()
        ));
        return out;
    }

    private List<Agent> selectAgents(AgenticRoutingDecision decision, AgenticRunRequest request) {
        Map<String, Agent> byName = new LinkedHashMap<>();
        for (Agent a : agents) {
            if (a == null) continue;
            String n = a.name();
            if (n == null || n.isBlank()) continue;
            byName.putIfAbsent(n, a);
        }

        List<String> desired = new ArrayList<>(8);
        if (decision.domain() == AgenticDomain.FINANCE) {
            desired.add("FinancialRatiosAgent");
            desired.add("CashFlowStressAgent");
            desired.add("ComplianceAgent");
        } else {
            desired.add("ComplianceAgent");
            desired.add("JurisprudenceAgent");
            if (looksContract(request)) desired.add("ContractAnalysisAgent");
            if (looksTax(decision, request)) desired.add("TaxAgent");
        }

        ArrayList<Agent> out = new ArrayList<>(desired.size());
        for (String n : desired) {
            Agent a = byName.get(n);
            if (a != null) out.add(a);
        }

        if (out.isEmpty() && !agents.isEmpty()) {
            out.addAll(agents.stream().filter(Objects::nonNull).limit(4).toList());
        }

        return List.copyOf(out);
    }

    private static boolean looksContract(AgenticRunRequest request) {
        if (request == null) return false;
        String t = String.valueOf(request.getTask()).toLowerCase(Locale.ROOT);
        if (t.contains("contrato") || t.contains("clausula") || t.contains("cláusula")) return true;
        Map<String, Object> in = request.getInput();
        if (in == null) return false;
        Object docType = in.get("documentType");
        if (docType != null && String.valueOf(docType).toLowerCase(Locale.ROOT).contains("contrat")) return true;
        return in.containsKey("contractText") || in.containsKey("clausulas");
    }

    private static boolean looksTax(AgenticRoutingDecision decision, AgenticRunRequest request) {
        MateriaJurisdicao m = decision != null && decision.materia() != null ? decision.materia().materia() : MateriaJurisdicao.MULTIMATERIA;
        if (m == MateriaJurisdicao.TRIBUTARIA || m == MateriaJurisdicao.EXECUCAO_FISCAL) return true;
        if (request == null) return false;
        String t = String.valueOf(request.getTask()).toLowerCase(Locale.ROOT);
        return t.contains("icms") || t.contains("iss") || t.contains("irpf") || t.contains("irpj") || t.contains("execucao fiscal") || t.contains("execução fiscal");
    }

    private List<AgentResult> executeAgents(List<Agent> selected, AgenticRunRequest request) {
        if (selected == null || selected.isEmpty()) {
            return List.of();
        }

        Duration timeout = Duration.ofSeconds(8);

        List<CompletableFuture<AgentResult>> futures = new ArrayList<>(selected.size());
        for (Agent a : selected) {
            futures.add(executionOrchestrator.supply(
                            PjbExecutionDescriptor.burst("agentic.orchestrator." + safeAgentName(a), timeout),
                            () -> safeExecute(a, request))
                    .exceptionally(ex -> errorResult(a, ex)));
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.get(timeout.plusMillis(250).toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            cancelAll(futures);
        } catch (TimeoutException | ExecutionException | CompletionException ex) {
            cancelAll(futures);
        }

        ArrayList<AgentResult> results = new ArrayList<>(selected.size());
        for (CompletableFuture<AgentResult> f : futures) {
            try {
                AgentResult r = f.getNow(null);
                if (r != null) results.add(r);
            } catch (Exception ignored) {
            }
        }

        results.sort(Comparator.comparing(AgentResult::getAgent, Comparator.nullsLast(String::compareTo)));
        return List.copyOf(results);
    }

    private static void cancelAll(List<CompletableFuture<AgentResult>> futures) {
        if (futures == null || futures.isEmpty()) {
            return;
        }
        for (CompletableFuture<AgentResult> future : futures) {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private static String safeAgentName(Agent agent) {
        String raw = agent == null || agent.name() == null || agent.name().isBlank() ? "unknown-agent" : agent.name();
        return raw.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static AgentResult safeExecute(Agent agent, AgenticRunRequest request) {
        AgentResult r;
        try {
            r = agent.execute(request);
        } catch (Exception e) {
            return errorResult(agent, e);
        }
        if (r == null) {
            r = new AgentResult();
            r.setAgent(agent.name());
            r.setConfidence(0.0);
            r.setHumanReviewRequired(true);
            r.setData(Map.of("error", "null_result"));
        }
        if (r.getAgent() == null || r.getAgent().isBlank()) {
            r.setAgent(agent.name());
        }
        return r;
    }

    private static AgentResult errorResult(Agent agent, Throwable ex) {
        AgentResult r = new AgentResult();
        r.setAgent(agent != null ? agent.name() : "unknown_agent");
        r.setConfidence(0.0);
        r.setHumanReviewRequired(true);
        r.setData(Map.of(
                "error", "agent_failed",
                "message", ex == null ? "unknown" : String.valueOf(ex.getMessage())
        ));
        return r;
    }

    private static double aggregateConfidence(List<AgentResult> results, AgenticRoutingDecision decision) {
        if (results == null || results.isEmpty()) {
            double base = 0.55;
            if (decision != null && decision.materia() != null) {
                base = 0.35 + 0.65 * decision.materia().confidence();
            }
            return Math.max(0.05, Math.min(0.92, base));
        }

        double sum = 0.0;
        double wsum = 0.0;
        for (AgentResult r : results) {
            double c = clamp01(r.getConfidence());
            double w = 0.75 + 0.25 * c;
            sum += c * w;
            wsum += w;
        }
        double avg = wsum <= 0.0 ? 0.6 : sum / wsum;

        MateriaDecision m = decision != null ? decision.materia() : null;
        if (m != null) {
            avg = 0.80 * avg + 0.20 * clamp01(m.confidence());
        }

        return Math.max(0.05, Math.min(0.98, avg));
    }

    private static List<String> aggregateActions(List<AgentResult> results) {
        if (results == null || results.isEmpty()) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (AgentResult r : results) {
            Object a = r.getData() != null ? r.getData().get("actions") : null;
            if (a instanceof List<?> list) {
                for (Object x : list) {
                    if (x == null) continue;
                    String s = String.valueOf(x).trim();
                    if (!s.isBlank()) out.add(s);
                }
            }
        }
        return out.stream().toList();
    }

    private static boolean needsHumanApproval(AgenticRunRequest request,
                                             AgenticRoutingDecision decision,
                                             double confidence,
                                             List<String> actions) {
        HumanInLoopPolicy policy = request != null ? request.getPolicy() : null;
        if (policy == null) policy = HumanInLoopPolicy.defaultPolicy();

        NivelSigilo nivel = decision != null && decision.sigilo() != null ? decision.sigilo().nivel() : NivelSigilo.PUBLICO;
        if (nivel.exigeCredencial()) {
            return true;
        }

        return policy.needsHumanApproval(confidence, actions);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
