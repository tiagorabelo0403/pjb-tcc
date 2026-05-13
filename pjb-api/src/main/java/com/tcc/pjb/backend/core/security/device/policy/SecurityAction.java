package com.tcc.pjb.backend.core.security.device.policy;

public enum SecurityAction {
    READ_PUBLIC,
    READ_CASE,
    READ_RESTRICTED,
    WRITE_CASE,
    WRITE_PROTOCOL,
    WRITE_JUDICIAL_ACT,
    PUBLISH_JUDICIAL_ACT,
    CERTIFY_TRANSIT,
    ARCHIVE_CASE,
    EXECUTE_JUDICIAL_ACT,
    ISSUE_MANDATE,
    ISSUE_RELEASE_ORDER,
    SIGN_DOCUMENT,
    ADMIN,
    UNKNOWN;

    public static SecurityAction parseOrUnknown(String v) {
        if (v == null) return UNKNOWN;
        String s = v.trim();
        if (s.isEmpty()) return UNKNOWN;
        try {
            return SecurityAction.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
