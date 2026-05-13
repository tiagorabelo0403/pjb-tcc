package com.tcc.pjb.backend.model.repository.processo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcc.pjb.backend.model.entity.processo.ProcessoNote;

public interface ProcessoNoteRepository extends JpaRepository<ProcessoNote, Long> {

  List<ProcessoNote> findByProcessoIdOrderByUpdatedAtDesc(Long processoId);

  Optional<ProcessoNote> findByIdAndProcessoId(Long id, Long processoId);

  long countByProcessoId(Long processoId);
}
