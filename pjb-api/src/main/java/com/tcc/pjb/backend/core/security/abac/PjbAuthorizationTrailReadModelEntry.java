package com.tcc.pjb.backend.core.security.abac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_authz_trail_read_model", indexes = {
        @Index(name = "idx_authz_trail_occurred", columnList = "occurred_at"),
        @Index(name = "idx_authz_trail_action_resource", columnList = "action, resource_type, occurred_at"),
        @Index(name = "idx_authz_trail_actor", columnList = "actor_id, occurred_at"),
        @Index(name = "idx_authz_trail_request", columnList = "request_id"),
        @Index(name = "idx_authz_trail_integration", columnList = "integration_code, occurred_at"),
        @Index(name = "idx_authz_trail_institutional", columnList = "institutional_unit_code, institutional_box_code, institutional_capability_code, occurred_at"),
        @Index(name = "idx_authz_trail_payload", columnList = "payload_hash", unique = true)
})
public class PjbAuthorizationTrailReadModelEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "audit_event_code", nullable = false, length = 120)
    private String auditEventCode;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 120)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 240)
    private String resourceId;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Column(name = "reason", nullable = false, length = 160)
    private String reason;

    @Column(name = "policy_version", nullable = false, length = 80)
    private String policyVersion;

    @Column(name = "policy_descriptor_sha256", nullable = false, length = 128)
    private String policyDescriptorSha256;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_type", nullable = false, length = 80)
    private String actorType;

    @Column(name = "request_id", nullable = false, length = 120)
    private String requestId;

    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "effective_sigilo", nullable = false, length = 80)
    private String effectiveSigilo;

    @Column(name = "risk_level", nullable = false, length = 40)
    private String riskLevel;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "step_up_channel", nullable = false, length = 80)
    private String stepUpChannel;

    @Column(name = "step_up_code", nullable = false, length = 120)
    private String stepUpCode;

    @Column(name = "step_up_required", nullable = false)
    private boolean stepUpRequired;

    @Column(name = "step_up_satisfied", nullable = false)
    private boolean stepUpSatisfied;

    @Column(name = "governance_channel", nullable = false, length = 80)
    private String governanceChannel;

    @Column(name = "governance_code", nullable = false, length = 120)
    private String governanceCode;

    @Column(name = "governance_scope", nullable = false, length = 120)
    private String governanceScope;

    @Column(name = "governance_required", nullable = false)
    private boolean governanceRequired;

    @Column(name = "governance_satisfied", nullable = false)
    private boolean governanceSatisfied;

    @Column(name = "integration_code", nullable = false, length = 120)
    private String integrationCode;

    @Column(name = "institutional_unit_code", nullable = false, length = 120)
    private String institutionalUnitCode;

    @Column(name = "institutional_box_code", nullable = false, length = 120)
    private String institutionalBoxCode;

    @Column(name = "institutional_capability_code", nullable = false, length = 120)
    private String institutionalCapabilityCode;

    @Column(name = "expedicao_uuid", nullable = false, length = 120)
    private String expedicaoUuid;

    @Column(name = "payload_hash", nullable = false, length = 128, unique = true)
    private String payloadHash;

    @Column(name = "audit_description", nullable = false, columnDefinition = "TEXT")
    private String auditDescription;

    public static PjbAuthorizationTrailReadModelEntry from(PjbAuthorizationTrailSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        PjbAuthorizationTrailReadModelEntry entry = new PjbAuthorizationTrailReadModelEntry();
        entry.occurredAt = LocalDateTime.ofInstant(snapshot.occurredAt(), ZoneOffset.UTC);
        entry.auditEventCode = snapshot.auditEventCode();
        entry.action = snapshot.action();
        entry.resourceType = snapshot.resourceType();
        entry.resourceId = snapshot.resourceId();
        entry.allowed = snapshot.allowed();
        entry.reason = snapshot.reason();
        entry.policyVersion = snapshot.policyVersion();
        entry.policyDescriptorSha256 = snapshot.policyDescriptorSha256();
        entry.actorId = snapshot.actorId();
        entry.actorType = snapshot.actorType();
        entry.requestId = snapshot.requestId();
        entry.justificativa = snapshot.justificativa();
        entry.effectiveSigilo = snapshot.effectiveSigilo();
        entry.riskLevel = snapshot.riskLevel().name();
        entry.riskScore = snapshot.riskScore();
        entry.stepUpChannel = snapshot.stepUpChannel();
        entry.stepUpCode = snapshot.stepUpCode();
        entry.stepUpRequired = snapshot.stepUpRequired();
        entry.stepUpSatisfied = snapshot.stepUpSatisfied();
        entry.governanceChannel = snapshot.governanceChannel();
        entry.governanceCode = snapshot.governanceCode();
        entry.governanceScope = snapshot.governanceScope();
        entry.governanceRequired = snapshot.governanceRequired();
        entry.governanceSatisfied = snapshot.governanceSatisfied();
        entry.integrationCode = snapshot.integrationCode();
        entry.institutionalUnitCode = snapshot.institutionalUnitCode();
        entry.institutionalBoxCode = snapshot.institutionalBoxCode();
        entry.institutionalCapabilityCode = snapshot.institutionalCapabilityCode();
        entry.expedicaoUuid = snapshot.expedicaoUuid();
        entry.payloadHash = snapshot.payloadHash();
        entry.auditDescription = snapshot.auditDescription();
        return entry;
    }

    public PjbAuthorizationTrailSnapshot toSnapshot() {
        return new PjbAuthorizationTrailSnapshot(
                occurredAt == null ? Instant.now() : occurredAt.toInstant(ZoneOffset.UTC),
                auditEventCode,
                action,
                resourceType,
                resourceId,
                allowed,
                reason,
                policyVersion,
                policyDescriptorSha256,
                actorId,
                actorType,
                requestId,
                justificativa,
                effectiveSigilo,
                PjbAuthorizationRiskLevel.valueOf(riskLevel),
                riskScore,
                stepUpChannel,
                stepUpCode,
                stepUpRequired,
                stepUpSatisfied,
                governanceChannel,
                governanceCode,
                governanceScope,
                governanceRequired,
                governanceSatisfied,
                integrationCode,
                institutionalUnitCode,
                institutionalBoxCode,
                institutionalCapabilityCode,
                expedicaoUuid,
                payloadHash,
                auditDescription
        );
    }

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
