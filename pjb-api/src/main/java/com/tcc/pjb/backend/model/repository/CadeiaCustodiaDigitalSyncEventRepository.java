package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalSyncEvent;

@Repository
public interface CadeiaCustodiaDigitalSyncEventRepository extends JpaRepository<CadeiaCustodiaDigitalSyncEvent, Long> {

    List<CadeiaCustodiaDigitalSyncEvent> findTop100ByChaveCustodiaOrderByOccurredAtDesc(String chaveCustodia);
}
