package com.tcc.pjb.backend.model.repository.recursalmesh;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalMeshReindexCheckpoint;

public interface RecursalMeshReindexCheckpointRepository extends JpaRepository<RecursalMeshReindexCheckpoint, String> {
}
