package com.tcc.pjb.backend.modules.laiane.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.modules.laiane.model.LaianeDeadlineDelegationStatus;
import lombok.*;

@Entity
@Table(name = "tb_laiane_deadline_delegation", indexes = {
        @Index(name = "idx_deadline_delegation_work_item", columnList = "work_item_id"),
        @Index(name = "idx_deadline_delegation_delegator", columnList = "delegator_id"),
        @Index(name = "idx_deadline_delegation_delegatee", columnList = "delegatee_id"),
        @Index(name = "idx_deadline_delegation_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDeadlineDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id", nullable = false)
    private Usuario delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegatee_id", nullable = false)
    private Usuario delegatee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private WorkItem workItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    @Builder.Default
    private LaianeDeadlineDelegationStatus status = LaianeDeadlineDelegationStatus.PENDENTE;

    @Lob
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = LaianeDeadlineDelegationStatus.PENDENTE;
    }
}
