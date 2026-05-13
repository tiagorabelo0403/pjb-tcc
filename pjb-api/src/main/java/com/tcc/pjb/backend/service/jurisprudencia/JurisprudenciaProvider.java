package com.tcc.pjb.backend.service.jurisprudencia;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;

public interface JurisprudenciaProvider {

    String getName();

    List<Precedente> fetchUpdates(LocalDateTime since);
}
