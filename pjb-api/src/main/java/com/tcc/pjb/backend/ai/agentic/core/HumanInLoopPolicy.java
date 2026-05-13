package com.tcc.pjb.backend.ai.agentic.core;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class HumanInLoopPolicy {

    private double approvalThreshold;
    private Set<String> alwaysRequireApprovalForActions = new HashSet<>();

    public static HumanInLoopPolicy defaultPolicy() {
        HumanInLoopPolicy p = new HumanInLoopPolicy();
        p.approvalThreshold = 0.80;
        p.alwaysRequireApprovalForActions.add("LEGAL_FILING");
        p.alwaysRequireApprovalForActions.add("ASSET_FREEZE_REQUEST");
        p.alwaysRequireApprovalForActions.add("PAYMENT_ORDER");
        p.alwaysRequireApprovalForActions.add("BANK_ACCOUNT_BLOCK");
        p.alwaysRequireApprovalForActions.add("SANCTION_RECOMMENDATION");
        return p;
    }

    public double getApprovalThreshold() {
        return approvalThreshold;
    }

    public void setApprovalThreshold(double approvalThreshold) {
        this.approvalThreshold = approvalThreshold;
    }

    public Set<String> getAlwaysRequireApprovalForActions() {
        return alwaysRequireApprovalForActions;
    }

    public void setAlwaysRequireApprovalForActions(Set<String> alwaysRequireApprovalForActions) {
        this.alwaysRequireApprovalForActions = alwaysRequireApprovalForActions;
    }

    public boolean needsHumanApproval(double confidence, List<String> actions) {
        actions = Objects.requireNonNullElse(actions, List.of());

        for (String a : actions) {
            if (a == null) continue;
            String x = a.toLowerCase(Locale.ROOT);
            if (x.contains("high_risk") || x.contains("critical") || x.contains("sanction")) {
                return true;
            }
        }

        return !shouldAutoApprove(confidence, actions);
    }

    @Deprecated(forRemoval = true)
    public boolean requiresApproval(double confidence, List<String> actions) {
        return needsHumanApproval(confidence, actions);
    }

    private boolean shouldAutoApprove(double confidence, List<String> actions) {
        if (confidence < approvalThreshold) return false;
        if (actions == null || actions.isEmpty()) return true;

        for (String a : actions) {
            if (a == null || a.isBlank()) continue;
            String normalized = a.trim().toUpperCase(Locale.ROOT);
            if (alwaysRequireApprovalForActions.contains(normalized)) {
                return false;
            }
        }
        return true;
    }
}
