package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncCursorEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbSubstituicaoComunicacaoSyncCursorRepository extends JpaRepository<PjbSubstituicaoComunicacaoSyncCursorEntity, Long> {

    List<PjbSubstituicaoComunicacaoSyncCursorEntity> findByExecucaoIdOrderByJanelaInicioAsc(Long execucaoId);

    Optional<PjbSubstituicaoComunicacaoSyncCursorEntity> findByExecucaoIdAndCanalOrigemAndJanelaInicioAndJanelaFim(Long execucaoId,
                                                                                                                     String canalOrigem,
                                                                                                                     Instant janelaInicio,
                                                                                                                     Instant janelaFim);

    List<PjbSubstituicaoComunicacaoSyncCursorEntity> findByTribunalCodigoOrderByUpdatedAtDesc(String tribunalCodigo);
}
