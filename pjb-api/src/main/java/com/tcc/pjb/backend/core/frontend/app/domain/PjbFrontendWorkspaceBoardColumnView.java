package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendWorkspaceBoardColumnView(
        String key,
        String label,
        long total,
        String accentHex,
        String surfaceHex,
        List<PjbFrontendWorkspaceBoardItemView> items
) {
}
