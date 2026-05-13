package com.tcc.pjb.backend.model.entity.recursalmesh;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_recursal_mesh_reindex_checkpoint")
public class RecursalMeshReindexCheckpoint {

    @Id
    @Column(name = "checkpoint_key", nullable = false, length = 120)
    private String checkpointKey;

    @Column(name = "index_name", nullable = false, length = 120)
    private String indexName;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "lock_owner", length = 120)
    private String lockOwner;

    @Column(name = "last_processed_updated_at")
    private Instant lastProcessedUpdatedAt;

    @Column(name = "last_processed_recurso_id", length = 160)
    private String lastProcessedRecursoId;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "indexed_count", nullable = false)
    private int indexedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "batch_count", nullable = false)
    private int batchCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getCheckpointKey() { return checkpointKey; }
    public void setCheckpointKey(String checkpointKey) { this.checkpointKey = checkpointKey; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLockOwner() { return lockOwner; }
    public void setLockOwner(String lockOwner) { this.lockOwner = lockOwner; }
    public Instant getLastProcessedUpdatedAt() { return lastProcessedUpdatedAt; }
    public void setLastProcessedUpdatedAt(Instant lastProcessedUpdatedAt) { this.lastProcessedUpdatedAt = lastProcessedUpdatedAt; }
    public String getLastProcessedRecursoId() { return lastProcessedRecursoId; }
    public void setLastProcessedRecursoId(String lastProcessedRecursoId) { this.lastProcessedRecursoId = lastProcessedRecursoId; }
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public int getIndexedCount() { return indexedCount; }
    public void setIndexedCount(int indexedCount) { this.indexedCount = indexedCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public int getBatchCount() { return batchCount; }
    public void setBatchCount(int batchCount) { this.batchCount = batchCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
