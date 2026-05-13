package com.tcc.pjb.backend.repository.ai.legal;

import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalKnowledgeCorpusSourceRepository extends JpaRepository<LegalKnowledgeCorpusSource, Long> {

    Optional<LegalKnowledgeCorpusSource> findBySourceId(String sourceId);

    List<LegalKnowledgeCorpusSource> findAllByOrderByOfficialSourceDescDoctrineSourceAscInstitutionAscTitleAsc();

    long countByOfficialSourceTrue();

    long countByDoctrineSourceTrue();
}
