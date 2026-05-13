package com.tcc.pjb.backend.financial.ai.v3;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = FinancialAiRequestV3.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FinancialAiRequestV3 implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String correlationId;

    private final String tenantId;
    private final String usuarioId;

    
    private final String operation;

    
    private final String contractVersion;

    private final Instant timestamp;
    private final Map<String, Object> payload;

    private FinancialAiRequestV3(Builder b) {
        this.requestId = b.requestId != null ? b.requestId : UUID.randomUUID().toString();
        this.correlationId = b.correlationId != null ? b.correlationId : this.requestId;
        this.tenantId = b.tenantId;
        this.usuarioId = b.usuarioId;
        this.operation = Objects.requireNonNull(b.operation, "operation é obrigatória");
        this.contractVersion = b.contractVersion != null ? b.contractVersion : "v3";
        this.timestamp = b.timestamp != null ? b.timestamp : Instant.now();
        this.payload = b.payload.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(b.payload));
    }

    public String getRequestId() { return requestId; }

    public String getCorrelationId() { return correlationId; }

    public String getTenantId() { return tenantId; }

    public String getUsuarioId() { return usuarioId; }

    public String getOperation() { return operation; }

    public String getContractVersion() { return contractVersion; }

    public Instant getTimestamp() { return timestamp; }

    public Map<String, Object> getPayload() { return payload; }

    @JsonIgnore
    public String getSafeString(String key) {
        Object v = payload.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    @JsonIgnore
    public Double getSafeDouble(String key) {
        Object v = payload.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @JsonIgnore
    public List<String> getSafeStringList(String key) {
        Object v = payload.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public String toString() {
        return "FinancialAiRequestV3{" +
                "id='" + requestId + '\'' +
                ", operation='" + operation + '\'' +
                ", tenant='" + tenantId + '\'' +
                ", user='" + usuarioId + '\'' +
                ", payloadKeys=" + payload.keySet() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "with")
    public static final class Builder {
        private String requestId;
        private String correlationId;
        private String tenantId;
        private String usuarioId;
        private String operation;
        private String contractVersion;
        private Instant timestamp;
        private final Map<String, Object> payload = new HashMap<>();

        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder withUsuarioId(String usuarioId) {
            this.usuarioId = usuarioId;
            return this;
        }

        public Builder withOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder withContractVersion(String contractVersion) {
            this.contractVersion = contractVersion;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withPayload(Map<String, Object> payload) {
            if (payload != null && !payload.isEmpty()) {
                this.payload.putAll(payload);
            }
            return this;
        }

        public Builder addPayload(String key, Object value) {
            if (key != null && value != null) {
                this.payload.put(key, value);
            }
            return this;
        }

        
        public Builder requestId(String requestId) { return withRequestId(requestId); }
        public Builder correlationId(String correlationId) { return withCorrelationId(correlationId); }
        public Builder tenantId(String tenantId) { return withTenantId(tenantId); }
        public Builder usuarioId(String usuarioId) { return withUsuarioId(usuarioId); }
        public Builder operation(String operation) { return withOperation(operation); }
        public Builder contractVersion(String contractVersion) { return withContractVersion(contractVersion); }
        public Builder timestamp(Instant timestamp) { return withTimestamp(timestamp); }
        public Builder payload(String key, Object value) { return addPayload(key, value); }

        public FinancialAiRequestV3 build() {
            return new FinancialAiRequestV3(this);
        }
    }
}
