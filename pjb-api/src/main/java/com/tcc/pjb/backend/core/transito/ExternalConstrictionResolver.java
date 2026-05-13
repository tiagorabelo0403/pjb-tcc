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
public class ExternalConstrictionResolver {

    public ExternalConstrictionProfile resolve(Processo processo,
                                               String ato,
                                               String bem,
                                               String convenio,
                                               double valorOperacao) {
        String actType = normalizeActType(ato);
        String assetKind = resolveAssetKind(bem, convenio);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean highValue = valorOperacao >= 250_000D;
        String gatewayCode = resolveGatewayCode(assetKind, convenio);
        String requestMode = resolveRequestMode(gatewayCode, assetKind);
        String protocolMode = gatewayCode.equals("OFICIO_ELETRONICO") ? "PROTOCOLO_ASSINCRONO" : "PROTOCOLO_ESTRUTURADO";
        String responseMode = resolveResponseMode(gatewayCode, assetKind);
        String auditMode = gatewayCode.equals("OFICIO_ELETRONICO") ? "AUDITORIA_DOCUMENTAL" : "AUDITORIA_BIDIRECIONAL";
        String retryMode = highValue || sigilo ? "RETRY_CONTROLADO_COM_DUPLA_CONFERENCIA" : "RETRY_EXPONENCIAL_AUDITAVEL";
        String contingencyMode = resolveContingencyMode(gatewayCode, assetKind);
        String reconciliationMode = gatewayCode.equals("OFICIO_ELETRONICO") ? "RECONCILIACAO_HUMANA_ASSISTIDA" : "RECONCILIACAO_AUTOMATICA_COM_RECONSULTA";
        String queueCode = resolveQueueCode(gatewayCode, sigilo);
        String inboxKey = resolveInboxKey(gatewayCode, assetKind);
        TipoUsuario assignedRole = gatewayCode.equals("OFICIO_ELETRONICO") ? TipoUsuario.SERVIDOR : TipoUsuario.SERVIDOR_FORUM;
        int priority = Math.min(99, (gatewayCode.equals("SISBAJUD") ? 96 : 90) + (sigilo ? 2 : 0) + (highValue ? 1 : 0));
        boolean blocking = !gatewayCode.equals("OFICIO_ELETRONICO");
        long dueAmount = gatewayCode.equals("OFICIO_ELETRONICO") ? 2L : 6L;
        ChronoUnit dueUnit = gatewayCode.equals("OFICIO_ELETRONICO") ? ChronoUnit.DAYS : ChronoUnit.HOURS;
        String statusTarget = gatewayCode.equals("OFICIO_ELETRONICO") ? "PENDING" : "ACCEPTED";
        String proofBundleMode = gatewayCode.equals("OFICIO_ELETRONICO") ? "OFICIO_COM_COMPROVANTE" : "PROTOCOLO_RESPOSTA_HASH_RETORNO";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (sigilo) {
            warnings.add("Integração externa em caso sigiloso exige mascaramento, trilha de acesso e reconciliação com acesso restrito.");
        }
        if (highValue) {
            warnings.add("Ordem externa de alto valor deve ter protocolo estável, dupla conferência e reconciliação obrigatória.");
        }
        reviewChecklist.add("Conferir aderência do gateway, ordem judicial, parâmetros enviados e protocolo de resposta esperado.");
        reviewChecklist.add("Validar política de replay, contingência e reconciliação antes de reenviar ordem externa.");
        if (!gatewayCode.equals("OFICIO_ELETRONICO")) {
            reviewChecklist.add("Reconsultar resposta externa quando houver retorno parcial, timeout ou divergência entre ordem e resultado.");
        }
        fundamentos.add(resolveBaseLegal(assetKind));
        fundamentos.add("Gateway: " + gatewayCode.replace('_', ' '));
        fundamentos.add("Modo de resposta: " + responseMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("gatewayCode", gatewayCode);
        metadata.put("requestMode", requestMode);
        metadata.put("protocolMode", protocolMode);
        metadata.put("responseMode", responseMode);
        metadata.put("auditMode", auditMode);
        metadata.put("retryMode", retryMode);
        metadata.put("contingencyMode", contingencyMode);
        metadata.put("reconciliationMode", reconciliationMode);
        metadata.put("proofBundleMode", proofBundleMode);
        metadata.put("baseLegal", resolveBaseLegal(assetKind));
        metadata.put("sigilo", sigilo);
        metadata.put("highValue", highValue);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExternalConstrictionProfile(
                actType,
                assetKind,
                gatewayCode,
                requestMode,
                protocolMode,
                responseMode,
                auditMode,
                retryMode,
                contingencyMode,
                reconciliationMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                statusTarget,
                proofBundleMode,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String normalizeActType(String ato) {
        String token = normalize(ato);
        if (token.contains("BLOQUEIO") || token.contains("PENHORA")) {
            return "ORDEM_CONSTRICAO_EXTERNA";
        }
        if (token.contains("DESBLOQUEIO") || token.contains("LEVANTAMENTO")) {
            return "ORDEM_REVISAO_EXTERNA";
        }
        return "ORDEM_CONSTRICAO_EXTERNA";
    }

    private String resolveAssetKind(String bem, String convenio) {
        String token = normalize(bem) + ' ' + normalize(convenio);
        if (token.contains("DINHEIRO") || token.contains("CONTA") || token.contains("SISBAJUD")) {
            return "DINHEIRO";
        }
        if (token.contains("VEICULO") || token.contains("RENAJUD") || token.contains("PLACA")) {
            return "VEICULO";
        }
        if (token.contains("IMOVEL") || token.contains("CNIB") || token.contains("MATRICULA")) {
            return "IMOVEL";
        }
        if (token.contains("QUOTA") || token.contains("SOCIETARIA") || token.contains("EMPRESA")) {
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
            case "FATURAMENTO" -> "OFICIO_ELETRONICO";
            default -> "OFICIO_ELETRONICO";
        };
    }

    private String resolveRequestMode(String gatewayCode, String assetKind) {
        if (gatewayCode.equals("SISBAJUD")) {
            return "REQUISICAO_ESTRUTURADA_SINCRONA";
        }
        if (gatewayCode.equals("RENAJUD") || gatewayCode.equals("CNIB")) {
            return "REQUISICAO_ESTRUTURADA_ASSINCRONA";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "REQUISICAO_HIBRIDA_COM_VALIDACAO_HUMANA";
        }
        return "OFICIO_ELETRONICO_CONTROLADO";
    }

    private String resolveResponseMode(String gatewayCode, String assetKind) {
        if (gatewayCode.equals("SISBAJUD")) {
            return "RETORNO_SINCRONO_COM_RESULTADO_PARCIAL_OU_TOTAL";
        }
        if (gatewayCode.equals("RENAJUD") || gatewayCode.equals("CNIB")) {
            return "RETORNO_DIFERIDO_COM_RECONSULTA";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "RETORNO_MANUAL_ASSISTIDO";
        }
        return "RETORNO_DOCUMENTAL_ASSINCRONO";
    }

    private String resolveContingencyMode(String gatewayCode, String assetKind) {
        if (gatewayCode.equals("SISBAJUD") || gatewayCode.equals("RENAJUD") || gatewayCode.equals("CNIB")) {
            return "CONTINGENCIA_COM_FILA_DE_RECONCILIACAO";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "CONTINGENCIA_COM_OFICIO_SUBSTITUTIVO";
        }
        return "CONTINGENCIA_DOCUMENTAL";
    }

    private String resolveQueueCode(String gatewayCode, boolean sigilo) {
        return "INTEGRACAO_CONSTRICAO_" + gatewayCode + (sigilo ? "_SIGILO" : "");
    }

    private String resolveInboxKey(String gatewayCode, String assetKind) {
        if (gatewayCode.equals("SISBAJUD")) {
            return "inbox.execucao.integracao.sisbajud";
        }
        if (gatewayCode.equals("RENAJUD")) {
            return "inbox.execucao.integracao.renajud";
        }
        if (gatewayCode.equals("CNIB")) {
            return "inbox.execucao.integracao.cnib";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "inbox.execucao.integracao.societario";
        }
        return "inbox.execucao.integracao.oficio";
    }

    private String resolveBaseLegal(String assetKind) {
        return switch (assetKind) {
            case "DINHEIRO" -> "Arts. 835, 854 e 855 do CPC";
            case "VEICULO" -> "Arts. 831, 835, 844 e 845 do CPC";
            case "IMOVEL" -> "Arts. 799, 828, 835 e 844 do CPC";
            case "QUOTAS_SOCIAIS" -> "Arts. 835, 861 e 862 do CPC";
            case "FATURAMENTO" -> "Arts. 835, 866 e 867 do CPC";
            default -> "Arts. 797, 835 e 854 do CPC";
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
