package com.tcc.pjb.backend.core.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogVersionRepository extends JpaRepository<CatalogVersion, Long> {

    Optional<CatalogVersion> findTopByKeyAndActiveTrueOrderByIdDesc(String key);

    Optional<CatalogVersion> findByKeyAndVersion(String key, String version);
}
