package com.tcc.pjb.backend.core.security.device.reqhash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestHashService {

    public String compute(String method,
                          String path,
                          String query,
                          Long equipeId,
                          Long deviceId,
                          String bodyHash) {
        String m = normMethod(method);
        String p = normPath(path);
        String q = normQuery(query);
        String eq = equipeId != null ? String.valueOf(equipeId) : "";
        String did = deviceId != null ? String.valueOf(deviceId) : "";
        String bh = bodyHash != null ? normHex64(bodyHash) : "";

        String material = String.join("\n",
                "v1",
                m,
                p,
                q,
                eq,
                did,
                bh
        );
        return sha256Hex(material);
    }

    public String computeFromRequest(HttpServletRequest request) {
        Objects.requireNonNull(request, "request");
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        Long equipeId = parseLong(request.getHeader("X-Equipe-ID"));
        Long deviceId = parseLong(request.getHeader("X-Device-ID"));
        String bodyHash = request.getHeader("X-PJB-Body-Hash");
        return compute(method, path, query, equipeId, deviceId, bodyHash);
    }

    public static String sha256Hex(String material) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String normMethod(String method) {
        if (method == null) throw new IllegalArgumentException("method obrigatório");
        String m = method.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() || m.length() > 12) throw new IllegalArgumentException("method inválido");
        return m;
    }

    private static String normPath(String path) {
        if (path == null) throw new IllegalArgumentException("path obrigatório");
        String p = path.trim();
        if (p.isEmpty() || p.length() > 300) throw new IllegalArgumentException("path inválido");
        if (!p.startsWith("/")) throw new IllegalArgumentException("path inválido");
        return p;
    }

    private static String normQuery(String query) {
        if (query == null) return "";
        String q = query.trim();
        if (q.length() > 1000) q = q.substring(0, 1000);
        return q;
    }

    private static String normHex64(String v) {
        String s = v.trim();
        if (s.isEmpty()) return "";
        if (s.length() != 64) throw new IllegalArgumentException("hash inválido");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) throw new IllegalArgumentException("hash inválido");
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private static Long parseLong(String v) {
        try {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
