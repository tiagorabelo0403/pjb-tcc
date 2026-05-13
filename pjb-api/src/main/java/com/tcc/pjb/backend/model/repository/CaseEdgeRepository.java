package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.casefile.CaseEdge;

@Repository
public interface CaseEdgeRepository extends JpaRepository<CaseEdge, Long> {

    List<CaseEdge> findAllByCaseFileId(Long caseFileId);
}
