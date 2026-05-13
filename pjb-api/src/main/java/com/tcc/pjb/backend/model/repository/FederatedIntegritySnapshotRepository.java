package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.FederatedIntegritySnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FederatedIntegritySnapshotRepository extends JpaRepository<FederatedIntegritySnapshot, UUID> {

    List<FederatedIntegritySnapshot> findTop20ByScopeTypeAndScopeValueAndSourceKindOrderByCreatedAtDesc(String scopeType,
                                                                                                         String scopeValue,
                                                                                                         String sourceKind);
}
