package com.tcc.pjb.backend.service.painel.shared;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PainelSharedExperienceService {

    public Map<String, Object> snapshot(String panelCode) {
        String normalized = normalize(panelCode);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("panelCode", normalized);
        out.put("calendar", calendarBlock(normalized));
        out.put("deadlines", deadlineBlock(normalized));
        out.put("colors", colorBlock(normalized));
        out.put("calculator", calculatorBlock(normalized));
        out.put("reading", readingBlock(normalized));
        out.put("notes", notes(normalized));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> calendarBlock(String panelCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate to = today.plusDays(30);
        out.put("enabled", true);
        out.put("workspacePath", "/api/v1/calendar/workspace?from=" + today + "&to=" + to + defaultProcessHint(panelCode));
        out.put("panelPath", "/api/v1/calendar/panel?from=" + today + "&to=" + to + defaultProcessHint(panelCode));
        out.put("eventsPath", "/api/v1/calendar/events?from=" + today + "&to=" + to + defaultProcessHint(panelCode));
        out.put("notificationPreviewPath", "/api/v1/calendar/notification-preview?from=" + today + "&to=" + to + defaultProcessHint(panelCode));
        out.put("recommendedLane", recommendedLane(panelCode));
        out.put("recommendedFocus", recommendedCalendarFocus(panelCode));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> deadlineBlock(String panelCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", true);
        out.put("calcularPrazoPath", "/api/v1/processual/prazos/calcular");
        out.put("diaForensePath", "/api/v1/processual/prazos/dia-forense");
        out.put("recommendedProfile", recommendedDeadlineProfile(panelCode));
        out.put("signals", List.of("VENCENDO_48H", "CRITICO_24H", "EXPIRADO", "SOB_RECESSO"));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> colorBlock(String panelCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", true);
        out.put("legendPath", "/api/v1/ui/legend");
        out.put("presentationPath", "/api/v1/ui/presentation");
        out.put("accessibilityPath", "/api/v1/ui/accessibility");
        out.put("readabilityProfilePreviewPath", "/api/v1/ui/accessibility/readability-profile/preview");
        out.put("plainLanguagePreviewPath", "/api/v1/ui/accessibility/plain-language/preview");
        out.put("recommendedPersona", recommendedPersona(panelCode));
        out.put("statusTags", List.of("NORMAL", "ATENCAO", "CRITICO", "SIGILOSO", "CONCLUSAO", "AGENDADO"));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> calculatorBlock(String panelCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean relevant = isCalculatorRelevant(panelCode);
        out.put("enabled", relevant);
        out.put("workspacePath", "/api/v1/processual/calculos/workspace");
        out.put("catalogPath", "/api/v1/processual/calculos/catalogo/frontend");
        out.put("economicReferencesPath", "/api/v1/processual/calculos/referencias/economicas");
        out.put("preferredDomains", preferredCalculatorDomains(panelCode));
        out.put("experienceMode", recommendedCalculatorExperience(panelCode));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> readingBlock(String panelCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", true);
        out.put("processReadingPathTemplate", "/api/v1/processos/{processoId}/painel-leitura");
        out.put("historyPath", "/api/v1/ui/history");
        out.put("recommendedMode", recommendedReadingMode(panelCode));
        return Collections.unmodifiableMap(out);
    }

    private List<String> notes(String panelCode) {
        if (isCalculatorRelevant(panelCode)) {
            return List.of(
                    "Este painel agora referencia calendário unificado, prazos processuais, legenda de cores e calculadora do PJB.",
                    "A calculadora foi conectada como trilha compartilhada para evitar experiência isolada por perfil."
            );
        }
        return List.of(
                "Este painel agora referencia calendário unificado, prazos processuais e legenda de cores do PJB.",
                "A calculadora permanece disponível por rota compartilhada, mas sem destaque primário neste perfil."
        );
    }

    private String defaultProcessHint(String panelCode) {
        return switch (panelCode) {
            case "CIDADAO", "DEFENSOR_PUBLICO", "MINISTERIO_PUBLICO", "PERITO", "PSICOSSOCIAL", "CONCILIADOR", "ASSESSOR", "CURADOR_AUSENTES", "LEILOEIRO", "CARTORIO_EXTRAJUDICIAL" -> "";
            default -> "";
        };
    }

    private String recommendedLane(String panelCode) {
        return switch (panelCode) {
            case "OFICIAL_JUSTICA" -> "DILIGENCIAS";
            case "DELEGADO" -> "INQUERITOS";
            case "PERITO" -> "ENTREGAS_TECNICAS";
            case "PSICOSSOCIAL" -> "VISITAS_E_ESTUDOS";
            case "CONCILIADOR" -> "SESSOES";
            case "ASSESSOR" -> "GABINETE";
            case "MINISTERIO_PUBLICO" -> "MANIFESTACOES";
            case "DEFENSOR_PUBLICO" -> "ATENDIMENTOS_E_PRAZOS";
            case "LEILOEIRO" -> "ATOS_E_EDITAIS";
            case "CARTORIO_EXTRAJUDICIAL" -> "CERTIDOES_E_ATOS";
            default -> "GERAL";
        };
    }

    private String recommendedCalendarFocus(String panelCode) {
        return switch (panelCode) {
            case "OFICIAL_JUSTICA" -> "ROTAS_E_CUMPRIMENTOS";
            case "DELEGADO" -> "INQUERITOS_E_REQUISICOES";
            case "PERITO" -> "NOMEACOES_E_LAUDOS";
            case "PSICOSSOCIAL" -> "VISITAS_E_PARECERES";
            case "CONCILIADOR" -> "SESSOES_E_ACORDOS";
            case "ASSESSOR" -> "GABINETE_E_MINUTAS";
            case "MINISTERIO_PUBLICO" -> "PARECERES_E_DILIGENCIAS";
            case "DEFENSOR_PUBLICO" -> "ATENDIMENTOS_E_RECURSOS";
            case "LEILOEIRO" -> "EDITAIS_E_PRACAS";
            case "CARTORIO_EXTRAJUDICIAL" -> "ATOS_E_INDISPONIBILIDADES";
            case "CURADOR_AUSENTES" -> "BENS_E_PRESTACOES";
            case "CIDADAO" -> "PROCESSOS_E_EVENTOS";
            default -> "ROTINA_INSTITUCIONAL";
        };
    }

    private String recommendedDeadlineProfile(String panelCode) {
        return switch (panelCode) {
            case "OFICIAL_JUSTICA" -> "CUMPRIMENTO_E_INTIMACAO";
            case "DELEGADO" -> "INQUERITO_E_REQUISICOES";
            case "PERITO" -> "LAUDO_E_ENTREGA";
            case "PSICOSSOCIAL" -> "RELATORIO_TECNICO";
            case "CONCILIADOR" -> "SESSAO_E_HOMOLOGACAO";
            case "ASSESSOR" -> "GABINETE_E_MINUTA";
            case "MINISTERIO_PUBLICO" -> "PARECER_E_RECURSO";
            case "DEFENSOR_PUBLICO" -> "DEFESA_E_RECURSO";
            case "LEILOEIRO" -> "EDITAL_E_PRESTACAO_DE_CONTAS";
            case "CARTORIO_EXTRAJUDICIAL" -> "CERTIDAO_E_ATO_REGISTRAL";
            case "CURADOR_AUSENTES" -> "CURADORIA_PATRIMONIAL";
            case "CIDADAO" -> "CIENCIA_E_COMPARECIMENTO";
            default -> "OPERACIONAL_GERAL";
        };
    }

    private String recommendedPersona(String panelCode) {
        return switch (panelCode) {
            case "CIDADAO" -> "CIDADAO";
            case "CURADOR_AUSENTES", "PSICOSSOCIAL" -> "ACESSIBILIDADE_ESTENDIDA";
            default -> "INSTITUCIONAL";
        };
    }

    private boolean isCalculatorRelevant(String panelCode) {
        return switch (panelCode) {
            case "CIDADAO", "DEFENSOR_PUBLICO", "MINISTERIO_PUBLICO", "OFICIAL_JUSTICA", "PERITO", "LEILOEIRO", "CARTORIO_EXTRAJUDICIAL", "ASSESSOR", "CONCILIADOR" -> true;
            default -> false;
        };
    }

    private List<String> preferredCalculatorDomains(String panelCode) {
        return switch (panelCode) {
            case "OFICIAL_JUSTICA", "LEILOEIRO", "CARTORIO_EXTRAJUDICIAL" -> List.of("CUSTAS_PROCESSUAIS", "FAZENDA_TRIBUTARIO");
            case "PERITO" -> List.of("CUSTAS_PROCESSUAIS", "TRABALHISTA");
            case "MINISTERIO_PUBLICO", "DEFENSOR_PUBLICO", "ASSESSOR", "CONCILIADOR", "CIDADAO" -> List.of("TRABALHISTA", "FEDERAL_PREVIDENCIARIO_CJF", "CUSTAS_PROCESSUAIS", "FAZENDA_TRIBUTARIO");
            default -> List.of("CUSTAS_PROCESSUAIS");
        };
    }

    private String recommendedCalculatorExperience(String panelCode) {
        return switch (panelCode) {
            case "CIDADAO", "CONCILIADOR" -> "GUIDED";
            case "ASSESSOR", "MINISTERIO_PUBLICO", "DEFENSOR_PUBLICO", "PERITO" -> "PROFESSIONAL";
            default -> "STANDARD";
        };
    }

    private String recommendedReadingMode(String panelCode) {
        return switch (panelCode) {
            case "CIDADAO" -> "SIMPLIFICADO";
            case "PSICOSSOCIAL", "CURADOR_AUSENTES" -> "CONFORTO_VISUAL";
            default -> "TECNICO_ESTRUTURADO";
        };
    }

    private String normalize(String panelCode) {
        if (panelCode == null || panelCode.isBlank()) {
            return "GERAL";
        }
        return panelCode.trim().toUpperCase(Locale.ROOT);
    }
}
