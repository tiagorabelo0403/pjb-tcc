package com.tcc.pjb.backend.service.processual.document.identity;

import java.util.Map;


    public record InstitutionalAssignment(
            Map<String, Object> payload,
            String etiquetaLotacao,
            String unidadeJudiciariaCodigo,
            String tipoLotacao
    ) {
    }
