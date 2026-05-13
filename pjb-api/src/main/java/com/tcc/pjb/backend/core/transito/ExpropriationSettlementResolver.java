package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExpropriationSettlementResolver {

    public ExpropriationSettlementProfile resolve(Processo processo,
                                                  String bem,
                                                  String modoProduto,
                                                  String preferencia,
                                                  String subrogacao,
                                                  double valorProduto,
                                                  double saldoExecutado,
                                                  double saldoCredor) {
        String assetKind = resolveAssetKind(bem);
        String proceedsToken = normalize(modoProduto);
        String preferenceToken = normalize(preferencia);
        String subrogationToken = normalize(subrogacao);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean totalSettlement = saldoCredor <= 0.009D || valorProduto >= saldoCredor;
        boolean hasSurplus = valorProduto > saldoCredor && saldoCredor > 0D;
        boolean hasResidual = saldoCredor > valorProduto;

        String settlementMode = totalSettlement ? "LIQUIDACAO_INTEGRAL_DO_PRODUTO" : hasResidual ? "LIQUIDACAO_PARCIAL_COM_SALDO_REMANESCENTE" : "LIQUIDACAO_CONTROLADA_SEM_REFERENCIA_COMPLETA";
        String proceedsMode = proceedsToken.contains("DEPOSITO") || proceedsToken.contains("JUDICIAL") ? "PRODUTO_EM_DEPOSITO_JUDICIAL_COM_LIBERACAO_GRADUAL" : proceedsToken.contains("PARCEL") ? "PRODUTO_PARCELADO_COM_CONTROLE_DE_VENCIMENTOS" : "PRODUTO_EXPROPRIATORIO_COM_RATEIO_E_CONTROLE";
        String preferenceMode = preferenceToken.contains("TRABALH") ? "PREFERENCIA_TRABALHISTA_PRIORITARIA" : preferenceToken.contains("FISCAL") ? "PREFERENCIA_FISCAL_SUBSIDIARIA" : preferenceToken.contains("HIPOTEC") || preferenceToken.contains("GARANT") ? "PREFERENCIA_GARANTIDA_COM_SUBROGACAO_DO_ONUS" : "RATEIO_ORDINARIO_COM_RESERVA_DE_PREFERENCIA";
        String subrogationMode = subrogationToken.contains("SIM") || subrogationToken.contains("SUBROG") ? "SUBROGACAO_ATIVA_DOS_ONUS_E_PREFERENCIAS" : "SUBROGACAO_AFASTADA_COM_BAIXA_CONTROLADA_DE_ONUS";
        String balanceMode = hasResidual ? "SALDO_REMANESCENTE_EXECUTIVO_ATIVO" : hasSurplus ? "SALDO_EXCEDENTE_DEVOLUCAO_EXECUTADO" : "SALDO_ZERO_APOS_RATEIO";
        String creditorRankingMode = preferenceMode.contains("PREFERENCIA") ? "RANQUEAMENTO_REFORCADO_DE_CREDORES" : "RANQUEAMENTO_LINEAR_DE_CREDORES";
        String surplusMode = hasSurplus ? "EXCEDENTE_COM_DEVOLUCAO_CONTROLADA" : "SEM_EXCEDENTE_RELEVANTE";
        String queueCode = "EXPROPRIACAO_PRODUTO_" + assetKind + (sigilo ? "_SIGILO" : "");
        String inboxKey = "inbox.execucao.expropriacao.produto";
        TipoUsuario assignedRole = hasSurplus || hasResidual ? TipoUsuario.CONTADOR_JUDICIAL : TipoUsuario.SERVIDOR_FORUM;
        int priority = Math.min(99, 89 + (hasResidual ? 4 : 0) + (sigilo ? 3 : 0));
        boolean blocking = true;
        long dueAmount = hasResidual ? 2L : 1L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = "Arts. 904, 905, 908 e 909 do CPC";
        String terminalDispositionHint = totalSettlement ? "BAIXA_TERMINAL_INTEGRAL" : hasResidual ? "BAIXA_PARCIAL_COM_SALDO" : "BAIXA_CONDICIONADA_A_RATEIO";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (hasResidual) {
            warnings.add("Produto da expropriação não quita integralmente o crédito e exige preservação do saldo executivo remanescente.");
        }
        if (hasSurplus) {
            warnings.add("Produto supera o crédito principal e exige devolução controlada do excedente ao executado após preferências e custas.");
        }
        if (subrogationMode.contains("ATIVA")) {
            warnings.add("Sub-rogação ativa exige conferência dos ônus transferidos, dos preferenciais e da cadeia registral do bem expropriado.");
        }
        reviewChecklist.add("Conferir comissão, custas, credores preferenciais, reserva de preferência, saldo, excedente e sub-rogação.");
        reviewChecklist.add("Validar produto líquido, ordem de pagamento, destinação do excedente e atualização do saldo remanescente.");
        reviewChecklist.add("Checar compatibilidade entre liquidação do produto, homologação final e pista para baixa terminal.");
        fundamentos.add(baseLegal);
        fundamentos.add("Liquidação do produto: " + settlementMode.replace('_', ' '));
        fundamentos.add("Preferência e sub-rogação: " + preferenceMode.replace('_', ' ') + " / " + subrogationMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("assetKind", assetKind);
        metadata.put("valorProduto", valorProduto);
        metadata.put("saldoExecutado", saldoExecutado);
        metadata.put("saldoCredor", saldoCredor);
        metadata.put("hasResidual", hasResidual);
        metadata.put("hasSurplus", hasSurplus);
        metadata.put("sigilo", sigilo);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExpropriationSettlementProfile(
                assetKind,
                settlementMode,
                proceedsMode,
                preferenceMode,
                subrogationMode,
                balanceMode,
                creditorRankingMode,
                surplusMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                terminalDispositionHint,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveAssetKind(String bem) {
        String token = normalize(bem);
        if (token.contains("DINHEIRO") || token.contains("CONTA")) {
            return "DINHEIRO";
        }
        if (token.contains("FATURAMENTO") || token.contains("RECEITA")) {
            return "FATURAMENTO";
        }
        if (token.contains("VEICULO") || token.contains("PLACA")) {
            return "VEICULO";
        }
        if (token.contains("QUOTA") || token.contains("SOCIET")) {
            return "QUOTAS_SOCIAIS";
        }
        return token.contains("IMOVEL") || token.contains("MATRICULA") ? "IMOVEL" : "OUTROS_ATIVOS";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }
}
