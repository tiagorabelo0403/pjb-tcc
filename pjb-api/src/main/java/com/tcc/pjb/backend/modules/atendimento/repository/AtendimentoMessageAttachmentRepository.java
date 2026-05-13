package com.tcc.pjb.backend.modules.atendimento.repository;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachmentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AtendimentoMessageAttachmentRepository extends JpaRepository<AtendimentoMessageAttachment, AtendimentoMessageAttachmentId> {

    @Query("select ma from AtendimentoMessageAttachment ma where ma.id.messageId in :messageIds")
    List<AtendimentoMessageAttachment> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}
