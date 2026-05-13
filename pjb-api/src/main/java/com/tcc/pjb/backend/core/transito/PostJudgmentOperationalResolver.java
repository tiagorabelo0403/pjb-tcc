package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PostJudgmentOperationalResolver {

    public PostJudgmentOperationalProfile resolve(Processo processo,
                                                  ProcessoLifecycleAction action,
                                                  String qualifier,
                                                  double valorExequendo) {
        RitoProcessual rito = processo == null ? null : processo.getRito();
        String group = resolveGroup(rito);
        String normalizedQualifier = normalizeQualifier(qualifier);
        boolean segredo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean provisional = normalizedQualifier.contains("PROVISORIO") || normalizedQualifier.contains("ANTECIPADO");
        boolean guaranteeSensitive = normalizedQualifier.contains("PENHORA")
                || normalizedQualifier.contains("BACEN")
                || normalizedQualifier.contains("SISBAJUD")
                || normalizedQualifier.contains("BLOQUEIO");
        boolean highValue = valorExequendo >= 100_000D;

        String executionTrack = resolveExecutionTrack(action, group, provisional, normalizedQualifier);
        String archiveTrack = resolveArchiveTrack(action, group, provisional, normalizedQualifier);
        String operationMode = resolveOperationMode(action, executionTrack, archiveTrack, normalizedQualifier);
        String queueCode = resolveQueueCode(action, group, executionTrack, archiveTrack, normalizedQualifier, segredo);
        String inboxKey = resolveInboxKey(action, group, executionTrack, archiveTrack, provisional);
        TipoUsuario assignedRole = resolveAssignedRole(action, group, executionTrack);
        int priority = resolvePriority(action, group, highValue, provisional, segredo, guaranteeSensitive);
        boolean blocking = resolveBlocking(action, provisional, guaranteeSensitive);
        long dueAmount = resolveDueAmount(action, group, provisional, guaranteeSensitive);
        ChronoUnit dueUnit = resolveDueUnit(action, group, provisional);
        String baseLegal = resolveBaseLegal(action, rito, group, provisional);
        String resJudicataScope = resolveResJudicataScope(group, provisional);
        String reviewDesk = resolveReviewDesk(action, group, executionTrack, archiveTrack, segredo);
        String retentionMode = resolveRetentionMode(action, group, provisional);
        String satisfactionMode = resolveSatisfactionMode(action, group, normalizedQualifier, highValue);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        fundamentos.add("Operação pós-julgamento classificada em " + operationMode + '.');
        fundamentos.add("Fila operacional direcionada para " + queueCode + '.');
        fundamentos.add("Responsabilidade principal atribuída a " + assignedRole.name() + '.');
        fundamentos.add("Regime de coisa julgada tratado como " + resJudicataScope + '.');
        fundamentos.add("Base normativa sintetizada em " + baseLegal + '.');

        if (segredo) {
            warnings.add("Processo sigiloso exige segregação reforçada na trilha pós-julgamento.");
            reviewChecklist.add("Conferir visibilidade restrita em secretaria, gabinete, execução e arquivo.");
        }
        if (provisional) {
            warnings.add("Cumprimento provisório ou antecipado exige cautela quanto à irreversibilidade prática.");
            reviewChecklist.add("Validar caução, reversibilidade e limites de expropriação provisória.");
        }
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && guaranteeSensitive) {
            reviewChecklist.add("Conferir convênios de constrição, penhora e ordem de satisfação antes do despacho inicial.");
        }
        if (action == ProcessoLifecycleAction.ARQUIVAR) {
            reviewChecklist.add("Certificar inexistência de pendências abertas, incidentes pendentes e comunicação final às partes.");
        }
        if (action == ProcessoLifecycleAction.DESARQUIVAR) {
            reviewChecklist.add("Verificar motivo superveniente, fase de retorno e necessidade de nova conclusão ao gabinete.");
        }
        if ("PENAL".equals(group) || "MILITAR".equals(group)) {
            reviewChecklist.add("Conferir guia, custódia, antecedentes executórios e comunicações obrigatórias do juízo competente.");
        }
        if ("TRIBUTARIO_FAZENDA".equals(group) && action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            reviewChecklist.add("Validar liquidez do título, CDA, garantias e fila de constrição patrimonial fazendária.");
        }
        if (highValue && action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            warnings.add("Execução de alto valor foi marcada para governança reforçada.");
            reviewChecklist.add("Aplicar mesa de cálculo, auditoria patrimonial e dupla conferência da memória executiva.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("riteGroup", group);
        metadata.put("qualifier", normalizedQualifier.isBlank() ? null : normalizedQualifier);
        metadata.put("highValue", highValue);
        metadata.put("provisional", provisional);
        metadata.put("guaranteeSensitive", guaranteeSensitive);
        metadata.put("descriptor", String.join(":",
                normalize(operationMode, "OPERACAO"),
                normalize(firstNonBlank(executionTrack, archiveTrack, "TRILHA"), "TRILHA"),
                normalize(reviewDesk, "MESA"),
                normalize(queueCode, "FILA")));
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);
        metadata.put("statusAtual", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("faseAtual", processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        metadata.put("processoSigiloso", segredo);
        metadata.put("dueWindow", dueAmount + "_" + dueUnit.name());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new PostJudgmentOperationalProfile(
                operationMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                resJudicataScope,
                executionTrack,
                archiveTrack,
                reviewDesk,
                retentionMode,
                satisfactionMode,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveOperationMode(ProcessoLifecycleAction action,
                                        String executionTrack,
                                        String archiveTrack,
                                        String normalizedQualifier) {
        return switch (action) {
            case CERTIFICAR_TRANSITO -> "CERTIFICACAO_E_FECHAMENTO_RECURSAL";
            case INICIAR_CUMPRIMENTO -> normalizedQualifier.contains("PENHORA")
                    ? "ABERTURA_EXECUTIVA_COM_CONSTRICAO"
                    : "ABERTURA_EXECUTIVA_CONTROLADA";
            case ARQUIVAR -> "BAIXA_E_GUARDA_DEFINITIVA";
            case DESARQUIVAR -> "REATIVACAO_CONTROLADA_DE_ACERVO";
            default -> firstNonBlank(executionTrack, archiveTrack, action.operationalAxis());
        };
    }

    private String resolveQueueCode(ProcessoLifecycleAction action,
                                    String group,
                                    String executionTrack,
                                    String archiveTrack,
                                    String normalizedQualifier,
                                    boolean segredo) {
        String suffix = segredo ? "_SIGILO" : "";
        return switch (action) {
            case CERTIFICAR_TRANSITO -> "SECRETARIA_TRANSITO_" + group + suffix;
            case INICIAR_CUMPRIMENTO -> normalize(firstNonBlank(executionTrack, "CUMPRIMENTO"), "CUMPRIMENTO") + suffix;
            case ARQUIVAR -> normalize(firstNonBlank(archiveTrack, "ARQUIVAMENTO"), "ARQUIVAMENTO") + suffix;
            case DESARQUIVAR -> "SECRETARIA_DESARQUIVAMENTO_" + group + suffix;
            default -> normalize(action.operationalAxis(), "OPERACIONAL") + suffix;
        };
    }

    private String resolveInboxKey(ProcessoLifecycleAction action,
                                   String group,
                                   String executionTrack,
                                   String archiveTrack,
                                   boolean provisional) {
        return switch (action) {
            case CERTIFICAR_TRANSITO -> "CUMPRIMENTO_" + group + "_TRIGGER";
            case INICIAR_CUMPRIMENTO -> provisional
                    ? "GABINETE_CUMPRIMENTO_PROVISORIO"
                    : group.equals("TRIBUTARIO_FAZENDA")
                            ? "SECRETARIA_EXECUCAO_FAZENDA"
                            : group.equals("PENAL") || group.equals("MILITAR")
                                    ? "JUIZO_EXECUCAO_PENAL"
                                    : executionTrack.contains("TRABALHISTA")
                                            ? "GABINETE_EXECUCAO_TRABALHISTA"
                                            : "JUIZ_CUMPRIMENTO_DESPACHO";
            case ARQUIVAR -> archiveTrack.contains("PROVISORIA") ? "ARQUIVO_INTERMEDIARIO" : "ARQUIVO_DEFINITIVO";
            case DESARQUIVAR -> group.equals("PENAL") || group.equals("MILITAR") ? "JUIZO_EXECUCAO_REATIVACAO" : "GABINETE_DESPACHO";
            default -> "WORKSPACE_OPERACIONAL";
        };
    }

    private TipoUsuario resolveAssignedRole(ProcessoLifecycleAction action,
                                            String group,
                                            String executionTrack) {
        return switch (action) {
            case CERTIFICAR_TRANSITO, ARQUIVAR -> TipoUsuario.SERVIDOR_FORUM;
            case INICIAR_CUMPRIMENTO -> group.equals("TRIBUTARIO_FAZENDA")
                    ? TipoUsuario.SERVIDOR_FORUM
                    : TipoUsuario.JUIZ;
            case DESARQUIVAR -> TipoUsuario.JUIZ;
            default -> TipoUsuario.SERVIDOR_FORUM;
        };
    }

    private int resolvePriority(ProcessoLifecycleAction action,
                                String group,
                                boolean highValue,
                                boolean provisional,
                                boolean segredo,
                                boolean guaranteeSensitive) {
        if (action == ProcessoLifecycleAction.DESARQUIVAR) {
            return 1;
        }
        if (action == ProcessoLifecycleAction.CERTIFICAR_TRANSITO) {
            return 1;
        }
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && (provisional || highValue || guaranteeSensitive)) {
            return 0;
        }
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && ("PENAL".equals(group) || "MILITAR".equals(group))) {
            return 0;
        }
        if (action == ProcessoLifecycleAction.ARQUIVAR && segredo) {
            return 2;
        }
        return action == ProcessoLifecycleAction.ARQUIVAR ? 4 : 1;
    }

    private boolean resolveBlocking(ProcessoLifecycleAction action,
                                    boolean provisional,
                                    boolean guaranteeSensitive) {
        return action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && (provisional || guaranteeSensitive)
                || action == ProcessoLifecycleAction.DESARQUIVAR;
    }

    private long resolveDueAmount(ProcessoLifecycleAction action,
                                  String group,
                                  boolean provisional,
                                  boolean guaranteeSensitive) {
        return switch (action) {
            case CERTIFICAR_TRANSITO -> 2L;
            case INICIAR_CUMPRIMENTO -> provisional ? 5L
                    : guaranteeSensitive ? 3L
                    : group.equals("TRABALHISTA") ? 10L
                    : group.equals("PENAL") || group.equals("MILITAR") ? 3L
                    : 15L;
            case ARQUIVAR -> group.equals("PENAL") || group.equals("MILITAR") ? 2L : 5L;
            case DESARQUIVAR -> 24L;
            default -> 1L;
        };
    }

    private ChronoUnit resolveDueUnit(ProcessoLifecycleAction action,
                                      String group,
                                      boolean provisional) {
        return switch (action) {
            case CERTIFICAR_TRANSITO, DESARQUIVAR -> ChronoUnit.HOURS;
            default -> ChronoUnit.DAYS;
        };
    }

    private String resolveBaseLegal(ProcessoLifecycleAction action,
                                    RitoProcessual rito,
                                    String group,
                                    boolean provisional) {
        return switch (action) {
            case CERTIFICAR_TRANSITO -> switch (group) {
                case "TRABALHISTA" -> "CLT e regime de imutabilidade da decisão trabalhista";
                case "PENAL", "MILITAR" -> "CPP e estabilização do título condenatório ou absolutório";
                case "ESPECIAL_CONSTITUCIONAL" -> "Regime constitucional e regimental de estabilização do julgado";
                default -> "Art. 502 CPC — Coisa julgada material";
            };
            case INICIAR_CUMPRIMENTO -> {
                if (group.equals("TRIBUTARIO_FAZENDA") || rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO) {
                    yield "Lei 6.830/80 e regime de satisfação fazendária";
                }
                if (group.equals("TRABALHISTA")) {
                    yield "CLT e fase executiva trabalhista";
                }
                if (group.equals("PENAL") || group.equals("MILITAR")) {
                    yield "CPP/LEP e regime de execução penal";
                }
                yield provisional ? "Arts. 520 e seguintes do CPC — Cumprimento provisório" : "Art. 513 CPC — Cumprimento de sentença";
            }
            case ARQUIVAR -> switch (group) {
                case "TRIBUTARIO_FAZENDA" -> "Lei 6.830/80 e baixa do acervo executivo";
                case "PENAL", "MILITAR" -> "CPP/LEP e encerramento do controle executivo";
                default -> "Arts. 316 e 924 do CPC — baixa e extinção";
            };
            case DESARQUIVAR -> "Reativação judicial do acervo mediante decisão superveniente";
            default -> "Governança processual institucional";
        };
    }

    private String resolveResJudicataScope(String group,
                                           boolean provisional) {
        if (provisional) {
            return "ESTABILIDADE_PROVISORIA_CONTROLADA";
        }
        return switch (group) {
            case "ESPECIAL_CONSTITUCIONAL" -> "COISA_JULGADA_CONSTITUCIONAL_E_EFICACIA_EXPANSIVA";
            case "PENAL", "MILITAR" -> "IMUTABILIDADE_PENAL_COM_EFEITOS_EXECUTORIOS";
            default -> "COISA_JULGADA_FORMAL_E_MATERIAL";
        };
    }

    private String resolveReviewDesk(ProcessoLifecycleAction action,
                                     String group,
                                     String executionTrack,
                                     String archiveTrack,
                                     boolean segredo) {
        String prefix = segredo ? "REVISAO_SIGILO_" : "REVISAO_";
        return switch (action) {
            case CERTIFICAR_TRANSITO -> prefix + group + "_TRANSITO";
            case INICIAR_CUMPRIMENTO -> prefix + normalize(executionTrack, group + "_CUMPRIMENTO");
            case ARQUIVAR -> prefix + normalize(archiveTrack, group + "_ARQUIVAMENTO");
            case DESARQUIVAR -> prefix + group + "_REATIVACAO";
            default -> prefix + group + "_OPERACIONAL";
        };
    }

    private String resolveRetentionMode(ProcessoLifecycleAction action,
                                        String group,
                                        boolean provisional) {
        if (action == ProcessoLifecycleAction.DESARQUIVAR) {
            return "RETENCAO_REABERTA";
        }
        if (action == ProcessoLifecycleAction.ARQUIVAR) {
            return provisional ? "GUARDA_INTERMEDIARIA_COM_RETORNO_RAPIDO"
                    : group.equals("PENAL") || group.equals("MILITAR") ? "GUARDA_EXECUTIVA_PENAL_E_COMUNICACOES"
                    : "GUARDA_DEFINITIVA_DIGITAL";
        }
        return "RETENCAO_OPERACIONAL_ATIVA";
    }

    private String resolveSatisfactionMode(ProcessoLifecycleAction action,
                                           String group,
                                           String normalizedQualifier,
                                           boolean highValue) {
        if (action != ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            return "NAO_APLICAVEL";
        }
        if (group.equals("TRIBUTARIO_FAZENDA")) {
            return "SATISFACAO_FAZENDARIA_COM_GARANTIA";
        }
        if (group.equals("PENAL") || group.equals("MILITAR")) {
            return "CUMPRIMENTO_EXECUTORIO_PENAL";
        }
        if (normalizedQualifier.contains("ALIMENTO")) {
            return "SATISFACAO_PRIORITARIA_DE_PRESTACAO_ALIMENTAR";
        }
        if (highValue) {
            return "SATISFACAO_COM_AUDITORIA_PATRIMONIAL";
        }
        return "SATISFACAO_PATRIMONIAL_PROGRESSIVA";
    }

    private String resolveExecutionTrack(ProcessoLifecycleAction action,
                                         String group,
                                         boolean provisional,
                                         String normalizedQualifier) {
        if (action != ProcessoLifecycleAction.CERTIFICAR_TRANSITO && action != ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            return "POS_JULGAMENTO_SEM_EXECUCAO_IMEDIATA";
        }
        if (group.equals("TRIBUTARIO_FAZENDA")) {
            return "EXECUCAO_FAZENDARIA_COM_CONTROLE_DE_GARANTIA";
        }
        if (group.equals("TRABALHISTA")) {
            return "CUMPRIMENTO_TRABALHISTA_COM_LIQUIDACAO";
        }
        if (group.equals("PENAL") || group.equals("MILITAR")) {
            return "EXECUCAO_PENAL_JUDICIALIZADA";
        }
        if (normalizedQualifier.contains("OBRIGACAO_DE_FAZER")) {
            return "CUMPRIMENTO_DE_OBRIGACAO_ESPECIFICA";
        }
        return provisional ? "CUMPRIMENTO_PROVISORIO_COM_CAUTELA" : "CUMPRIMENTO_CIVEL_QUANTIA";
    }

    private String resolveArchiveTrack(ProcessoLifecycleAction action,
                                       String group,
                                       boolean provisional,
                                       String normalizedQualifier) {
        if (action != ProcessoLifecycleAction.ARQUIVAR && action != ProcessoLifecycleAction.DESARQUIVAR) {
            return "ACERVO_ATIVO";
        }
        if (normalizedQualifier.contains("PROVISORIO")) {
            return "ARQUIVAMENTO_PROVISORIO_CONTROLADO";
        }
        if (group.equals("PENAL") || group.equals("MILITAR")) {
            return "ARQUIVAMENTO_COM_CONTROLE_EXECUTIVO_PENAL";
        }
        if (group.equals("TRIBUTARIO_FAZENDA")) {
            return "ARQUIVAMENTO_DE_EXECUCAO_FAZENDARIA";
        }
        return provisional ? "ARQUIVAMENTO_INTERMEDIARIO" : "ARQUIVAMENTO_DEFINITIVO_DIGITAL";
    }

    private String resolveGroup(RitoProcessual rito) {
        if (rito == null) {
            return "CIVIL";
        }
        return switch (rito.group()) {
            case "TRIBUTARIO_FAZENDA" -> "TRIBUTARIO_FAZENDA";
            case "TRABALHISTA" -> "TRABALHISTA";
            case "PENAL" -> "PENAL";
            case "MILITAR" -> "MILITAR";
            case "ELEITORAL" -> "ELEITORAL";
            case "PREVIDENCIARIO" -> "PREVIDENCIARIO";
            case "ESPECIAL_CONSTITUCIONAL" -> "ESPECIAL_CONSTITUCIONAL";
            default -> "CIVIL";
        };
    }

    private String normalizeQualifier(String qualifier) {
        if (qualifier == null || qualifier.isBlank()) {
            return "";
        }
        return qualifier.strip().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static String normalize(String value, String fallback) {
        String base = firstNonBlank(value, fallback);
        if (base == null) {
            return fallback;
        }
        return base.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
