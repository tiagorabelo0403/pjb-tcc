package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingDocumentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProcessEntryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingSpecializationResolver {

    public ProcessReadingSpecializationResponse resolve(Processo processo,
                                                       ProcessReadingModeProfile modeProfile,
                                                       ProcessReadingFlowResponse processFlow,
                                                       ProcessReadingProceduralContextResponse proceduralContext,
                                                       List<ProcessReadingDocumentResponse> documents) {
        String scopeCode = resolveScopeCode(proceduralContext);
        String chamberMode = resolveChamberMode(proceduralContext);
        String decisionMode = resolveDecisionMode(processo, proceduralContext);
        String evidenceMode = resolveEvidenceMode(processo, modeProfile);
        String resourceMode = resolveResourceMode(processo, processFlow, proceduralContext);
        String embargoMode = resolveEmbargoMode(processo, processFlow, proceduralContext);
        String hearingMode = resolveHearingMode(processo, proceduralContext);
        String executionMode = resolveExecutionMode(processo, proceduralContext);
        boolean nativeHtmlPriority = proceduralContext.htmlInlinePreferred() || processFlow.totalInlineActs() > 0;
        boolean signedPdfInspectionRequired = proceduralContext.pdfSignedPreferred() || (documents != null && documents.stream().anyMatch(doc -> isPdf(doc.contentType())));
        List<String> openingSequence = resolveOpeningSequence(processo, proceduralContext, nativeHtmlPriority, signedPdfInspectionRequired);
        List<String> preferredActModes = resolvePreferredActModes(processo, processFlow, proceduralContext);
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        markers.add(scopeCode);
        markers.add(chamberMode);
        markers.add(resourceMode);
        markers.add(embargoMode);
        markers.add(executionMode);
        if (nativeHtmlPriority) markers.add("HTML_NATIVO_PRIORIZADO");
        if (signedPdfInspectionRequired) markers.add("CONFERENCIA_PDF_ASSINADO_ATIVA");
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) markers.add("LEITURA_RECURSAL_ESTRUTURADA");
        if (processo != null && (processo.getFaseAtual() == FaseProcessual.EXECUCAO || processo.getFaseAtual() == FaseProcessual.CUMPRIMENTO_SENTENCA)) {
            markers.add("MALHA_EXECUTIVA_EM_LEITURA");
        }
        if (processo != null && processo.getRito() != null && processo.getRito().requiresSegredoByDefault()) markers.add("SIGILO_DEFAULTO_SENSIVEL");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("justiceTrack", proceduralContext.justiceTrack());
        metadata.put("tribunalTier", proceduralContext.tribunalTier());
        metadata.put("ramo", proceduralContext.ramo());
        metadata.put("materia", proceduralContext.materia());
        metadata.put("rito", proceduralContext.rito());
        metadata.put("fase", proceduralContext.fase());
        metadata.put("instanciaLeitura", proceduralContext.instanciaLeitura());
        metadata.put("orgaoLeitura", proceduralContext.orgaoLeitura());
        metadata.put("nativeActTrack", proceduralContext.nativeActTrack());
        metadata.put("signatureTrack", proceduralContext.signatureTrack());
        metadata.put("totalEntries", processFlow.totalEntries());
        metadata.put("totalInlineActs", processFlow.totalInlineActs());
        metadata.put("totalMovements", processFlow.totalMovements());
        metadata.put("totalEvents", processFlow.totalEvents());
        metadata.put("totalDocuments", documents == null ? 0 : documents.size());
        metadata.put("supportsAllJusticeBranches", true);
        metadata.put("supportsAllBrazilianRites", true);
        metadata.put("supportsAllBrazilianRights", true);
        metadata.put("supportsAllProceduralGuarantees", true);
        metadata.put("supportsAllInstances", true);
        metadata.put("supportsRecursosEEmbargos", true);
        metadata.put("supportsNativeHtmlActs", true);
        metadata.put("supportsSignedPdfInspection", signedPdfInspectionRequired);
        metadata.put("supportsInlineAndPdfHybridReading", nativeHtmlPriority && signedPdfInspectionRequired);
        metadata.put("supportsPecaSpecialization", true);
        metadata.put("openingSequenceSize", openingSequence.size());
        return new ProcessReadingSpecializationResponse(
                scopeCode,
                chamberMode,
                decisionMode,
                evidenceMode,
                resourceMode,
                embargoMode,
                hearingMode,
                executionMode,
                modeProfile.supportDeskMode(),
                nativeHtmlPriority,
                signedPdfInspectionRequired,
                openingSequence,
                preferredActModes,
                List.copyOf(markers),
                metadata
        );
    }

    private static String resolveScopeCode(ProcessReadingProceduralContextResponse proceduralContext) {
        return joinCodes(
                proceduralContext.justiceTrack(),
                proceduralContext.tribunalTier(),
                proceduralContext.ritoFamily(),
                proceduralContext.instanciaLeitura()
        );
    }

    private static String resolveChamberMode(ProcessReadingProceduralContextResponse proceduralContext) {
        String tier = normalize(proceduralContext.tribunalTier());
        String orgao = normalize(proceduralContext.orgaoLeitura());
        if (containsAny(tier, "TRIBUNAL_SUPERIOR")) return "GABINETE_TURMA_SECAO_PLENARIO_SUPERIOR";
        if (containsAny(tier, "SEGUNDO_GRAU")) return containsAny(orgao, "CAM", "TURMA", "SECAO", "PLENARIO")
                ? "CAMARA_TURMA_SECAO_PLENARIO"
                : "GABINETE_RELATOR_E_ORGAO_FRACIONARIO";
        if (containsAny(orgao, "JUIZADO")) return "JUIZADO_E_SECRETARIA";
        if (containsAny(orgao, "AUDITORIA")) return "AUDITORIA_E_CONSELHO";
        if (containsAny(orgao, "ZONA")) return "ZONA_E_CARTORIO_ELEITORAL";
        return "VARA_GABINETE_SECRETARIA";
    }

    private static String resolveDecisionMode(Processo processo, ProcessReadingProceduralContextResponse proceduralContext) {
        RitoProcessual rito = processo != null ? processo.getRito() : null;
        FaseProcessual fase = processo != null ? processo.getFaseAtual() : null;
        if (fase == FaseProcessual.RECURSAL) return containsAny(normalize(proceduralContext.tribunalTier()), "TRIBUNAL_SUPERIOR")
                ? "ACORDAO_PRECEDENTE_RECURSO_EMBARGOS_SUPERIORES"
                : "DECISAO_ATACADA_RAZOES_CONTRARRAZOES_VOTO_ACORDAO";
        if (fase == FaseProcessual.EXECUCAO || fase == FaseProcessual.CUMPRIMENTO_SENTENCA) return "TITULO_CALCULO_INCIDENTES_CONSTRICAO_EXPROPRIACAO_SATISFACAO";
        if (rito == null) return "PETICAO_PROVA_SANEAMENTO_DECISAO";
        if (rito.isPenal()) return "CUSTODIA_DENUNCIA_RESPOSTA_INSTRUCAO_SENTENCA_RECURSO";
        if (rito.isTrabalhista()) return "PETICAO_DEFESA_AUDIENCIA_PROVA_SENTENCA_CALCULO_EXECUCAO";
        if (rito.isPrevidenciario()) return "DER_REQUISITO_PROVA_SOCIAL_LAUDO_SENTENCA_RECURSO";
        if (rito.isTribFazenda()) return "INICIAL_CONTESTACAO_DOCUMENTOS_PRECEDENTES_SENTENCA_RECURSO";
        if (rito.isEleitoral()) return "REGISTRO_PROPAGANDA_ELEGIBILIDADE_PROVA_DIGITAL_DECISAO_RECURSO";
        if (rito.isMilitar()) return "TIPICIDADE_MATERIALIDADE_HIERARQUIA_INSTRUCAO_JULGAMENTO_RECURSO";
        if (rito.isEspecialConstitucional()) return "PETICAO_INICIAL_INFORMACOES_PARECER_DECISAO_MONOCRATICA_OU_COLEGIADA";
        return "PETICAO_PROVA_SANEAMENTO_DECISAO";
    }

    private static String resolveEvidenceMode(Processo processo, ProcessReadingModeProfile modeProfile) {
        RitoProcessual rito = processo != null ? processo.getRito() : null;
        if (rito == null) return modeProfile.evidenceMode();
        if (rito.isPenal()) return "MATERIALIDADE_AUTORIA_CADEIA_CUSTODIA_TESTEMUNHAS_PERICIA";
        if (rito.isTrabalhista()) return "JORNADA_RECIBOS_PAGAMENTO_TESTEMUNHAS_CALCULOS";
        if (rito.isPrevidenciario()) return "DER_CADASTRO_PERICIA_SOCIAL_DOCUMENTOS_MEDICOS";
        if (rito.isTribFazenda()) return "CDA_LANCAMENTO_PLANILHAS_PRECEDENTES_DOCUMENTOS_OFICIAIS";
        if (rito.isEleitoral()) return "MIDIA_DIGITAL_PROPAGANDA_ARRECADAÇÃO_ELEGIBILIDADE";
        if (rito.isMilitar()) return "AUTO_RELATORIOS_PERICIA_HIERARQUIA_E_ORDEM_DE_SERVICO";
        return modeProfile.evidenceMode();
    }

    private static String resolveResourceMode(Processo processo,
                                              ProcessReadingFlowResponse processFlow,
                                              ProcessReadingProceduralContextResponse proceduralContext) {
        String track = normalize(proceduralContext.recursalTrack());
        if (containsAny(track, "DECISAO_RECURSO_CONTRARRAZOES_E_JULGAMENTO")) return "APELACAO_AGRAVO_RECURSO_ORDINARIO_CONTRARRAZOES_VOTO_ACORDAO";
        if (containsAny(track, "RECUSAL_INCIDENTAL_MATERIALIZADA", "RECURSAL_INCIDENTAL_MATERIALIZADA")) return "RECURSO_INCIDENTAL_NO_CURSO_DO_PROCESSO";
        boolean recursalEntries = processFlow.entries().stream().anyMatch(entry -> containsAny(normalize(entry.originMode()), "RECUR", "AGRAV", "APELA", "CONTRARRAZ", "ACORDAO"));
        if (recursalEntries) return "TRILHA_RECURSAL_IDENTIFICADA_POR_ATOS";
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) return "FASE_RECURSAL_SEM_PECA_INDEXADA";
        return "SEM_TRILHA_RECURSAL_PRIORITARIA";
    }

    private static String resolveEmbargoMode(Processo processo,
                                             ProcessReadingFlowResponse processFlow,
                                             ProcessReadingProceduralContextResponse proceduralContext) {
        if (!"SEM_MALHA_DE_EMBARGOS".equals(proceduralContext.embargoTrack())) return "EMBARGOS_DECLARACAO_INTEGRACAO_PREQUESTIONAMENTO";
        boolean embargos = processFlow.entries().stream().anyMatch(entry -> containsAny(normalize(entry.originMode()), "EMBARG"));
        if (embargos) return "EMBARGOS_IDENTIFICADOS_POR_ATO_NATIVO";
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) return "EMBARGOS_POTENCIAIS_CONFORME_FASE_RECURSAL";
        return "SEM_EMBARGOS_PRIORIZADOS";
    }

    private static String resolveHearingMode(Processo processo, ProcessReadingProceduralContextResponse proceduralContext) {
        RitoProcessual rito = processo != null ? processo.getRito() : null;
        if (rito == null) return "AUDIENCIA_E_GABINETE_HIBRIDO";
        if (rito.isPenal()) return "CUSTODIA_INSTRUCAO_JURI_SESSAO_PENAL";
        if (rito.isTrabalhista()) return "AUDIENCIA_UNA_CONCILIACAO_INSTRUCAO";
        if (rito.isEleitoral()) return "AUDIENCIA_DILIGENCIA_SUSTENTACAO_ELEITORAL";
        if (rito.isMilitar()) return "AUDITORIA_CONSELHO_JUSTICA_E_SESSAO_MILITAR";
        if (containsAny(normalize(proceduralContext.tribunalTier()), "SEGUNDO_GRAU", "TRIBUNAL_SUPERIOR")) return "GABINETE_SUSTENTACAO_SESSAO_COLEGIADA";
        return "AUDIENCIA_SANEAMENTO_GABINETE";
    }

    private static String resolveExecutionMode(Processo processo, ProcessReadingProceduralContextResponse proceduralContext) {
        FaseProcessual fase = processo != null ? processo.getFaseAtual() : null;
        RitoProcessual rito = processo != null ? processo.getRito() : null;
        if (fase == FaseProcessual.EXECUCAO || fase == FaseProcessual.CUMPRIMENTO_SENTENCA) return "EXECUCAO_CONSTRICAO_EXPROPRIACAO_SATISFACAO_E_BAIXA";
        if (rito == null) return "EXECUCAO_NAO_PRIORITARIA";
        if (rito == RitoProcessual.EXECUCAO_FISCAL || rito == RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL) return "EXECUCAO_FISCAL_CDA_GARANTIA_EMBARGOS_EXPROPRIACAO";
        if (rito == RitoProcessual.EXECUCAO_PENAL) return "EXECUCAO_PENAL_REGIME_BENEFICIOS_INCIDENTES";
        if (rito.isTrabalhista() && containsAny(rito.name(), "EXECUCAO", "CUMPRIMENTO")) return "EXECUCAO_TRABALHISTA_CALCULOS_GARANTIA_SATISFACAO";
        return containsAny(proceduralContext.ritoFamily(), "EXECUCAO") ? "EXECUCAO_SETORIAL_PRIORIZADA" : "EXECUCAO_NAO_PRIORITARIA";
    }

    private static List<String> resolveOpeningSequence(Processo processo,
                                                       ProcessReadingProceduralContextResponse proceduralContext,
                                                       boolean nativeHtmlPriority,
                                                       boolean signedPdfInspectionRequired) {
        LinkedHashSet<String> order = new LinkedHashSet<>();
        order.add("RESUMO");
        order.add("CONTEXTO_PROCEDIMENTAL");
        if (nativeHtmlPriority) order.add("ATOS_HTML_NATIVOS");
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            order.add("DECISAO_ATACADA");
            order.add("RECURSOS");
            order.add("CONTRARRAZOES");
            order.add("VOTOS_E_ACORDAO");
            order.add("EMBARGOS_E_INCIDENTES");
        }
        if (processo != null && (processo.getFaseAtual() == FaseProcessual.EXECUCAO || processo.getFaseAtual() == FaseProcessual.CUMPRIMENTO_SENTENCA)) {
            order.add("TITULO_E_CALCULOS");
            order.add("INCIDENTES_EXECUTIVOS");
            order.add("CONSTRICAO_E_EXPROPRIACAO");
            order.add("SATISFACAO_E_BAIXA");
        }
        if (processo != null && processo.getRito() != null && processo.getRito().isPenal()) {
            order.add("DENUNCIA_E_RESPOSTA");
            order.add("PROVAS_E_PERICIAS");
            order.add("AUDIENCIAS_E_DECISOES");
        } else {
            order.add("PECAS_E_PROVAS");
            order.add("EVENTOS_E_PRAZOS");
        }
        if (signedPdfInspectionRequired) order.add("CONFERENCIA_PDF_ASSINADO");
        return List.copyOf(order);
    }

    private static List<String> resolvePreferredActModes(Processo processo,
                                                         ProcessReadingFlowResponse processFlow,
                                                         ProcessReadingProceduralContextResponse proceduralContext) {
        List<String> preferred = processFlow.entries().stream()
                .map(ProcessReadingProcessEntryResponse::originMode)
                .filter(ProcessReadingSpecializationResolver::hasText)
                .map(ProcessReadingSpecializationResolver::normalize)
                .distinct()
                .limit(12)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!preferred.isEmpty()) return List.copyOf(preferred);
        LinkedHashSet<String> fallback = new LinkedHashSet<>();
        if (proceduralContext.htmlInlinePreferred()) fallback.add("ATO_TEXTUAL_HTML_ASSINAVEL");
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            fallback.add("ATO_RECURSAL_HTML");
            fallback.add("EMBARGOS_DECLARACAO_HTML");
            fallback.add("ACORDAO_HTML_NATIVO");
        }
        if (processo != null && processo.getRito() != null && processo.getRito().isPenal()) {
            fallback.add("DECISAO_INTERLOCUTORIA_HTML_NATIVA");
            fallback.add("SENTENCA_HTML_NATIVA_ASSINAVEL");
        }
        fallback.add("MOVIMENTACAO_HTML_NATIVA");
        return List.copyOf(fallback);
    }

    private static String joinCodes(String... values) {
        return Arrays.stream(values == null ? new String[0] : values)
                .filter(ProcessReadingSpecializationResolver::hasText)
                .map(ProcessReadingSpecializationResolver::normalize)
                .collect(Collectors.joining("__"));
    }

    private static boolean isPdf(String contentType) {
        return hasText(contentType) && contentType.toLowerCase(Locale.ROOT).contains("pdf");
    }

    private static boolean containsAny(String text, String... tokens) {
        if (!hasText(text)) return false;
        String normalized = normalize(text);
        for (String token : tokens) {
            if (hasText(token) && normalized.contains(normalize(token))) return true;
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }
}
