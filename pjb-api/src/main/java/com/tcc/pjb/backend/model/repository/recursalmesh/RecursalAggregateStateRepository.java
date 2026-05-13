package com.tcc.pjb.backend.model.repository.recursalmesh;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;

public interface RecursalAggregateStateRepository extends JpaRepository<RecursalAggregateState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from RecursalAggregateState a where a.recursoId = :recursoId")
    Optional<RecursalAggregateState> findForUpdateByRecursoId(@Param("recursoId") String recursoId);
}
