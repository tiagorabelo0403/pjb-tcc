package com.tcc.pjb.backend.service.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;

@Component
public class ApiResponseFactory {

    public <T> ApiCommandResponse<T> commandOk(String message, T data, List<String> warnings) {
        return new ApiCommandResponse<>(
                "OK",
                resolveCorrelationId(),
                message,
                data,
                warnings,
                Instant.now()
        );
    }

    public <T> ApiCommandResponse<T> commandAccepted(String message, T data, List<String> warnings) {
        return new ApiCommandResponse<>(
                "ACCEPTED",
                resolveCorrelationId(),
                message,
                data,
                warnings,
                Instant.now()
        );
    }

    public <T> ApiQueryResponse<T> queryOk(T data, List<String> warnings) {
        return new ApiQueryResponse<>(
                "OK",
                resolveCorrelationId(),
                data,
                warnings,
                Instant.now()
        );
    }

    private String resolveCorrelationId() {
        return RequestContext.getRequestId().filter(value -> !value.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
    }
}
