package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComarcaRepository extends JpaRepository<Comarca, Long> {
    Optional<Comarca> findByMunicipioSedeIbgeAndTribunal(String municipioSedeIbge, Tribunal tribunal);
    Optional<Comarca> findByNomeIgnoreCaseAndUf(String nome, String uf);
}
