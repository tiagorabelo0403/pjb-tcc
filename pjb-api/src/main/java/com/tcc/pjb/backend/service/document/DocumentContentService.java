package com.tcc.pjb.backend.service.document;

import java.io.IOException;
import java.util.Objects;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;

@Service
public class DocumentContentService {

    private final ObjectStoragePort objectStorage;

    public DocumentContentService(ObjectStoragePort objectStorage) {
        this.objectStorage = objectStorage;
    }

    public ResolvedDocumentContent resolvePdf(DocumentoProcessual doc) {
        Objects.requireNonNull(doc, "doc");

        byte[] inline = doc.getPdf();
        if (inline != null && inline.length > 0) {
            return new ResolvedDocumentContent(new ByteArrayResource(inline), inline.length, "application/pdf", true);
        }

        String backend = doc.getStorageBackend();
        String key = doc.getStorageUri();
        if (backend == null || backend.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException("conteúdo ausente para documento: " + doc.getId());
        }

        if (!"LOCALFS".equalsIgnoreCase(backend)) {
            throw new IllegalStateException("storage_backend não suportado no build atual: " + backend);
        }

        try {
            ObjectReadResult r = objectStorage.get(key);
            return new ResolvedDocumentContent(r.resource(), r.contentLength(), r.contentType(), false);
        } catch (IOException e) {
            throw new IllegalStateException("falha ao ler conteúdo externo do documento: " + doc.getId(), e);
        }
    }

    public record ResolvedDocumentContent(Resource resource, long contentLength, String contentType, boolean inlineDb) {
    }
}
