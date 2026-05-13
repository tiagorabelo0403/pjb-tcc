package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.List;

public record PessoaLocalizacaoResponse(
        String correlationId,
        Instant generatedAt,
        String executorPerfil,
        String scope,
        String finalidade,
        String cpfMascarado,
        boolean enderecoEstritoLiberado,
        String nivelExposicao,
        GovernancaConsultaResumo governanca,
        SecurityPostureResumo securityPosture,
        IdentificacaoPessoa identificacao,
        List<EnderecoCandidato> enderecos,
        List<VinculoProcessualResumo> vinculosProcessuais,
        List<RestricaoResumo> restricoes,
        List<FonteConsultaResumo> fontes,
        List<String> alertas,
        String recomendacaoOperacional
) {

    public record GovernancaConsultaResumo(
            String fundamento,
            boolean possuiContextoFormal,
            boolean consultaSemProcessoAutorizada,
            boolean enderecoEstritoElegivel,
            String trilhaGovernanca,
            List<String> controlesAplicados,
            String referenciaProcedimental,
            boolean consultaPersistida
    ) {
    }

    public record SecurityPostureResumo(
            String level,
            int score,
            boolean requiresReview,
            boolean offHours,
            String releaseMode,
            boolean stepUpRequired,
            boolean stepUpSatisfied,
            String challengeHint,
            List<String> sinais
    ) {
    }

    public record IdentificacaoPessoa(
            String nome,
            String documentoMascarado,
            boolean cadastroInterno,
            String perfilInterno,
            String comarcaReferencia,
            String ufReferencia,
            int totalProcessosRelacionados,
            String nivelConfianca
    ) {
    }

    public record EnderecoCandidato(
            String fonte,
            String tipo,
            String descricao,
            String bairro,
            String cidade,
            String uf,
            String cep,
            boolean principal,
            boolean parcial,
            double confianca,
            Instant atualizadoEm
    ) {
    }

    public record VinculoProcessualResumo(
            Long processoId,
            String numero,
            String polo,
            String classe,
            String assunto,
            String tribunal,
            String status,
            Instant atualizadoEm
    ) {
    }

    public record RestricaoResumo(
            String sistema,
            String tipo,
            String situacao,
            String referencia,
            String descricao,
            Instant atualizadoEm
    ) {
    }

    public record FonteConsultaResumo(
            String fonte,
            boolean habilitada,
            boolean realtime,
            int itensEncontrados,
            List<String> highlights
    ) {
    }
}
