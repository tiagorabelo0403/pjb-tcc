package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.ProvidenciaInstitucional;

@Repository
public interface ProvidenciaInstitucionalRepository extends JpaRepository<ProvidenciaInstitucional, Long> {

    List<ProvidenciaInstitucional> findByEvento_IdIn(List<Long> eventoIds);
}
