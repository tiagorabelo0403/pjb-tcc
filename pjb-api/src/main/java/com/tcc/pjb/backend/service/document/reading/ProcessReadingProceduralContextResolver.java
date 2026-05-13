package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingProceduralContextResolver {

    public ProcessReadingProceduralContextResponse resolve(Processo processo,
                                                           ProcessReadingModeProfile modeProfile,
                                                           ProcessReadingFlowResponse processFlow,
                                                           ProcessReadingNavigationResponse navigation,
                                                           long totalDocumentos,
                                                           long totalPaginas) {
        String justiceTrack = resolveJusticeTrack(processo);
        String tribunalTier = resolveTribunalTier(processo);
        String ramo = processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "MULTIMATERIA";
        String materia = processo != null && processo.getMateria() != null ? processo.getMateria().name() : "MULTIMATERIA";
        String rito = processo != null && processo.getRito() != null ? processo.getRito().name() : RitoProcessual.COMUM_ORDINARIO.name();
        String ritoFamily = resolveRitoFamily(processo != null ? processo.getRito() : null, processo != null ? processo.getFaseAtual() : null);
        String fase = processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "NAO_CLASSIFICADA";
        String instanciaLeitura = resolveInstanciaLeitura(processo, tribunalTier);
        String orgaoLeitura = resolveOrgaoLeitura(processo, tribunalTier);
        String recursalTrack = resolveRecursalTrack(processo, processFlow);
        String embargoTrack = resolveEmbargoTrack(processFlow);
        String nativeActTrack = resolveNativeActTrack(processFlow, totalDocumentos, totalPaginas);
        String signatureTrack = resolveSignatureTrack(processFlow, totalDocumentos);
        boolean htmlInlinePreferred = processFlow != null && processFlow.totalEntries() > 0 && (totalPaginas == 0L || processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL);
        boolean pdfSignedPreferred = totalDocumentos > 0 && (!htmlInlinePreferred || signatureTrack.contains("PDF"));
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        markers.add(justiceTrack);
        markers.add(tribunalTier);
        markers.add(ritoFamily);
        markers.add(nativeActTrack);
        if (htmlInlinePreferred) markers.add("HTML_INLINE_PRIORITARIO");
        if (pdfSignedPreferred) markers.add("PDF_ASSINADO_DISPONIVEL");
        if (modeProfile.sigiloReforcado()) markers.add("SIGILO_REFORCADO");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", processo != null ? firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado()) : null);
        metadata.put("unidadeJudiciariaCodigo", processo != null ? processo.getUnidadeJudiciariaCodigo() : null);
        metadata.put("competenciaTerritorialModo", processo != null ? processo.getCompetenciaTerritorialModo() : null);
        metadata.put("preventionMode", processo != null ? processo.getPreventionMode() : null);
        metadata.put("linkageMode", processo != null ? processo.getLinkageMode() : null);
        metadata.put("connectorSystem", processo != null ? processo.getConnectorSystem() : null);
        metadata.put("supportsAllBrazilianRites", true);
        metadata.put("supportsAllBrazilianRights", true);
        metadata.put("supportsAllProceduralGuarantees", true);
        metadata.put("supportsTribunalVariations", true);
        metadata.put("supportsResourceAndEmbargoReading", true);
        metadata.put("supportsNativeHtmlActs", true);
        metadata.put("supportsHtmlToPdfAInspection", true);
        metadata.put("supportsInlineAndSignedHybridReading", htmlInlinePreferred && pdfSignedPreferred);
        metadata.put("totalNodes", navigation != null ? navigation.totalNodes() : 0);
        metadata.put("totalEntries", processFlow != null ? processFlow.totalEntries() : 0L);
        return new ProcessReadingProceduralContextResponse(
                justiceTrack,
                tribunalTier,
                ramo,
                materia,
                rito,
                ritoFamily,
                fase,
                instanciaLeitura,
                orgaoLeitura,
                recursalTrack,
                embargoTrack,
                nativeActTrack,
                signatureTrack,
                htmlInlinePreferred,
                pdfSignedPreferred,
                List.copyOf(markers),
                metadata
        );
    }

    private static String resolveJusticeTrack(Processo processo) {
        TipoJustica tipo = processo != null ? processo.getTipoJustica() : null;
        if (tipo == null) return "JUSTICA_NAO_CLASSIFICADA";
        return switch (tipo) {
            case ESTADUAL -> "JUSTICA_ESTADUAL";
            case FEDERAL -> "JUSTICA_FEDERAL";
            case ELEITORAL -> "JUSTICA_ELEITORAL";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "JUSTICA_MILITAR";
            case TRABALHO -> "JUSTICA_TRABALHO";
            case SUPERIOR -> "JUSTICA_SUPERIOR";
        };
    }

    private static String resolveTribunalTier(Processo processo) {
        String tribunal = processo != null ? firstNonBlankNormalized(processo.getTribunal(), processo.getTribunalCodigoRoteado()) : null;
        if (tribunal == null) return processo != null && processo.getUnidadeJudiciariaCodigo() != null ? "PRIMEIRO_GRAU" : "TRIBUNAL_NAO_CLASSIFICADO";
        if (tribunal.startsWith("STF") || tribunal.startsWith("STJ") || tribunal.startsWith("TST") || tribunal.startsWith("TSE") || tribunal.startsWith("STM")) return "TRIBUNAL_SUPERIOR";
        if (tribunal.startsWith("TJ") || tribunal.startsWith("TRF") || tribunal.startsWith("TRT") || tribunal.startsWith("TRE")) return "SEGUNDO_GRAU";
        return processo != null && processo.getUnidadeJudiciariaCodigo() != null ? "PRIMEIRO_GRAU" : "TRIBUNAL_REGIONAL_OU_LOCAL";
    }

    private static String resolveRitoFamily(RitoProcessual rito, FaseProcessual fase) {
        if (rito == null) return fase == FaseProcessual.RECURSAL ? "RECURSAL_E_JULGAMENTO" : "PROCEDIMENTO_COMUM";
        if (rito.isPenal()) return "PENAL_E_PROBATORIO";
        if (rito.isTrabalhista()) return "TRABALHISTA_E_EXECUCAO_SOCIAL";
        if (rito.isPrevidenciario()) return "PREVIDENCIARIO_E_SOCIAL";
        if (rito.isTribFazenda()) return "FAZENDA_PUBLICA_E_TRIBUTARIO";
        if (rito.isEleitoral()) return "ELEITORAL_E_CONTENCIOSO_ELEITORAL";
        if (rito.isMilitar()) return "MILITAR_E_DISCIPLINAR";
        if (rito.isEspecialConstitucional()) return "CONSTITUCIONAL_E_REMEDIOS";
        if (fase == FaseProcessual.EXECUCAO || fase == FaseProcessual.CUMPRIMENTO_SENTENCA) return "EXECUCAO_E_SATISFACAO";
        return "PROCEDIMENTO_COMUM";
    }

    private static String resolveInstanciaLeitura(Processo processo, String tribunalTier) {
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL && "TRIBUNAL_SUPERIOR".equals(tribunalTier)) return "RECURSAL_SUPERIOR";
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) return "RECURSAL_ORDINARIA";
        if ("SEGUNDO_GRAU".equals(tribunalTier)) return "JULGAMENTO_COLEGIADO";
        return "PRIMEIRO_GRAU_E_UNIDADE_JUDICIARIA";
    }

    private static String resolveOrgaoLeitura(Processo processo, String tribunalTier) {
        if (processo != null && processo.getUnidadeJudiciariaCodigo() != null && !processo.getUnidadeJudiciariaCodigo().isBlank()) return processo.getUnidadeJudiciariaCodigo().trim();
        return switch (tribunalTier) {
            case "TRIBUNAL_SUPERIOR" -> "GABINETE_OU_ORGAO_SUPERIOR";
            case "SEGUNDO_GRAU" -> "CAMARA_TURMA_SECAO_OU_PLENARIO";
            default -> "VARA_JUIZADO_AUDITORIA_OU_ZONA";
        };
    }

    private static String resolveRecursalTrack(Processo processo, ProcessReadingFlowResponse flow) {
        boolean hasRecursalEntries = flow != null && flow.entries().stream().anyMatch(entry -> {
            String origin = normalize(entry.originMode());
            return origin != null && (origin.contains("RECUR") || origin.contains("APELA") || origin.contains("AGRAV") || origin.contains("EMBARG") || origin.contains("ACORDAO"));
        });
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) return hasRecursalEntries ? "DECISAO_RECURSO_CONTRARRAZOES_E_JULGAMENTO" : "RECURSAL_SEM_MAPA_COMPLETO";
        return hasRecursalEntries ? "RECURSAL_INCIDENTAL_MATERIALIZADA" : "TRILHA_LINEAR";
    }

    private static String resolveEmbargoTrack(ProcessReadingFlowResponse flow) {
        boolean embargos = flow != null && flow.entries().stream().anyMatch(entry -> {
            String origin = normalize(entry.originMode());
            return origin != null && origin.contains("EMBARG");
        });
        return embargos ? "EMBARGOS_E_INTEGRACAO_DECISORIA" : "SEM_MALHA_DE_EMBARGOS";
    }

    private static String resolveNativeActTrack(ProcessReadingFlowResponse flow, long totalDocumentos, long totalPaginas) {
        if (flow != null && flow.totalInlineActs() > 0 && totalDocumentos > 0) return "HTML_NATIVO_E_PDF_ASSINADO_HIBRIDOS";
        if (flow != null && flow.totalInlineActs() > 0) return "HTML_NATIVO_PRIORITARIO";
        if (totalPaginas > 0) return "PDF_PRIORITARIO";
        return "SUPERFICIE_NAO_CLASSIFICADA";
    }

    private static String resolveSignatureTrack(ProcessReadingFlowResponse flow, long totalDocumentos) {
        boolean decision = flow != null && flow.entries().stream().anyMatch(entry -> {
            String origin = normalize(entry.originMode());
            return origin != null && (origin.contains("DESPACHO") || origin.contains("DECISAO") || origin.contains("SENTENCA") || origin.contains("ACORDAO"));
        });
        if (decision && totalDocumentos > 0) return "ATO_HTML_COM_CONFERENCIA_PDF_ASSINADO";
        if (decision) return "ATO_HTML_NATIVO_ASSINAVEL";
        return totalDocumentos > 0 ? "PDF_ASSINADO_COM_EXPORTACAO_CONTROLADA" : "ASSINATURA_NAO_PRIORITARIA";
    }


    private static String firstNonBlankNormalized(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
