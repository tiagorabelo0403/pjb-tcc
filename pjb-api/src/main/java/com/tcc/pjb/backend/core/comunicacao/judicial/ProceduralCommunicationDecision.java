package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.List;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

public record ProceduralCommunicationDecision(
        NationalPrazoEngine.TipoPrazo tipoPrazo,
        boolean priorizarRepresentanteDigital,
        boolean priorizarOficialJustica,
        boolean bloquearPresuncao,
        boolean admitirHoraCerta,
        boolean exigirCuradorSeFrustrado,
        String eixoProcedimental,
        String fundamentoSintetico,
        List<String> marcadores
) {
}
