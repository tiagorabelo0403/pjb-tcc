package com.tcc.pjb.backend.legal.skills.v1;

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
import com.tcc.pjb.backend.legal.skills.contract.LegalSkillRequestContract;

@JsonDeserialize(builder = LegalSkillRequestV1.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LegalSkillRequestV1 implements Serializable, LegalSkillRequestContract {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String correlationId;

    private final String tenantId;
    private final String usuarioId;

    
    private final String skill;

    
    private final String contractVersion;

    private final Instant timestamp;
    private final Map<String, Object> payload;

    private LegalSkillRequestV1(Builder b) {
        this.requestId = b.requestId != null ? b.requestId : UUID.randomUUID().toString();
        this.correlationId = b.correlationId != null ? b.correlationId : this.requestId;
        this.tenantId = b.tenantId;
        this.usuarioId = b.usuarioId;
        this.skill = Objects.requireNonNull(b.skill, "skill é obrigatória");
        this.contractVersion = b.contractVersion != null ? b.contractVersion : "v1";
        this.timestamp = b.timestamp != null ? b.timestamp : Instant.now();
        this.payload = b.payload.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(b.payload));
    }

    public String getRequestId() { return requestId; }

    public String getCorrelationId() { return correlationId; }

    public String getTenantId() { return tenantId; }

    public String getUsuarioId() { return usuarioId; }

    public String getSkill() { return skill; }

    public String getContractVersion() { return contractVersion; }

    public Instant getTimestamp() { return timestamp; }

    public Map<String, Object> getPayload() { return payload; }

    

    @JsonIgnore
    public String getSafeString(String key) {
        Object v = payload.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    @JsonIgnore
    public Long getSafeLong(String key) {
        Object v = payload.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
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
    public Boolean getSafeBoolean(String key) {
        Object v = payload.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
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
        return "LegalSkillRequestV1{" +
                "id='" + requestId + '\'' +
                ", skill='" + skill + '\'' +
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
        private String skill;
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

        public Builder withSkill(String skill) {
            this.skill = skill;
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
        public Builder skill(String skill) { return withSkill(skill); }
        public Builder contractVersion(String contractVersion) { return withContractVersion(contractVersion); }
        public Builder timestamp(Instant timestamp) { return withTimestamp(timestamp); }
        public Builder payload(String key, Object val) { return addPayload(key, val); }

        public LegalSkillRequestV1 build() {
            return new LegalSkillRequestV1(this);
        }
    }
}
