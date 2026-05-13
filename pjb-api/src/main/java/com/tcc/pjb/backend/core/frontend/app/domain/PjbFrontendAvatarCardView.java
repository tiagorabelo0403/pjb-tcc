package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendAvatarCardView(
        Long userId,
        String nome,
        String subtitle,
        String avatarUrl,
        String avatarEtag,
        String fallbackInitials,
        boolean online,
        String accentTag,
        String accentHex,
        String badgeHex,
        String presenceHex,
        String route
) {
}
