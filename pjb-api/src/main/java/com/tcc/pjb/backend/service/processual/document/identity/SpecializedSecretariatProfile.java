package com.tcc.pjb.backend.service.processual.document.identity;


    public record SpecializedSecretariatProfile(
            String secretariaSegundaInstancia,
            String secretariaInstanciaSuperior,
            String secretariaJuizadoEspecial,
            String secretariaTrabalhista,
            String secretariaEleitoral,
            String secretariaMilitar,
            String secretariaEspecializada,
            String secretariaInstanciaClassificada,
            String secretariaRamoClassificado,
            String namespacePjb,
            String painelPjb,
            String tipoLotacao
    ) {
    }
