package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.model.dto.intelligence.RecursalAttachmentUploadResponse;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.recursal.attachments.RecursalAttachmentProperties;
import com.tcc.pjb.backend.service.recursal.attachments.RecursalAttachmentStorageService;
import com.tcc.pjb.backend.service.recursal.attachments.RecursalStoredFileRef;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

@Service
public class RecursalAttachmentSurfaceFacadeService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalAttachmentStorageService storage;
    private final RecursalAttachmentProperties props;
    private final AuditoriaInteligenteService auditoria;

    public RecursalAttachmentSurfaceFacadeService(ProcessoRepository processoRepository,
                                                  PjbAuthorizationService authorizationService,
                                                  RecursalAttachmentStorageService storage,
                                                  RecursalAttachmentProperties props,
                                                  AuditoriaInteligenteService auditoria) {
        this.processoRepository = processoRepository;
        this.authorizationService = authorizationService;
        this.storage = storage;
        this.props = props;
        this.auditoria = auditoria;
    }

    public ResponseEntity<RecursalAttachmentUploadResponse> upload(Authentication authentication, Long processoId, MultipartFile file) {
        Processo proc = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        authorizationService.requireWriteProcesso(proc);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        long max = props.getMaxUploadBytes();
        if (max > 0 && file.getSize() > max) {
            return ResponseEntity.status(413)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RecursalAttachmentUploadResponse(processoId, null, null, file.getSize(), file.getContentType(), file.getOriginalFilename(), null));
        }
        try (InputStream in = file.getInputStream()) {
            RecursalStoredFileRef ref = storage.store(processoId, file.getOriginalFilename(), file.getContentType(), in);
            String name = Objects.toString(file.getOriginalFilename(), "");
            String filename = filenameFromKey(ref.storageKey());
            String downloadUrl = "/api/v1/intelligence/recursal/processo/" + processoId + "/attachments/" + filename;
            registrarAuditoriaUpload(authentication, processoId, ref);
            return ResponseEntity.ok(new RecursalAttachmentUploadResponse(
                    processoId,
                    ref.storageKey(),
                    ref.sha256(),
                    ref.sizeBytes(),
                    ref.contentType(),
                    name,
                    downloadUrl
            ));
        } catch (RecursalAttachmentStorageService.PayloadTooLargeException e) {
            return ResponseEntity.status(413)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RecursalAttachmentUploadResponse(processoId, null, null, file.getSize(), file.getContentType(), file.getOriginalFilename(), null));
        } catch (Exception e) {
            throw new IllegalStateException("Falha no upload", e);
        }
    }

    public ResponseEntity<InputStreamResource> download(Authentication authentication, Long processoId, String filename) {
        Processo proc = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        authorizationService.requireReadProcesso(proc);
        if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("filename inválido");
        }
        Path path = storage.resolveLocalPath(processoId, filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        try {
            long size = Files.size(path);
            String contentType = Files.probeContentType(path);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            registrarAuditoriaDownload(authentication, processoId, filename, size);
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(path, java.nio.file.StandardOpenOption.READ));
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(size)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new IllegalStateException("Falha no download", e);
        }
    }

    private void registrarAuditoriaUpload(Authentication authentication, Long processoId, RecursalStoredFileRef ref) {
        try {
            auditoria.registrarEventoImutavel(actionFor(authentication, "DOCUMENTO_UPLOAD"), "RECURSAL_ATTACHMENT", ref.storageKey(),
                    "processoId=" + processoId + ";sizeBytes=" + ref.sizeBytes() + ";sha256=" + ref.sha256());
        } catch (Exception ignored) {
        }
    }

    private void registrarAuditoriaDownload(Authentication authentication, Long processoId, String filename, long size) {
        try {
            auditoria.registrarEventoImutavel(actionFor(authentication, "DOCUMENTO_DOWNLOAD"), "RECURSAL_ATTACHMENT", processoId + ":" + filename,
                    "processoId=" + processoId + ";filename=" + filename + ";sizeBytes=" + size);
        } catch (Exception ignored) {
        }
    }

    private static String filenameFromKey(String storageKey) {
        if (storageKey == null) {
            return "";
        }
        int idx = storageKey.lastIndexOf('/');
        return idx < 0 ? storageKey : storageKey.substring(idx + 1);
    }

    private static String actionFor(Authentication authentication, String suffix) {
        String s = suffix == null ? "EVENT" : suffix.trim().toUpperCase();
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean adv = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADVOGADO".equals(a.getAuthority()));
            if (adv) return "ADV_" + s;
            boolean cid = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_CIDADAO".equals(a.getAuthority()));
            if (cid) return "CID_" + s;
            boolean srv = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SERVIDOR".equals(a.getAuthority()));
            if (srv) return "SRV_" + s;
            boolean mag = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MAGISTRADO".equals(a.getAuthority()));
            if (mag) return "MAG_" + s;
        }
        return s;
    }
}
