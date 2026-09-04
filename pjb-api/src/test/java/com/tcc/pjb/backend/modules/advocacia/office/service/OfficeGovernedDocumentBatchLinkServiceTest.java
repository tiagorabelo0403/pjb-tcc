package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.upload.UploadBatchFinalizeResponse;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.upload.BulkUploadService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficeGovernedDocumentBatchLinkServiceTest {

    private final OfficeDocumentBatchGovernanceService governanceService = mock(OfficeDocumentBatchGovernanceService.class);
    private final BulkUploadService bulkUploadService = mock(BulkUploadService.class);
    private final DocumentoProcessualRepository documentoProcessualRepository = mock(DocumentoProcessualRepository.class);
    private final OfficeGovernedDocumentBatchLinkService service = new OfficeGovernedDocumentBatchLinkService(
            governanceService, bulkUploadService, documentoProcessualRepository);

    private OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot snapshot(UUID batchId, Long processoId, String status, long uploadedCount, String fingerprint) {
        return new OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot(
                batchId, processoId, 1L, status, null, 1, uploadedCount, 0, 0, 0, 100L, fingerprint);
    }

    @Test
    void execute_lotePronto_finalizaEVinculaDocumentos() {
        UUID batchId = UUID.randomUUID();
        Long processoId = 5L;
        when(governanceService.snapshot(batchId)).thenReturn(snapshot(batchId, processoId, "INITIATED", 1, "fp1"));
        UUID docId = UUID.randomUUID();
        when(bulkUploadService.finalizeBatch(batchId)).thenReturn(new UploadBatchFinalizeResponse(batchId, 1, List.of(docId)));
        DocumentoProcessual doc = new DocumentoProcessual();
        when(documentoProcessualRepository.findAllById(List.of(docId))).thenReturn(List.of(doc));

        Map<String, Object> result = service.execute(processoId, batchId, "Título", DocumentoCategoria.PESSOAL, NivelSigilo.SEGREDO_JUSTICA, null, "fp1", 1);

        assertThat(result.get("status")).isEqualTo("DOCUMENTOS_JUNTADOS");
        assertThat(doc.getCategoria()).isEqualTo(DocumentoCategoria.PESSOAL);
        assertThat(doc.getNivelSigilo()).isEqualTo(NivelSigilo.SEGREDO_JUSTICA);
        assertThat(doc.getOrigemSistema()).isEqualTo("FRONTEND_OFFICE_WORKSPACE");
        assertThat(doc.getTitulo()).isEqualTo("Título");
        verify(documentoProcessualRepository).saveAll(List.of(doc));
    }

    @Test
    void execute_processoDivergente_lancaExcecao() {
        UUID batchId = UUID.randomUUID();
        when(governanceService.snapshot(batchId)).thenReturn(snapshot(batchId, 999L, "INITIATED", 1, "fp1"));

        assertThatThrownBy(() -> service.execute(5L, batchId, null, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outro processo");
    }

    @Test
    void execute_loteNaoInicializado_lancaExcecao() {
        UUID batchId = UUID.randomUUID();
        when(governanceService.snapshot(batchId)).thenReturn(snapshot(batchId, 5L, "FINALIZED", 1, "fp1"));

        assertThatThrownBy(() -> service.execute(5L, batchId, null, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apto");
    }

    @Test
    void execute_loteSemDocumentos_lancaExcecao() {
        UUID batchId = UUID.randomUUID();
        when(governanceService.snapshot(batchId)).thenReturn(snapshot(batchId, 5L, "INITIATED", 0, "fp1"));

        assertThatThrownBy(() -> service.execute(5L, batchId, null, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem documentos");
    }

    @Test
    void execute_fingerprintDivergente_lancaExcecao() {
        UUID batchId = UUID.randomUUID();
        when(governanceService.snapshot(batchId)).thenReturn(snapshot(batchId, 5L, "INITIATED", 1, "fp-real"));

        assertThatThrownBy(() -> service.execute(5L, batchId, null, null, null, null, "fp-esperado", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alterado");
    }
}
