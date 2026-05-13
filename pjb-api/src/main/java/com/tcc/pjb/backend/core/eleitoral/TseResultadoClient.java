package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.eleitoral.domain.DiplomacaoResultado;
import java.util.List;

public interface TseResultadoClient {

    List<DiplomacaoResultado> consultarDiplomacoesPendentes();
}
