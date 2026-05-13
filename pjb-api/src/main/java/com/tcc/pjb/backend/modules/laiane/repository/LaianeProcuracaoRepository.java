package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;

public interface LaianeProcuracaoRepository extends JpaRepository<LaianeProcuracao, Long> {

    Page<LaianeProcuracao> findByAdvogado_Id(Long advogadoId, Pageable pageable);

    List<LaianeProcuracao> findByAdvogado_IdAndProcessoIdAndStatusOrderByUpdatedAtDescIdDesc(Long advogadoId, Long processoId, LaianeProcuracaoStatus status, Pageable pageable);

    default Optional<LaianeProcuracao> findByAdvogado_IdAndProcessoIdAndStatus(Long advogadoId, Long processoId, LaianeProcuracaoStatus status) {
        return findByAdvogado_IdAndProcessoIdAndStatusOrderByUpdatedAtDescIdDesc(advogadoId, processoId, status, PageRequest.of(0, 1)).stream().findFirst();
    }

    boolean existsByAdvogado_IdAndProcessoIdAndStatusIn(Long advogadoId, Long processoId, Collection<LaianeProcuracaoStatus> statuses);

    boolean existsByAdvogado_IdAndProcessoIdAndStatus(Long advogadoId, Long processoId, LaianeProcuracaoStatus status);

    default boolean existsByAdvogadoIdAndProcessoIdAndStatus(Long advogadoId, Long processoId, LaianeProcuracaoStatus status) {
        return existsByAdvogado_IdAndProcessoIdAndStatus(advogadoId, processoId, status);
    }

    Page<LaianeProcuracao> findByStatusOrderByCreatedAtDesc(LaianeProcuracaoStatus status, Pageable pageable);

    @Query("select p.processoId from LaianeProcuracao p where p.advogado.id = :advId and p.status = :status")
    List<Long> findProcessoIdsByAdvogadoAndStatus(@Param("advId") Long advogadoId, @Param("status") LaianeProcuracaoStatus status);

    @Query("select distinct p.advogado from LaianeProcuracao p where p.processoId = :pid and p.status = :status")
    List<Usuario> findDistinctAdvogadosByProcessoIdAndStatus(@Param("pid") Long processoId, @Param("status") LaianeProcuracaoStatus status);

    List<LaianeProcuracao> findByProcessoIdAndStatusOrderByCreatedAtAsc(Long processoId, LaianeProcuracaoStatus status);
}
