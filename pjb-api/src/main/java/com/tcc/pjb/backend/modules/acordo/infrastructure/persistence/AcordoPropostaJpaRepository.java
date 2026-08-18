package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcordoPropostaJpaRepository extends JpaRepository<AcordoPropostaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AcordoPropostaEntity p where p.id = :id")
    Optional<AcordoPropostaEntity> findByIdForUpdate(@Param("id") Long id);
}
