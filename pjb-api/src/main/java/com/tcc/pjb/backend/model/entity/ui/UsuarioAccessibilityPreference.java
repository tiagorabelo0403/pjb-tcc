package com.tcc.pjb.backend.model.entity.ui;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_usuario_accessibility_pref")
public class UsuarioAccessibilityPreference {

  public enum Source {
    USER,
    SUGGESTION
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "preset", nullable = false, length = 60)
  private UiAccessibilityPreset preset;

  @Column(name = "accessibility_flags", nullable = false)
  private long accessibilityFlags;

  @Column(name = "suppress_suggestions", nullable = false)
  private boolean suppressSuggestions;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 30)
  private Source source;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_evaluated_at")
  private Instant lastEvaluatedAt;

  @Column(name = "next_eligible_at")
  private Instant nextEligibleAt;

  @Column(name = "last_suggestion_hash", length = 64)
  private String lastSuggestionHash;

  @Column(name = "reading_mode_enabled", nullable = false)
  private boolean readingModeEnabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "reading_intensity", nullable = false, length = 20)
  private UiReadingIntensity readingIntensity;

  protected UsuarioAccessibilityPreference() {
  }

  public UsuarioAccessibilityPreference(Long usuarioId) {
    this.usuarioId = usuarioId;
    this.preset = UiAccessibilityPreset.DEFAULT;
    this.accessibilityFlags = 0L;
    this.suppressSuggestions = false;
    this.source = Source.USER;
    this.readingModeEnabled = true;
    this.readingIntensity = UiReadingIntensity.SOFT;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public UiAccessibilityPreset getPreset() {
    return preset;
  }

  public void setPreset(UiAccessibilityPreset preset) {
    this.preset = preset;
  }

  public long getAccessibilityFlags() {
    return accessibilityFlags;
  }

  public void setAccessibilityFlags(long accessibilityFlags) {
    this.accessibilityFlags = accessibilityFlags;
  }

  public boolean isSuppressSuggestions() {
    return suppressSuggestions;
  }

  public void setSuppressSuggestions(boolean suppressSuggestions) {
    this.suppressSuggestions = suppressSuggestions;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(Instant acceptedAt) {
    this.acceptedAt = acceptedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getLastEvaluatedAt() {
    return lastEvaluatedAt;
  }

  public void setLastEvaluatedAt(Instant lastEvaluatedAt) {
    this.lastEvaluatedAt = lastEvaluatedAt;
  }

  public Instant getNextEligibleAt() {
    return nextEligibleAt;
  }

  public void setNextEligibleAt(Instant nextEligibleAt) {
    this.nextEligibleAt = nextEligibleAt;
  }

  public String getLastSuggestionHash() {
    return lastSuggestionHash;
  }

  public void setLastSuggestionHash(String lastSuggestionHash) {
    this.lastSuggestionHash = lastSuggestionHash;
  }

  public boolean isReadingModeEnabled() {
    return readingModeEnabled;
  }

  public void setReadingModeEnabled(boolean readingModeEnabled) {
    this.readingModeEnabled = readingModeEnabled;
  }

  public UiReadingIntensity getReadingIntensity() {
    return readingIntensity;
  }

  public void setReadingIntensity(UiReadingIntensity readingIntensity) {
    this.readingIntensity = readingIntensity;
  }

  public boolean canSuggestNow(Instant now) {
    if (suppressSuggestions) {
      return false;
    }
    if (nextEligibleAt == null) {
      return true;
    }
    return !nextEligibleAt.isAfter(now);
  }
}
