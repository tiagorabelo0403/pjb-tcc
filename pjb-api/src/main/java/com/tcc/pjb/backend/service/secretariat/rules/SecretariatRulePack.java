package com.tcc.pjb.backend.service.secretariat.rules;

import java.time.Duration;
import java.util.List;

public record SecretariatRulePack(
        String ramoDireito,
        Duration prazoResposta,
        String despachoTemplate,
        List<String> templatesDisponiveis,
        boolean processamentoEmHoras,
        boolean exigeAtuacaoMinisterioPublico,
        boolean admiteFluxoConciliatorio,
        boolean geraSigiloAutomatico
) {
}
