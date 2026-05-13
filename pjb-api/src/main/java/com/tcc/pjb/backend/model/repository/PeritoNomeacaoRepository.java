package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;

public interface PeritoNomeacaoRepository extends JpaRepository<PeritoNomeacao, Long> {

    boolean existsByProcesso_IdAndPerito_IdAndStatus(Long processoId, Long peritoId, PeritoNomeacaoStatus status);

    default boolean existsByProcessoIdAndPeritoIdAndStatus(Long processoId, Long peritoId, PeritoNomeacaoStatus status) {
        return existsByProcesso_IdAndPerito_IdAndStatus(processoId, peritoId, status);
    }

    boolean existsByProcesso_IdAndPerito_IdAndStatusIn(Long processoId, Long peritoId, Collection<PeritoNomeacaoStatus> statuses);

    long countByPerito_IdAndStatus(Long peritoId, PeritoNomeacaoStatus status);

    long countByPerito_IdAndStatusIn(Long peritoId, Collection<PeritoNomeacaoStatus> statuses);

    Optional<PeritoNomeacao> findTopByProcesso_IdAndPerito_IdAndStatusOrderByNomeadoEmDesc(Long processoId, Long peritoId, PeritoNomeacaoStatus status);

    List<PeritoNomeacao> findTop200ByProcesso_IdOrderByNomeadoEmDesc(Long processoId);

    List<PeritoNomeacao> findTop100ByPerito_IdAndStatusOrderByNomeadoEmDesc(Long peritoId, PeritoNomeacaoStatus status);

    List<PeritoNomeacao> findTop100ByPerito_IdAndStatusInOrderByNomeadoEmDesc(Long peritoId, Collection<PeritoNomeacaoStatus> statuses);

    List<PeritoNomeacao> findByPerito_IdAndStatusInAndNomeadoEmBetweenOrderByNomeadoEmAsc(Long peritoId,
                                                                                           Collection<PeritoNomeacaoStatus> statuses,
                                                                                           LocalDateTime from,
                                                                                           LocalDateTime to);

}
