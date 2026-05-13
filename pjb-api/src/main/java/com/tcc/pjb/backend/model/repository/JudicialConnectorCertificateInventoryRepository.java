package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCertificateInventory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorCertificateInventoryRepository extends JpaRepository<JudicialConnectorCertificateInventory, UUID> {
    Optional<JudicialConnectorCertificateInventory> findByConnectorSystemAndTribunalCodigoAndEnvironmentNameAndBindingId(JudicialSystem connectorSystem,
                                                                                                                          String tribunalCodigo,
                                                                                                                          String environmentName,
                                                                                                                          String bindingId);
    List<JudicialConnectorCertificateInventory> findAllByOrderByConnectorSystemAscTribunalCodigoAscEnvironmentNameAscBindingIdAsc();
    List<JudicialConnectorCertificateInventory> findAllByConnectorSystemOrderByTribunalCodigoAscEnvironmentNameAscBindingIdAsc(JudicialSystem connectorSystem);
}
