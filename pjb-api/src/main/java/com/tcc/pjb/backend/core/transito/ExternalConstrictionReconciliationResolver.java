package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExternalConstrictionReconciliationResolver {

    public ExternalConstrictionReconciliationProfile resolve(Processo processo,
                                                             String bem,
                                                             String convenio,
                                                             String statusExterno,
                                                             String referenciaExterna,
                                                             double valorOperacao) {
        String assetKind = resolveAssetKind(bem, convenio);
        String gatewayCode = resolveGatewayCode(assetKind, convenio);
        String externalStatus = normalizeStatus(statusExterno);
        boolean highValue = valorOperacao >= 250_000D;
        boolean hasReference = referenciaExterna != null && !referenciaExterna.isBlank();
        String actType = externalStatus.equals("RECONCILIATION_REQUIRED") ? "RECONCILIACAO_FORCADA" : "RECONCILIACAO_STATUS_EXTERNO";
        String reconciliationStatus = resolveReconciliationStatus(externalStatus, hasReference);
        String contingencyDesk = resolveContingencyDesk(gatewayCode, externalStatus);
        String reconciliationDesk = externalStatus.equals("UNAVAILABLE") ? "MESA_CONTINGENCIA_EXTERNOS" : "MESA_RECONCILIACAO_CONSTRICAO";
        String retryWindowMode = externalStatus.equals("UNAVAILABLE") ? "RETRY_JANELA_DEGRADADA" : externalStatus.equals("PENDING") ? "RETRY_RECONSULTA_PROGRAMADA" : "SEM_RETRY_IMEDIATO";
        String proofDesk = gatewayCode.equals("OFICIO_ELETRONICO") ? "MESA_PROVA_DOCUMENTAL" : "MESA_PROVA_PROTOCOLAR";
        String finalizationMode = resolveFinalizationMode(externalStatus);
        String queueCode = "RECONCILIACAO_" + gatewayCode + '_' + externalStatus;
        String inboxKey = gatewayCode.equals("OFICIO_ELETRONICO") ? "inbox.execucao.reconciliacao.oficio" : "inbox.execucao.reconciliacao.gateway";
        TipoUsuario assignedRole = externalStatus.equals("PARTIAL_SUCCESS") || externalStatus.equals("RECONCILIATION_REQUIRED") ? TipoUsuario.SERVIDOR : TipoUsuario.SERVIDOR_FORUM;
        int priority = Math.min(99, (externalStatus.equals("UNAVAILABLE") ? 95 : externalStatus.equals("PARTIAL_SUCCESS") ? 92 : 88) + (highValue ? 3 : 0));
        boolean blocking = !externalStatus.equals("ACCEPTED");
        long dueAmount = externalStatus.equals("UNAVAILABLE") ? 2L : externalStatus.equals("PENDING") ? 6L : 4L;
        ChronoUnit dueUnit = ChronoUnit.HOURS;
        String baseLegal = gatewayCode.equals("SISBAJUD") ? "Arts. 835, 854 e 855 do CPC" : gatewayCode.equals("RENAJUD") ? "Arts. 831, 835 e 844 do CPC" : gatewayCode.equals("CNIB") ? "Arts. 799, 828 e 844 do CPC" : "Arts. 797, 798 e 805 do CPC";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (!hasReference) {
            warnings.add("Reconciliação externa sem referência protocolar forte exige saneamento do bundle probatório.");
        }
        if (externalStatus.equals("PARTIAL_SUCCESS")) {
            warnings.add("Retorno parcial requer confronto entre ordem, resposta e patrimônio ainda pendente de constrição.");
        }
        if (externalStatus.equals("UNAVAILABLE")) {
            warnings.add("Gateway indisponível exige mesa de contingência e trilha substitutiva auditável.");
        }
        if (highValue) {
            warnings.add("Reconciliação de alto valor marcada para conferência reforçada de saldo, alcance e parcialidade do retorno.");
        }
        reviewChecklist.add("Conferir protocolo, carimbo temporal, parâmetros enviados, resposta recebida e aderência entre ordem e retorno.");
        reviewChecklist.add("Verificar necessidade de replay, contingência, reconsulta e consolidação do estado patrimonial persistido.");
        fundamentos.add(baseLegal);
        fundamentos.add("Status externo: " + externalStatus.replace('_', ' '));
        fundamentos.add("Situação de reconciliação: " + reconciliationStatus.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("gatewayCode", gatewayCode);
        metadata.put("externalStatus", externalStatus);
        metadata.put("reconciliationStatus", reconciliationStatus);
        metadata.put("contingencyDesk", contingencyDesk);
        metadata.put("reconciliationDesk", reconciliationDesk);
        metadata.put("retryWindowMode", retryWindowMode);
        metadata.put("proofDesk", proofDesk);
        metadata.put("finalizationMode", finalizationMode);
        metadata.put("hasReference", hasReference);
        metadata.put("highValue", highValue);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExternalConstrictionReconciliationProfile(
                actType,
                assetKind,
                gatewayCode,
                externalStatus,
                reconciliationStatus,
                contingencyDesk,
                reconciliationDesk,
                retryWindowMode,
                proofDesk,
                finalizationMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveAssetKind(String bem, String convenio) {
        String token = normalize(bem) + ' ' + normalize(convenio);
        if (token.contains("DINHEIRO") || token.contains("SISBAJUD")) {
            return "DINHEIRO";
        }
        if (token.contains("VEICULO") || token.contains("RENAJUD")) {
            return "VEICULO";
        }
        if (token.contains("IMOVEL") || token.contains("CNIB")) {
            return "IMOVEL";
        }
        if (token.contains("QUOTA") || token.contains("SOCIET")) {
            return "QUOTAS_SOCIAIS";
        }
        if (token.contains("FATURAMENTO") || token.contains("RECEITA")) {
            return "FATURAMENTO";
        }
        return "OUTROS_ATIVOS";
    }

    private String resolveGatewayCode(String assetKind, String convenio) {
        if (convenio != null && !convenio.isBlank()) {
            return normalize(convenio).replace(' ', '_');
        }
        return switch (assetKind) {
            case "DINHEIRO" -> "SISBAJUD";
            case "VEICULO" -> "RENAJUD";
            case "IMOVEL" -> "CNIB";
            case "QUOTAS_SOCIAIS" -> "REGISTRO_EMPRESARIAL_INTEGRADO";
            default -> "OFICIO_ELETRONICO";
        };
    }

    private String normalizeStatus(String statusExterno) {
        String token = normalize(statusExterno);
        if (token.contains("PARTIAL") || token.contains("PARCIAL")) {
            return "PARTIAL_SUCCESS";
        }
        if (token.contains("REJECT") || token.contains("NEGADO") || token.contains("RECUSADO")) {
            return "REJECTED";
        }
        if (token.contains("UNAVAILABLE") || token.contains("INDISPONIVEL") || token.contains("TIMEOUT")) {
            return "UNAVAILABLE";
        }
        if (token.contains("RECONCILIATION") || token.contains("DIVERGENCIA") || token.contains("DIVERG")) {
            return "RECONCILIATION_REQUIRED";
        }
        if (token.contains("PENDING") || token.contains("PENDENTE") || token.contains("AGUARD")) {
            return "PENDING";
        }
        return "ACCEPTED";
    }

    private String resolveReconciliationStatus(String externalStatus, boolean hasReference) {
        if (!hasReference) {
            return "PENDENCIA_DE_PROVA_EXTERNAMENTE_REFERENCIADA";
        }
        return switch (externalStatus) {
            case "ACCEPTED" -> "RECONCILIADO_COM_SUCESSO";
            case "PENDING" -> "AGUARDANDO_RECONSULTA_CONTROLADA";
            case "PARTIAL_SUCCESS" -> "RECONCILIACAO_PARCIAL_COM_DIFERENCA";
            case "REJECTED" -> "RETORNO_REJEITADO_COM_REVISAO_NECESSARIA";
            case "UNAVAILABLE" -> "CONTINGENCIA_E_REPROCESSAMENTO";
            default -> "RECONCILIACAO_OBRIGATORIA_COM_DIVERGENCIA";
        };
    }

    private String resolveContingencyDesk(String gatewayCode, String externalStatus) {
        if (externalStatus.equals("UNAVAILABLE")) {
            return "MESA_CONTINGENCIA_" + gatewayCode;
        }
        if (externalStatus.equals("REJECTED")) {
            return "MESA_REVISAO_PROTOCOLO_" + gatewayCode;
        }
        return "MESA_CONFORMIDADE_" + gatewayCode;
    }

    private String resolveFinalizationMode(String externalStatus) {
        return switch (externalStatus) {
            case "ACCEPTED" -> "CONSOLIDAR_STATUS_E_ATUALIZAR_LEDGER";
            case "PENDING" -> "REAGENDAR_RECONSULTA_E_MANTER_BLOQUEIO";
            case "PARTIAL_SUCCESS" -> "CONSOLIDAR_PARCIAL_E_ABRIR_COMPLEMENTACAO";
            case "REJECTED" -> "ABRIR_REVISAO_JUDICIAL_E_REENQUADRAMENTO";
            case "UNAVAILABLE" -> "ACIONAR_CONTINGENCIA_E_RETRY";
            default -> "ABRIR_FILA_DE_RECONCILIACAO_ESPECIAL";
        };
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
