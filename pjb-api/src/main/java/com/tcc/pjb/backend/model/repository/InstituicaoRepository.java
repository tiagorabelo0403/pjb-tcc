package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {

    List<Instituicao> findByTipo(TipoInstituicao tipo);
}
