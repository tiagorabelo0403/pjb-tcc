package com.tcc.pjb.backend.service.processual.document.identity;

import java.util.Map;


    public record ResolvedPersonIdentity(
            boolean identidadeConferida,
            String nivelConfianca,
            int scoreConfianca,
            String nomeCertificado,
            String cpfCertificado,
            String emailCertificado,
            String registroCertificado,
            String oabCertificado,
            String secretariaRecursal,
            String secretariaEmbargos,
            Map<String, Object> payload,
            String coerenciaResumo
    ) {
    }
