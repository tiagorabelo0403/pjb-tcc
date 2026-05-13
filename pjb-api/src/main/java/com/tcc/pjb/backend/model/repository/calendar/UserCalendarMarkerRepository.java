package com.tcc.pjb.backend.model.repository.calendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarMarker;

public interface UserCalendarMarkerRepository extends JpaRepository<UserCalendarMarker, Long> {

  List<UserCalendarMarker> findByUsuarioId(Long usuarioId);

  Optional<UserCalendarMarker> findByUsuarioIdAndEventTypeAndEventId(Long usuarioId, String eventType, Long eventId);
}
