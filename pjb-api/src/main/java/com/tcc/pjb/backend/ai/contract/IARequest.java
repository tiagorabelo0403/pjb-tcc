package com.tcc.pjb.backend.ai.contract;

import java.util.Collections;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = IARequest.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class IARequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final String requestId;     
    private final String correlationId; 
    private final String origem;        
    private final String acao;          
    private final String usuarioId;     
    private final Instant timestamp;
    private final Map<String, Object> payload;

    private IARequest(Builder builder) {
        this.requestId = builder.requestId != null ? builder.requestId : UUID.randomUUID().toString();
        this.correlationId = builder.correlationId != null ? builder.correlationId : this.requestId;

        this.origem = Objects.requireNonNull(builder.origem, "origem é obrigatória");
        this.acao = Objects.requireNonNull(builder.acao, "ação é obrigatória");

        this.usuarioId = builder.usuarioId; 
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();

        this.payload = builder.payload.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(builder.payload));
    }

    

    public String getRequestId() { return requestId; }
    public String getCorrelationId() { return correlationId; }
    public String getOrigem() { return origem; }
    public String getAcao() { return acao; }
    public String getUsuarioId() { return usuarioId; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getPayload() { return payload; }

    

    @JsonIgnore
    public String getSafeString(String chave) {
        Object v = payload.get(chave);
        return v != null ? String.valueOf(v) : null;
    }

    @JsonIgnore
    public Long getSafeLong(String chave) {
        Object v = payload.get(chave);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @JsonIgnore
    public Boolean getSafeBoolean(String chave) {
        Object v = payload.get(chave);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    
    @JsonIgnore
    public Double getSafeDouble(String chave) {
        Object v = payload.get(chave);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    
    @JsonIgnore
    public List<String> getSafeStringList(String chave) {
        Object v = payload.get(chave);
        if (v instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    

    @Override
    public String toString() {
        return "IARequest{" +
                "id='" + requestId + '\'' +
                ", action='" + acao + '\'' +
                ", origin='" + origem + '\'' +
                ", user='" + usuarioId + '\'' +
                ", payloadKeys=" + payload.keySet() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IARequest iaRequest = (IARequest) o;
        return Objects.equals(requestId, iaRequest.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "with")
    public static final class Builder {

        private String requestId;
        private String correlationId;
        private String origem;
        private String acao;
        private String usuarioId;
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

        public Builder withOrigem(String origem) {
            this.origem = origem;
            return this;
        }

        public Builder withAcao(String acao) {
            this.acao = acao;
            return this;
        }

        public Builder withUsuarioId(String usuarioId) {
            this.usuarioId = usuarioId;
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
        public Builder origem(String origem) { return withOrigem(origem); }
        public Builder acao(String acao) { return withAcao(acao); }
        public Builder payload(String key, Object val) { return addPayload(key, val); }

        public Builder correlationId(String correlationId) { return withCorrelationId(correlationId); }
        public Builder usuarioId(String usuarioId) { return withUsuarioId(usuarioId); }
        public Builder timestamp(Instant timestamp) { return withTimestamp(timestamp); }

        public IARequest build() {
            return new IARequest(this);
        }
    }
}