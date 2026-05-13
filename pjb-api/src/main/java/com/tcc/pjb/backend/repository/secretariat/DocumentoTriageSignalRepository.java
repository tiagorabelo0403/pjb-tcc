package com.tcc.pjb.backend.repository.secretariat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.secretariat.DocumentoTriageSignal;

public interface DocumentoTriageSignalRepository extends JpaRepository<DocumentoTriageSignal, UUID> {

  @Query("""
      select d from DocumentoTriageSignal d
      where d.docId = :docId
      order by d.createdAt desc
      """)
  List<DocumentoTriageSignal> findByDocIdOrderByCreatedAtDesc(@Param("docId") UUID docId, Pageable pageable);

  default Optional<DocumentoTriageSignal> findFirstByDocId(UUID docId) {
    return findByDocIdOrderByCreatedAtDesc(docId, PageRequest.of(0, 1)).stream().findFirst();
  }
}
