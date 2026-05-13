package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.eleitoral.FeitoEleitoralEspecial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeitoEleitoralEspecialRepository extends JpaRepository<FeitoEleitoralEspecial, Long> {

    Optional<FeitoEleitoralEspecial> findByProcessoId(Long processoId);

    List<FeitoEleitoralEspecial> findTop100ByStatusEleitoralIgnoreCaseAndDiplomadoEmIsNullOrderByIdAsc(String statusEleitoral);
}
