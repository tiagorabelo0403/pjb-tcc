package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaTutelaColetivaResponse(
        int scoreNacional,
        boolean malhaTutelaColetivaPronta,
        boolean tutelaColetivaConectada,
        boolean demandasEstruturaisGovernadas,
        boolean execucaoColetivaGovernada,
        boolean cumprimentoMassaGovernado,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaTutelaColetivaTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
}
