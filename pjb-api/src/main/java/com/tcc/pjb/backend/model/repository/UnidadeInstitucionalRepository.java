package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeInstitucionalRepository extends JpaRepository<UnidadeInstitucional, Long> {

    List<UnidadeInstitucional> findByInstituicaoAndParentIsNull(Instituicao instituicao);

    List<UnidadeInstitucional> findByParent(UnidadeInstitucional parent);
}
