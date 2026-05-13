package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;

@Repository
public interface AtendimentoThreadRepository extends JpaRepository<AtendimentoThread, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from AtendimentoThread t where t.id = :id")
  Optional<AtendimentoThread> findByIdForUpdate(@Param("id") Long id);

  List<AtendimentoThread> findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioIdOrderByUpdatedAtDescIdDesc(Long processoId, Long advogadoId, Long cidadaoUsuarioId, Pageable pageable);

  default Optional<AtendimentoThread> findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioId(Long processoId, Long advogadoId, Long cidadaoUsuarioId) {
    return findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioIdOrderByUpdatedAtDescIdDesc(processoId, advogadoId, cidadaoUsuarioId, PageRequest.of(0, 1)).stream().findFirst();
  }

  Page<AtendimentoThread> findByAdvogadoIdOrderByUpdatedAtDesc(Long advogadoId, Pageable pageable);

  Page<AtendimentoThread> findByCidadaoUsuarioIdOrderByUpdatedAtDesc(Long cidadaoUsuarioId, Pageable pageable);

  List<AtendimentoThread> findByProcessoIdAndAdvogadoIdOrderByUpdatedAtDesc(Long processoId, Long advogadoId);

  List<AtendimentoThread> findByProcessoIdAndCidadaoUsuarioIdOrderByUpdatedAtDesc(Long processoId, Long cidadaoUsuarioId);

  List<AtendimentoThread> findByProcessoIdOrderByUpdatedAtDesc(Long processoId);
}
