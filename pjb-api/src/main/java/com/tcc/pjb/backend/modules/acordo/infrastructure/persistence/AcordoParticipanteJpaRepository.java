package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcordoParticipanteJpaRepository extends JpaRepository<AcordoParticipanteEntity, Long> {

    Optional<AcordoParticipanteEntity> findBySessaoIdAndUsuarioId(Long sessaoId, Long usuarioId);

    List<AcordoParticipanteEntity> findBySessaoIdOrderByIdAsc(Long sessaoId);

    long countBySessaoIdAndStatus(Long sessaoId, AcordoParticipanteStatus status);
}
