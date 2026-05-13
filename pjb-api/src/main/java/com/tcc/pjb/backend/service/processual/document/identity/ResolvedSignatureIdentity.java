package com.tcc.pjb.backend.service.processual.document.identity;

import java.util.List;
import java.util.Map;


    public record ResolvedSignatureIdentity(
            String papelDetalhado,
            String segmentoInstitucional,
            String ramoJustica,
            String esferaInstitucional,
            String instancia,
            String orgaoAssinante,
            Map<String, Object> lotacaoInstitucional,
            String lotacaoAssinante,
            String etiquetaHierarquica,
            Map<String, Object> classificacao,
            ResolvedPersonIdentity personIdentity,
            EntryCertificateContext entryCertificate,
            List<String> governanceTags
    ) {
    }
