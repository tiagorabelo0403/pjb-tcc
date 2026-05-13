package com.tcc.pjb.backend.core.governance.idempotency.persistence.repo;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.core.governance.idempotency.persistence.entity.RequestIdempotencyEntity;

public interface RequestIdempotencyRepository extends JpaRepository<RequestIdempotencyEntity, String> {

    Optional<RequestIdempotencyEntity> findByRequestHash(String requestHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RequestIdempotencyEntity e where e.requestHash = :requestHash")
    Optional<RequestIdempotencyEntity> findForUpdate(@Param("requestHash") String requestHash);
}
