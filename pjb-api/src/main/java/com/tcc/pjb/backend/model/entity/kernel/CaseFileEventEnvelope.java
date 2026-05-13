package com.tcc.pjb.backend.model.entity.kernel;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_case_file_event")
public class CaseFileEventEnvelope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_file_id", nullable = false)
    private Long caseFileId;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role", length = 60)
    private String actorRole;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
