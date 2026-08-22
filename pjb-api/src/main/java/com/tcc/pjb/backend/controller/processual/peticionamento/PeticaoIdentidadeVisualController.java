package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/peticionamento/identidade-visual")
@PreAuthorize("isAuthenticated()")
public class PeticaoIdentidadeVisualController {

    private final PeticaoIdentidadeVisualService service;

    public PeticaoIdentidadeVisualController(PeticaoIdentidadeVisualService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @GetMapping
    public ResponseEntity<IdentidadeVisualResponse> obter() {
        return ResponseEntity.ok(service.obterMinha());
    }

    @PutMapping
    public ResponseEntity<IdentidadeVisualResponse> salvar(@Valid @RequestBody IdentidadeVisualRequest request) {
        return ResponseEntity.ok(service.salvarMinha(request));
    }

    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IdentidadeVisualResponse> uploadLogo(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String ct = file.getContentType();
        String normalized = ct == null ? "" : ct.trim().toLowerCase();
        if (!normalized.equals("image/jpeg") && !normalized.equals("image/jpg") && !normalized.equals("image/png")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        try {
            return ResponseEntity.ok(service.uploadLogo(file.getBytes(), normalized));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> lerLogo(HttpServletRequest request) throws IOException {
        PeticaoIdentidadeVisualService.LogoLeitura logo = service.lerLogo();
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = '"' + logo.sha256() + '"';
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null && !ifNoneMatch.isBlank() && ifNoneMatch.trim().equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                    .build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .contentLength(logo.sizeBytes() == null ? logo.read().contentLength() : logo.sizeBytes())
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(logo.read().resource());
    }

    @DeleteMapping("/logo")
    public ResponseEntity<IdentidadeVisualResponse> removerLogo() {
        return ResponseEntity.ok(service.removerLogo());
    }
}
