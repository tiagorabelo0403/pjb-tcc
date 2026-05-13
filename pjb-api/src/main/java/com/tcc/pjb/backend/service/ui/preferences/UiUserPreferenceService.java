package com.tcc.pjb.backend.service.ui.preferences;

import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;
import com.tcc.pjb.backend.repository.ui.UsuarioAccessibilityPreferenceRepository;
import com.tcc.pjb.backend.service.ui.presentation.ReadingModeProperties;

@Service
public class UiUserPreferenceService {

  private final UsuarioAccessibilityPreferenceRepository repo;
  private final ReadingModeProperties reading;

  public UiUserPreferenceService(UsuarioAccessibilityPreferenceRepository repo, ReadingModeProperties reading) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.reading = Objects.requireNonNull(reading, "reading");
  }

  @Transactional
  public UsuarioAccessibilityPreference loadOrCreate(long usuarioId) {
    return repo.findByUsuarioId(usuarioId).orElseGet(() -> create(usuarioId));
  }

  @Transactional
  public UsuarioAccessibilityPreference create(long usuarioId) {
    UsuarioAccessibilityPreference p = new UsuarioAccessibilityPreference(usuarioId);
    p.setReadingModeEnabled(reading.isEnabledByDefault());
    UiReadingIntensity def = reading.getDefaultIntensity();
    p.setReadingIntensity(def == null ? UiReadingIntensity.SOFT : def);
    p.setUpdatedAt(Instant.now());

    try {
      return repo.save(p);
    } catch (DataIntegrityViolationException race) {
      return repo.findByUsuarioId(usuarioId).orElse(p);
    }
  }

  @Transactional
  public UsuarioAccessibilityPreference save(UsuarioAccessibilityPreference pref) {
    return repo.save(Objects.requireNonNull(pref, "pref"));
  }
}
