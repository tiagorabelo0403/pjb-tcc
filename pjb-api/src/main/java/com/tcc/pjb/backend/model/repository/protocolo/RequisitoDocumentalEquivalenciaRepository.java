package com.tcc.pjb.backend.model.repository.protocolo;

import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumentalEquivalencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisitoDocumentalEquivalenciaRepository extends JpaRepository<RequisitoDocumentalEquivalencia, Long> {

    List<RequisitoDocumentalEquivalencia> findByRequisitoId(Long requisitoId);
}
