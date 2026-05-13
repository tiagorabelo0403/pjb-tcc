package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalOfficialSourceAttestationSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionalOfficialSourceAttestationSnapshotRepository extends JpaRepository<InstitutionalOfficialSourceAttestationSnapshot, Long> {
    Optional<InstitutionalOfficialSourceAttestationSnapshot> findBySubjectTypeAndSubjectId(String subjectType, String subjectId);
    Optional<InstitutionalOfficialSourceAttestationSnapshot> findByAffiliationId(String affiliationId);
    Optional<InstitutionalOfficialSourceAttestationSnapshot> findByRequestId(String requestId);
    @Query("""
            select s from InstitutionalOfficialSourceAttestationSnapshot s
            where s.subjectType = :subjectType
              and (s.dueNow = true or (s.nextRefreshAt is not null and s.nextRefreshAt <= :reference))
            order by s.updatedAt asc
            """)
    List<InstitutionalOfficialSourceAttestationSnapshot> findDueBySubjectType(@Param("subjectType") String subjectType,
                                                                              @Param("reference") Instant reference);
    List<InstitutionalOfficialSourceAttestationSnapshot> findAllByOrderByUpdatedAtAsc();
}
