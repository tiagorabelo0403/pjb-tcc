package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.LotacaoInstitucional;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotacaoInstitucionalRepository extends JpaRepository<LotacaoInstitucional, Long> {

    @Query("SELECT l FROM LotacaoInstitucional l WHERE l.usuario = :usuario AND l.fim IS NULL")
    List<LotacaoInstitucional> findAtivasByUsuario(@Param("usuario") Usuario usuario);
}
