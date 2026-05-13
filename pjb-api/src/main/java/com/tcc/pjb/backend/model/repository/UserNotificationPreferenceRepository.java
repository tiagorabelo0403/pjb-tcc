package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    Optional<UserNotificationPreference> findByUsuario_Id(Long usuarioId);
}
