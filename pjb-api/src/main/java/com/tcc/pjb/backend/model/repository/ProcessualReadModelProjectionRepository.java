package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessualReadModelProjectionRepository extends JpaRepository<ProcessualReadModelProjection, Long> {

    Optional<ProcessualReadModelProjection> findByDomainIgnoreCaseAndMaterializationKeyIgnoreCase(String domain, String materializationKey);

    List<ProcessualReadModelProjection> findTop20ByOrderByUpdatedAtDesc();

    long countByStatusIgnoreCase(String status);

    @Query("""
            select p from ProcessualReadModelProjection p
             where upper(p.domain) = upper(:domain)
               and (:tribunalCode = '' or upper(coalesce(p.tribunalCode, '')) = :tribunalCode)
               and (:ramoCode = '' or upper(coalesce(p.ramoCode, '')) = :ramoCode)
               and (:scopeKey = '' or upper(coalesce(p.scopeKey, '')) = :scopeKey)
             order by p.updatedAt desc
            """)
    List<ProcessualReadModelProjection> findForRecomposition(@Param("domain") String domain,
                                                             @Param("tribunalCode") String tribunalCode,
                                                             @Param("ramoCode") String ramoCode,
                                                             @Param("scopeKey") String scopeKey,
                                                             Pageable pageable);
}
