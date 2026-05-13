package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendWorkspaceBoardItemView(
        String key,
        String title,
        String subtitle,
        String meta,
        String accentTone,
        String accentHex,
        String surfaceHex,
        String route
) {
}
