package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelMaterializationTrail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessualReadModelMaterializationTrailRepository extends JpaRepository<ProcessualReadModelMaterializationTrail, Long> {

    List<ProcessualReadModelMaterializationTrail> findTop20ByOrderByCreatedAtDesc();

    List<ProcessualReadModelMaterializationTrail> findTop20ByProjectionDomainIgnoreCaseAndProjectionKeyIgnoreCaseOrderByProjectionVersionDesc(String projectionDomain, String projectionKey);
}
