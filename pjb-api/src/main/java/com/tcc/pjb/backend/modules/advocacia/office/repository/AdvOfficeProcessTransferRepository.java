package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessTransfer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvOfficeProcessTransferRepository extends JpaRepository<AdvOfficeProcessTransfer, Long> {

    @Query("""
            select t from AdvOfficeProcessTransfer t
            join fetch t.sourceEquipe
            join fetch t.targetEquipe
            where t.id = :transferId
            """)
    Optional<AdvOfficeProcessTransfer> fetchById(@Param("transferId") Long transferId);

    @Query("""
            select t from AdvOfficeProcessTransfer t
            join fetch t.sourceEquipe
            join fetch t.targetEquipe
            where t.sourceEquipe.id = :equipeId or t.targetEquipe.id = :equipeId
            order by t.createdAt desc
            """)
    List<AdvOfficeProcessTransfer> findByEquipe(@Param("equipeId") Long equipeId);

    @Query("""
            select t from AdvOfficeProcessTransfer t
            join fetch t.sourceEquipe
            join fetch t.targetEquipe
            where t.targetResponsibleUserId = :userId
               or t.targetEquipe.id in (
                    select m.equipe.id from MembroEquipe m
                    where m.usuario.id = :userId and m.ativo = true
               )
            order by t.createdAt desc
            """)
    List<AdvOfficeProcessTransfer> findIncomingForUser(@Param("userId") Long userId);
}
