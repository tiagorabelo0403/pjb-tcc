package com.tcc.pjb.backend.platform.security.idempotency;

import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class PjbIdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final String REPLAY_HEADER = "X-Idempotent-Replay";
    private static final Set<String> GUARDED_PREFIXES = Set.of(
            "/api/v1/peticionamento",
            "/api/v1/processual/protocolo"
    );

    private final PjbIdempotencyService service;

    public PjbIdempotencyFilter(PjbIdempotencyService service) {
        this.service = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isGuarded(req)) {
            chain.doFilter(req, res);
            return;
        }
        String key = req.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            writeJsonError(res, HttpServletResponse.SC_BAD_REQUEST, "Idempotency-Key header obrigatório");
            return;
        }
        if (!service.acquire(key)) {
            replayOrReject(key, res);
            return;
        }
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(res);
        try {
            chain.doFilter(req, wrapper);
            if (wrapper.getStatus() < 400) {
                service.complete(
                        key,
                        wrapper.getStatus(),
                        wrapper.getContentType(),
                        extractBody(wrapper),
                        wrapper.getHeader("Location")
                );
            } else {
                service.release(key);
            }
            wrapper.copyBodyToResponse();
        } catch (Exception e) {
            service.release(key);
            throw e;
        }
    }

    private void replayOrReject(String key, HttpServletResponse res) throws IOException {
        String status = service.status(key);
        if ("PROCESSING".equals(status)) {
            res.setStatus(HttpServletResponse.SC_CONFLICT);
            res.setHeader("Retry-After", String.valueOf(service.retryAfterSeconds()));
            writeJsonBody(res, "{\"error\":\"Requisição em processamento\"}");
            return;
        }
        Optional<PjbIdempotencyReplayPayload> payload = service.loadReplay(key);
        if (payload.isPresent()) {
            writeReplay(res, payload.get());
            return;
        }
        res.setStatus(HttpServletResponse.SC_OK);
        res.setHeader(REPLAY_HEADER, "true");
        writeJsonBody(res, "{\"idempotent\":true,\"key\":\"" + sanitizeJson(key) + "\"}");
    }

    private void writeReplay(HttpServletResponse res, PjbIdempotencyReplayPayload payload) throws IOException {
        res.setStatus(payload.status());
        res.setHeader(REPLAY_HEADER, "true");
        if (payload.location() != null) {
            res.setHeader("Location", payload.location());
        }
        if (payload.contentType() != null && !payload.contentType().isBlank()) {
            res.setContentType(payload.contentType());
        } else {
            res.setContentType("application/json");
        }
        if (payload.body() != null) {
            res.getWriter().write(payload.body());
        }
    }

    private void writeJsonError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        writeJsonBody(res, "{\"error\":\"" + sanitizeJson(message) + "\"}");
    }

    private void writeJsonBody(HttpServletResponse res, String body) throws IOException {
        if (res.getContentType() == null) {
            res.setContentType("application/json");
        }
        res.getWriter().write(body);
    }

    private boolean isGuarded(HttpServletRequest req) {
        if (!"POST".equals(req.getMethod()) && !"PUT".equals(req.getMethod())) {
            return false;
        }
        String path = req.getRequestURI();
        return GUARDED_PREFIXES.stream().anyMatch(path::startsWith) || isSecretariatProtocol(path);
    }

    private boolean isSecretariatProtocol(String path) {
        return path != null
                && path.startsWith("/api/v1/secretariat/")
                && path.endsWith("/protocolar");
    }

    private String extractBody(ContentCachingResponseWrapper wrapper) {
        byte[] body = wrapper.getContentAsByteArray();
        if (body == null || body.length == 0) {
            return null;
        }
        Charset charset = wrapper.getCharacterEncoding() == null || wrapper.getCharacterEncoding().isBlank()
                ? StandardCharsets.UTF_8
                : Charset.forName(wrapper.getCharacterEncoding());
        return new String(body, charset);
    }

    private String sanitizeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
