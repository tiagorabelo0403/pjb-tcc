package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record DistribuicaoProcessualRequest(
        @NotBlank String numeroProcesso,
        @NotBlank @Size(min = 2, max = 2) String uf,
        @NotBlank String comarca,
        @NotNull RitoProcessual rito,
        @PositiveOrZero double valorCausa,
        String autor,
        String reu,
        @NotNull GrauJurisdicao grau,
        String cidade,
        String foro,
        String secaoJudiciaria,
        String subsecaoJudiciaria,
        String circunscricao,
        String cidadeAutor,
        String cidadeReu,
        String cidadeFato,
        String municipioFato,
        String preventionReference,
        String processoReferencia,
        String classeProcessual,
        String assunto,
        boolean dependenciaDeclarada,
        boolean conexaoDeclarada,
        boolean continenciaDeclarada,
        boolean pedidoLiminar,
        boolean plantaoJudicial,
        boolean segredoSolicitado,
        boolean redistribuicaoImpedimento) {

    public boolean hasRelationalSignal() {
        return notBlank(preventionReference)
                || notBlank(processoReferencia)
                || dependenciaDeclarada
                || conexaoDeclarada
                || continenciaDeclarada
                || redistribuicaoImpedimento;
    }

    public boolean hasTerritorialSignal() {
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

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
