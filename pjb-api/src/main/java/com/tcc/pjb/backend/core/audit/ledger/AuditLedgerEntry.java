package com.tcc.pjb.backend.core.audit.ledger;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pjb_audit_ledger", indexes = {
        @Index(name = "idx_audit_ledger_resource", columnList = "resource_type, resource_id, created_at"),
        @Index(name = "idx_audit_ledger_actor", columnList = "actor_user_id, created_at"),
        @Index(name = "idx_audit_ledger_request", columnList = "request_id")
})
public class AuditLedgerEntry {

    public AuditLedgerEntry() {
    }

    public AuditLedgerEntry(String action, String resourceType, String resourceId, String payloadHash, String justificativa, java.time.Instant createdAt) {
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payloadHash = payloadHash;
        this.justificativa = justificativa;
        this.createdAt = createdAt == null ? null : java.time.LocalDateTime.ofInstant(createdAt, java.time.ZoneOffset.UTC);
        this.entryHash = payloadHash;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    
    @Column(name = "actor_key", length = 180)
    private String actorKey;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "resource_type", length = 80)
    private String resourceType;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(name = "justificativa", columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    
    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    
    @Column(name = "entry_hash", nullable = false, unique = true, length = 64)
    private String entryHash;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
