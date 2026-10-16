package com.tcc.pjb.backend.core.security.device.reqhash;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;

public class RequestBodyHashFilter extends OncePerRequestFilter {

    private final DeviceSecurityProperties props;
    private final BodyHashService bodyHashService;
    private final ObjectMapper objectMapper;

    public RequestBodyHashFilter(DeviceSecurityProperties props, BodyHashService bodyHashService, ObjectMapper objectMapper) {
        this.props = Objects.requireNonNull(props);
        this.bodyHashService = Objects.requireNonNull(bodyHashService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (method == null) return true;
        String m = method.toUpperCase(Locale.ROOT);
        if (!(m.equals("POST") || m.equals("PUT") || m.equals("PATCH"))) return true;

        String ct = request.getContentType();
        if (ct == null) return true;
        if (!ct.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_JSON_VALUE)) return true;

        int max = Math.max(1024, props.getBodyHashMaxBytes());
        int len = request.getContentLength();
        if (len < 0) return false;
        return len > max;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        int max = Math.max(1024, props.getBodyHashMaxBytes());
        byte[] body = request.getInputStream().readNBytes(max + 1);
        if (body.length > max) {
            filterChain.doFilter(new CorpoComInicioJaLido(request, body), response);
            return;
        }

        if (body.length == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String computed;
        try {
            computed = bodyHashService.canonicalJsonHash(body);
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
            return;
        }

        String header = request.getHeader("X-PJB-Body-Hash");
        if (header != null && !header.isBlank()) {
            String norm = normalizeHex64(header);
            if (norm == null) {
                writeJson(response, 400, "BODY_HASH_INVALID", "Body hash inválido.", Map.of());
                return;
            }
            if (!norm.equals(computed)) {
                writeJson(response, 409, "BODY_HASH_MISMATCH", "Body hash não confere.", Map.of());
                return;
            }
        }

        var wrapped = new CachedBodyHttpServletRequest(request, body);
        wrapped.setAttribute("PJB_BODY_HASH", computed);
        filterChain.doFilter(wrapped, response);
    }

    private static final class CorpoComInicioJaLido extends HttpServletRequestWrapper {

        private final byte[] inicio;
        private ServletInputStream corpoUnico;

        private CorpoComInicioJaLido(HttpServletRequest request, byte[] inicio) {
            super(request);
            this.inicio = inicio;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (corpoUnico == null) {
                corpoUnico = encadear(super.getInputStream());
            }
            return corpoUnico;
        }

        private ServletInputStream encadear(ServletInputStream restante) {
            ByteArrayInputStream prefixo = new ByteArrayInputStream(inicio);
            InputStream corpo = new SequenceInputStream(prefixo, restante);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return corpo.read();
                }

                @Override
                public int read(byte[] destino, int deslocamento, int tamanho) throws IOException {
                    return corpo.read(destino, deslocamento, tamanho);
                }

                @Override
                public boolean isFinished() {
                    return prefixo.available() == 0 && restante.isFinished();
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new IllegalStateException("leitura assincrona nao suportada");
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static String normalizeHex64(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() != 64) return null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) return null;
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           String code,
                           String message,
                           Map<String, Object> details) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (details != null && !details.isEmpty()) body.put("details", details);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
