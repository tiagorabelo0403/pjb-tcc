package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProcessEntryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSurfaceResponse;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessReadingSurfaceResolverTest {

    private final ProcessReadingSurfaceResolver resolver = new ProcessReadingSurfaceResolver();

    @Test
    void resolveDocumentFlagsHybridOcrWhenCoverageIsPartial() {
        DocumentoProcessual documento = new DocumentoProcessual();
        documento.setId(UUID.randomUUID());
        documento.setTitulo("Petição inicial digitalizada");
        documento.setContentType("application/pdf");

        DocumentoPagina page1 = new DocumentoPagina();
        page1.setPageNumber(1);
        page1.setTextoExtraido("Texto OCR disponível na primeira página.");
        DocumentoPagina page2 = new DocumentoPagina();
        page2.setPageNumber(2);
        page2.setTextoExtraido(null);

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "LEITURA_EQUILIBRADA", "AMBAR_JURIDICO", "AMBAR_PROGRESSIVO", "CONTRASTE_EQUILIBRADO", "108", "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_DOCUMENTO", "ROLAGEM_ASSISTIDA", "PROVA_GERAL", "TRILHA_LINEAR", "PECA_E_MOVIMENTACAO",
                "MARCADOR_SEMANTICO", "LEITURA_CONTINUA_SUAVE", "SINOPSE_DIRETA", 1, 2, 50, false, false, false, List.of());
        ProcessReadingPresetProfile presetProfile = new ProcessReadingPresetProfile(
                true, "MEDIUM", "LEITURA_JURIDICA_ADAPTATIVA", "AMBAR_JURIDICO", 108, 1.7, 0.8, 0.002, 72, 6,
                "FOCO_DISCRETO_POR_PECA", "SEM_MASCARA", "ATALHOS_E_FOCO_FIXO", "LINHA_DO_TEMPO_PROCESSUAL",
                "MAPA_ARTIGOS_PRECEDENTES_E_TEMAS", "MARCADORES_DE_LEITURA", "BUSCA_SEMANTICA_POR_PECA_E_PAGINA", "ANCORAS_PROCESSUAIS_FIXAS");

        ProcessReadingSurfaceResponse surface = resolver.resolveDocument(documento, List.of(page1, page2), modeProfile, presetProfile);

        assertEquals("PDF_HIBRIDO_OCR_PROGRESSIVO", surface.displayMode());
        assertEquals("OCR_PARTIAL", surface.ocrStatus());
        assertTrue(surface.markers().contains("OCR_PENDENTE"));
    }

    @Test
    void resolveNativeEntryKeepsDirectCopyMode() {
        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "TRIAGEM_OPERACIONAL_ASSISTIDA", "MARFIM_SUAVE", "SUAVIZACAO_NEUTRA", "CONTRASTE_EQUILIBRADO", "100", "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_DOCUMENTO", "ROLAGEM_ASSISTIDA", "PROVA_GERAL", "TRILHA_LINEAR", "PENDENCIA_PRAZO_E_MOVIMENTACAO",
                "MARCADOR_SEMANTICO", "LEITURA_CONTINUA_SUAVE", "SINOPSE_DIRETA", 0, 0, 0, false, false, false, List.of());
        ProcessReadingPresetProfile presetProfile = new ProcessReadingPresetProfile(
                true, "SOFT", "LEITURA_SERVIDOR_MALHA_OPERACIONAL", "MARFIM_SUAVE", 100, 1.6, 0.7, 0.002, 78, 5,
                "FOCO_DISCRETO_POR_PECA", "SEM_MASCARA", "ATALHOS_E_FOCO_FIXO", "LINHA_DO_TEMPO_PROCESSUAL",
                "MAPA_ARTIGOS_PRECEDENTES_E_TEMAS", "PRAZOS_PENDENCIAS_E_IMPULSO", "BUSCA_SEMANTICA_POR_PECA_E_PAGINA", "ANCORAS_PROCESSUAIS_FIXAS");
        ProcessReadingProcessEntryResponse entry = new ProcessReadingProcessEntryResponse(
                "MOV-77", "MOVIMENTACAO_PROCESSUAL", "MOVIMENTACAO_INLINE", "Juntada", "Houve juntada de manifestação.", "Servidor",
                "2026-03-18T12:00:00Z", "ATOS", "medium", false, "/api/v1/processos/1/painel-leitura/fluxo#MOV-77", null,
                List.of("MOVIMENTACAO"), Map.of());

        ProcessReadingSurfaceResponse surface = resolver.resolveNativeEntry(1L, entry, modeProfile, presetProfile);

        assertEquals("COPIA_TEXTO_DIRETA", surface.selectionMode());
        assertEquals("ATO_PROCESSUAL_NATIVO_COM_EXPORTACAO_CONTROLADA", surface.preservationMode());
    }
}
