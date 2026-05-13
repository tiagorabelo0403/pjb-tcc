package com.tcc.pjb.backend.modules.atendimento.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceiptId;

public interface AtendimentoMessageReceiptRepository extends JpaRepository<AtendimentoMessageReceipt, AtendimentoMessageReceiptId> {

  List<AtendimentoMessageReceipt> findByThreadIdAndUsuarioIdAndMessageIdIn(Long threadId, Long usuarioId, List<Long> messageIds);

  @Modifying
  @Query(value = "update tb_atendimento_message_receipt set delivered_at = :at, updated_at = :at "
      + "where thread_id = :threadId and usuario_id = :uid and message_id >= :fromId and message_id <= :toId and delivered_at is null",
      nativeQuery = true)
  int markDeliveredRange(@Param("threadId") long threadId,
                         @Param("uid") long usuarioId,
                         @Param("fromId") long fromId,
                         @Param("toId") long toId,
                         @Param("at") Instant at);

  @Modifying
  @Query(value = "update tb_atendimento_message_receipt set read_at = :at, updated_at = :at "
      + "where thread_id = :threadId and usuario_id = :uid and message_id >= :fromId and message_id <= :toId and read_at is null",
      nativeQuery = true)
  int markReadRange(@Param("threadId") long threadId,
                    @Param("uid") long usuarioId,
                    @Param("fromId") long fromId,
                    @Param("toId") long toId,
                    @Param("at") Instant at);


  



  @Modifying
  @Query(value = "insert into tb_atendimento_message_receipt(message_id, thread_id, usuario_id, created_at, updated_at) "
      + "select m.id, m.thread_id, :uid, :at, :at from tb_atendimento_message m "
      + "where m.thread_id = :threadId and m.id >= :fromId and m.id <= :toId and m.sender_usuario_id <> :uid "
      + "on conflict (message_id, usuario_id) do nothing",
      nativeQuery = true)
  int ensureReceiptsForRange(@Param("threadId") long threadId,
                             @Param("uid") long usuarioId,
                             @Param("fromId") long fromId,
                             @Param("toId") long toId,
                             @Param("at") Instant at);

}
