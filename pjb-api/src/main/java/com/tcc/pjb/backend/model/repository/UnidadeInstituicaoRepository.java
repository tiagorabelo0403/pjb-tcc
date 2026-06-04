package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeInstituicaoRepository extends JpaRepository<UnidadeInstituicao, Long> {

    List<UnidadeInstituicao> findByInstituicaoAndParentIsNull(Instituicao instituicao);

    List<UnidadeInstituicao> findByParent(UnidadeInstituicao parent);
}
