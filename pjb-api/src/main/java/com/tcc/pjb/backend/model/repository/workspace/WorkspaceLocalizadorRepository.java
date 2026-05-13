package com.tcc.pjb.backend.model.repository.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceLocalizador;

@Repository
public interface WorkspaceLocalizadorRepository extends JpaRepository<WorkspaceLocalizador, UUID> {

    Optional<WorkspaceLocalizador> findByOwnerUserIdAndNomeIgnoreCase(Long ownerUserId, String nome);

    @Query("SELECT l FROM WorkspaceLocalizador l WHERE l.ownerUserId = :owner OR l.compartilhado = true ORDER BY l.ownerUserId ASC, l.nome ASC")
    List<WorkspaceLocalizador> listForUser(@Param("owner") Long owner);
}
