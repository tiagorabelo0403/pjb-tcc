package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExecutionEnforcementResolver {

    public ExecutionEnforcementProfile resolve(Processo processo,
                                               String ato,
                                               String detalhe,
                                               double valorOperacao) {
        String actType = resolveActType(ato);
        RitoProcessual rito = processo == null ? null : processo.getRito();
        String speciesCode = resolveSpeciesCode(rito, actType, detalhe);
        boolean fiscal = rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO;
        boolean penal = rito == RitoProcessual.EXECUCAO_PENAL || (rito != null && rito.isPenal()) || (rito != null && rito.isMilitar());
        boolean trabalhista = rito != null && rito.isTrabalhista();
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean archived = processo != null && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO;
        boolean highValue = valorOperacao >= 250_000D;
        boolean forcedExpropriation = actType.equals("ALIENACAO_JUDICIAL") || actType.equals("HASTA_PUBLICA");

        String actMode = resolveActMode(actType, speciesCode, fiscal, penal, trabalhista);
        String constrictionMode = resolveConstrictionMode(actType, speciesCode);
        String expropriationMode = resolveExpropriationMode(actType, speciesCode);
        String satisfactionMode = resolveSatisfactionMode(actType, speciesCode, fiscal, penal, trabalhista);
        String queueCode = resolveQueueCode(actType, speciesCode, sigilo);
        String inboxKey = resolveInboxKey(actType, speciesCode, fiscal, penal, trabalhista);
        TipoUsuario assignedRole = resolveAssignedRole(actType, speciesCode, fiscal);
        int priority = resolvePriority(actType, speciesCode, highValue, sigilo, fiscal, penal);
        boolean blocking = resolveBlocking(actType, speciesCode, forcedExpropriation);
        long dueAmount = resolveDueAmount(actType, speciesCode, fiscal, penal);
        ChronoUnit dueUnit = resolveDueUnit(actType);
        String baseLegal = resolveBaseLegal(actType, speciesCode, fiscal, penal, trabalhista);
        String evidenceMode = resolveEvidenceMode(actType, speciesCode, highValue, fiscal);
        String escalationDesk = resolveEscalationDesk(actType, speciesCode, fiscal, penal, trabalhista);
        String executionImpact = resolveExecutionImpact(actType, speciesCode, forcedExpropriation);
        String ledgerMode = resolveLedgerMode(actType, speciesCode);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (archived) {
            warnings.add("Processo arquivado exige desarquivamento antes da prática de ato executivo.");
        }
        if (sigilo) {
            warnings.add("Ato executivo com sigilo reforçado exige trilha de auditoria e acesso credenciado.");
        }
        if (highValue) {
            warnings.add("Execução de alto valor marcada para dupla conferência patrimonial e mesa reforçada.");
        }
        if (actType.equals("PENHORA") || actType.equals("BLOQUEIO_ATIVOS")) {
            reviewChecklist.add("Validar ordem legal da penhora, menor onerosidade e rastreabilidade do convênio patrimonial.");
        }
        if (actType.equals("AVALIACAO_BENS")) {
            reviewChecklist.add("Conferir laudo, estado do bem, avaliação oficial e intimação das partes.");
        }
        if (actType.equals("ALIENACAO_JUDICIAL") || actType.equals("HASTA_PUBLICA")) {
            reviewChecklist.add("Verificar editais, avaliação válida, intimações prévias e reserva de preferências incidentais.");
        }
        if (actType.equals("SATISFACAO_FINAL") || actType.equals("EXTINCAO_EXECUTIVA")) {
            reviewChecklist.add("Confirmar quitação integral, levantamento, custas finais e baixa das constrições pendentes.");
        }
        if (speciesCode.equals("OBRIGACAO_FAZER") || speciesCode.equals("OBRIGACAO_NAO_FAZER") || speciesCode.equals("OBRIGACAO_FAZER_TRABALHISTA")) {
            reviewChecklist.add("Checar multa cominatória, coerção específica, cronograma de adimplemento e prova de cumprimento.");
        }
        if (speciesCode.equals("ENTREGA_COISA")) {
            reviewChecklist.add("Conferir individualização do bem, cadeia de custódia, depósito e termo de entrega.");
        }
        if (speciesCode.equals("EXECUCAO_PENAL")) {
            reviewChecklist.add("Verificar guias, comunicações obrigatórias e compatibilidade do ato com a LEP.");
        }
        if (fiscal) {
            reviewChecklist.add("Validar CDA, garantias, preferências fazendárias e integração com o núcleo fiscal.");
        }
        fundamentos.add(baseLegal);
        fundamentos.add("Espécie executiva: " + speciesCode.replace('_', ' '));
        fundamentos.add("Trilha: " + actMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actType", actType);
        metadata.put("actMode", actMode);
        metadata.put("speciesCode", speciesCode);
        metadata.put("fiscal", fiscal);
        metadata.put("penal", penal);
        metadata.put("trabalhista", trabalhista);
        metadata.put("sigilo", sigilo);
        metadata.put("highValue", highValue);
        metadata.put("constrictionMode", constrictionMode);
        metadata.put("expropriationMode", expropriationMode);
        metadata.put("satisfactionMode", satisfactionMode);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);
        metadata.put("statusAtual", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("descriptor", String.join(":",
                normalize(actType),
                normalize(speciesCode),
                normalize(actMode),
                normalize(queueCode)));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new ExecutionEnforcementProfile(
                actType,
                actMode,
                speciesCode,
                constrictionMode,
                expropriationMode,
                satisfactionMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                evidenceMode,
                escalationDesk,
                executionImpact,
                ledgerMode,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveActType(String ato) {
        String token = normalize(ato);
        return switch (token) {
            case "PENHORA", "PENHORA_ONLINE", "PENHORA_BENS" -> "PENHORA";
            case "BLOQUEIO", "BLOQUEIO_ATIVOS", "SISBAJUD" -> "BLOQUEIO_ATIVOS";
            case "AVALIACAO", "AVALIACAO_BEM", "AVALIACAO_BENS" -> "AVALIACAO_BENS";
            case "ADJUDICACAO" -> "ADJUDICACAO";
            case "ALIENACAO", "ALIENACAO_JUDICIAL" -> "ALIENACAO_JUDICIAL";
            case "HASTA", "PRACA", "LEILAO", "HASTA_PUBLICA" -> "HASTA_PUBLICA";
            case "LEVANTAMENTO", "SATISFACAO", "SATISFACAO_FINAL" -> "SATISFACAO_FINAL";
            case "EXTINCAO", "EXTINCAO_EXECUTIVA", "QUITACAO_FINAL" -> "EXTINCAO_EXECUTIVA";
            default -> "ATO_EXECUTIVO_GERAL";
        };
    }

    private String resolveSpeciesCode(RitoProcessual rito, String actType, String detalhe) {
        String detail = normalize(detalhe);
        if (rito == null) {
            return detail.contains("FAZER") ? "OBRIGACAO_FAZER" : detail.contains("NAO_FAZER") ? "OBRIGACAO_NAO_FAZER" : "QUANTIA";
        }
        if (rito == RitoProcessual.EXECUCAO_PENAL) {
            return "EXECUCAO_PENAL";
        }
        if (rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO || rito == RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL) {
            return "EXECUCAO_FISCAL";
        }
        if (rito.isTrabalhista()) {
            if (detail.contains("FAZER")) {
                return "OBRIGACAO_FAZER_TRABALHISTA";
            }
            return "QUANTIA_TRABALHISTA";
        }
        if (detail.contains("NAO_FAZER")) {
            return "OBRIGACAO_NAO_FAZER";
        }
        if (detail.contains("FAZER")) {
            return "OBRIGACAO_FAZER";
        }
        if (detail.contains("ENTREGA") || detail.contains("COISA")) {
            return "ENTREGA_COISA";
        }
        if (actType.equals("EXTINCAO_EXECUTIVA") && detail.contains("PENAL")) {
            return "EXECUCAO_PENAL";
        }
        return "QUANTIA";
    }

    private String resolveActMode(String actType, String speciesCode, boolean fiscal, boolean penal, boolean trabalhista) {
        return switch (actType) {
            case "PENHORA", "BLOQUEIO_ATIVOS" -> speciesCode.startsWith("OBRIGACAO_")
                    ? "COERCAO_EXECUTIVA_ESPECIFICA"
                    : "CONSTRICAO_PATRIMONIAL_CONTROLADA";
            case "AVALIACAO_BENS" -> "AFERICAO_DE_BENS_E_VALORES";
            case "ADJUDICACAO" -> "TRANSFERENCIA_EXECUTIVA_DIRETA";
            case "ALIENACAO_JUDICIAL", "HASTA_PUBLICA" -> fiscal
                    ? "EXPROPRIACAO_FISCAL_ESTRUTURADA"
                    : "EXPROPRIACAO_JUDICIAL_ESTRUTURADA";
            case "SATISFACAO_FINAL" -> trabalhista ? "PAGAMENTO_E_LIBERACAO_TRABALHISTA" : "PAGAMENTO_E_LIBERACAO_CONTROLADA";
            case "EXTINCAO_EXECUTIVA" -> penal ? "ENCERRAMENTO_EXECUTIVO_PENAL" : "ENCERRAMENTO_EXECUTIVO_CONTROLADO";
            default -> penal ? "ATO_EXECUTIVO_PENAL" : "ATO_EXECUTIVO_GERAL";
        };
    }

    private String resolveConstrictionMode(String actType, String speciesCode) {
        if (actType.equals("PENHORA")) {
            return speciesCode.startsWith("OBRIGACAO_") ? "ASTREINTE_E_SUBSTITUICAO_DE_VONTADE" : "PENHORA_CLASSICA";
        }
        if (actType.equals("BLOQUEIO_ATIVOS")) {
            return "BLOQUEIO_FINANCEIRO_ELETRONICO";
        }
        if (actType.equals("AVALIACAO_BENS")) {
            return "AVALIACAO_PREPARATORIA";
        }
        return "SEM_CONSTRICAO_NOVA";
    }

    private String resolveExpropriationMode(String actType, String speciesCode) {
        return switch (actType) {
            case "ADJUDICACAO" -> "ADJUDICACAO_DIRETA";
            case "ALIENACAO_JUDICIAL" -> "ALIENACAO_PRIVADA_OU_GERIDA";
            case "HASTA_PUBLICA" -> "PRACA_LEILAO_HASTA";
            case "SATISFACAO_FINAL", "EXTINCAO_EXECUTIVA" -> "ENCERRAMENTO_POS_EXPROPRIACAO";
            default -> speciesCode.equals("ENTREGA_COISA") ? "APREENSAO_E_ENTREGA_CONTROLADA" : "A_GUARDAR_EXPROPRIACAO";
        };
    }

    private String resolveSatisfactionMode(String actType, String speciesCode, boolean fiscal, boolean penal, boolean trabalhista) {
        if (actType.equals("SATISFACAO_FINAL")) {
            return trabalhista ? "LIBERACAO_CREDITO_TRABALHISTA" : fiscal ? "AMORTIZACAO_DEBITO_FISCAL" : penal ? "CUMPRIMENTO_INTEGRAL_DA_PENA_OU_OBRIGACAO" : "LEVANTAMENTO_E_QUITACAO";
        }
        if (actType.equals("EXTINCAO_EXECUTIVA")) {
            return penal ? "BAIXA_DA_EXECUCAO_PENAL" : "QUITACAO_E_EXTINCAO";
        }
        if (speciesCode.startsWith("OBRIGACAO_")) {
            return "ADIMPLEMENTO_ESPECIFICO";
        }
        if (speciesCode.equals("ENTREGA_COISA")) {
            return "TRADICAO_CONTROLADA_DO_BEM";
        }
        return "SATISFACAO_PATRIMONIAL_PROGRESSIVA";
    }

    private String resolveQueueCode(String actType, String speciesCode, boolean sigilo) {
        String suffix = sigilo ? "_SIGILO" : "";
        return switch (actType) {
            case "PENHORA" -> "EXECUCAO_PENHORA_" + speciesCode + suffix;
            case "BLOQUEIO_ATIVOS" -> "EXECUCAO_BLOQUEIO_ATIVOS_" + speciesCode + suffix;
            case "AVALIACAO_BENS" -> "EXECUCAO_AVALIACAO_" + speciesCode + suffix;
            case "ADJUDICACAO" -> "EXECUCAO_ADJUDICACAO_" + speciesCode + suffix;
            case "ALIENACAO_JUDICIAL" -> "EXECUCAO_ALIENACAO_" + speciesCode + suffix;
            case "HASTA_PUBLICA" -> "EXECUCAO_HASTA_PUBLICA_" + speciesCode + suffix;
            case "SATISFACAO_FINAL" -> "EXECUCAO_SATISFACAO_" + speciesCode + suffix;
            case "EXTINCAO_EXECUTIVA" -> "EXECUCAO_EXTINCAO_" + speciesCode + suffix;
            default -> "EXECUCAO_ATO_GERAL_" + speciesCode + suffix;
        };
    }

    private String resolveInboxKey(String actType, String speciesCode, boolean fiscal, boolean penal, boolean trabalhista) {
        if (actType.equals("PENHORA") || actType.equals("BLOQUEIO_ATIVOS")) {
            return fiscal ? "NUCLEO_CONSTRICAO_FISCAL" : trabalhista ? "GABINETE_EXECUCAO_TRABALHISTA" : penal ? "JUIZO_EXECUCAO_PENAL" : "NUCLEO_CONSTRICAO_CIVEL";
        }
        if (actType.equals("ALIENACAO_JUDICIAL") || actType.equals("HASTA_PUBLICA")) {
            return fiscal ? "NUCLEO_EXPROPRIACAO_FISCAL" : "NUCLEO_HASTA_E_ALIENACAO";
        }
        if (speciesCode.startsWith("OBRIGACAO_")) {
            return "GABINETE_CUMPRIMENTO_ESPECIFICO";
        }
        if (actType.equals("SATISFACAO_FINAL") || actType.equals("EXTINCAO_EXECUTIVA")) {
            return "SECRETARIA_BAIXA_EXECUTIVA";
        }
        return "WORKSPACE_EXECUCAO";
    }

    private TipoUsuario resolveAssignedRole(String actType, String speciesCode, boolean fiscal) {
        if (actType.equals("AVALIACAO_BENS")) {
            return TipoUsuario.SERVIDOR_FORUM;
        }
        if (actType.equals("SATISFACAO_FINAL") || actType.equals("EXTINCAO_EXECUTIVA")) {
            return fiscal ? TipoUsuario.SERVIDOR_FORUM : TipoUsuario.JUIZ;
        }
        if (speciesCode.startsWith("OBRIGACAO_")) {
            return TipoUsuario.JUIZ;
        }
        return fiscal ? TipoUsuario.SERVIDOR_FORUM : TipoUsuario.JUIZ;
    }

    private int resolvePriority(String actType,
                                String speciesCode,
                                boolean highValue,
                                boolean sigilo,
                                boolean fiscal,
                                boolean penal) {
        if (actType.equals("BLOQUEIO_ATIVOS") || actType.equals("PENHORA")) {
            return 0;
        }
        if (actType.equals("HASTA_PUBLICA") || actType.equals("ALIENACAO_JUDICIAL")) {
            return 1;
        }
        if (highValue || sigilo || fiscal || penal || speciesCode.startsWith("OBRIGACAO_")) {
            return 1;
        }
        return 2;
    }

    private boolean resolveBlocking(String actType, String speciesCode, boolean forcedExpropriation) {
        return actType.equals("PENHORA")
                || actType.equals("BLOQUEIO_ATIVOS")
                || actType.equals("HASTA_PUBLICA")
                || actType.equals("ALIENACAO_JUDICIAL")
                || actType.equals("EXTINCAO_EXECUTIVA")
                || speciesCode.startsWith("OBRIGACAO_")
                || forcedExpropriation;
    }

    private long resolveDueAmount(String actType, String speciesCode, boolean fiscal, boolean penal) {
        return switch (actType) {
            case "PENHORA", "BLOQUEIO_ATIVOS" -> penal ? 1L : 2L;
            case "AVALIACAO_BENS" -> 5L;
            case "ADJUDICACAO" -> 5L;
            case "ALIENACAO_JUDICIAL", "HASTA_PUBLICA" -> fiscal ? 7L : 10L;
            case "SATISFACAO_FINAL" -> 3L;
            case "EXTINCAO_EXECUTIVA" -> speciesCode.equals("EXECUCAO_PENAL") ? 1L : 5L;
            default -> 5L;
        };
    }

    private ChronoUnit resolveDueUnit(String actType) {
        return actType.equals("BLOQUEIO_ATIVOS") ? ChronoUnit.HOURS : ChronoUnit.DAYS;
    }

    private String resolveBaseLegal(String actType,
                                    String speciesCode,
                                    boolean fiscal,
                                    boolean penal,
                                    boolean trabalhista) {
        return switch (actType) {
            case "PENHORA", "BLOQUEIO_ATIVOS" -> speciesCode.startsWith("OBRIGACAO_")
                    ? "Arts. 536 e 537 CPC — tutela específica e medidas coercitivas"
                    : fiscal ? "Lei 6.830/80 e ordem de penhora fiscal" : "Arts. 831 a 854 CPC — penhora e bloqueio patrimonial";
            case "AVALIACAO_BENS" -> "Arts. 870 e 872 CPC — avaliação judicial dos bens";
            case "ADJUDICACAO" -> "Art. 876 CPC — adjudicação";
            case "ALIENACAO_JUDICIAL", "HASTA_PUBLICA" -> fiscal ? "Lei 6.830/80 e atos expropriatórios fiscais" : "Arts. 879 a 903 CPC — alienação judicial e expropriação";
            case "SATISFACAO_FINAL" -> trabalhista ? "CLT e atos de satisfação do crédito trabalhista" : penal ? "LEP e cumprimento da obrigação executiva" : "Arts. 904 e 924 CPC — satisfação e extinção";
            case "EXTINCAO_EXECUTIVA" -> penal ? "LEP e encerramento da execução penal" : "Art. 924 CPC — extinção da execução";
            default -> "Governança executiva geral";
        };
    }

    private String resolveEvidenceMode(String actType, String speciesCode, boolean highValue, boolean fiscal) {
        if (actType.equals("AVALIACAO_BENS")) {
            return "LAUDO_AVALIATIVO_E_LASTRO_DOCUMENTAL";
        }
        if (actType.equals("ALIENACAO_JUDICIAL") || actType.equals("HASTA_PUBLICA")) {
            return "EDITAIS_INTIMACOES_E_MATRIZ_EXPROPRIATORIA";
        }
        if (speciesCode.equals("ENTREGA_COISA")) {
            return "INDIVIDUALIZACAO_E_TERMO_DE_ENTREGA";
        }
        if (fiscal) {
            return "CDA_MEMORIA_FISCAL_E_GARANTIAS";
        }
        return highValue ? "TRILHA_PATRIMONIAL_REFORCADA" : "TRILHA_EXECUTIVA_PADRAO";
    }

    private String resolveEscalationDesk(String actType,
                                         String speciesCode,
                                         boolean fiscal,
                                         boolean penal,
                                         boolean trabalhista) {
        if (actType.equals("HASTA_PUBLICA") || actType.equals("ALIENACAO_JUDICIAL")) {
            return fiscal ? "NUCLEO_EXPROPRIACAO_FISCAL" : "MESA_EXPROPRIATORIA";
        }
        if (speciesCode.startsWith("OBRIGACAO_")) {
            return "GABINETE_CUMPRIMENTO_ESPECIFICO";
        }
        if (penal) {
            return "JUIZO_EXECUCAO_PENAL_COORDENACAO";
        }
        if (trabalhista) {
            return "MESA_EXECUCAO_TRABALHISTA";
        }
        return fiscal ? "NUCLEO_EXECUCAO_FISCAL" : "COORDENACAO_EXECUCAO_CIVEL";
    }

    private String resolveExecutionImpact(String actType, String speciesCode, boolean forcedExpropriation) {
        if (actType.equals("PENHORA") || actType.equals("BLOQUEIO_ATIVOS")) {
            return "CONSTRICAO_IMEDIATA_DO_PATRIMONIO";
        }
        if (actType.equals("AVALIACAO_BENS")) {
            return "PREPARACAO_PARA_EXPROPRIACAO";
        }
        if (actType.equals("ADJUDICACAO")) {
            return "TRANSFERENCIA_DIRETA_DO_BEM";
        }
        if (forcedExpropriation) {
            return "CONVERSAO_DO_BEM_EM_NUMERARIO";
        }
        if (actType.equals("SATISFACAO_FINAL")) {
            return speciesCode.startsWith("OBRIGACAO_") ? "PROVA_DE_ADIMPLEMENTO_ESPECIFICO" : "QUITACAO_DO_CREDITO";
        }
        if (actType.equals("EXTINCAO_EXECUTIVA")) {
            return "ENCERRAMENTO_DA_TRILHA_EXECUTIVA";
        }
        return "AJUSTE_DA_MARCHA_EXECUTIVA";
    }

    private String resolveLedgerMode(String actType, String speciesCode) {
        if (actType.equals("SATISFACAO_FINAL") || actType.equals("EXTINCAO_EXECUTIVA")) {
            return "LEDGER_TERMINAL";
        }
        if (actType.equals("ALIENACAO_JUDICIAL") || actType.equals("HASTA_PUBLICA")) {
            return "LEDGER_EXPROPRIATORIO";
        }
        if (speciesCode.startsWith("OBRIGACAO_")) {
            return "LEDGER_CUMPRIMENTO_ESPECIFICO";
        }
        return "LEDGER_EXECUTIVO_PATRIMONIAL";
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ATO_EXECUTIVO_GERAL";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }
}
