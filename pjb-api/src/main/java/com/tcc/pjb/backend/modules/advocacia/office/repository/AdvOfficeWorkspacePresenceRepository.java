package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePresence;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvOfficeWorkspacePresenceRepository extends JpaRepository<AdvOfficeWorkspacePresence, Long> {

    Optional<AdvOfficeWorkspacePresence> findByEquipe_IdAndUserId(Long equipeId, Long userId);

    List<AdvOfficeWorkspacePresence> findByEquipe_IdAndLastSeenAtAfterOrderByLastSeenAtDesc(Long equipeId, Instant cutoff);

    long countByEquipe_IdAndLastSeenAtAfter(Long equipeId, Instant cutoff);

    void deleteByLastSeenAtBefore(Instant cutoff);
}
