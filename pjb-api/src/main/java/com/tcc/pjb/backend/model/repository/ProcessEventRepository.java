package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;

public interface ProcessEventRepository extends JpaRepository<ProcessEventEnvelope, Long> {

    @Query("select max(e.seq) from ProcessEventEnvelope e where e.processoId = :pid")
    Optional<Long> findMaxSeq(@Param("pid") Long processoId);

    @Query("""
            select e from ProcessEventEnvelope e
            where e.processoId = :pid
            order by e.seq desc, e.id desc
            """)
    List<ProcessEventEnvelope> findLatestByProcessoId(@Param("pid") Long processoId, Pageable pageable);

    default Optional<String> findLastChainHash(@Param("pid") Long processoId) {
        return findLatestByProcessoId(processoId, PageRequest.of(0, 1)).stream().map(ProcessEventEnvelope::getChainHash).findFirst();
    }

    default Optional<ProcessEventEnvelope> findLastEnvelope(@Param("pid") Long processoId) {
        return findLatestByProcessoId(processoId, PageRequest.of(0, 1)).stream().findFirst();
    }

    List<ProcessEventEnvelope> findAllByProcessoIdOrderBySeqAsc(Long processoId);

    @Query("""
            select e from ProcessEventEnvelope e
            where e.processoId = :processoId
              and e.payloadHash = :payloadHash
            order by e.seq desc, e.id desc
            """)
    List<ProcessEventEnvelope> findByProcessoIdAndPayloadHashOrderBySeqDescIdDesc(@Param("processoId") Long processoId,
                                                                                  @Param("payloadHash") String payloadHash,
                                                                                  Pageable pageable);

    default Optional<ProcessEventEnvelope> findFirstByProcessoIdAndPayloadHash(Long processoId, String payloadHash) {
        return findByProcessoIdAndPayloadHashOrderBySeqDescIdDesc(processoId, payloadHash, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
            select e from ProcessEventEnvelope e
            where e.processoId = :processoId
              and e.seq = :seq
            order by e.id desc
            """)
    List<ProcessEventEnvelope> findByProcessoIdAndSeqOrderByIdDesc(@Param("processoId") Long processoId,
                                                                   @Param("seq") Long seq,
                                                                   Pageable pageable);

    default Optional<ProcessEventEnvelope> findFirstByProcessoIdAndSeq(Long processoId, Long seq) {
        return findByProcessoIdAndSeqOrderByIdDesc(processoId, seq, PageRequest.of(0, 1)).stream().findFirst();
    }

    Optional<ProcessEventEnvelope> findFirstByProcessoIdAndEventTypeOrderBySeqDesc(Long processoId, String eventType);

    List<ProcessEventEnvelope> findTop20ByProcessoIdAndEventTypeOrderBySeqDesc(Long processoId, String eventType);

    @Query("select e from ProcessEventEnvelope e where e.processoId = :pid and e.eventType in :types order by e.seq desc")
    List<ProcessEventEnvelope> findRecentByProcessoIdAndTypes(@Param("pid") Long processoId, @Param("types") List<String> types, Pageable pageable);

    long countByProcessoId(Long processoId);
}
