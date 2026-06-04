package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotacaoInstituicaoRepository extends JpaRepository<LotacaoInstituicao, Long> {

    @Query("SELECT l FROM LotacaoInstituicao l JOIN FETCH l.unidade WHERE l.usuario = :usuario AND l.fim IS NULL")
    List<LotacaoInstituicao> findAtivasByUsuario(@Param("usuario") Usuario usuario);
}
