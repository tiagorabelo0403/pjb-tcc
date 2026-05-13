package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaPosColetivaResponse(
        int scoreNacional,
        boolean malhaPosColetivaPronta,
        boolean coisaJulgadaColetivaGovernada,
        boolean liquidacaoColetivaGovernada,
        boolean habilitacaoIndividualGovernada,
        boolean cumprimentoPulverizadoLotesGovernado,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaPosColetivaTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
}
