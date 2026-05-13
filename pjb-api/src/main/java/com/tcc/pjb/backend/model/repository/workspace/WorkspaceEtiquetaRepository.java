package com.tcc.pjb.backend.model.repository.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceEtiqueta;

@Repository
public interface WorkspaceEtiquetaRepository extends JpaRepository<WorkspaceEtiqueta, UUID> {

    List<WorkspaceEtiqueta> findAllByOwnerUserIdOrderByNomeAsc(Long ownerUserId);

    Optional<WorkspaceEtiqueta> findByOwnerUserIdAndNomeIgnoreCase(Long ownerUserId, String nome);

    List<WorkspaceEtiqueta> findAllByOwnerUserIdInOrderByNomeAsc(List<Long> ownerUserIds);
}
