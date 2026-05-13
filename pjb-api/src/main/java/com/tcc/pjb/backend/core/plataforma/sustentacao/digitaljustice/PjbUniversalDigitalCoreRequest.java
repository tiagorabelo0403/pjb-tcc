package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.math.BigDecimal;
import java.util.List;

public record PjbUniversalDigitalCoreRequest(
        String tribunalCode,
        String comarcaCode,
        String varaReferencia,
        String classeProcessual,
        String assuntoPrincipal,
        BigDecimal valorCausa,
        boolean juizoDigitalDisponivel,
        boolean parteAutoraAceitaTramitacaoDigital,
        boolean parteDemandadaOposicaoTempestiva,
        boolean exigePericiaTecnicaComplexa,
        boolean possuiPessoaVulneravel,
        boolean pedidoUrgente,
        boolean distribuicaoConcluida,
        List<String> sinaisProcessuais
) {

    public List<String> safeSignals() {
        return sinaisProcessuais == null ? List.of() : List.copyOf(sinaisProcessuais);
    }
}
