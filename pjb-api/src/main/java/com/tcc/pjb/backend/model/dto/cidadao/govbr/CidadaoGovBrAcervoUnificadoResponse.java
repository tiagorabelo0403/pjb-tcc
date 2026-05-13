package com.tcc.pjb.backend.model.dto.cidadao.govbr;

import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import java.time.LocalDateTime;
import java.util.List;

public record CidadaoGovBrAcervoUnificadoResponse(
        LocalDateTime generatedAt,
        String cpfMascarado,
        boolean govBrEnabled,
        boolean govBrLinked,
        String govBrNivel,
        String modoEntrada,
        String modoConsolidacao,
        LocalDateTime ultimaSincronizacaoIdentidade,
        Summary summary,
        List<SourceSummary> fontes,
        List<RoleSummary> papeis,
        List<RitoSection> ritos,
        AreaLinks links
) {

    public record Summary(
            int totalProcessos,
            int totalExternos,
            int totalLocais,
            int comAudiencia,
            int comNovidade,
            int comPendencia,
            int comStepUp,
            int sigiloReforcado
    ) {
    }

    public record SourceSummary(
            String sistemaOrigem,
            String sistemaLabel,
            int total,
            int externos,
            int locais,
            int comAudiencia,
            int comPendencia,
            int comNovidade
    ) {
    }

    public record RoleSummary(
            String papel,
            String papelLabel,
            int total
    ) {
    }

    public record RitoSection(
            String rito,
            String ritoLabel,
            String ramo,
            String colorToken,
            String colorLabel,
            int total,
            int externos,
            int locais,
            List<LegacyCaseCard> processos
    ) {
    }

    public record LegacyCaseCard(
            CidadaoProcessoCardDto processo,
            String sistemaOrigem,
            String sistemaLabel,
            String tribunalCodigo,
            String uf,
            String comarca,
            String unidadeJudicial,
            String papelProcessual,
            String papelLabel,
            boolean origemExterna,
            boolean exigeStepUp,
            boolean possuiAudiencia,
            boolean possuiPendencia,
            boolean possuiNovidade,
            String colorToken,
            String origemExternaUrl
    ) {
    }
}
