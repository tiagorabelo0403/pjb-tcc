package com.tcc.pjb.backend.model.entity.ui;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_accessibility_usage_snapshot")
public class AccessibilityUsageSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "observed_at", nullable = false)
  private Instant observedAt;

  @Column(name = "score", nullable = false)
  private int score;

  @Column(name = "confidence", nullable = false, precision = 6, scale = 4)
  private BigDecimal confidence;

  @Lob
  @Column(name = "top_reason_codes", nullable = false, columnDefinition = "TEXT")
  private String topReasonCodes;

  @Lob
  @Column(name = "top_reasons", nullable = false, columnDefinition = "TEXT")
  private String topReasons;

  @Lob
  @Column(name = "metrics_json", nullable = false, length = 12000, columnDefinition = "TEXT")
  private String metricsJson;

  @Column(name = "policy_version", nullable = false)
  private int policyVersion;

  @Column(name = "suggestion_hash", nullable = false, length = 64)
  private String suggestionHash;

  protected AccessibilityUsageSnapshot() {
  }

  public AccessibilityUsageSnapshot(
      Long usuarioId,
      Instant observedAt,
      int score,
      BigDecimal confidence,
      String topReasonCodes,
      String topReasons,
      String metricsJson,
      int policyVersion,
      String suggestionHash
  ) {
    this.usuarioId = usuarioId;
    this.observedAt = observedAt;
    this.score = score;
    this.confidence = confidence;
    this.topReasonCodes = topReasonCodes;
    this.topReasons = topReasons;
    this.metricsJson = metricsJson;
    this.policyVersion = policyVersion;
    this.suggestionHash = suggestionHash;
  }

  public Long getId() {
    return id;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public Instant getObservedAt() {
    return observedAt;
  }

  public int getScore() {
    return score;
  }

  public double getConfidence() {
    return confidence.doubleValue();
  }

  public BigDecimal getConfidenceDecimal() {
    return confidence;
  }

  public String getTopReasonCodes() {
    return topReasonCodes;
  }

  public String getTopReasons() {
    return topReasons;
  }

  public String getMetricsJson() {
    return metricsJson;
  }

  public int getPolicyVersion() {
    return policyVersion;
  }

  public String getSuggestionHash() {
    return suggestionHash;
  }
}
