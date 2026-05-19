package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcordoSessaoJpaRepository extends JpaRepository<AcordoSessaoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AcordoSessaoEntity s where s.id = :id")
    Optional<AcordoSessaoEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s from AcordoSessaoEntity s
            where s.expiraEm <= :now
              and s.status not in :terminalStatuses
            order by s.expiraEm asc, s.id asc
            """)
    List<AcordoSessaoEntity> findSessoesExpiradas(@Param("now") Instant now,
                                                  @Param("terminalStatuses") Collection<AcordoSessaoStatus> terminalStatuses,
                                                  Pageable pageable);
}
