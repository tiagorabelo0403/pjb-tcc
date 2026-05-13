package com.tcc.pjb.backend.model.dto.profile.operational;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record DistribuicaoProcessualDiagnosticoRequest(
        @NotNull RitoProcessual rito,
        @NotNull GrauJurisdicao grau,
        @NotBlank String comarca,
        @Size(min = 2, max = 2) String uf,
        BigDecimal valorCausa,
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

    public boolean hasAdvancedRelationalSignal() {
        return (preventionReference != null && !preventionReference.isBlank())
                || (processoReferencia != null && !processoReferencia.isBlank())
                || dependenciaDeclarada
                || conexaoDeclarada
                || continenciaDeclarada
                || redistribuicaoImpedimento;
    }
}
