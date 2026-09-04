package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.model.dto.upload.UploadBatchFinalizeResponse;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.upload.BulkUploadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Finalização de lote de upload e vínculo dos documentos ao processo (juntada governada).
 * Extraído de {@link OfficeGovernedProcessOperationService} porque
 * {@code documentoProcessualRepository}, {@code bulkUploadService} e
 * {@code officeDocumentBatchGovernanceService} são usados exclusivamente por essa operação.
 */
@Service
public class OfficeGovernedDocumentBatchLinkService {

    private final OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService;
    private final BulkUploadService bulkUploadService;
    private final DocumentoProcessualRepository documentoProcessualRepository;

    public OfficeGovernedDocumentBatchLinkService(OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService,
                                                   BulkUploadService bulkUploadService,
                                                   DocumentoProcessualRepository documentoProcessualRepository) {
        this.officeDocumentBatchGovernanceService = Objects.requireNonNull(officeDocumentBatchGovernanceService);
        this.bulkUploadService = Objects.requireNonNull(bulkUploadService);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
    }

    public Map<String, Object> execute(Long processoId,
                                       UUID batchId,
                                       String titulo,
                                       DocumentoCategoria categoria,
                                       NivelSigilo nivelSigilo,
                                       String origemSistema,
                                       String expectedBatchFingerprint,
                                       Integer expectedUploadedCount) {
        OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot snapshot = officeDocumentBatchGovernanceService.snapshot(batchId);
        if (!Objects.equals(snapshot.processoId(), processoId)) {
            throw new IllegalStateException("Lote de upload vinculado a outro processo.");
        }
        if (!"INITIATED".equals(snapshot.status())) {
            throw new IllegalStateException("Lote de upload nao esta apto para juntada governada.");
        }
        if (snapshot.uploadedCount() <= 0) {
            throw new IllegalStateException("Lote de upload sem documentos enviados.");
        }
        if (expectedUploadedCount != null && snapshot.uploadedCount() != expectedUploadedCount.longValue()) {
            throw new IllegalStateException("Lote de upload alterado desde o preview do frontend.");
        }
        if (expectedBatchFingerprint != null && !expectedBatchFingerprint.isBlank() && !Objects.equals(snapshot.fingerprint(), expectedBatchFingerprint)) {
            throw new IllegalStateException("Lote de upload alterado desde o preview do frontend.");
        }
        UploadBatchFinalizeResponse response = bulkUploadService.finalizeBatch(batchId);
        List<DocumentoProcessual> docs = documentoProcessualRepository.findAllById(response.documentoIds());
        DocumentoCategoria resolvedCategoria = categoria == null ? DocumentoCategoria.PUBLICO : categoria;
        NivelSigilo resolvedNivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
        String resolvedOrigem = origemSistema == null || origemSistema.isBlank() ? "FRONTEND_OFFICE_WORKSPACE" : origemSistema.trim();
        for (DocumentoProcessual doc : docs) {
            if (titulo != null && !titulo.isBlank()) {
                String normalizedTitle = titulo.trim();
                doc.setTitulo(docs.size() == 1 ? normalizedTitle : normalizedTitle + " — " + (doc.getNomeOriginal() == null ? doc.getId() : doc.getNomeOriginal()));
            }
            doc.setCategoria(resolvedCategoria);
            doc.setNivelSigilo(resolvedNivelSigilo);
            doc.setOrigemSistema(resolvedOrigem);
        }
        documentoProcessualRepository.saveAll(docs);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "DOCUMENTOS_JUNTADOS");
        out.put("processoId", processoId);
        out.put("batchId", batchId.toString());
        out.put("linkedCount", response.documentosCriados());
        out.put("linkedDocumentIds", response.documentoIds());
        out.put("batchFingerprint", snapshot.fingerprint());
        return out;
    }
}
