package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorAdminOperation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorAdminOperationRepository extends JpaRepository<JudicialConnectorAdminOperation, UUID> {
    List<JudicialConnectorAdminOperation> findTop100ByOrderByCreatedAtDesc();
}
