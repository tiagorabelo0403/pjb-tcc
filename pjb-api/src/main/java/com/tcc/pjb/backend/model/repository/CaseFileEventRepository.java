package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.kernel.CaseFileEventEnvelope;

public interface CaseFileEventRepository extends JpaRepository<CaseFileEventEnvelope, Long> {

    @Query("select max(e.seq) from CaseFileEventEnvelope e where e.caseFileId = :cid")
    Optional<Long> findMaxSeq(@Param("cid") Long caseFileId);

    List<CaseFileEventEnvelope> findAllByCaseFileIdOrderBySeqAsc(Long caseFileId);

    @Query("""
            select e from CaseFileEventEnvelope e
            where e.caseFileId = :caseFileId
              and e.payloadHash = :payloadHash
            order by e.seq desc, e.id desc
            """)
    List<CaseFileEventEnvelope> findByCaseFileIdAndPayloadHashOrderBySeqDescIdDesc(@Param("caseFileId") Long caseFileId,
                                                                                   @Param("payloadHash") String payloadHash,
                                                                                   Pageable pageable);

    default Optional<CaseFileEventEnvelope> findFirstByCaseFileIdAndPayloadHash(Long caseFileId, String payloadHash) {
        return findByCaseFileIdAndPayloadHashOrderBySeqDescIdDesc(caseFileId, payloadHash, PageRequest.of(0, 1)).stream().findFirst();
    }
}
