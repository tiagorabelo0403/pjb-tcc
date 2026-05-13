package com.tcc.pjb.backend.repository.gov;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.gov.GovServiceRegistry;

@Repository
public interface GovServiceRegistryRepository extends JpaRepository<GovServiceRegistry, UUID> {

    @Query("select g from GovServiceRegistry g where g.enabled = true and g.uf in (:ufs) order by g.uf asc, g.category asc, g.name asc")
    List<GovServiceRegistry> findEnabledByUfs(@Param("ufs") Collection<String> ufs);

    @Query("select max(g.updatedAt) from GovServiceRegistry g where g.enabled = true and g.uf in (:ufs)")
    java.time.Instant findMaxUpdatedAtEnabledByUfs(@Param("ufs") Collection<String> ufs);
}
