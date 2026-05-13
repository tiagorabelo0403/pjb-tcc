package com.tcc.pjb.backend.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tcc.pjb.backend.model.entity.RitoFeedback;

@Repository
public interface RitoFeedbackRepository extends JpaRepository<RitoFeedback, UUID> {

    @Query("""
        select f.ritoResolved as ritoResolved, count(f) as cnt, coalesce(avg(f.confidence), 0) as avgConf
        from RitoFeedback f
        where f.createdAt >= :since and f.confidence is not null and f.confidence < :threshold
        group by f.ritoResolved
        order by cnt desc
    """)
    List<Object[]> lowConfidenceStats(@Param("since") OffsetDateTime since, @Param("threshold") double threshold);

    @Query("""
        select f.processoId as pid, count(f) as cnt, max(f.createdAt) as lastAt
        from RitoFeedback f
        where f.createdAt >= :since
        group by f.processoId
        order by cnt desc
    """)
    List<Object[]> mostCorrectedProcesses(@Param("since") OffsetDateTime since);

    @Query("""
        select f.ritoResolved as fromR, f.ritoChosen as toR, count(f) as cnt
        from RitoFeedback f
        where f.createdAt >= :since and (f.ritoResolved is not null and f.ritoResolved <> f.ritoChosen)
        group by f.ritoResolved, f.ritoChosen
        order by cnt desc
    """)
    List<Object[]> topSuggestions(@Param("since") OffsetDateTime since);

    @Query("""
        select f from RitoFeedback f
        where f.createdAt >= :since
          and f.ritoResolved = :ritoResolved
          and f.ritoChosen = :ritoChosen
        order by f.createdAt desc
    """)
    List<RitoFeedback> recentSamples(@Param("since") OffsetDateTime since,
                                    @Param("ritoResolved") String ritoResolved,
                                    @Param("ritoChosen") String ritoChosen);

    @Query("""
        select max(f.createdAt) from RitoFeedback f
        where f.processoId = :processoId
    """)
    Optional<OffsetDateTime> lastFeedbackAt(@Param("processoId") Long processoId);
}
