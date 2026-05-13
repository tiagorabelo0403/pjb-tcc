package com.tcc.pjb.backend.core.audit.cross.persistence.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_cross_audit_link")
public class CrossAuditLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_key", length = 180, nullable = false)
    private String correlationKey;

    @Column(name = "left_resource_type", length = 64, nullable = false)
    private String leftResourceType;

    @Column(name = "left_resource_id", length = 120, nullable = false)
    private String leftResourceId;

    @Column(name = "right_resource_type", length = 64, nullable = false)
    private String rightResourceType;

    @Column(name = "right_resource_id", length = 120, nullable = false)
    private String rightResourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "ver", nullable = false)
    private long ver;

    public Long getId() {
        return id;
    }

    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }

    public String getLeftResourceType() {
        return leftResourceType;
    }

    public void setLeftResourceType(String leftResourceType) {
        this.leftResourceType = leftResourceType;
    }

    public String getLeftResourceId() {
        return leftResourceId;
    }

    public void setLeftResourceId(String leftResourceId) {
        this.leftResourceId = leftResourceId;
    }

    public String getRightResourceType() {
        return rightResourceType;
    }

    public void setRightResourceType(String rightResourceType) {
        this.rightResourceType = rightResourceType;
    }

    public String getRightResourceId() {
        return rightResourceId;
    }

    public void setRightResourceId(String rightResourceId) {
        this.rightResourceId = rightResourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getVer() {
        return ver;
    }

    public void setVer(long ver) {
        this.ver = ver;
    }
}
