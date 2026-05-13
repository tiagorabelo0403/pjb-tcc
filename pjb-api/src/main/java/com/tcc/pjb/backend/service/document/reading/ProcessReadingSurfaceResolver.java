package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProcessEntryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSurfaceResponse;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingSurfaceResolver {

    public ProcessReadingSurfaceResponse resolveDocument(DocumentoProcessual documento,
                                                        List<DocumentoPagina> paginas,
                                                        ProcessReadingModeProfile modeProfile,
                                                        ProcessReadingPresetProfile presetProfile) {
        long totalPaginas = paginas == null ? 0L : paginas.size();
        long paginasComTexto = paginas == null ? 0L : paginas.stream().filter(p -> !blank(p.getTextoExtraido())).count();
        return resolveDocument(documento, totalPaginas, paginasComTexto, modeProfile, presetProfile);
    }

    public ProcessReadingSurfaceResponse resolveDocument(DocumentoProcessual documento,
                                                        long totalPaginas,
                                                        long paginasComTexto,
                                                        ProcessReadingModeProfile modeProfile,
                                                        ProcessReadingPresetProfile presetProfile) {
        int cobertura = totalPaginas == 0L ? 0 : (int) Math.round((paginasComTexto * 100.0d) / totalPaginas);
        String contentType = documento != null && documento.getContentType() != null ? documento.getContentType().toLowerCase(Locale.ROOT) : "";
        boolean pdf = contentType.contains("pdf") || title(documento).toLowerCase(Locale.ROOT).endsWith(".pdf");
        String displayMode = resolveDisplayMode(totalPaginas, cobertura, pdf);
        String extractionMode = resolveExtractionMode(totalPaginas, cobertura, pdf);
        String selectionMode = cobertura >= 70 ? "COPIA_TEXTO_DIRETA" : cobertura >= 20 ? "COPIA_HIBRIDA_ASSISTIDA" : "COPIA_INDIRETA_COM_OCR";
        String ocrStatus = totalPaginas == 0L ? "OCR_NOT_APPLICABLE" : cobertura >= 85 ? "OCR_READY" : cobertura > 0 ? "OCR_PARTIAL" : "OCR_REQUIRED";
        String preservationMode = pdf ? "PDF_A_JUDICIAL_PRESERVADO" : "FORMATO_NATIVO_CONTROLCOPY";
        String timelineMode = modeProfile.totalPaginas() == 0L ? "FLOW_FIRST" : "DOCUMENT_FIRST";
        String originMode = pdf ? "DOCUMENTO_PDF_PROCESSUAL" : "DOCUMENTO_NATIVO_PROCESSUAL";
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        if (pdf) markers.add("PDF_JUDICIAL");
        if (pdf) markers.add("PRESERVACAO_LONGO_PRAZO");
        if (totalPaginas >= 40L) markers.add("PECA_EXTENSA");
        if (cobertura < 70 && totalPaginas > 0L) markers.add("OCR_PENDENTE");
        if (modeProfile.sigiloReforcado()) markers.add("SIGILO_REFORCADO");
        if (modeProfile.recursal()) markers.add("TRILHA_RECURSAL");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("totalPages", totalPaginas);
        metadata.put("textCoverage", cobertura);
        metadata.put("chunkPageSize", presetProfile.chunkPageSize());
        metadata.put("focusBandMode", presetProfile.focusBandMode());
        metadata.put("originMode", originMode);
        metadata.put("htmlInlineMode", true);
        metadata.put("nativeActsPreferred", modeProfile.totalPaginas() == 0L);
        metadata.put("viewerLayerMode", pdf ? "WEB_RENDER_TIMELINE" : "INLINE_STRUCTURED_READER");
        return new ProcessReadingSurfaceResponse(
                "DOCUMENTO_PROCESSUAL",
                pdf ? "PDF_DOCUMENT" : "BINARY_DOCUMENT",
                displayMode,
                extractionMode,
                selectionMode,
                ocrStatus,
                preservationMode,
                timelineMode,
                documento != null && documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/painel-leitura/conteudo" : null,
                documento != null && documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/pdf" : null,
                documento != null && documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/download" : null,
                List.copyOf(markers),
                metadata
        );
    }

    public ProcessReadingSurfaceResponse resolveNativeEntry(Long processoId,
                                                            ProcessReadingProcessEntryResponse entry,
                                                            ProcessReadingModeProfile modeProfile,
                                                            ProcessReadingPresetProfile presetProfile) {
        String sourceType = entry == null ? "PROCESS_INLINE_TEXT" : safe(entry.sourceType(), "PROCESS_INLINE_TEXT");
        String originMode = entry == null ? null : safe(entry.originMode(), null);
        String displayMode = containsAny(originMode, "DESPACHO", "DECISAO", "SENTENCA", "ACORDAO")
                ? "ATO_TEXTUAL_HTML_ASSINAVEL"
                : containsAny(originMode, "RECURSAL", "APELACAO", "EMBARGOS", "AGRAVO")
                ? "PECA_RECURSAL_HTML_ASSISTIDA"
                : switch (sourceType) {
                    case "MOVIMENTACAO_PROCESSUAL" -> "LINHA_DO_TEMPO_PROCESSUAL_ESTRUTURADA";
                    case "EVENTO_PROCESSUAL" -> "AGENDA_E_EVENTO_INLINE";
                    default -> "ATO_TEXTUAL_INLINE_ASSISTIDO";
                };
        String selectionMode = "COPIA_TEXTO_DIRETA";
        String extractionMode = originMode != null && originMode.contains("HTML") ? "NATIVE_HTML_TEXT_INLINE" : "NATIVE_TEXT_INLINE";
        String ocrStatus = "OCR_NOT_APPLICABLE";
        String preservationMode = originMode != null && originMode.contains("ASSIN") ? "HTML_NATIVO_ASSINAVEL_COM_EXPORTACAO_PDF_A" : "ATO_PROCESSUAL_NATIVO_COM_EXPORTACAO_CONTROLADA";
        String timelineMode = safe(presetProfile.chronologyMode(), "LINHA_DO_TEMPO_PROCESSUAL");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("entryId", entry != null ? entry.entryId() : null);
        metadata.put("lane", entry != null ? entry.lane() : null);
        metadata.put("severity", entry != null ? entry.severity() : null);
        metadata.put("anchorMode", presetProfile.anchorMode());
        metadata.put("focusBandMode", presetProfile.focusBandMode());
        metadata.put("originMode", originMode);
        metadata.put("htmlInlineMode", true);
        ArrayList<String> markers = new ArrayList<>();
        markers.add("ATO_NATIVO_INLINE");
        if (originMode != null && originMode.contains("HTML")) markers.add("HTML_EDITOR_NATIVO");
        if (modeProfile.sigiloReforcado()) markers.add("SIGILO_REFORCADO");
        if (entry != null && "high".equalsIgnoreCase(entry.severity())) markers.add("ATENCAO_ELEVADA");
        return new ProcessReadingSurfaceResponse(
                "FLUXO_PROCESSUAL",
                sourceType,
                displayMode,
                extractionMode,
                selectionMode,
                ocrStatus,
                preservationMode,
                timelineMode,
                processoId != null && entry != null ? "/api/v1/processos/" + processoId + "/painel-leitura/conteudo?entryId=" + entry.entryId() : null,
                entry != null ? entry.pdfEndpoint() : null,
                entry != null ? entry.pdfEndpoint() : null,
                List.copyOf(markers),
                metadata
        );
    }

    private static String resolveDisplayMode(long totalPaginas, int cobertura, boolean pdf) {
        if (!pdf) {
            return cobertura > 0 ? "DOCUMENTO_BINARIO_COM_TEXTO_EXTRAIDO" : "DOCUMENTO_BINARIO_CONTROLADO";
        }
        if (totalPaginas == 0L) {
            return "DOCUMENTO_PDF_SEM_PAGINACAO_INDEXADA";
        }
        if (cobertura >= 85) {
            return "PDF_TEXTUAL_ASSISTIDO";
        }
        if (cobertura >= 25) {
            return "PDF_HIBRIDO_OCR_PROGRESSIVO";
        }
        return "PDF_IMAGEM_COM_LEITURA_ASSISTIDA";
    }

    private static String resolveExtractionMode(long totalPaginas, int cobertura, boolean pdf) {
        if (totalPaginas == 0L) {
            return pdf ? "PDF_METADATA_ONLY" : "BINARY_METADATA_ONLY";
        }
        if (cobertura >= 85) {
            return "TEXT_LAYER_READY";
        }
        if (cobertura >= 25) {
            return "PARTIAL_OCR_LAYER";
        }
        return "IMAGE_ONLY_OR_LOW_TEXT_LAYER";
    }

    private static boolean containsAny(String text, String... tokens) {
        if (text == null) return false;
        String base = text.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token != null && base.contains(token.toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String title(DocumentoProcessual documento) {
        if (documento == null) {
            return "Documento";
        }
        if (!blank(documento.getTitulo())) {
            return documento.getTitulo().trim();
        }
        if (!blank(documento.getNomeOriginal())) {
            return documento.getNomeOriginal().trim();
        }
        return documento.getId() != null ? documento.getId().toString() : "Documento";
    }

    private static String safe(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
