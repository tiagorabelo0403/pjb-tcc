package com.tcc.pjb.backend.model.entity.ai.legal;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_legal_ai_knowledge_source",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_legal_ai_knowledge_source_source_id", columnNames = "source_id")
        },
        indexes = {
                @Index(name = "idx_legal_ai_knowledge_source_lane", columnList = "storage_lane,active"),
                @Index(name = "idx_legal_ai_knowledge_source_refresh", columnList = "refresh_strategy,next_refresh_at"),
                @Index(name = "idx_legal_ai_knowledge_source_branch_summary", columnList = "institution,authority_level")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class LegalKnowledgeCorpusSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "source_id", nullable = false, length = 120)
    private String sourceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "source_kind", nullable = false, length = 80)
    private String sourceKind;

    @Column(name = "authority_level", nullable = false, length = 80)
    private String authorityLevel;

    @Column(name = "institution", nullable = false, length = 160)
    private String institution;

    @Column(name = "storage_lane", nullable = false, length = 80)
    private String storageLane;

    @Column(name = "licensing_model", nullable = false, length = 80)
    private String licensingModel;

    @Column(name = "base_url", length = 600)
    private String baseUrl;

    @Column(name = "refresh_strategy", nullable = false, length = 80)
    private String refreshStrategy;

    @Column(name = "branch_codes_json", nullable = false, columnDefinition = "TEXT")
    private String branchCodesJson;

    @Column(name = "artifact_types_json", nullable = false, columnDefinition = "TEXT")
    private String artifactTypesJson;

    @Column(name = "retrieval_tags_json", nullable = false, columnDefinition = "TEXT")
    private String retrievalTagsJson;

    @Column(name = "restrictions_json", nullable = false, columnDefinition = "TEXT")
    private String restrictionsJson;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "version_tag", nullable = false, length = 80)
    private String versionTag;

    @Column(name = "official_source", nullable = false)
    private boolean officialSource;

    @Column(name = "doctrine_source", nullable = false)
    private boolean doctrineSource;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "artifact_count", nullable = false)
    private int artifactCount;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "next_refresh_at")
    private Instant nextRefreshAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
