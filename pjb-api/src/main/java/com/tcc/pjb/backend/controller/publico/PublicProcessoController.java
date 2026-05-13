package com.tcc.pjb.backend.controller.publico;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoConsultaResponse;
import com.tcc.pjb.backend.model.dto.publico.TimelinePublicaDto;
import com.tcc.pjb.backend.service.publico.PublicDocumentoDownloadService;
import com.tcc.pjb.backend.service.publico.PublicProcessoConsultaService;
import com.tcc.pjb.backend.service.publico.PublicProcessoTimelineService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/processos")
@RequiredArgsConstructor
@Validated
public class PublicProcessoController {

    private final PublicProcessoConsultaService consultaService;
    private final PublicDocumentoDownloadService downloadService;
    private final PublicProcessoTimelineService timelineService;

    @GetMapping("/{numero}")
    public ResponseEntity<PublicProcessoConsultaResponse> consultar(@PathVariable @NotBlank String numero) {
        return ResponseEntity.ok(consultaService.consultarPorNumero(numero));
    }

    @GetMapping("/{numero}/timeline")
    public ResponseEntity<TimelinePublicaDto> timeline(@PathVariable @NotBlank String numero) {
        return ResponseEntity.ok(timelineService.timeline(numero));
    }

    
    @GetMapping("/documentos/{documentoId}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> baixarPdf(@PathVariable UUID documentoId, HttpServletRequest request) {
        if (request != null) {
            request.setAttribute("PJB_DOCUMENTO_ID", documentoId.toString());
        }
        return downloadService.baixarPdf(documentoId);
    }
}
