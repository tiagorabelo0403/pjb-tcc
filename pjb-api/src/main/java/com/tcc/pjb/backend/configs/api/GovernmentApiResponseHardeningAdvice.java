package com.tcc.pjb.backend.configs.api;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GovernmentApiResponseHardeningAdvice implements ResponseBodyAdvice<Object> {

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
        if (isGovernmentSensitivePath(path)) {
            response.getHeaders().setCacheControl("no-store, no-cache, max-age=0, must-revalidate");
            response.getHeaders().setPragma("no-cache");
            response.getHeaders().setExpires(0);
            response.getHeaders().set("X-Robots-Tag", "noindex, nofollow, noarchive");
            response.getHeaders().set("Referrer-Policy", "no-referrer");
            mergeHeader(response, "Vary", "Authorization");
            mergeHeader(response, "Vary", "X-Request-Id");
        }
        return body;
    }

    private boolean isGovernmentSensitivePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/api/v1/auth/govbr")
                || path.startsWith("/api/v1/processual/govbr")
                || path.startsWith("/api/v1/cidadao/govbr");
    }

    private void mergeHeader(ServerHttpResponse response, String name, String value) {
        String current = response.getHeaders().getFirst(name);
        if (current == null || current.isBlank()) {
            response.getHeaders().set(name, value);
            return;
        }
        for (String token : current.split(",")) {
            if (value.equalsIgnoreCase(token.trim())) {
                return;
            }
        }
        response.getHeaders().set(name, current + ", " + value);
    }
}
