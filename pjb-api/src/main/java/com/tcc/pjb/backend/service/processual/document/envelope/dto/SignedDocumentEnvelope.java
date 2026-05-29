package com.tcc.pjb.backend.service.processual.document.envelope.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SignedDocumentEnvelope(
        String tituloDocumento,
        String renderedContent,
        String contentHash,
        boolean selado,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {
    public record QualifiedSignatureMetadata(
            String envelopeId,
            String assinaturaHash,
            String conteudoBaseHash,
            String documentoAssinadoHash,
            boolean rubrica,
            String rubricaEletronica,
            LocalDate data,
            LocalTime hora,
            String local,
            String signatario,
            String papelAssinante,
            String papelAssinanteDetalhado,
            String segmentoInstitucional,
            String ramoJustica,
            String esferaInstitucional,
            String instancia,
            String orgaoAssinante,
            String lotacaoAssinante,
            String registroProfissional,
            boolean coerenciaCertificadoUsuario,
            String coerenciaCertificadoUsuarioResumo,
            String sessionBindingHash,
            String replayShieldHash,
            SovereignValidationResult validacaoSoberana
    ) {
    }

    public record SovereignValidationResult(
            String status,
            String fonte,
            String politicaAssinatura,
            boolean cadeiaCustodiaElegivel,
            boolean assinaturaCompletaMaterializada,
            boolean rubricaDataHoraLocalPresentes,
            boolean classificacaoContextualCoerente,
            boolean certificadoEntradaVinculado,
            String papelAssinanteDetalhado,
            String ramoJustica,
            String instancia,
            String lotacaoAssinante,
            String sessionBindingHash,
            String replayShieldHash,
            String documentoAssinadoHash,
            List<ValidationRule> regrasAplicadas
    ) {
    }

    public record ValidationRule(
            String codigo,
            String descricao,
            String resultado
    ) {
    }
}
