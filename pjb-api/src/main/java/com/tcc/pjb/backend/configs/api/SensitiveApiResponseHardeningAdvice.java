package com.tcc.pjb.backend.configs.api;

import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class SensitiveApiResponseHardeningAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  org.springframework.http.MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (isSensitivePath(path)) {
            response.getHeaders().setCacheControl(CacheControl.noStore().mustRevalidate());
            response.getHeaders().setPragma("no-cache");
            response.getHeaders().setExpires(0);
            response.getHeaders().set("X-Robots-Tag", "noindex, nofollow, noarchive");
        }
        return body;
    }

    private boolean isSensitivePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/v1/ia/")
                || path.startsWith("/api/ai/")
                || path.startsWith("/api/v1/triad")
                || path.startsWith("/api/v1/magistratura/face");
    }
}
