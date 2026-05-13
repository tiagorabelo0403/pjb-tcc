package com.tcc.pjb.backend.modules.atendimento.repository;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AtendimentoAttachmentRepository extends JpaRepository<AtendimentoAttachment, Long> {

    Optional<AtendimentoAttachment> findByIdAndThreadId(Long id, Long threadId);

    @Query("select a from AtendimentoAttachment a where a.status = :status and a.createdAt >= :minCreatedAt order by a.id asc")
    List<AtendimentoAttachment> findPendingSince(@Param("status") AtendimentoAttachmentStatus status,
                                                 @Param("minCreatedAt") Instant minCreatedAt);

    @Query("select a from AtendimentoAttachment a where a.createdAt < :cutoff and a.status in (com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus.READY, com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus.REJECTED) order by a.id asc")
    List<AtendimentoAttachment> findForRetentionCleanup(@Param("cutoff") Instant cutoff);
}
