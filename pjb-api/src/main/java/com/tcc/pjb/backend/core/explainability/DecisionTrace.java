package com.tcc.pjb.backend.core.explainability;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pjb_decision_trace", indexes = {
        @Index(name = "idx_decision_subject", columnList = "subject_type, subject_id, created_at"),
        @Index(name = "idx_decision_actor", columnList = "actor_user_id, created_at"),
        @Index(name = "idx_decision_request", columnList = "request_id")
})
public class DecisionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_key", length = 180)
    private String actorKey;

    @Column(name = "decision_type", nullable = false, length = 80)
    private String decisionType;

    @Column(name = "subject_type", length = 80)
    private String subjectType;

    @Column(name = "subject_id", length = 120)
    private String subjectId;

    @Column(name = "confidence", precision = 5, scale = 4)
    private java.math.BigDecimal confidence;

    @Column(name = "reasons_json", columnDefinition = "TEXT")
    private String reasonsJson;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(name = "input_digest", length = 64)
    private String inputDigest;

    @Column(name = "output_digest", length = 64)
    private String outputDigest;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "model_version", length = 80)
    private String modelVersion;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
