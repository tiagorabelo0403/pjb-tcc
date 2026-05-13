package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Estados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadosRepository extends JpaRepository<Estados, String> {
    boolean existsByUfIgnoreCaseAndAtivoTrue(String uf);
}
