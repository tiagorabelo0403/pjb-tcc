package com.tcc.pjb.backend.model.repository.executionmesh;

import com.tcc.pjb.backend.model.entity.executionmesh.ExecutionMeshState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecutionMeshStateRepository extends JpaRepository<ExecutionMeshState, String> {

    Optional<ExecutionMeshState> findByProcesso_Id(Long processoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExecutionMeshState e where e.processo.id = :processoId")
    Optional<ExecutionMeshState> findForUpdateByProcessoId(@Param("processoId") Long processoId);
}
