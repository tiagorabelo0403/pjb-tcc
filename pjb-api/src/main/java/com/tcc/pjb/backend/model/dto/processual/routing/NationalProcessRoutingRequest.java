package com.tcc.pjb.backend.model.dto.processual.routing;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record NationalProcessRoutingRequest(
        @NotNull RitoProcessual rito,
        RamoDireito ramo,
        @NotNull GrauJurisdicao grau,
        @Size(min = 2, max = 2) String uf,
        @Size(max = 120) String comarca,
        BigDecimal valorCausa,
        @Size(max = 160) String classeProcessual,
        @Size(max = 180) String assunto,
        Instant referenceAt,
        @Size(max = 32) String numeroProcesso,
        @Size(max = 120) String cidade,
        @Size(max = 160) String foro,
        @Size(max = 120) String secaoJudiciaria,
        @Size(max = 120) String subsecaoJudiciaria,
        @Size(max = 120) String circunscricao,
        @Size(max = 120) String cidadeAutor,
        @Size(max = 120) String cidadeReu,
        @Size(max = 120) String cidadeFato,
        @Size(max = 120) String municipioFato,
        @Size(max = 64) String preventionReference,
        @Size(max = 64) String processoReferencia,
        @Size(max = 32) String tribunalCodigoHint,
        boolean dependenciaDeclarada,
        boolean conexaoDeclarada,
        boolean continenciaDeclarada,
        boolean pedidoLiminar,
        boolean plantaoJudicial,
        boolean segredoSolicitado,
        boolean redistribuicaoImpedimento) {

    public NationalProcessRoutingRequest {
        if (uf != null && !uf.isBlank() && uf.trim().length() != 2) {
            throw new IllegalArgumentException("UF deve conter 2 caracteres quando informada.");
        }
        if (!hasTerritorialAnchor(comarca, cidade, foro, secaoJudiciaria, subsecaoJudiciaria, circunscricao, cidadeAutor, cidadeReu, cidadeFato, municipioFato)
                && !hasRelationalAnchor(preventionReference, processoReferencia, dependenciaDeclarada, conexaoDeclarada, continenciaDeclarada, redistribuicaoImpedimento)) {
            throw new IllegalArgumentException("É necessário informar âncora territorial ou referência processual/prevenção para o diagnóstico.");
        }
        valorCausa = valorCausa == null ? null : valorCausa.stripTrailingZeros();
        classeProcessual = trimToNull(classeProcessual);
        assunto = trimToNull(assunto);
        numeroProcesso = trimToNull(numeroProcesso);
        cidade = trimToNull(cidade);
        comarca = trimToNull(comarca);
        foro = trimToNull(foro);
        secaoJudiciaria = trimToNull(secaoJudiciaria);
        subsecaoJudiciaria = trimToNull(subsecaoJudiciaria);
        circunscricao = trimToNull(circunscricao);
        cidadeAutor = trimToNull(cidadeAutor);
        cidadeReu = trimToNull(cidadeReu);
        cidadeFato = trimToNull(cidadeFato);
        municipioFato = trimToNull(municipioFato);
        preventionReference = trimToNull(preventionReference);
        processoReferencia = trimToNull(processoReferencia);
        tribunalCodigoHint = trimToNull(tribunalCodigoHint);
    }

    public boolean hasTerritorialAnchor() {
        return hasTerritorialAnchor(comarca, cidade, foro, secaoJudiciaria, subsecaoJudiciaria, circunscricao, cidadeAutor, cidadeReu, cidadeFato, municipioFato);
    }

    public boolean hasRelationalAnchor() {
        return hasRelationalAnchor(preventionReference, processoReferencia, dependenciaDeclarada, conexaoDeclarada, continenciaDeclarada, redistribuicaoImpedimento);
    }

    public boolean hasUrgencySignal() {
        return pedidoLiminar || plantaoJudicial;
    }

    public String preferredTerritory() {
        return firstNonBlank(foro, subsecaoJudiciaria, comarca, cidade, cidadeFato, municipioFato, cidadeAutor, cidadeReu, circunscricao);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean hasTerritorialAnchor(String comarca,
                                                String cidade,
                                                String foro,
                                                String secaoJudiciaria,
                                                String subsecaoJudiciaria,
                                                String circunscricao,
                                                String cidadeAutor,
                                                String cidadeReu,
                                                String cidadeFato,
                                                String municipioFato) {
        return notBlank(comarca)
                || notBlank(cidade)
                || notBlank(foro)
                || notBlank(secaoJudiciaria)
                || notBlank(subsecaoJudiciaria)
                || notBlank(circunscricao)
                || notBlank(cidadeAutor)
                || notBlank(cidadeReu)
                || notBlank(cidadeFato)
                || notBlank(municipioFato);
    }

    private static boolean hasRelationalAnchor(String preventionReference,
                                               String processoReferencia,
                                               boolean dependenciaDeclarada,
                                               boolean conexaoDeclarada,
                                               boolean continenciaDeclarada,
                                               boolean redistribuicaoImpedimento) {
        return notBlank(preventionReference)
                || notBlank(processoReferencia)
                || dependenciaDeclarada
                || conexaoDeclarada
                || continenciaDeclarada
                || redistribuicaoImpedimento;
    }

    private static String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
