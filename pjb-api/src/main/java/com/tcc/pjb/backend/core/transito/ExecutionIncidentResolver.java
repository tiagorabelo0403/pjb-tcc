package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
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
public class ExecutionIncidentResolver {

    public ExecutionIncidentProfile resolve(Processo processo,
                                           String incidente,
                                           String fundamentacao,
                                           double valorGarantia) {
        String incidentType = resolveIncidentType(incidente);
        RitoProcessual rito = processo == null ? null : processo.getRito();
        String group = rito == null ? "CIVIL" : rito.group();
        boolean fiscal = "TRIBUTARIO_FAZENDA".equals(group) || rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO;
        boolean penal = "PENAL".equals(group) || "MILITAR".equals(group);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean archived = processo != null && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO;
        boolean highValue = valorGarantia >= 100_000D;

        String incidentMode = resolveIncidentMode(incidentType, fiscal, penal);
        String admissibilityTrack = resolveAdmissibilityTrack(incidentType, fiscal, penal);
        String queueCode = resolveQueueCode(incidentType, fiscal, penal, sigilo);
        String inboxKey = resolveInboxKey(incidentType, fiscal, penal);
        TipoUsuario assignedRole = resolveAssignedRole(incidentType, fiscal);
        int priority = resolvePriority(incidentType, highValue, penal, sigilo);
        boolean blocking = resolveBlocking(incidentType);
        long dueAmount = resolveDueAmount(incidentType, penal);
        ChronoUnit dueUnit = resolveDueUnit(incidentType);
        String baseLegal = resolveBaseLegal(incidentType, fiscal, penal);
        String contradictionMode = resolveContradictionMode(incidentType);
        String evidenceMode = resolveEvidenceMode(incidentType, fiscal, highValue);
        String escalationDesk = resolveEscalationDesk(incidentType, penal, fiscal);
        String executionImpact = resolveExecutionImpact(incidentType, fiscal, penal);
        String preventionMode = resolvePreventionMode(incidentType);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(incidentType);
        labels.add(incidentMode);
        labels.add(admissibilityTrack);
        if (fiscal) {
            labels.add("EXECUCAO_FISCAL");
        }
        if (penal) {
            labels.add("EXECUCAO_PENAL");
        }
        if (sigilo) {
            labels.add("SIGILO_REFORCADO");
        }
        if (highValue) {
            labels.add("ALTO_VALOR");
        }

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (archived) {
            warnings.add("Processo arquivado exige reativação controlada antes do incidente executivo.");
        }
        if (processo != null && processo.getFaseAtual() != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            warnings.add("Incidente executivo em fase recursal exige conferência da eficácia imediata do título.");
        }
        if (incidentType.equals("EXCECAO_PRE_EXECUTIVIDADE")) {
            reviewChecklist.add("Conferir nulidades cognoscíveis de ofício e desnecessidade de garantia do juízo.");
        }
        if (incidentType.equals("EMBARGOS_EXECUCAO")) {
            reviewChecklist.add("Validar tempestividade, garantia do juízo e efeito suspensivo quando cabível.");
        }
        if (incidentType.equals("IMPUGNACAO_CUMPRIMENTO")) {
            reviewChecklist.add("Conferir excesso, inexequibilidade, prescrição superveniente e cálculo atualizado.");
        }
        if (incidentType.equals("DESCONSIDERACAO_PERSONALIDADE")) {
            reviewChecklist.add("Assegurar contraditório dos terceiros e lastro patrimonial mínimo do incidente.");
        }
        if (incidentType.equals("HABILITACAO_CREDITO")) {
            reviewChecklist.add("Conferir legitimidade, cadeia sucessória e documentos de titularidade do crédito.");
        }
        if (incidentType.equals("CONCURSO_PREFERENCIAS")) {
            reviewChecklist.add("Ordenar preferências legais, penhoras concorrentes e reserva proporcional do produto expropriatório.");
        }
        if (fundamentacao == null || fundamentacao.isBlank()) {
            reviewChecklist.add("Anexar fundamentação mínima e peças essenciais antes da conclusão do incidente.");
        }
        if (highValue) {
            reviewChecklist.add("Aplicar mesa reforçada de cálculo e auditoria patrimonial por envolver garantia elevada.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("incidentType", incidentType);
        metadata.put("incidentMode", incidentMode);
        metadata.put("fiscal", fiscal);
        metadata.put("penal", penal);
        metadata.put("sigilo", sigilo);
        metadata.put("archived", archived);
        metadata.put("highValue", highValue);
        metadata.put("statusAtual", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("faseAtual", processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);
        metadata.put("descriptor", String.join(":",
                normalize(incidentType),
                normalize(incidentMode),
                normalize(admissibilityTrack),
                normalize(queueCode)));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new ExecutionIncidentProfile(
                incidentType,
                incidentMode,
                admissibilityTrack,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                contradictionMode,
                evidenceMode,
                escalationDesk,
                executionImpact,
                preventionMode,
                List.copyOf(labels),
                List.copyOf(warnings),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveIncidentType(String incidente) {
        String token = normalize(incidente);
        return switch (token) {
            case "IMPUGNACAO", "IMPUGNACAO_CUMPRIMENTO", "IMPUGNACAO_AO_CUMPRIMENTO" -> "IMPUGNACAO_CUMPRIMENTO";
            case "EMBARGOS", "EMBARGOS_EXECUCAO", "EMBARGOS_A_EXECUCAO", "EMBARGOS_EXECUCAO_FISCAL" -> "EMBARGOS_EXECUCAO";
            case "EXCECAO_PRE_EXECUTIVIDADE", "EXCECAO_PREEXECUTIVIDADE", "PRE_EXECUTIVIDADE" -> "EXCECAO_PRE_EXECUTIVIDADE";
            case "DESCONSIDERACAO", "IDPJ", "DESCONSIDERACAO_PERSONALIDADE" -> "DESCONSIDERACAO_PERSONALIDADE";
            case "HABILITACAO", "HABILITACAO_CREDITO", "HABILITACAO_SUCESSOR" -> "HABILITACAO_CREDITO";
            case "CONCURSO", "CONCURSO_PREFERENCIAS", "CONCURSO_CREDORES" -> "CONCURSO_PREFERENCIAS";
            default -> "INCIDENTE_EXECUTIVO_GERAL";
        };
    }

    private String resolveIncidentMode(String incidentType, boolean fiscal, boolean penal) {
        return switch (incidentType) {
            case "IMPUGNACAO_CUMPRIMENTO" -> "DEFESA_EXECUTIVA_NOS_PROPRIOS_AUTOS";
            case "EMBARGOS_EXECUCAO" -> fiscal ? "DEFESA_EXECUTIVA_AUTONOMA_FISCAL" : "DEFESA_EXECUTIVA_AUTONOMA";
            case "EXCECAO_PRE_EXECUTIVIDADE" -> "CONTROLE_COGNOSCIVEL_SEM_GARANTIA";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "EXPANSAO_SUBJETIVA_DA_EXECUCAO";
            case "HABILITACAO_CREDITO" -> "SUBSTITUICAO_OU_INGRESSO_DE_SUJEITO_CREDITICIO";
            case "CONCURSO_PREFERENCIAS" -> "ORDENACAO_DE_PREFERENCIAS_EXECUTIVAS";
            default -> penal ? "INCIDENTE_EXECUTIVO_PENAL" : "INCIDENTE_EXECUTIVO_PADRAO";
        };
    }

    private String resolveAdmissibilityTrack(String incidentType, boolean fiscal, boolean penal) {
        if (incidentType.equals("DESCONSIDERACAO_PERSONALIDADE")) {
            return "TRIAGEM_IDPJ_COM_CONTRADITORIO_PREVIO";
        }
        if (incidentType.equals("EXCECAO_PRE_EXECUTIVIDADE")) {
            return "TRIAGEM_MATERIA_DE_ORDEM_PUBLICA";
        }
        if (incidentType.equals("EMBARGOS_EXECUCAO")) {
            return fiscal ? "TRIAGEM_GARANTIA_E_TEMPESTIVIDADE_FISCAL" : "TRIAGEM_GARANTIA_E_TEMPESTIVIDADE";
        }
        if (incidentType.equals("CONCURSO_PREFERENCIAS")) {
            return "TRIAGEM_RESERVA_E_PREFERENCIAS";
        }
        return penal ? "TRIAGEM_EXECUCAO_PENAL" : "TRIAGEM_EXECUTIVA";
    }

    private String resolveQueueCode(String incidentType, boolean fiscal, boolean penal, boolean sigilo) {
        String base = switch (incidentType) {
            case "IMPUGNACAO_CUMPRIMENTO" -> "INCIDENTE_IMPUGNACAO_CUMPRIMENTO";
            case "EMBARGOS_EXECUCAO" -> fiscal ? "INCIDENTE_EMBARGOS_EXECUCAO_FISCAL" : "INCIDENTE_EMBARGOS_EXECUCAO";
            case "EXCECAO_PRE_EXECUTIVIDADE" -> "INCIDENTE_EXCECAO_PRE_EXECUTIVIDADE";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "INCIDENTE_IDPJ_EXECUCAO";
            case "HABILITACAO_CREDITO" -> "INCIDENTE_HABILITACAO_CREDITO";
            case "CONCURSO_PREFERENCIAS" -> "INCIDENTE_CONCURSO_PREFERENCIAS";
            default -> penal ? "INCIDENTE_EXECUCAO_PENAL" : "INCIDENTE_EXECUTIVO_GERAL";
        };
        return sigilo ? base + "_SIGILO" : base;
    }

    private String resolveInboxKey(String incidentType, boolean fiscal, boolean penal) {
        return switch (incidentType) {
            case "IMPUGNACAO_CUMPRIMENTO", "EXCECAO_PRE_EXECUTIVIDADE" -> "GABINETE_EXECUCAO_CONTROLE";
            case "EMBARGOS_EXECUCAO" -> fiscal ? "SECRETARIA_EXECUCAO_FISCAL_INCIDENTES" : "GABINETE_EXECUCAO_INCIDENTES";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "GABINETE_IDPJ_EXECUCAO";
            case "HABILITACAO_CREDITO", "CONCURSO_PREFERENCIAS" -> "SECRETARIA_EXECUCAO_INCIDENTES";
            default -> penal ? "JUIZO_EXECUCAO_PENAL" : "WORKSPACE_EXECUCAO";
        };
    }

    private TipoUsuario resolveAssignedRole(String incidentType, boolean fiscal) {
        return switch (incidentType) {
            case "HABILITACAO_CREDITO", "CONCURSO_PREFERENCIAS" -> TipoUsuario.SERVIDOR_FORUM;
            case "EMBARGOS_EXECUCAO" -> fiscal ? TipoUsuario.SERVIDOR_FORUM : TipoUsuario.JUIZ;
            default -> TipoUsuario.JUIZ;
        };
    }

    private int resolvePriority(String incidentType, boolean highValue, boolean penal, boolean sigilo) {
        if (incidentType.equals("EXCECAO_PRE_EXECUTIVIDADE") || incidentType.equals("CONCURSO_PREFERENCIAS")) {
            return 0;
        }
        if (highValue || penal || sigilo) {
            return 1;
        }
        return 2;
    }

    private boolean resolveBlocking(String incidentType) {
        return switch (incidentType) {
            case "EXCECAO_PRE_EXECUTIVIDADE", "CONCURSO_PREFERENCIAS", "DESCONSIDERACAO_PERSONALIDADE" -> true;
            default -> false;
        };
    }

    private long resolveDueAmount(String incidentType, boolean penal) {
        return switch (incidentType) {
            case "EXCECAO_PRE_EXECUTIVIDADE" -> 3L;
            case "CONCURSO_PREFERENCIAS" -> 5L;
            case "DESCONSIDERACAO_PERSONALIDADE" -> 10L;
            case "HABILITACAO_CREDITO" -> 7L;
            default -> penal ? 3L : 10L;
        };
    }

    private ChronoUnit resolveDueUnit(String incidentType) {
        return switch (incidentType) {
            case "EXCECAO_PRE_EXECUTIVIDADE" -> ChronoUnit.DAYS;
            default -> ChronoUnit.DAYS;
        };
    }

    private String resolveBaseLegal(String incidentType, boolean fiscal, boolean penal) {
        return switch (incidentType) {
            case "IMPUGNACAO_CUMPRIMENTO" -> "Art. 525 CPC — Impugnação ao cumprimento de sentença";
            case "EMBARGOS_EXECUCAO" -> fiscal ? "Lei 6.830/80 e embargos do executado fiscal" : "Art. 914 CPC — Embargos à execução";
            case "EXCECAO_PRE_EXECUTIVIDADE" -> "Controle judicial de nulidades executivas e matérias cognoscíveis de ofício";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "Arts. 133 a 137 CPC — Incidente de desconsideração";
            case "HABILITACAO_CREDITO" -> "Habilitação de crédito e sucessão processual na fase executiva";
            case "CONCURSO_PREFERENCIAS" -> "Art. 908 CPC — Concurso singular e preferências";
            default -> penal ? "CPP/LEP e incidentes da execução penal" : "Governança geral de incidentes executivos";
        };
    }

    private String resolveContradictionMode(String incidentType) {
        return switch (incidentType) {
            case "DESCONSIDERACAO_PERSONALIDADE" -> "CONTRADITORIO_PREVIO_DE_TERCEIROS";
            case "CONCURSO_PREFERENCIAS" -> "CONTRADITORIO_MULTIPOLAR";
            case "HABILITACAO_CREDITO" -> "MANIFESTACAO_DAS_PARTES_E_TERCEIROS";
            default -> "CONTRADITORIO_BILATERAL";
        };
    }

    private String resolveEvidenceMode(String incidentType, boolean fiscal, boolean highValue) {
        if (incidentType.equals("DESCONSIDERACAO_PERSONALIDADE")) {
            return "LASTRO_PATRIMONIAL_E_SOCIETARIO";
        }
        if (incidentType.equals("CONCURSO_PREFERENCIAS")) {
            return "MATRIZ_DE_PENHORAS_E_CREDITOS_CONCORRENTES";
        }
        if (fiscal) {
            return "CDA_GARANTIA_E_CALCULO_FAZENDARIO";
        }
        return highValue ? "MEMORIA_EXECUTIVA_AUDITAVEL" : "PECAS_EXECUTIVAS_PADRAO";
    }

    private String resolveEscalationDesk(String incidentType, boolean penal, boolean fiscal) {
        if (incidentType.equals("DESCONSIDERACAO_PERSONALIDADE")) {
            return "GABINETE_IDPJ_REFORCADO";
        }
        if (incidentType.equals("CONCURSO_PREFERENCIAS")) {
            return "MESA_CONCURSO_CREDITOS";
        }
        if (fiscal) {
            return "NUCLEO_EXECUCAO_FISCAL";
        }
        return penal ? "JUIZO_EXECUCAO_PENAL_COORDENACAO" : "GABINETE_EXECUCAO_COORDENACAO";
    }

    private String resolveExecutionImpact(String incidentType, boolean fiscal, boolean penal) {
        return switch (incidentType) {
            case "EXCECAO_PRE_EXECUTIVIDADE" -> "POTENCIAL_SUSPENSAO_DA_MARCHA_EXECUTIVA";
            case "CONCURSO_PREFERENCIAS" -> "REORDENACAO_DA_SATISFACAO_DO_CREDITO";
            case "DESCONSIDERACAO_PERSONALIDADE" -> "EXPANSAO_DO_POLO_PASSIVO";
            case "HABILITACAO_CREDITO" -> "ALTERACAO_SUBJETIVA_DA_DESTINACAO_DO_PAGAMENTO";
            default -> penal ? "AJUSTE_DO_CUMPRIMENTO_DA_PENA" : fiscal ? "RECALIBRAGEM_DA_EXECUCAO_FISCAL" : "AJUSTE_DA_MARCHA_EXECUTIVA";
        };
    }

    private String resolvePreventionMode(String incidentType) {
        return switch (incidentType) {
            case "CONCURSO_PREFERENCIAS", "DESCONSIDERACAO_PERSONALIDADE" -> "PREVENCAO_DO_JUIZO_EXECUTOR";
            default -> "MESMO_JUIZO_DA_EXECUCAO";
        };
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "INCIDENTE_EXECUTIVO_GERAL";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }
}
