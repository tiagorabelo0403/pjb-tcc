package com.tcc.pjb.backend.modules.advocacia.office.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoUsage;

public interface EquipeOfficeDelegacaoUsageRepository extends JpaRepository<EquipeOfficeDelegacaoUsage, Long> {

    @Query("select u from EquipeOfficeDelegacaoUsage u where u.equipe.id = :equipeId and u.usuario.id = :userId and u.dia = :dia")
    Optional<EquipeOfficeDelegacaoUsage> findByEquipeUserDia(@Param("equipeId") Long equipeId,
                                                           @Param("userId") Long userId,
                                                           @Param("dia") LocalDate dia);
}
