package com.tcc.pjb.backend.model.entity.identity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_identidade_grafo_projection_batch",
        indexes = {
                @Index(name = "idx_identidade_batch_correlacao", columnList = "correlacao_id, created_at"),
                @Index(name = "idx_identidade_batch_fingerprint", columnList = "merge_fingerprint")
        })
public class IdentidadeJuridicaProjectionBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlacao_id", nullable = false, length = 160)
    private String correlacaoId;

    @Column(name = "backend", nullable = false, length = 80)
    private String backend;

    @Column(name = "storage_key", nullable = false, length = 400)
    private String storageKey;

    @Column(name = "merge_fingerprint", nullable = false, length = 128)
    private String mergeFingerprint;

    @Column(name = "total_vertices", nullable = false)
    private int totalVertices;

    @Column(name = "total_arestas", nullable = false)
    private int totalArestas;

    @Column(name = "projection_json", nullable = false, columnDefinition = "TEXT")
    private String projectionJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public String getCorrelacaoId() {
        return correlacaoId;
    }

    public void setCorrelacaoId(String correlacaoId) {
        this.correlacaoId = correlacaoId;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getMergeFingerprint() {
        return mergeFingerprint;
    }

    public void setMergeFingerprint(String mergeFingerprint) {
        this.mergeFingerprint = mergeFingerprint;
    }

    public int getTotalVertices() {
        return totalVertices;
    }

    public void setTotalVertices(int totalVertices) {
        this.totalVertices = totalVertices;
    }

    public int getTotalArestas() {
        return totalArestas;
    }

    public void setTotalArestas(int totalArestas) {
        this.totalArestas = totalArestas;
    }

    public String getProjectionJson() {
        return projectionJson;
    }

    public void setProjectionJson(String projectionJson) {
        this.projectionJson = projectionJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
