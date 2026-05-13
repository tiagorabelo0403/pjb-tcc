package com.tcc.pjb.backend.model.repository.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceProcessoEtiqueta;

@Repository
public interface WorkspaceProcessoEtiquetaRepository extends JpaRepository<WorkspaceProcessoEtiqueta, UUID> {

    boolean existsByProcesso_IdAndEtiqueta_Id(Long processoId, UUID etiquetaId);

    Optional<WorkspaceProcessoEtiqueta> findByProcesso_IdAndEtiqueta_Id(Long processoId, UUID etiquetaId);

    void deleteByProcesso_IdAndEtiqueta_Id(Long processoId, UUID etiquetaId);

    @Query("SELECT pe FROM WorkspaceProcessoEtiqueta pe JOIN FETCH pe.etiqueta e WHERE pe.processo.id IN :ids")
    List<WorkspaceProcessoEtiqueta> findAllByProcessoIds(@Param("ids") List<Long> processoIds);

    @Query("SELECT pe FROM WorkspaceProcessoEtiqueta pe JOIN FETCH pe.etiqueta e WHERE pe.processo.id = :pid")
    List<WorkspaceProcessoEtiqueta> findAllByProcessoId(@Param("pid") Long processoId);
}
