package com.tcc.pjb.backend.core.security.abac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "tb_authz_trail_dedup")
class PjbAuthzTrailDedup implements Persistable<String> {

    @Id
    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "trail_id")
    private Long trailId;

    @Column(name = "occurred_month", nullable = false)
    private int occurredMonth;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    PjbAuthzTrailDedup() {}

    PjbAuthzTrailDedup(String payloadHash, Long trailId, int occurredMonth) {
        this.payloadHash = payloadHash;
        this.trailId = trailId;
        this.occurredMonth = occurredMonth;
        this.occurredAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @Override
    @Transient
    public String getId() {
        return payloadHash;
    }

    @Override
    @Transient
    public boolean isNew() {
        return true;
    }
}
