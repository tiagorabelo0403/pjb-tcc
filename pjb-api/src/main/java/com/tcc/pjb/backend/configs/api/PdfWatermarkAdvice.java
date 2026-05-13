package com.tcc.pjb.backend.configs.api;

import java.io.InputStream;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.download.DownloadBudgetService;
import com.tcc.pjb.backend.core.security.device.download.DownloadBudgetContextResolver;
import com.tcc.pjb.backend.core.security.device.download.DownloadEventService;
import com.tcc.pjb.backend.core.security.device.download.PdfWatermarkService;
import com.tcc.pjb.backend.model.entity.Usuario;

@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({
        DeviceSecurityProperties.class,
        PdfWatermarkService.class,
        CurrentUserService.class,
        DownloadEventService.class,
        DownloadBudgetService.class,
        AuditLedgerService.class
})
public class PdfWatermarkAdvice implements ResponseBodyAdvice<Object> {

    private final DeviceSecurityProperties props;
    private final PdfWatermarkService watermarkService;
    private final CurrentUserService currentUserService;
    private final DownloadEventService downloadEventService;
    private final DownloadBudgetService downloadBudgetService;
    private final AuditLedgerService auditLedgerService;

    public PdfWatermarkAdvice(DeviceSecurityProperties props,
                             PdfWatermarkService watermarkService,
                             CurrentUserService currentUserService,
                             DownloadEventService downloadEventService,
                             DownloadBudgetService downloadBudgetService,
                             AuditLedgerService auditLedgerService) {
        this.props = Objects.requireNonNull(props);
        this.watermarkService = Objects.requireNonNull(watermarkService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.downloadEventService = Objects.requireNonNull(downloadEventService);
        this.downloadBudgetService = Objects.requireNonNull(downloadBudgetService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (!props.isWatermarkEnabled()) return body;
        if (!MediaType.APPLICATION_PDF.isCompatibleWith(selectedContentType)) return body;
        if (!(request instanceof ServletServerHttpRequest sr)) return body;
        if (!(response instanceof ServletServerHttpResponse ssr)) return body;

        var servletRequest = sr.getServletRequest();
        Object required = servletRequest.getAttribute("PJB_WATERMARK_REQUIRED");
        if (!(required instanceof Boolean b) || !b) return body;

        String watermarkId = safeAttr(servletRequest.getAttribute("PJB_WATERMARK_ID"));
        if (watermarkId == null) return body;

        Usuario u = currentUserService.getOptional().orElse(null);
        if (u == null || u.getId() == null) return body;

        Long deviceId = attrLong(servletRequest.getAttribute("PJB_DEVICE_ID"));
        String path = servletRequest.getRequestURI() != null ? servletRequest.getRequestURI() : "";

        var ctx = DownloadBudgetContextResolver.resolve(servletRequest);

        downloadBudgetService.enforceRestrictedBudget(u.getId(), deviceId, ctx.processoId(), ctx.documentoId());

        byte[] original = readBytes(body, props.getWatermarkMaxBytes());
        if (original == null) return body;

        String label = u.getEmail() != null && !u.getEmail().isBlank() ? u.getEmail() : String.valueOf(u.getId());
        byte[] stamped = watermarkService.watermark(original, watermarkId, label);

        ssr.getServletResponse().setHeader("X-PJB-Watermark-Id", watermarkId);
        response.getHeaders().setCacheControl("no-store");
        response.getHeaders().setPragma("no-cache");
        response.getHeaders().setContentLength(stamped.length);

        downloadEventService.record(u, deviceId, path, stamped.length, watermarkId, ctx.processoId(), ctx.documentoId());
        auditLedgerService.appendSafely("PDF_WATERMARK_ISSUED", "HTTP", path, watermarkId);

        return new ByteArrayResource(stamped);
    }

    private byte[] readBytes(Object body, long maxBytes) {
        try {
            if (body == null) return null;
            if (body instanceof byte[] b) {
                if (b.length > maxBytes) throw new AccessDeniedPjbException("PDF muito grande para download sensível.");
                return b;
            }
            if (body instanceof Resource r) {
                long max = Math.max(1024L, maxBytes);
                try (InputStream in = r.getInputStream()) {
                    return readLimited(in, max);
                }
            }
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("falha ao ler PDF", e);
        }
    }

    private byte[] readLimited(InputStream in, long maxBytes) throws Exception {
        if (maxBytes <= 0) maxBytes = 1;
        int max = (int) Math.min(Integer.MAX_VALUE - 1L, maxBytes);
        byte[] buf = in.readNBytes(max + 1);
        if (buf.length > max) {
            throw new AccessDeniedPjbException("PDF muito grande para download sensível.");
        }
        return buf;
    }

    private static String safeAttr(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        if (s.length() > 96) s = s.substring(0, 96);
        return s;
    }

    private static Long attrLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
