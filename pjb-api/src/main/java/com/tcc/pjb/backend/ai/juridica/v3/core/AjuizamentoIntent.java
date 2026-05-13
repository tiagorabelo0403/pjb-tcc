package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import java.util.List;

public record AjuizamentoIntent(
        String rito,
        String ramoDireito,
        String subRamo,
        String esfera,
        String competencia,
        String tipoAcao,
        String fundamento,
        double confianca,
        List<String> camposObrigatorios,
        List<String> alertas,
        List<String> documentosEssenciais,
        List<String> proximosPassos,
        boolean segredoJustica,
        boolean exigeMP,
        boolean admiteConciliacao,
        ProceduralRoutingReport proceduralRouting
) {
}
