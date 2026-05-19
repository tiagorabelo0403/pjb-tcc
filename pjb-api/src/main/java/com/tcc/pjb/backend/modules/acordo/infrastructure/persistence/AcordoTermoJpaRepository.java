package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcordoTermoJpaRepository extends JpaRepository<AcordoTermoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AcordoTermoEntity t where t.id = :id")
    Optional<AcordoTermoEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<AcordoTermoEntity> findFirstBySessaoIdOrderByIdDesc(Long sessaoId);

    Optional<AcordoTermoEntity> findByPropostaId(Long propostaId);
}
