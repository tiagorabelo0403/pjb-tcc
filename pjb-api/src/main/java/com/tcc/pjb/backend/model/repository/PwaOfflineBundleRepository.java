package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.offline.PwaOfflineBundle;

public interface PwaOfflineBundleRepository extends JpaRepository<PwaOfflineBundle, Long> {

    Optional<PwaOfflineBundle> findByBundleToken(String bundleToken);

    List<PwaOfflineBundle> findTop50BySolicitante_IdOrderByCreatedAtDesc(Long solicitanteId);
}
