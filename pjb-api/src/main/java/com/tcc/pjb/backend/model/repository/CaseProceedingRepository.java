package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;

@Repository
public interface CaseProceedingRepository extends JpaRepository<CaseProceeding, Long> {

    Optional<CaseProceeding> findByProceedingKey(String proceedingKey);

    List<CaseProceeding> findAllByCaseFileId(Long caseFileId);

    @Query("""
            select p from CaseProceeding p
            where p.caseFileId = :caseFileId
              and p.linkedProcessoId = :linkedProcessoId
            order by p.updatedAt desc, p.id desc
            """)
    List<CaseProceeding> findByCaseFileIdAndLinkedProcessoIdOrderByUpdatedAtDescIdDesc(@Param("caseFileId") Long caseFileId,
                                                                                        @Param("linkedProcessoId") Long linkedProcessoId,
                                                                                        Pageable pageable);

    default Optional<CaseProceeding> findFirstByCaseFileIdAndLinkedProcessoId(Long caseFileId, Long linkedProcessoId) {
        return findByCaseFileIdAndLinkedProcessoIdOrderByUpdatedAtDescIdDesc(caseFileId, linkedProcessoId, PageRequest.of(0, 1)).stream().findFirst();
    }

    Optional<CaseProceeding> findFirstByCaseFileIdAndLinkedProcessoIdAndParentProceedingKeyIsNull(Long caseFileId, Long linkedProcessoId);

    @Query("""
            select p from CaseProceeding p
            where p.linkedProcessoId = :linkedProcessoId
            order by p.updatedAt desc, p.id desc
            """)
    List<CaseProceeding> findByLinkedProcessoIdOrderByUpdatedAtDescIdDesc(@Param("linkedProcessoId") Long linkedProcessoId, Pageable pageable);

    default Optional<CaseProceeding> findFirstByLinkedProcessoId(Long linkedProcessoId) {
        return findByLinkedProcessoIdOrderByUpdatedAtDescIdDesc(linkedProcessoId, PageRequest.of(0, 1)).stream().findFirst();
    }


    List<CaseProceeding> findAllByCaseFileIdAndShadowTrueAndStatus(Long caseFileId, CaseProceedingStatus status);

    
    @Query(value = """
            select p.secrecy
            from tb_case_proceeding p
            where p.case_file_id = :caseFileId
            order by pjb_sigilo_rank(p.secrecy) desc
            limit 1
            """, nativeQuery = true)
    String findMaxSecrecyTokenByCaseFileId(@Param("caseFileId") Long caseFileId);


    long countByContinuityTrack(CaseContinuityTrack continuityTrack);

    long countByLastSyncAtBefore(Instant threshold);
}
