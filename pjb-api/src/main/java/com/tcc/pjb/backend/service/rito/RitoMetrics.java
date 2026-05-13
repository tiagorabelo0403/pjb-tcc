package com.tcc.pjb.backend.service.rito;




import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;







@Component
public class RitoMetrics {

    private static final int MAX_TAGGED_COUNTERS = 256;

    private final MeterRegistry registry;

    private final Counter feedbackRegistered;
    private final Counter overrideApplied;
    private final Counter reportGenerated;
    private final Counter reportCsvDownloaded;

    private final Counter proposalCreated;
    private final Counter proposalApproved;
    private final Counter proposalApprovedStage1;
    private final Counter proposalApprovedStage2;
    private final Counter proposalRejected;

    private final ConcurrentHashMap<String, Counter> feedbackTagged = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> proposalTagged = new ConcurrentHashMap<>();

    public RitoMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);

        this.feedbackRegistered = Counter.builder("pjb.ritos.feedback.registered")
                .description("Quantidade de feedbacks de rito registrados")
                .register(registry);

        this.overrideApplied = Counter.builder("pjb.ritos.override.applied")
                .description("Quantidade de overrides de rito aplicados")
                .register(registry);

        this.reportGenerated = Counter.builder("pjb.ritos.report.generated")
                .description("Quantidade de relatórios de ritos gerados")
                .register(registry);

        this.reportCsvDownloaded = Counter.builder("pjb.ritos.report.csv.downloaded")
                .description("Quantidade de downloads do relatório CSV")
                .register(registry);

        this.proposalCreated = Counter.builder("pjb.ritos.rule_proposal.created")
                .description("Quantidade de propostas de regra criadas")
                .register(registry);

        this.proposalApproved = Counter.builder("pjb.ritos.rule_proposal.approved")
                .description("Quantidade de propostas de regra aprovadas")
                .register(registry);

        this.proposalApprovedStage1 = Counter.builder("pjb.ritos.rule_proposal.approved.stage1")
                .description("Quantidade de primeiras aprovações (4 olhos)")
                .register(registry);

        this.proposalApprovedStage2 = Counter.builder("pjb.ritos.rule_proposal.approved.stage2")
                .description("Quantidade de segundas aprovações (4 olhos)")
                .register(registry);

        this.proposalRejected = Counter.builder("pjb.ritos.rule_proposal.rejected")
                .description("Quantidade de propostas de regra rejeitadas")
                .register(registry);
    }

    public void incFeedbackRegistered() {
        feedbackRegistered.increment();
    }

    public void incFeedbackRegistered(String ritoResolved, String ritoChosen, boolean override) {
        incFeedbackRegistered();
        String r = safeTag(ritoResolved);
        String c = safeTag(ritoChosen);
        feedbackCounter(r, c, override).increment();
    }

    public void incOverrideApplied() {
        overrideApplied.increment();
    }

    public void incReportGenerated() {
        reportGenerated.increment();
    }

    public void incReportCsvDownloaded() {
        reportCsvDownloaded.increment();
    }

    public void incRuleProposalCreated(String ritoResolved, String ritoChosen) {
        proposalCreated.increment();
        incProposalTagged("created", ritoResolved, ritoChosen);
    }

    public void incRuleProposalApproved(String ritoResolved, String ritoChosen) {
        proposalApproved.increment();
        incProposalTagged("approved", ritoResolved, ritoChosen);
    }

    public void incRuleProposalApprovedStage1(String ritoResolved, String ritoChosen) {
        proposalApprovedStage1.increment();
        incProposalTagged("approved1", ritoResolved, ritoChosen);
    }

    public void incRuleProposalApprovedStage2(String ritoResolved, String ritoChosen) {
        proposalApprovedStage2.increment();
        incProposalTagged("approved2", ritoResolved, ritoChosen);
    }

    public void incRuleProposalRejected(String ritoResolved, String ritoChosen) {
        proposalRejected.increment();
        incProposalTagged("rejected", ritoResolved, ritoChosen);
    }

    private void incProposalTagged(String action, String ritoResolved, String ritoChosen) {
        String r = safeTag(ritoResolved);
        String c = safeTag(ritoChosen);
        proposalCounter(action, r, c).increment();
    }

    private Counter feedbackCounter(String resolved, String chosen, boolean override) {
        String key = resolved + "|" + chosen + "|" + (override ? "1" : "0");
        Counter existing = feedbackTagged.get(key);
        if (existing != null) {
            return existing;
        }
        if (feedbackTagged.size() >= MAX_TAGGED_COUNTERS) {
            return feedbackTagged.computeIfAbsent("OTHER|OTHER|MIXED", ignored -> Counter.builder("pjb.ritos.feedback.registered.tagged")
                    .description("Feedback de rito por par resolved->chosen")
                    .tag("resolved", "OTHER")
                    .tag("chosen", "OTHER")
                    .tag("override", "mixed")
                    .register(registry));
        }
        return feedbackTagged.computeIfAbsent(key, ignored -> Counter.builder("pjb.ritos.feedback.registered.tagged")
                .description("Feedback de rito por par resolved->chosen")
                .tag("resolved", resolved)
                .tag("chosen", chosen)
                .tag("override", override ? "true" : "false")
                .register(registry));
    }

    private Counter proposalCounter(String action, String resolved, String chosen) {
        String key = action + "|" + resolved + "|" + chosen;
        Counter existing = proposalTagged.get(key);
        if (existing != null) {
            return existing;
        }
        if (proposalTagged.size() >= MAX_TAGGED_COUNTERS) {
            return proposalTagged.computeIfAbsent(action + "|OTHER|OTHER", ignored -> Counter.builder("pjb.ritos.rule_proposal.tagged")
                    .description("Propostas de regra por par resolved->chosen")
                    .tag("action", action)
                    .tag("resolved", "OTHER")
                    .tag("chosen", "OTHER")
                    .register(registry));
        }
        return proposalTagged.computeIfAbsent(key, ignored -> Counter.builder("pjb.ritos.rule_proposal.tagged")
                .description("Propostas de regra por par resolved->chosen")
                .tag("action", action)
                .tag("resolved", resolved)
                .tag("chosen", chosen)
                .register(registry));
    }

    private static String safeTag(String s) {
        if (s == null || s.isBlank()) return "UNKNOWN";
        String v = s.trim().toUpperCase();
        if (v.length() > 64) v = v.substring(0, 64);
        return v.replaceAll("[^A-Z0-9_-]", "_");
    }
}
