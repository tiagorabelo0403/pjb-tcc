package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorPolicy;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorPolicyRepository extends JpaRepository<JudicialConnectorPolicy, UUID> {
    @QueryHints({
            @QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"),
            @QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true")
    })
    List<JudicialConnectorPolicy> findAllByActiveTrueOrderByConnectorSystemAscTribunalCodigoAscCreatedAtDesc();

    @QueryHints({
            @QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"),
            @QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true")
    })
    List<JudicialConnectorPolicy> findAllByConnectorSystemAndActiveTrueOrderByCreatedAtDesc(JudicialSystem connectorSystem);
}
