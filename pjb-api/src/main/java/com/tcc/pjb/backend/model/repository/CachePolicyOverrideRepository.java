package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.infra.CachePolicyOverride;

@Repository
public interface CachePolicyOverrideRepository extends JpaRepository<CachePolicyOverride, Long> {

    Optional<CachePolicyOverride> findByCacheNameIgnoreCaseAndRoleNameIgnoreCase(String cacheName, String roleName);

    List<CachePolicyOverride> findByEnabledTrueOrderByCacheNameAscRoleNameAsc();
}
