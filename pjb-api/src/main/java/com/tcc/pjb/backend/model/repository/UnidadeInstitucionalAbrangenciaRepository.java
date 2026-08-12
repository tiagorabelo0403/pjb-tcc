package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeInstitucionalAbrangenciaRepository extends JpaRepository<UnidadeInstitucionalAbrangencia, Long> {

    List<UnidadeInstitucionalAbrangencia> findByUnidadeInstitucionalId(Long unidadeInstitucionalId);
}
