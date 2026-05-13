package com.tcc.pjb.backend.modules.advocacia.office.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;

public interface EquipeOfficeDelegacaoRegraRepository extends JpaRepository<EquipeOfficeDelegacaoRegra, Long> {

    @Query("select r from EquipeOfficeDelegacaoRegra r where r.equipe.id = :equipeId and r.usuario.id = :userId")
    Optional<EquipeOfficeDelegacaoRegra> findByEquipeAndUser(@Param("equipeId") Long equipeId, @Param("userId") Long userId);

    @Query("select r from EquipeOfficeDelegacaoRegra r where r.equipe.id = :equipeId")
    List<EquipeOfficeDelegacaoRegra> findByEquipe(@Param("equipeId") Long equipeId);

    @Query("select r from EquipeOfficeDelegacaoRegra r where r.usuario.id = :userId")
    List<EquipeOfficeDelegacaoRegra> findByUser(@Param("userId") Long userId);

    @Query("select r from EquipeOfficeDelegacaoRegra r where r.usuario.id = :userId and r.ativo = true")
    List<EquipeOfficeDelegacaoRegra> findActiveByUser(@Param("userId") Long userId);
}
