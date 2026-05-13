package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.OrgaoJudiciario;

@Repository
public interface OrgaoJudiciarioRepository extends JpaRepository<OrgaoJudiciario, Long> {

    Optional<OrgaoJudiciario> findBySigla(String sigla);

    List<OrgaoJudiciario> findByComarca(String comarca);

    List<OrgaoJudiciario> findByTipo(String tipo);

    
    List<OrgaoJudiciario> findByComarcaAndTipoContaining(String comarca, String tipo);
}