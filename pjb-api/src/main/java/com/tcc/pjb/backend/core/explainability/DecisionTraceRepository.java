package com.tcc.pjb.backend.core.explainability;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionTraceRepository extends JpaRepository<DecisionTrace, Long> {

    List<DecisionTrace> findTop200BySubjectTypeAndSubjectIdOrderByCreatedAtDesc(String subjectType, String subjectId);
}
