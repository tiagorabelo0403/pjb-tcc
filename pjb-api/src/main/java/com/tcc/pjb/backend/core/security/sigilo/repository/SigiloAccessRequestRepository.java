package com.tcc.pjb.backend.core.security.sigilo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;

@Repository
public interface SigiloAccessRequestRepository extends JpaRepository<SigiloAccessRequest, UUID> {

    List<SigiloAccessRequest> findByAdvogadoIdOrderByRequestedAtDesc(Long advogadoId);

    Optional<SigiloAccessRequest> findByIdAndAdvogadoId(UUID id, Long advogadoId);

    Optional<SigiloAccessRequest> findByIdAndStatus(UUID id, SigiloAccessStatus status);

    List<SigiloAccessRequest> findByProcessoIdAndStatus(Long processoId, SigiloAccessStatus status);

    List<SigiloAccessRequest> findByStatusAndExpiresAtBefore(SigiloAccessStatus status, LocalDateTime now);
}
