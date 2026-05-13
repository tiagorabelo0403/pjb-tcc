package com.tcc.pjb.backend.controller;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.service.ajuizamento.ProcessoCommandSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/v1/processos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProcessoCommandController {

    private final ProcessoCommandSurfaceFacadeService processoCommandSurfaceFacadeService;

    @PostMapping(value = "/ajuizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessoResponse> ajuizar(
            @RequestPart("dados") @Valid ProcessoRequest request,
            @RequestPart(value = "anexos", required = false) List<MultipartFile> arquivos) {
        return ResponseEntity.ok(processoCommandSurfaceFacadeService.ajuizar(request, arquivos));
    }

}
