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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_legal_ai_knowledge_artifact",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_legal_ai_knowledge_artifact_key", columnNames = {"source_ref_id", "artifact_key"})
        },
        indexes = {
                @Index(name = "idx_legal_ai_knowledge_artifact_source", columnList = "source_ref_id,artifact_type"),
                @Index(name = "idx_legal_ai_knowledge_artifact_branch", columnList = "branch_code,storage_lane"),
                @Index(name = "idx_legal_ai_knowledge_artifact_revision", columnList = "revision_ref_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class LegalKnowledgeCorpusArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_ref_id", nullable = false)
    private LegalKnowledgeCorpusSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_ref_id")
    private LegalKnowledgeCorpusRevision revision;

    @Column(name = "artifact_key", nullable = false, length = 160)
    private String artifactKey;

    @Column(name = "external_id", length = 160)
    private String externalId;

    @Column(name = "branch_code", length = 80)
    private String branchCode;

    @Column(name = "artifact_type", nullable = false, length = 80)
    private String artifactType;

    @Column(name = "storage_lane", nullable = false, length = 80)
    private String storageLane;

    @Column(name = "authority_level", nullable = false, length = 80)
    private String authorityLevel;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "source_url", length = 700)
    private String sourceUrl;

    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;
}
