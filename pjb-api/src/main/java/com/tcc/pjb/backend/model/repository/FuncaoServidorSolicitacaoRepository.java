package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.enums.StatusFuncaoServidorSolicitacao;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuncaoServidorSolicitacaoRepository extends JpaRepository<FuncaoServidorSolicitacao, Long> {

    List<FuncaoServidorSolicitacao> findByStatus(StatusFuncaoServidorSolicitacao status);

    List<FuncaoServidorSolicitacao> findBySolicitanteIdOrderByRequestedAtDesc(Long solicitanteId);
}
