package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendAppBootstrapView(
        PjbFrontendCurrentUserView me,
        PjbFrontendContextView context,
        PjbFrontendCapabilitySummaryView capabilities,
        List<PjbFrontendMenuItemView> menu,
        PjbFrontendSupportCatalogView supportCatalogs,
        PjbFrontendOfficeModeView officeMode,
        PjbFrontendOfficeWorkspaceSummaryView officeWorkspaceSummary,
        List<String> nextApiCalls
) {
}
