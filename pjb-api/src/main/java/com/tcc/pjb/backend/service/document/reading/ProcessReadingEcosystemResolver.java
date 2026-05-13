package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingEcosystemResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingEcosystemResolver {

    public ProcessReadingEcosystemResponse resolve(Processo processo,
                                                   ProcessReadingModeProfile modeProfile,
                                                   ProcessReadingProceduralContextResponse proceduralContext,
                                                   ProcessReadingSpecializationResponse specialization) {
        NationalCompetenceMatrix competence = resolveCompetence(processo).orElse(null);
        JudicialSystem primarySystem = competence != null ? competence.sistemaJudicialPrimario() : JudicialSystem.PJE;
        JudicialSystem fallbackSystem = competence != null ? competence.sistemaJudicialFallback() : JudicialSystem.PDPJ;
        String convergenceMode = resolveConvergenceMode(primarySystem, fallbackSystem, proceduralContext, specialization);
        String legacyMigrationMode = resolveLegacyMigrationMode(primarySystem, fallbackSystem, competence, proceduralContext);
        String browserAccessMode = resolveBrowserAccessMode(primarySystem, fallbackSystem, proceduralContext);
        String signatureMode = resolveSignatureMode(primarySystem, fallbackSystem, specialization, proceduralContext);
        String mfaMode = resolveMfaMode(primarySystem, fallbackSystem, proceduralContext);
        String documentPipelineMode = resolveDocumentPipelineMode(modeProfile, proceduralContext, specialization);
        String ocrMode = resolveOcrMode(modeProfile, specialization);
        String aiAssistMode = resolveAiAssistMode(modeProfile, proceduralContext, specialization);
        String deadlineAggregationMode = resolveDeadlineAggregationMode(primarySystem, fallbackSystem, proceduralContext, specialization);
        List<String> strategicCapabilities = resolveStrategicCapabilities(primarySystem, fallbackSystem, proceduralContext, specialization, modeProfile);
        List<String> migrationTracks = resolveMigrationTracks(primarySystem, fallbackSystem, competence, proceduralContext, specialization);
        List<String> productionDifferentials = resolveProductionDifferentials(primarySystem, fallbackSystem, modeProfile, proceduralContext, specialization);
        LinkedHashMap<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("readerMode", "PROCESSO_CENTRICO_CONVERGENTE");
        frontend.put("defaultSurface", proceduralContext.htmlInlinePreferred() ? "HTML_NATIVO" : "PDF_ASSINADO");
        frontend.put("preferInlineHtml", proceduralContext.htmlInlinePreferred());
        frontend.put("preferSignedPdfInspection", specialization.signedPdfInspectionRequired());
        frontend.put("supportsCloudSigning", true);
        frontend.put("supportsMobileMfa", true);
        frontend.put("supportsOcrPipeline", true);
        frontend.put("supportsNationalDeadlineAggregation", true);
        frontend.put("supportsAiCopilot", true);
        frontend.put("supportsLegacyMigrationMesh", true);
        frontend.put("supportsBrowserNativeAccess", true);
        frontend.put("capabilityBadges", List.of(convergenceMode, signatureMode, aiAssistMode, deadlineAggregationMode));
        LinkedHashMap<String, Object> integrity = new LinkedHashMap<>();
        integrity.put("competenceResolved", competence != null);
        integrity.put("primarySystem", primarySystem.name());
        integrity.put("fallbackSystem", fallbackSystem.name());
        integrity.put("textCoverage", modeProfile.coberturaTextualPercentual());
        integrity.put("htmlInlinePreferred", proceduralContext.htmlInlinePreferred());
        integrity.put("signedPdfInspectionRequired", specialization.signedPdfInspectionRequired());
        integrity.put("recursalTrack", proceduralContext.recursalTrack());
        integrity.put("embargoTrack", proceduralContext.embargoTrack());
        return new ProcessReadingEcosystemResponse(
                processo.getId(),
                competence != null ? competence.codigo() : processo.getTribunal(),
                competence != null ? competence.nome() : normalizeLabel(processo.getTribunal()),
                primarySystem.name(),
                fallbackSystem.name(),
                convergenceMode,
                legacyMigrationMode,
                browserAccessMode,
                signatureMode,
                mfaMode,
                documentPipelineMode,
                ocrMode,
                aiAssistMode,
                deadlineAggregationMode,
                strategicCapabilities,
                migrationTracks,
                productionDifferentials,
                frontend,
                integrity
        );
    }

    private Optional<NationalCompetenceMatrix> resolveCompetence(Processo processo) {
        Optional<NationalCompetenceMatrix> byCode = NationalCompetenceMatrix.porCodigo(processo.getTribunal());
        if (byCode.isPresent()) {
            return byCode;
        }
        if (processo.getUf() == null || processo.getUf().isBlank()) {
            return Optional.empty();
        }
        return NationalCompetenceMatrix.porUF(processo.getUf()).stream().findFirst();
    }

    private String resolveConvergenceMode(JudicialSystem primarySystem,
                                          JudicialSystem fallbackSystem,
                                          ProcessReadingProceduralContextResponse proceduralContext,
                                          ProcessReadingSpecializationResponse specialization) {
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PDPJ) {
            if (specialization.nativeHtmlPriority() && proceduralContext.htmlInlinePreferred()) {
                return "PDPJ_CONVERGENCIA_COM_HTML_NATIVO_PRIORITARIO";
            }
            return "PDPJ_CONVERGENCIA_UNIFICADA_COM_PAINEL_UNICO";
        }
        if (primarySystem == JudicialSystem.EPROC) {
            return "EPROC_UNIFICADO_COM_MALHA_NACIONAL_DE_INTEROPERABILIDADE";
        }
        if (primarySystem == JudicialSystem.ESAJ) {
            return "LEGADO_PRIVADO_EM_TRANSICAO_CONTROLADA";
        }
        return "PLATAFORMA_JUDICIAL_CONVERGENTE_PROCESSO_CENTRICO";
    }

    private String resolveLegacyMigrationMode(JudicialSystem primarySystem,
                                              JudicialSystem fallbackSystem,
                                              NationalCompetenceMatrix competence,
                                              ProcessReadingProceduralContextResponse proceduralContext) {
        if (primarySystem == JudicialSystem.ESAJ) {
            return "MIGRACAO_ACERVO_ESAJ_PARA_EPROC_OU_PDPJ";
        }
        if (primarySystem == JudicialSystem.PROJUDI) {
            return "MIGRACAO_PROJUDI_PARA_PAINEL_NACIONAL_CONVERGENTE";
        }
        if (primarySystem == JudicialSystem.PJE && fallbackSystem == JudicialSystem.PDPJ) {
            return proceduralContext.htmlInlinePreferred()
                    ? "PJE_1X_2X_COM_ATO_HTML_NATIVO_E_PAINEL_PDPJ"
                    : "PJE_1X_2X_COM_CONVERGENCIA_PROGRESSIVA_PDPJ";
        }
        if (primarySystem == JudicialSystem.EPROC && fallbackSystem == JudicialSystem.PDPJ) {
            return "EPROC_COM_INTEROPERABILIDADE_TOTAL_PDPJ_MNI";
        }
        if (competence != null && competence.isMilitar()) {
            return "TRILHA_MILITAR_COM_INTEROPERABILIDADE_MNI";
        }
        return "MALHA_NACIONAL_SEM_DEPENDENCIA_DE_CLIENTE_LEGADO";
    }

    private String resolveBrowserAccessMode(JudicialSystem primarySystem,
                                            JudicialSystem fallbackSystem,
                                            ProcessReadingProceduralContextResponse proceduralContext) {
        if (proceduralContext.htmlInlinePreferred()) {
            return "ACESSO_BROWSER_NATIVO_COM_ATO_HTML_ASSISTIDO";
        }
        if (primarySystem == JudicialSystem.EPROC || fallbackSystem == JudicialSystem.PDPJ) {
            return "ACESSO_BROWSER_NATIVO_SEM_PLUGIN_PESADO";
        }
        return "ACESSO_HIBRIDO_BROWSER_E_CONFERENCIA_ASSINADA";
    }

    private String resolveSignatureMode(JudicialSystem primarySystem,
                                        JudicialSystem fallbackSystem,
                                        ProcessReadingSpecializationResponse specialization,
                                        ProcessReadingProceduralContextResponse proceduralContext) {
        if (specialization.signedPdfInspectionRequired() && proceduralContext.htmlInlinePreferred()) {
            return "ASSINATURA_NUVEM_MFA_COM_HTML_PRIMARIO_E_PDF_FORMAL";
        }
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PJE) {
            return "ASSINATURA_NUVEM_MFA_E_OFFICE_SILENCIOSO";
        }
        return "ASSINATURA_HIBRIDA_COM_TOKEN_NUVEM_E_CONTINGENCIA_LOCAL";
    }

    private String resolveMfaMode(JudicialSystem primarySystem,
                                  JudicialSystem fallbackSystem,
                                  ProcessReadingProceduralContextResponse proceduralContext) {
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PDPJ) {
            return "MFA_MOVEL_OBRIGATORIO_PARA_USUARIO_EXTERNO";
        }
        if (proceduralContext.justiceTrack() != null && proceduralContext.justiceTrack().contains("SUPERIOR")) {
            return "MFA_REFORCADO_COM_PASSKEY_E_STEP_UP";
        }
        return "MFA_ADAPTATIVO_POR_SENSIBILIDADE_PROCESSUAL";
    }

    private String resolveDocumentPipelineMode(ProcessReadingModeProfile modeProfile,
                                               ProcessReadingProceduralContextResponse proceduralContext,
                                               ProcessReadingSpecializationResponse specialization) {
        if (proceduralContext.htmlInlinePreferred() && specialization.signedPdfInspectionRequired()) {
            return "HTML_NATIVO_COM_CONVERSAO_PDF_A_E_ASSINATURA_FORMAL";
        }
        if (modeProfile.coberturaTextualPercentual() < 65) {
            return "PDF_A_COM_OCR_PROGRESSIVO_E_SUPERFICIE_HIBRIDA";
        }
        return "HTML_NATIVO_E_PDF_A_COM_CAMADA_TEXTUAL_ESTRUTURADA";
    }

    private String resolveOcrMode(ProcessReadingModeProfile modeProfile,
                                  ProcessReadingSpecializationResponse specialization) {
        if (modeProfile.coberturaTextualPercentual() < 35) {
            return "OCR_ALTA_PERFORMANCE_PRIORITARIO_COM_RECONSTRUCAO_SEMANTICA";
        }
        if (modeProfile.coberturaTextualPercentual() < 70) {
            return "OCR_PROGRESSIVO_COM_ENRIQUECIMENTO_POR_PECA";
        }
        return specialization.nativeHtmlPriority()
                ? "OCR_ASSISTIVO_SOMENTE_PARA_PECAS_DIGITALIZADAS"
                : "OCR_DE_CONTINGENCIA_COM_INDEXACAO_INCREMENTAL";
    }

    private String resolveAiAssistMode(ProcessReadingModeProfile modeProfile,
                                       ProcessReadingProceduralContextResponse proceduralContext,
                                       ProcessReadingSpecializationResponse specialization) {
        if (specialization.resourceMode().contains("RECURSAL") || proceduralContext.recursalTrack().contains("RECURSAL")) {
            return "COPILOTO_RECURSAL_COM_RESUMO_HTML_E_MAPA_DE_ENFRENTAMENTO";
        }
        if (specialization.evidenceMode().contains("PROBATORIA") || modeProfile.evidenceMode().contains("PROVA")) {
            return "COPILOTO_PROBATORIO_COM_CORRELACAO_DE_PROVAS_E_EVENTOS";
        }
        return "COPILOTO_NACIONAL_DE_ATOS_HTML_COM_SINOPSE_E_PRIORIZACAO";
    }

    private String resolveDeadlineAggregationMode(JudicialSystem primarySystem,
                                                  JudicialSystem fallbackSystem,
                                                  ProcessReadingProceduralContextResponse proceduralContext,
                                                  ProcessReadingSpecializationResponse specialization) {
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PDPJ) {
            return "PAINEL_UNIFICADO_DE_PRAZOS_PDPJ_MNI";
        }
        if (specialization.executionMode().contains("EXECUCAO")) {
            return "PAINEL_DE_PRAZOS_EXECUTIVOS_E_INCIDENTES_CONVERGENTE";
        }
        return proceduralContext.embargoTrack().contains("EMBARGOS")
                ? "PAINEL_RECURSAL_COM_PRAZOS_DE_RECURSOS_E_EMBARGOS"
                : "PAINEL_NACIONAL_DE_PRAZOS_E_AFASTAMENTO_DE_PLANILHAS_EXTERNAS";
    }

    private List<String> resolveStrategicCapabilities(JudicialSystem primarySystem,
                                                      JudicialSystem fallbackSystem,
                                                      ProcessReadingProceduralContextResponse proceduralContext,
                                                      ProcessReadingSpecializationResponse specialization,
                                                      ProcessReadingModeProfile modeProfile) {
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        capabilities.add("PAINEL_UNICO_POR_PROCESSO_COM_SUPERFICIE_HTML_E_PDF_A");
        capabilities.add("ASSINATURA_EM_NUVEM_COM_STEP_UP_E_MFA_MOVEL");
        capabilities.add("OCR_PROGRESSIVO_COM_INDEXACAO_DE_PECAS_ESCANEADAS");
        capabilities.add("COPILOTO_JURIDICO_NATIVO_PARA_ATOS_HTML");
        capabilities.add("AGREGACAO_DE_PRAZOS_E_TAREFAS_EM_MALHA_NACIONAL");
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PDPJ) {
            capabilities.add("PAINEL_CONVERGENTE_COM_PDPJ_E_INTEROPERABILIDADE_NACIONAL");
        }
        if (specialization.nativeHtmlPriority()) {
            capabilities.add("LEITURA_PRIORITARIA_DE_ATOS_TEXTUAIS_NATIVOS");
        }
        if (specialization.signedPdfInspectionRequired()) {
            capabilities.add("CONFERENCIA_FORMAL_DE_ASSINATURA_SEM_QUEBRAR_A_LEITURA_INLINE");
        }
        if (proceduralContext.embargoTrack().contains("EMBARGOS")) {
            capabilities.add("TRILHA_ESPECIALIZADA_DE_RECURSOS_E_EMBARGOS");
        }
        if (modeProfile.volumeExtenso()) {
            capabilities.add("RESUMO_PROGRESSIVO_PARA_AUTOS_VOLUMOSOS");
        }
        return List.copyOf(capabilities);
    }

    private List<String> resolveMigrationTracks(JudicialSystem primarySystem,
                                                JudicialSystem fallbackSystem,
                                                NationalCompetenceMatrix competence,
                                                ProcessReadingProceduralContextResponse proceduralContext,
                                                ProcessReadingSpecializationResponse specialization) {
        LinkedHashSet<String> tracks = new LinkedHashSet<>();
        if (primarySystem == JudicialSystem.ESAJ) {
            tracks.add("EXTRACAO_DE_ACERVO_ESAJ_COM_REMAPEAMENTO_DE_CLASSES_E_MOVIMENTACOES");
            tracks.add("NORMALIZACAO_DE_METADADOS_PARA_PDPJ_E_EPROC");
        }
        if (primarySystem == JudicialSystem.PJE) {
            tracks.add("PROMOCAO_DE_ATO_HTML_NATIVO_PARA_SUPERFICIE_PRIMARIA");
            tracks.add("CONVERSAO_CONTROLADA_HTML_PARA_PDF_A_ASSINADO");
        }
        if (primarySystem == JudicialSystem.EPROC) {
            tracks.add("INTEROPERABILIDADE_TOTAL_COM_MICROSSERVICOS_NACIONAIS");
        }
        if (fallbackSystem == JudicialSystem.PDPJ) {
            tracks.add("ORQUESTRACAO_DE_PAINEL_UNIFICADO_E_LOGIN_CONVERGENTE");
        }
        if (competence != null && competence.sistemaJudicialFallback() == JudicialSystem.MNI) {
            tracks.add("CANAL_MNI_PARA_TRAMITACAO_E_PRAZOS_AGREGADOS");
        }
        if (proceduralContext.recursalTrack().contains("RECURSAL") || specialization.resourceMode().contains("RECURSAL")) {
            tracks.add("MIGRACAO_DE_MALHA_RECURSAL_COM_DECISAO_RAZOES_CONTRARRAZOES");
        }
        if (proceduralContext.embargoTrack().contains("EMBARGOS")) {
            tracks.add("MIGRACAO_DE_EMBARGOS_E_PECAS_INTEGRATIVAS_COM_ORDEM_LOGICA");
        }
        return List.copyOf(tracks);
    }

    private List<String> resolveProductionDifferentials(JudicialSystem primarySystem,
                                                        JudicialSystem fallbackSystem,
                                                        ProcessReadingModeProfile modeProfile,
                                                        ProcessReadingProceduralContextResponse proceduralContext,
                                                        ProcessReadingSpecializationResponse specialization) {
        ArrayList<String> out = new ArrayList<>();
        out.add("PROCESSO_CENTRICO_EM_VEZ_DE_VISUALIZADOR_CENTRICO");
        out.add("ATO_HTML_NATIVO_COM_PRIORIZACAO_DINAMICA_POR_RITO_E_ORGAO");
        out.add("ASSINATURA_NUVEM_COM_MFA_MOVEL_E_CONTINGENCIA_SEGURA");
        out.add("PIPELINE_PDF_A_COM_OCR_ESTRUTURADO_E_CAMADA_DE_COPIA_CONFIAVEL");
        out.add("COPILOTO_DE_LEITURA_EM_HTML_COM_SINOPSE_MAPA_DE_CITACOES_E_PRIORIDADES");
        out.add("PAINEL_NACIONAL_DE_PRAZOS_SEM_DEPENDENCIA_DE_PLANILHAS_EXTERNAS");
        if (fallbackSystem == JudicialSystem.PDPJ || primarySystem == JudicialSystem.PDPJ) {
            out.add("CONVERGENCIA_NACIONAL_COM_PAINEL_UNICO_E_INTEROPERABILIDADE_PDPJ");
        }
        if (modeProfile.coberturaTextualPercentual() < 65) {
            out.add("RECUPERACAO_ATIVA_DE_TEXTO_EM_AUTOS_DIGITALIZADOS");
        }
        if (proceduralContext.htmlInlinePreferred()) {
            out.add("LEITURA_HTML_ESTRUTURADA_MELHOR_QUE_PREVIEW_DE_TIMELINE_LEGADA");
        }
        if (specialization.executionMode().contains("EXECUCAO")) {
            out.add("TRILHA_EXECUTIVA_COM_CALCULOS_ATOS_E_SATISFACAO_INTEGRADOS_AO_LEITOR");
        }
        return List.copyOf(out);
    }

    private String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Tribunal não identificado";
        }
        return value.replace('_', ' ').replace('-', ' ').toUpperCase(Locale.ROOT);
    }
}
