package com.tcc.pjb.backend.model.repository.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.pjb.backend.model.entity.calendar.UserCalendarCustomEvent;

public interface UserCalendarCustomEventRepository extends JpaRepository<UserCalendarCustomEvent, Long> {

  List<UserCalendarCustomEvent> findByUsuarioId(Long usuarioId);

  Optional<UserCalendarCustomEvent> findByIdAndUsuarioId(Long id, Long usuarioId);

  @Query("select e from UserCalendarCustomEvent e where e.usuarioId = :usuarioId and e.at between :from and :to")
  List<UserCalendarCustomEvent> findByUsuarioIdBetween(
      @Param("usuarioId") Long usuarioId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}
