package com.tcc.pjb.backend.repository.ai.legal;

import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusArtifact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalKnowledgeCorpusArtifactRepository extends JpaRepository<LegalKnowledgeCorpusArtifact, Long> {

    long countBySource_Id(Long sourceId);

    void deleteBySource_Id(Long sourceId);

    List<LegalKnowledgeCorpusArtifact> findTop10BySource_IdOrderByTitleAsc(Long sourceId);
}
