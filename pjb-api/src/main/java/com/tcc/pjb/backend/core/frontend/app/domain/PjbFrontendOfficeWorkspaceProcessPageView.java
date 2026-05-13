package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeWorkspaceProcessPageView(
        String mode,
        Long activeEquipeId,
        String activeEquipeNome,
        boolean includePersonalOwnCases,
        boolean canViewAllRamos,
        List<String> effectiveAllowedRamos,
        Integer currentTrustScore,
        Integer requiredMinTrustForAuto,
        int page,
        int size,
        long totalVisible,
        int returnedCount,
        List<String> blockers,
        List<String> warnings,
        List<PjbFrontendOfficeWorkspaceProcessView> items
) {

    public String getMode() {
        return mode();
    }

    public Long getActiveEquipeId() {
        return activeEquipeId();
    }

    public String getActiveEquipeNome() {
        return activeEquipeNome();
    }

    public boolean isIncludePersonalOwnCases() {
        return includePersonalOwnCases();
    }

    public boolean getIncludePersonalOwnCases() {
        return includePersonalOwnCases();
    }

    public boolean isCanViewAllRamos() {
        return canViewAllRamos();
    }

    public boolean getCanViewAllRamos() {
        return canViewAllRamos();
    }

    public List<String> getEffectiveAllowedRamos() {
        return effectiveAllowedRamos();
    }

    public Integer getCurrentTrustScore() {
        return currentTrustScore();
    }

    public Integer getRequiredMinTrustForAuto() {
        return requiredMinTrustForAuto();
    }

    public int getPage() {
        return page();
    }

    public int getSize() {
        return size();
    }

    public long getTotalVisible() {
        return totalVisible();
    }

    public int getReturnedCount() {
        return returnedCount();
    }

    public List<String> getBlockers() {
        return blockers();
    }

    public List<String> getWarnings() {
        return warnings();
    }

    public List<PjbFrontendOfficeWorkspaceProcessView> getItems() {
        return items();
    }

    public List<String> allowedRamos() {
        return effectiveAllowedRamos();
    }
}
