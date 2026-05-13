package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorPolicyRepository extends JpaRepository<JudicialConnectorPolicy, UUID> {
    List<JudicialConnectorPolicy> findAllByActiveTrueOrderByConnectorSystemAscTribunalCodigoAscCreatedAtDesc();
    List<JudicialConnectorPolicy> findAllByConnectorSystemAndActiveTrueOrderByCreatedAtDesc(JudicialSystem connectorSystem);
}
