package com.tcc.pjb.backend.modules.advocacia.office.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;

public interface EquipeOfficePolicyRepository extends JpaRepository<EquipeOfficePolicy, Long> {

    @Query("select p from EquipeOfficePolicy p where p.equipe.id = :equipeId")
    Optional<EquipeOfficePolicy> findByEquipeId(@Param("equipeId") Long equipeId);
}
