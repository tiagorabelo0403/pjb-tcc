package com.tcc.pjb.backend.core.governance.idempotency.persistence.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyStatus;

@Entity
@Table(name = "tb_request_idempotency")
public class RequestIdempotencyEntity {

    @Id
    @Column(name = "request_hash", length = 96, nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private RequestIdempotencyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(name = "response_hash", length = 96)
    private String responseHash;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    @Version
    @Column(name = "ver", nullable = false)
    private long ver;

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public RequestIdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(RequestIdempotencyStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLockUntil() {
        return lockUntil;
    }

    public void setLockUntil(Instant lockUntil) {
        this.lockUntil = lockUntil;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResponseHash() {
        return responseHash;
    }

    public void setResponseHash(String responseHash) {
        this.responseHash = responseHash;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public long getVer() {
        return ver;
    }

    public void setVer(long ver) {
        this.ver = ver;
    }
}
