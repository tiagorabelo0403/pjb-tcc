package com.tcc.pjb.backend.model.repository.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;

public interface UserCalendarSystemEventRepository extends JpaRepository<UserCalendarSystemEvent, Long> {

    Optional<UserCalendarSystemEvent> findByUsuarioIdAndDomainKey(Long usuarioId, String domainKey);

    @Query("select e from UserCalendarSystemEvent e where e.usuarioId = :usuarioId and e.at between :from and :to")
    List<UserCalendarSystemEvent> findByUsuarioIdBetween(@Param("usuarioId") Long usuarioId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);
}
