package com.tcc.pjb.backend.model.entity.ai.legal;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_legal_ai_knowledge_revision",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_legal_ai_knowledge_revision_key", columnNames = {"source_ref_id", "revision_key"})
        },
        indexes = {
                @Index(name = "idx_legal_ai_knowledge_revision_source", columnList = "source_ref_id,harvested_at"),
                @Index(name = "idx_legal_ai_knowledge_revision_status", columnList = "revision_status,harvested_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class LegalKnowledgeCorpusRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_ref_id", nullable = false)
    private LegalKnowledgeCorpusSource source;

    @Column(name = "revision_key", nullable = false, length = 96)
    private String revisionKey;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "revision_status", nullable = false, length = 40)
    private String revisionStatus;

    @Column(name = "artifact_count", nullable = false)
    private int artifactCount;

    @Column(name = "manifest_json", nullable = false, columnDefinition = "TEXT")
    private String manifestJson;

    @Column(name = "harvested_at", nullable = false)
    private Instant harvestedAt;
}
