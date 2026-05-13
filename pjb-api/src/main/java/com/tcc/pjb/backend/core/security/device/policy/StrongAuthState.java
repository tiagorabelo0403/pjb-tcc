package com.tcc.pjb.backend.core.security.device.policy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import jakarta.servlet.http.HttpServletRequest;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.stepup.FaceReauthTokenPayload;

public record StrongAuthState(
        String method,
        LocalDateTime issuedAt,
        Long sessionId,
        Long deviceId,
        String scopeAction,
        String scopeRequestHash,
        boolean scopeOneShot
) {

    public static StrongAuthState from(HttpServletRequest request) {
        if (request != null) {
            Object m = request.getAttribute("PJB_STRONG_AUTH_METHOD");
            Object at = request.getAttribute("PJB_STRONG_AUTH_AT");
            Object sid = request.getAttribute("PJB_STRONG_AUTH_SESSION_ID");
            Object did = request.getAttribute("PJB_STRONG_AUTH_DEVICE_ID");
            Object sa = request.getAttribute("PJB_STRONG_AUTH_SCOPE_ACTION");
            Object srh = request.getAttribute("PJB_STRONG_AUTH_SCOPE_REQ_HASH");
            Object oso = request.getAttribute("PJB_STRONG_AUTH_SCOPE_ONE_SHOT");

            String method = m != null ? String.valueOf(m) : null;
            LocalDateTime issuedAt = at instanceof LocalDateTime ldt ? ldt : null;
            Long sessionId = toLong(sid);
            Long deviceId = toLong(did);
            String scopeAction = sa != null ? normalize(String.valueOf(sa)) : null;
            String scopeRequestHash = srh != null ? normalize(String.valueOf(srh)) : null;
            boolean oneShot = toBool(oso);

            if (issuedAt != null || (method != null && !method.isBlank())) {
                return new StrongAuthState(normalize(method), issuedAt, sessionId, deviceId, scopeAction, scopeRequestHash, oneShot);
            }
        }

        FaceReauthTokenPayload face = RequestContext.getFaceCredential().orElse(null);
        if (face != null) {
            LocalDateTime issuedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(face.iat()), ZoneOffset.UTC);
            return new StrongAuthState("FACE", issuedAt, null, null, null, null, false);
        }

        return new StrongAuthState(null, null, null, null, null, null, false);
    }

    public boolean isPresent() {
        return method != null && !method.isBlank();
    }

    public boolean isScoped() {
        return scopeAction != null && scopeRequestHash != null;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return false;
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isEmpty() ? null : s;
    }
}
