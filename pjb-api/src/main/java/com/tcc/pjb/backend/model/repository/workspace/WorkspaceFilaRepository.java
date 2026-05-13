package com.tcc.pjb.backend.model.repository.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFila;

public interface WorkspaceFilaRepository extends JpaRepository<WorkspaceFila, UUID> {

    List<WorkspaceFila> findAllBySistemaTrueOrderByOrderIndexAscNomeAsc();

    List<WorkspaceFila> findAllByOwnerUserIdOrderByOrderIndexAscNomeAsc(Long ownerUserId);

    boolean existsByOwnerUserIdAndNomeIgnoreCase(Long ownerUserId, String nome);

    Optional<WorkspaceFila> findByOwnerUserIdAndId(Long ownerUserId, UUID id);
}
