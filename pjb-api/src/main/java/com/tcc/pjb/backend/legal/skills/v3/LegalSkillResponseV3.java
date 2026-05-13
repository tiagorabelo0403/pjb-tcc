package com.tcc.pjb.backend.legal.skills.v3;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.tcc.pjb.backend.legal.skills.contract.LegalSkillResponseContract;

@JsonDeserialize(builder = LegalSkillResponseV3.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LegalSkillResponseV3 implements Serializable, LegalSkillResponseContract {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        ALERT,
        ERROR
    }

    private final String requestId;
    private final String correlationId;
    private final String skill;

    private final Status status;
    private final double confidence;
    private final String message;

    private final Instant timestamp;
    private final Map<String, Object> outputs;
    private final List<String> warnings;

    private LegalSkillResponseV3(Builder b) {
        this.requestId = Objects.requireNonNull(b.requestId, "requestId é obrigatório");
        this.correlationId = b.correlationId != null ? b.correlationId : this.requestId;
        this.skill = b.skill;
        this.status = b.status != null ? b.status : Status.SUCCESS;
        this.confidence = b.confidence;
        this.message = b.message;
        this.timestamp = b.timestamp != null ? b.timestamp : Instant.now();
        this.outputs = b.outputs.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(b.outputs));
        this.warnings = b.warnings.isEmpty() ? List.of() : List.copyOf(b.warnings);
    }

    public String getRequestId() { return requestId; }

    public String getCorrelationId() { return correlationId; }

    public String getSkill() { return skill; }

    public Status getStatus() { return status; }

    public double getConfidence() { return confidence; }

    public String getMessage() { return message; }

    public Instant getTimestamp() { return timestamp; }

    public Map<String, Object> getOutputs() { return outputs; }

    public List<String> getWarnings() { return warnings; }

    public static Builder builder() {
        return new Builder();
    }

    public static LegalSkillResponseV3 success(LegalSkillRequestV3 req, String message, double confidence) {
        return builder()
                .requestId(req.getRequestId())
                .correlationId(req.getCorrelationId())
                .skill(req.getSkill())
                .status(Status.SUCCESS)
                .message(message)
                .confidence(confidence)
                .build();
    }

    public static LegalSkillResponseV3 alert(LegalSkillRequestV3 req, String message, double confidence) {
        return builder()
                .requestId(req.getRequestId())
                .correlationId(req.getCorrelationId())
                .skill(req.getSkill())
                .status(Status.ALERT)
                .message(message)
                .confidence(confidence)
                .build();
    }

    public static LegalSkillResponseV3 error(LegalSkillRequestV3 req, String message) {
        return builder()
                .requestId(req.getRequestId())
                .correlationId(req.getCorrelationId())
                .skill(req.getSkill())
                .status(Status.ERROR)
                .message(message)
                .confidence(0.0)
                .build();
    }

    @Override
    public String toString() {
        return "LegalSkillResponseV3{" +
                "requestId='" + requestId + '\'' +
                ", skill='" + skill + '\'' +
                ", status=" + status +
                ", confidence=" + confidence +
                ", outputsKeys=" + outputs.keySet() +
                '}';
    }

    @JsonPOJOBuilder(withPrefix = "with")
    public static final class Builder {
        private String requestId;
        private String correlationId;
        private String skill;
        private Status status;
        private double confidence;
        private String message;
        private Instant timestamp;
        private final Map<String, Object> outputs = new HashMap<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withSkill(String skill) {
            this.skill = skill;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withConfidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withOutputs(Map<String, Object> outputs) {
            if (outputs != null && !outputs.isEmpty()) {
                this.outputs.putAll(outputs);
            }
            return this;
        }

        public Builder putOutput(String key, Object value) {
            if (key != null && value != null) {
                this.outputs.put(key, value);
            }
            return this;
        }

        public Builder withWarnings(List<String> warnings) {
            if (warnings != null && !warnings.isEmpty()) {
                this.warnings.addAll(warnings);
            }
            return this;
        }

        public Builder addWarning(String warning) {
            if (warning != null && !warning.isBlank()) {
                this.warnings.add(warning);
            }
            return this;
        }

        
        public Builder requestId(String requestId) { return withRequestId(requestId); }
        public Builder correlationId(String correlationId) { return withCorrelationId(correlationId); }
        public Builder skill(String skill) { return withSkill(skill); }
        public Builder status(Status status) { return withStatus(status); }
        public Builder confidence(double confidence) { return withConfidence(confidence); }
        public Builder message(String message) { return withMessage(message); }
        public Builder timestamp(Instant timestamp) { return withTimestamp(timestamp); }
        public Builder output(String key, Object value) { return putOutput(key, value); }
        public Builder warning(String warning) { return addWarning(warning); }

        public LegalSkillResponseV3 build() {
            return new LegalSkillResponseV3(this);
        }
    }
}
