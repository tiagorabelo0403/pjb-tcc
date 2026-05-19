package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.ImpedimentoMinistro;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpedimentoMinistroRepository extends JpaRepository<ImpedimentoMinistro, Long> {

    Optional<ImpedimentoMinistro> findByProcessoIdAndMinistroId(Long processoId, Long ministroId);

    boolean existsByProcessoIdAndMinistroId(Long processoId, Long ministroId);
}
