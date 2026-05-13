package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.List;

public final class RecursalRetratacaoBlueprint {

    private RecursalRetratacaoBlueprint() {
    }

    public static boolean trilhaPotencial(String rota) {
        return switch (rota) {
            case "APELACAO", "AGRAVO_DE_INSTRUMENTO", "EMBARGOS_DECLARACAO" -> true;
            default -> false;
        };
    }

    public static List<String> secoesMinimas() {
        return List.of(
                RecursalFormalSectionLabels.CABIMENTO,
                RecursalFormalSectionLabels.TEMPESTIVIDADE,
                RecursalFormalSectionLabels.PEDIDO_SANEAMENTO_VICIO_FORMAL,
                RecursalFormalSectionLabels.RAZOES_RECURSAIS
        );
    }

    public static List<String> passos() {
        return List.of(
                "CHECAR_RETRATACAO_POTENCIAL",
                "ISOLAR_CAPITULO_PASSIVEL_DE_REVISAO",
                "VALIDAR_SE_HA_REENVIO_OU_MANTENCAO_DA_TRILHA",
                "AUDITAR_IMPACTO_NA_ROTA_PRIORITARIA"
        );
    }
}
