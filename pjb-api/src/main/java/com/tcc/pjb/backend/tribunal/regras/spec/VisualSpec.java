package com.tcc.pjb.backend.tribunal.regras.spec;


    public record VisualSpec(
            String corPrimaria,
            String corSecundaria,
            String corAcento,
            String corTextoSobrePrimaria,
            String corFundo,
            String brasaoUrl,
            String logoHorizontalUrl,
            String faviconUrl,
            String fonteInstitucional,
            String rodapeTexto,
            Boolean usaLogoEmDocumentos,
            Boolean usaAssinaturaCertificada
    ) {}
