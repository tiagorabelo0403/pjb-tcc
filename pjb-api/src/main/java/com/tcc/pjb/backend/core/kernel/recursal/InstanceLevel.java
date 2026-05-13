package com.tcc.pjb.backend.core.kernel.recursal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum InstanceLevel {
    FIRST_INSTANCE,
    SECOND_INSTANCE,
    SUPERIOR,
    EXTRAORDINARY;

    public static final InstanceLevel EXTRAORDINARY_INSTANCE = EXTRAORDINARY;
    public static final InstanceLevel FIRST_GRAU = FIRST_INSTANCE;
    public static final InstanceLevel SECOND_GRAU = SECOND_INSTANCE;

    public static InstanceLevel fromString(String raw) {
        if (raw == null || raw.isBlank()) return FIRST_INSTANCE;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return FIRST_INSTANCE;
        try {
            return InstanceLevel.valueOf(token);
        } catch (Exception ignored) {
            return FIRST_INSTANCE;
        }
    }

    @JsonCreator
    public static InstanceLevel jsonCreator(String raw) {
        return fromString(raw);
    }
}
