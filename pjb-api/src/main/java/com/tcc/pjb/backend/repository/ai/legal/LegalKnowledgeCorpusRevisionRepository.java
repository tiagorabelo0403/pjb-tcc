package com.tcc.pjb.backend.repository.ai.legal;

import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusRevision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalKnowledgeCorpusRevisionRepository extends JpaRepository<LegalKnowledgeCorpusRevision, Long> {

    Optional<LegalKnowledgeCorpusRevision> findTopBySource_IdOrderByHarvestedAtDesc(Long sourceId);

    long countBySource_Id(Long sourceId);
}
