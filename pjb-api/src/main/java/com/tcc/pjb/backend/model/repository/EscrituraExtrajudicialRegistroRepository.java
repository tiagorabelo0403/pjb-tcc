package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.extrajudicial.EscrituraExtrajudicialRegistro;

public interface EscrituraExtrajudicialRegistroRepository extends JpaRepository<EscrituraExtrajudicialRegistro, Long> {

    List<EscrituraExtrajudicialRegistro> findTop50ByCartorioResponsavel_IdOrderByLavradaEmDesc(Long cartorioResponsavelId);

    List<EscrituraExtrajudicialRegistro> findTop50ByProcessoVinculado_IdOrderByLavradaEmDesc(Long processoId);
}
