package com.tcc.pjb.backend.core.processo.eca;

import java.util.List;

public record EcaSigiloPolicy(
        boolean sigiloAbsoluto,
        boolean restringirAcessoPublico,
        boolean exigirAutenticacaoReforçada,
        boolean bloquearExportacaoDados,
        List<String> perfisAutorizados,
        String fundamentoLegal
) {
    public static EcaSigiloPolicy padrao() {
        return new EcaSigiloPolicy(
                true,
                true,
                true,
                true,
                List.of(
                        "MAGISTRADO_DIRETO",
                        "MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR",
                        "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
                        "ADVOGADO_DIRETO",
                        "ASSISTENCIA_SOCIAL__ASSISTENCIA_SOCIAL_OPERACAO",
                        "CONSELHO_TUTELAR__CONSELHO_TUTELAR_TITULAR"
                ),
                "ECA art. 143 e art. 247 — segredo de justiça obrigatório em todos os atos"
        );
    }

    public EcaSigiloPolicy {
        perfisAutorizados = perfisAutorizados == null ? List.of() : List.copyOf(perfisAutorizados);
    }
}
