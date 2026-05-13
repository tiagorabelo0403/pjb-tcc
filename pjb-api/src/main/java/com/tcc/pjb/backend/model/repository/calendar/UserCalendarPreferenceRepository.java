package com.tcc.pjb.backend.model.repository.calendar;

import com.tcc.pjb.backend.model.entity.calendar.UserCalendarPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCalendarPreferenceRepository extends JpaRepository<UserCalendarPreference, Long> {

    Optional<UserCalendarPreference> findByUsuarioId(Long usuarioId);
}
