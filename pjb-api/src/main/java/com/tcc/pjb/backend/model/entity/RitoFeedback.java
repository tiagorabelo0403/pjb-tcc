package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_rito_feedback")
public class RitoFeedback {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "rito_resolved", length = 64)
    private String ritoResolved;

    @Column(name = "rito_chosen", nullable = false, length = 64)
    private String ritoChosen;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "reasons_json", length = 12000)
    private String reasonsJson;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
