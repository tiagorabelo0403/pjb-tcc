package com.tcc.pjb.backend.model.dto.processual.recursal.documental;

import java.util.List;

public record RecursalDocumentViewerResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String artefatoId,
        String categoriaArtefato,
        String modoVisualizacao,
        String nivelSigilo,
        String politicaAcesso,
        String algoritmoHash,
        String hashReferencia,
        boolean downloadPermitido,
        boolean marcaDaguaObrigatoria,
        List<String> secoesObrigatorias,
        List<String> alertasTaticos,
        List<String> rotasRelacionadas) {
}
