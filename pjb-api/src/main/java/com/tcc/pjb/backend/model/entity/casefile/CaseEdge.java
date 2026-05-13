package com.tcc.pjb.backend.model.entity.casefile;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.*;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_case_edge",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_case_edge", columnNames = {
                        "case_file_id", "from_proceeding_key", "to_proceeding_key", "relation_type", "appeal_type"
                })
        },
        indexes = {
                @Index(name = "ix_case_edge_case_file", columnList = "case_file_id"),
                @Index(name = "ix_case_edge_from", columnList = "from_proceeding_key"),
                @Index(name = "ix_case_edge_to", columnList = "to_proceeding_key")
        })
public class CaseEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_file_id", nullable = false)
    private Long caseFileId;

    @Column(name = "from_proceeding_key", nullable = false, length = 64)
    private String fromProceedingKey;

    @Column(name = "to_proceeding_key", nullable = false, length = 64)
    private String toProceedingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 40)
    private RecursalRelationType relationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_type", nullable = false, length = 40)
    private LegalAppealType appealType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
