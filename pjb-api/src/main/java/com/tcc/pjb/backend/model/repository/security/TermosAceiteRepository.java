package com.tcc.pjb.backend.model.repository.security;

import com.tcc.pjb.backend.model.entity.security.TermosAceite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermosAceiteRepository extends JpaRepository<TermosAceite, Long> {

    @Query("select t from TermosAceite t where t.usuario.id = :usuarioId and t.versao = :versao")
    Optional<TermosAceite> findByUsuarioIdAndVersao(@Param("usuarioId") Long usuarioId, @Param("versao") String versao);
}
