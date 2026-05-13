package com.tcc.pjb.backend.modules.advocacia.office.enums;

public enum OfficeWorkspaceMode {
    PERSONAL,
    OFFICE,
    HYBRID;

    public static OfficeWorkspaceMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (OfficeWorkspaceMode value : values()) {
            if (value.name().equalsIgnoreCase(raw.trim())) {
                return value;
            }
        }
        return null;
    }
}
