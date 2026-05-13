package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PatrimonialConstrictionResolver {

    public PatrimonialConstrictionProfile resolve(Processo processo,
                                                  String ato,
                                                  String bem,
                                                  String detalhe,
                                                  String convenio,
                                                  double valorOperacao) {
        String actType = normalizeActType(ato);
        String assetKind = resolveAssetKind(bem, detalhe, convenio);
        String assetClass = resolveAssetClass(assetKind);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean fiscal = processo != null && (processo.getRito() == RitoProcessual.EXECUCAO_FISCAL || processo.getRito() == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO);
        boolean trabalhista = processo != null && processo.getRito() != null && processo.getRito().isTrabalhista();
        boolean highValue = valorOperacao >= 250_000D;
        boolean liquid = assetKind.equals("DINHEIRO") || assetKind.equals("FATURAMENTO");

        String constrictionMode = resolveConstrictionMode(assetKind, fiscal, trabalhista);
        String registryMode = resolveRegistryMode(assetKind);
        String evaluationMode = resolveEvaluationMode(assetKind);
        String expropriationMode = resolveExpropriationMode(assetKind);
        String queueCode = resolveQueueCode(assetKind, sigilo, fiscal);
        String inboxKey = resolveInboxKey(assetKind, fiscal, trabalhista);
        TipoUsuario assignedRole = resolveAssignedRole(assetKind, liquid, fiscal);
        int priority = resolvePriority(assetKind, sigilo, highValue, fiscal);
        boolean blocking = liquid || assetKind.equals("IMOVEL") || assetKind.equals("QUOTAS_SOCIAIS");
        long dueAmount = resolveDueAmount(assetKind, liquid, fiscal);
        ChronoUnit dueUnit = liquid ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        String baseLegal = resolveBaseLegal(assetKind, fiscal, trabalhista);
        String externalDependencyMode = resolveExternalDependencyMode(assetKind, convenio);
        String satisfactionPriority = resolveSatisfactionPriority(assetKind, liquid, fiscal);
        String patrimonialRisk = resolvePatrimonialRisk(assetKind, highValue, sigilo);
        String evidenceMode = resolveEvidenceMode(assetKind, liquid);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (sigilo) {
            warnings.add("Constrição patrimonial sigilosa exige acesso credenciado, trilha reforçada e prova de finalidade.");
        }
        if (highValue) {
            warnings.add("Constrição patrimonial de alto valor exige dupla validação, auditoria patrimonial e reconciliação financeira.");
        }
        if (assetKind.equals("DINHEIRO")) {
            reviewChecklist.add("Validar ordem preferencial de expropriação, conta atingida, impenhorabilidade e eventual desbloqueio parcial.");
        }
        if (assetKind.equals("FATURAMENTO")) {
            reviewChecklist.add("Fixar percentual, janela de retenção, fiscalização periódica e proteção da atividade empresarial mínima.");
        }
        if (assetKind.equals("IMOVEL")) {
            reviewChecklist.add("Conferir matrícula, averbações, ônus, avaliação válida e cadeia expropriatória do imóvel.");
        }
        if (assetKind.equals("VEICULO")) {
            reviewChecklist.add("Conferir restrição de circulação, registro, localização do bem, apreensão potencial e avaliação.");
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            reviewChecklist.add("Intimar sociedade, validar participação societária, liquidez das quotas e regras de preferência.");
        }
        fundamentos.add(baseLegal);
        fundamentos.add("Bem patrimonial: " + assetKind.replace('_', ' '));
        fundamentos.add("Modo de constrição: " + constrictionMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("assetKind", assetKind);
        metadata.put("assetClass", assetClass);
        metadata.put("fiscal", fiscal);
        metadata.put("trabalhista", trabalhista);
        metadata.put("sigilo", sigilo);
        metadata.put("highValue", highValue);
        metadata.put("externalDependencyMode", externalDependencyMode);
        metadata.put("registryMode", registryMode);
        metadata.put("evaluationMode", evaluationMode);
        metadata.put("expropriationMode", expropriationMode);
        metadata.put("satisfactionPriority", satisfactionPriority);
        metadata.put("patrimonialRisk", patrimonialRisk);
        metadata.put("convenioSugerido", suggestGateway(assetKind, convenio));
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);
        metadata.put("rito", processo != null && processo.getRito() != null ? processo.getRito().name() : null);
        metadata.put("detailToken", detailToken(detalhe));

        return new PatrimonialConstrictionProfile(
                actType,
                assetKind,
                assetClass,
                constrictionMode,
                registryMode,
                evaluationMode,
                expropriationMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                externalDependencyMode,
                satisfactionPriority,
                patrimonialRisk,
                evidenceMode,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String normalizeActType(String ato) {
        String token = normalize(ato);
        if (token.contains("BLOQUEIO") || token.contains("ATIVO")) {
            return "BLOQUEIO_ATIVOS";
        }
        if (token.contains("ALIENACAO")) {
            return "ALIENACAO_JUDICIAL";
        }
        if (token.contains("HASTA") || token.contains("LEILAO")) {
            return "HASTA_PUBLICA";
        }
        return "PENHORA";
    }

    private String resolveAssetKind(String bem, String detalhe, String convenio) {
        String token = normalize(bem) + ' ' + normalize(detalhe) + ' ' + normalize(convenio);
        if (token.contains("DINHEIRO") || token.contains("SALDO") || token.contains("CONTA") || token.contains("PIX") || token.contains("SISBAJUD")) {
            return "DINHEIRO";
        }
        if (token.contains("FATURAMENTO") || token.contains("RECEITA") || token.contains("CAIXA")) {
            return "FATURAMENTO";
        }
        if (token.contains("IMOVEL") || token.contains("MATRICULA") || token.contains("TERRENO") || token.contains("APARTAMENTO") || token.contains("CASA")) {
            return "IMOVEL";
        }
        if (token.contains("VEICULO") || token.contains("RENAVAM") || token.contains("PLACA") || token.contains("RENAJUD")) {
            return "VEICULO";
        }
        if (token.contains("QUOTA") || token.contains("SOCIETARIA") || token.contains("PARTICIPACAO") || token.contains("EMPRESA")) {
            return "QUOTAS_SOCIAIS";
        }
        return "OUTROS_ATIVOS";
    }

    private String resolveAssetClass(String assetKind) {
        return switch (assetKind) {
            case "DINHEIRO", "FATURAMENTO" -> "ATIVO_FINANCEIRO";
            case "IMOVEL" -> "ATIVO_REGISTRAL_IMOBILIARIO";
            case "VEICULO" -> "ATIVO_REGISTRAL_MOVEL";
            case "QUOTAS_SOCIAIS" -> "ATIVO_SOCIETARIO";
            default -> "ATIVO_PATRIMONIAL_GENERICO";
        };
    }

    private String resolveConstrictionMode(String assetKind, boolean fiscal, boolean trabalhista) {
        if (assetKind.equals("DINHEIRO")) {
            return fiscal ? "BLOQUEIO_FINANCEIRO_FISCAL" : "BLOQUEIO_FINANCEIRO_IMEDIATO";
        }
        if (assetKind.equals("FATURAMENTO")) {
            return "RETENCAO_PERCENTUAL_RECORRENTE";
        }
        if (assetKind.equals("IMOVEL") || assetKind.equals("VEICULO")) {
            return "AVERBACAO_RESTRITIVA_COM_CONSTRICAO";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return trabalhista ? "CONSTRICAO_SOCIETARIA_TRABALHISTA" : "CONSTRICAO_SOCIETARIA";
        }
        return "CONSTRICAO_PATRIMONIAL_GENERICA";
    }

    private String resolveRegistryMode(String assetKind) {
        return switch (assetKind) {
            case "IMOVEL" -> "MATRICULA_IMOBILIARIA";
            case "VEICULO" -> "REGISTRO_VEICULAR";
            case "QUOTAS_SOCIAIS" -> "REGISTRO_EMPRESARIAL";
            default -> "SEM_REGISTRO_ESTRUTURADO";
        };
    }

    private String resolveEvaluationMode(String assetKind) {
        return switch (assetKind) {
            case "DINHEIRO" -> "SALDO_LIQUIDO";
            case "FATURAMENTO" -> "APURACAO_PERIODICA";
            case "IMOVEL", "VEICULO" -> "AVALIACAO_OFICIAL";
            case "QUOTAS_SOCIAIS" -> "APURACAO_ECONOMICO_SOCIETARIA";
            default -> "AVALIACAO_SUMARIA";
        };
    }

    private String resolveExpropriationMode(String assetKind) {
        return switch (assetKind) {
            case "DINHEIRO" -> "CONVERSAO_DIRETA_EM_SATISFACAO";
            case "FATURAMENTO" -> "RETENCAO_CONTINUADA";
            case "IMOVEL", "VEICULO" -> "ALIENACAO_OU_ADJUDICACAO";
            case "QUOTAS_SOCIAIS" -> "LIQUIDACAO_PARTICIPATIVA";
            default -> "ALIENACAO_GENERICA";
        };
    }

    private String resolveQueueCode(String assetKind, boolean sigilo, boolean fiscal) {
        String prefix = fiscal ? "CONSTRICAO_FISCAL" : "CONSTRICAO_PATRIMONIAL";
        String suffix = switch (assetKind) {
            case "DINHEIRO" -> "DINHEIRO";
            case "FATURAMENTO" -> "FATURAMENTO";
            case "IMOVEL" -> "IMOVEL";
            case "VEICULO" -> "VEICULO";
            case "QUOTAS_SOCIAIS" -> "QUOTAS";
            default -> "GERAL";
        };
        return prefix + '_' + suffix + (sigilo ? "_SIGILO" : "");
    }

    private String resolveInboxKey(String assetKind, boolean fiscal, boolean trabalhista) {
        if (assetKind.equals("DINHEIRO") || assetKind.equals("FATURAMENTO")) {
            return fiscal ? "inbox.execucao.fiscal.financeiro" : "inbox.execucao.financeiro";
        }
        if (assetKind.equals("IMOVEL")) {
            return "inbox.execucao.patrimonio.imovel";
        }
        if (assetKind.equals("VEICULO")) {
            return trabalhista ? "inbox.execucao.trabalhista.veiculo" : "inbox.execucao.patrimonio.veiculo";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return "inbox.execucao.patrimonio.societario";
        }
        return "inbox.execucao.patrimonio.geral";
    }

    private TipoUsuario resolveAssignedRole(String assetKind, boolean liquid, boolean fiscal) {
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return TipoUsuario.JUIZ;
        }
        if (assetKind.equals("IMOVEL") || assetKind.equals("VEICULO")) {
            return TipoUsuario.OFICIAL_JUSTICA;
        }
        if (liquid || fiscal) {
            return TipoUsuario.SERVIDOR_FORUM;
        }
        return TipoUsuario.SERVIDOR;
    }

    private int resolvePriority(String assetKind, boolean sigilo, boolean highValue, boolean fiscal) {
        int base = switch (assetKind) {
            case "DINHEIRO" -> 96;
            case "FATURAMENTO" -> 90;
            case "IMOVEL" -> 88;
            case "VEICULO" -> 86;
            case "QUOTAS_SOCIAIS" -> 84;
            default -> 78;
        };
        if (sigilo) {
            base += 2;
        }
        if (highValue) {
            base += 3;
        }
        if (fiscal) {
            base += 1;
        }
        return Math.min(base, 99);
    }

    private long resolveDueAmount(String assetKind, boolean liquid, boolean fiscal) {
        if (liquid) {
            return fiscal ? 8L : 6L;
        }
        return switch (assetKind) {
            case "IMOVEL", "QUOTAS_SOCIAIS" -> 10L;
            case "VEICULO" -> 7L;
            case "FATURAMENTO" -> 5L;
            default -> 6L;
        };
    }

    private String resolveBaseLegal(String assetKind, boolean fiscal, boolean trabalhista) {
        if (assetKind.equals("DINHEIRO") || assetKind.equals("FATURAMENTO")) {
            return fiscal ? "Arts. 11 e 15 da Lei 6.830/80 e arts. 835 e 854 do CPC" : "Arts. 797, 835, 854 e 866 do CPC";
        }
        if (assetKind.equals("IMOVEL") || assetKind.equals("VEICULO")) {
            return "Arts. 831, 836, 845, 879 e 880 do CPC";
        }
        if (assetKind.equals("QUOTAS_SOCIAIS")) {
            return trabalhista ? "Arts. 789, 835 e 861 do CPC aplicados subsidiariamente e art. 769 da CLT" : "Arts. 835, 861 e 865 do CPC";
        }
        return "Arts. 789, 797, 835 e 847 do CPC";
    }

    private String resolveExternalDependencyMode(String assetKind, String convenio) {
        if (convenio != null && !convenio.isBlank()) {
            return "CONVENIO_DIRECIONADO";
        }
        return switch (assetKind) {
            case "DINHEIRO", "VEICULO", "IMOVEL", "QUOTAS_SOCIAIS" -> "CONVENIO_RECOMENDADO";
            case "FATURAMENTO" -> "MONITORAMENTO_INTERNO_ASSISTIDO";
            default -> "SEM_DEPENDENCIA_EXTERNA_OBRIGATORIA";
        };
    }

    private String resolveSatisfactionPriority(String assetKind, boolean liquid, boolean fiscal) {
        if (liquid) {
            return fiscal ? "SATISFACAO_PREFERENCIAL_FISCAL" : "SATISFACAO_IMEDIATA";
        }
        return switch (assetKind) {
            case "FATURAMENTO" -> "SATISFACAO_PROGRESSIVA";
            case "IMOVEL", "VEICULO" -> "SATISFACAO_APOS_EXPROPRIACAO";
            case "QUOTAS_SOCIAIS" -> "SATISFACAO_CONDICIONADA_A_LIQUIDACAO";
            default -> "SATISFACAO_SUBORDINADA";
        };
    }

    private String resolvePatrimonialRisk(String assetKind, boolean highValue, boolean sigilo) {
        String base = switch (assetKind) {
            case "DINHEIRO" -> "RISCO_BAIXO_COM_RECONCILIACAO";
            case "FATURAMENTO" -> "RISCO_MEDIO_CONTINUADO";
            case "IMOVEL", "QUOTAS_SOCIAIS" -> "RISCO_ELEVADO_DE_CONTESTACAO";
            case "VEICULO" -> "RISCO_MEDIO_DE_DESLOCAMENTO";
            default -> "RISCO_MEDIO";
        };
        if (highValue || sigilo) {
            return base + "_REFORCADO";
        }
        return base;
    }

    private String resolveEvidenceMode(String assetKind, boolean liquid) {
        if (liquid) {
            return "EXTRATO_ELETRONICO_AUDITAVEL";
        }
        return switch (assetKind) {
            case "IMOVEL" -> "MATRICULA_LAUDO_AVERBACAO";
            case "VEICULO" -> "REGISTRO_RESTRICAO_LAUDO";
            case "QUOTAS_SOCIAIS" -> "CONTRATO_SOCIAL_BALANCO_APURACAO";
            default -> "DOCUMENTACAO_PATRIMONIAL";
        };
    }

    private String suggestGateway(String assetKind, String convenio) {
        if (convenio != null && !convenio.isBlank()) {
            return convenio.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        }
        return switch (assetKind) {
            case "DINHEIRO" -> "SISBAJUD";
            case "VEICULO" -> "RENAJUD";
            case "IMOVEL" -> "CNIB";
            case "QUOTAS_SOCIAIS" -> "REGISTRO_EMPRESARIAL_INTEGRADO";
            case "FATURAMENTO" -> "OFICIO_ELETRONICO_MONITORADO";
            default -> "OFICIO_ELETRONICO";
        };
    }

    private String detailToken(String detalhe) {
        String normalized = normalize(detalhe);
        return normalized.isBlank() ? null : normalized;
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
