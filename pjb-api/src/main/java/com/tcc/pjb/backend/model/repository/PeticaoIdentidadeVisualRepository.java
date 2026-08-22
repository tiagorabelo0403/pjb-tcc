package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoIdentidadeVisual;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeticaoIdentidadeVisualRepository extends JpaRepository<PeticaoIdentidadeVisual, Long> {

    Optional<PeticaoIdentidadeVisual> findByUsuarioId(Long usuarioId);

    Optional<PeticaoIdentidadeVisual> findByEscopoAndEscopoRef(String escopo, String escopoRef);
}
