package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComarcaRepository extends JpaRepository<Comarca, Long> {

    List<Comarca> findAllByUfIgnoreCase(String uf);
}
