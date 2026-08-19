package com.tcc.pjb.backend.modules.suporte.repository;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicket;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @Query("SELECT t FROM SupportTicket t WHERE t.abertoPorId = :usuarioId ORDER BY t.criadoEm DESC")
    List<SupportTicket> findByAbertoPorId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT t FROM SupportTicket t WHERE t.status IN :statuses ORDER BY t.criadoEm ASC")
    List<SupportTicket> findFila(@Param("statuses") List<SupportTicketStatus> statuses);

    long countByAbertoPorIdAndStatusIn(Long abertoPorId, List<SupportTicketStatus> statuses);
}
