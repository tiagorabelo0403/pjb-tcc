package com.tcc.pjb.backend.model.dto.processual.cobertura;

import java.util.List;

public record ProcessoProceduralGuaranteeResponse(
        String rito,
        String ramo,
        String grupo,
        String protocoloSugerido,
        boolean segredoPadrao,
        boolean exigeMinisterioPublico,
        boolean admiteConciliacao,
        boolean admiteJuizado,
        boolean autocompositivo,
        boolean internacional,
        boolean coletivoOuEstrutural,
        List<String> justiceTracks,
        List<String> garantiasEssenciais,
        List<String> checkpointsOperacionais,
        List<String> marcadores
) {
}
