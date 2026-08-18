package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.painel.PainelAlertaPrazo;

public interface PainelAlertaPrazoRepository extends JpaRepository<PainelAlertaPrazo, UUID> {

    Optional<PainelAlertaPrazo> findByNupn(String nupn);

    List<PainelAlertaPrazo> findAllByAtivoTrueOrderByDiasExcedidosDesc();

    long countByAtivoTrue();

    long countByAtivoTrueAndTribunalCodigo(String tribunalCodigo);
}
