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
public class ExternalConstrictionContingencyResolver {

    public ExternalConstrictionContingencyProfile resolve(Processo processo,
                                                          String bem,
                                                          String convenio,
                                                          String statusExterno,
                                                          String referenciaExterna,
                                                          double valorOperacao) {
        String assetKind = resolveAssetKind(bem);
        String gatewayCode = resolveGateway(convenio, assetKind);
        String statusToken = normalize(statusExterno);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean highValue = valorOperacao >= 250_000D;
        boolean missingReference = referenciaExterna == null || referenciaExterna.isBlank();

        String contingencyMode = resolveContingencyMode(statusToken, gatewayCode, missingReference, highValue);
        String fallbackChannel = resolveFallbackChannel(gatewayCode, assetKind);
        String replayMode = statusToken.contains("UNAVAILABLE") || statusToken.contains("TIMEOUT") ? "REPLAY_COM_RECONCILIACAO_POSTERIOR" : "REPLAY_SELETIVO_POR_DIVERGENCIA";
        String manualReviewDesk = gatewayCode.equals("SISBAJUD") ? "MESA_FINANCEIRA_DE_CONTINGENCIA" : gatewayCode.equals("RENAJUD") ? "MESA_REGISTRAL_DE_CONTINGENCIA" : "MESA_GERAL_DE_CONTINGENCIA_EXECUTIVA";
        String proofGapMode = missingReference ? "LACUNA_PROBATORIA_CRITICA" : statusToken.contains("PARTIAL") ? "LACUNA_PROBATORIA_PARCIAL" : "LACUNA_PROBATORIA_CONTROLADA";
        String escalationLevel = highValue || sigilo ? "ESCALACAO_NIVEL_2" : "ESCALACAO_NIVEL_1";
        String queueCode = "CONTINGENCIA_CONSTRICAO_" + gatewayCode + '_' + assetKind + (sigilo ? "_SIGILO" : "");
        String inboxKey = gatewayCode.equals("SISBAJUD") ? "inbox.execucao.constricao.contingencia.financeira" : "inbox.execucao.constricao.contingencia.registral";
        TipoUsuario assignedRole = gatewayCode.equals("SISBAJUD") ? TipoUsuario.SERVIDOR : TipoUsuario.SERVIDOR_FORUM;
        int priority = Math.min(99, 84 + (highValue ? 7 : 0) + (sigilo ? 3 : 0) + (missingReference ? 2 : 0));
        boolean blocking = true;
        long dueAmount = statusToken.contains("UNAVAILABLE") || statusToken.contains("TIMEOUT") ? 6L : 12L;
        ChronoUnit dueUnit = ChronoUnit.HOURS;
        String baseLegal = gatewayCode.equals("SISBAJUD") ? "Arts. 797, 835, 854 e 921 do CPC" : "Arts. 797, 799, 835, 836 e 889 do CPC";
        String finalizationTarget = statusToken.contains("REJECT") ? "REENCAMINHAMENTO_COM_CORRECAO" : "RECONCILIACAO_OU_FALLBACK_MANUAL";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        warnings.add("Contingência externa exige preservação da ordem originária, rastreio protocolar e trilha formal de reprocessamento.");
        if (missingReference) {
            warnings.add("Ausência de referência externa impede fechamento automático e exige prova complementar do gateway.");
        }
        if (highValue) {
            warnings.add("Contingência de alto valor deve subir para mesa financeira/registral reforçada e reconciliação prioritária.");
        }
        reviewChecklist.add("Conferir gateway, referência externa, divergência de retorno, replay permitido e prova mínima do ato original.");
        reviewChecklist.add("Validar fallback manual, prazo de nova consulta, base patrimonial afetada e eventual desbloqueio ou reiteração do comando.");
        fundamentos.add(baseLegal);
        fundamentos.add("Modo de contingência: " + contingencyMode.replace('_', ' '));
        fundamentos.add("Canal de fallback: " + fallbackChannel.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("assetKind", assetKind);
        metadata.put("gatewayCode", gatewayCode);
        metadata.put("externalStatus", statusToken.isBlank() ? "UNKNOWN" : statusToken);
        metadata.put("contingencyMode", contingencyMode);
        metadata.put("fallbackChannel", fallbackChannel);
        metadata.put("replayMode", replayMode);
        metadata.put("manualReviewDesk", manualReviewDesk);
        metadata.put("proofGapMode", proofGapMode);
        metadata.put("escalationLevel", escalationLevel);
        metadata.put("missingReference", missingReference);
        metadata.put("highValue", highValue);
        metadata.put("sigilo", sigilo);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExternalConstrictionContingencyProfile(
                assetKind,
                gatewayCode,
                contingencyMode,
                fallbackChannel,
                replayMode,
                manualReviewDesk,
                proofGapMode,
                escalationLevel,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                finalizationTarget,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveContingencyMode(String statusToken, String gatewayCode, boolean missingReference, boolean highValue) {
        if (statusToken.contains("UNAVAILABLE") || statusToken.contains("TIMEOUT")) {
            return gatewayCode.equals("SISBAJUD") ? "CONTINGENCIA_FINANCEIRA_COM_REPETICAO_PRIORITARIA" : "CONTINGENCIA_REGISTRAL_COM_RECONSULTA_PRIORITARIA";
        }
        if (statusToken.contains("PARTIAL")) {
            return "CONTINGENCIA_POR_RESULTADO_PARCIAL";
        }
        if (statusToken.contains("REJECT")) {
            return "CONTINGENCIA_POR_REJEICAO_DE_GATEWAY";
        }
        if (missingReference) {
            return highValue ? "CONTINGENCIA_POR_LASTRO_INSUFICIENTE_CRITICA" : "CONTINGENCIA_POR_LASTRO_INSUFICIENTE";
        }
        return "CONTINGENCIA_POR_DIVERGENCIA_OPERACIONAL";
    }

    private String resolveFallbackChannel(String gatewayCode, String assetKind) {
        if (gatewayCode.equals("SISBAJUD")) {
            return "OFICIO_FINANCEIRO_E_REPROCESSAMENTO";
        }
        if (gatewayCode.equals("RENAJUD")) {
            return "RESTRICAO_REGISTRAL_MANUAL_E_REPROCESSAMENTO";
        }
        if (gatewayCode.equals("CNIB") || assetKind.equals("IMOVEL")) {
            return "AVERBACAO_REGISTRAL_E_OFICIO_CARTORARIO";
        }
        return "OFICIO_ELETRONICO_COM_CONFIRMACAO_OPERACIONAL";
    }

    private String resolveGateway(String convenio, String assetKind) {
        String token = normalize(convenio);
        if (token.contains("SISBAJUD") || assetKind.equals("DINHEIRO")) {
            return "SISBAJUD";
        }
        if (token.contains("RENAJUD") || assetKind.equals("VEICULO")) {
            return "RENAJUD";
        }
        if (token.contains("CNIB") || assetKind.equals("IMOVEL")) {
            return "CNIB";
        }
        if (token.contains("EMPRESAR") || assetKind.equals("QUOTAS_SOCIAIS")) {
            return "REGISTRO_EMPRESARIAL_INTEGRADO";
        }
        return "OFICIO_ELETRONICO";
    }

    private String resolveAssetKind(String bem) {
        String token = normalize(bem);
        if (token.contains("DINHEIRO") || token.contains("CONTA") || token.contains("SALDO")) {
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
