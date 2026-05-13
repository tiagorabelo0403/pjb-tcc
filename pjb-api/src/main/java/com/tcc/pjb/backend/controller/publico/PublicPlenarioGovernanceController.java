package com.tcc.pjb.backend.controller.publico;

import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioEsclarecimentoRequest;
import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioMediaRegistrationRequest;
import com.tcc.pjb.backend.model.dto.publico.PublicPlenarioRespostaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.publico.surface.PublicPlenarioGovernanceSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plenario-governanca")
public class PublicPlenarioGovernanceController {

    private final PublicPlenarioGovernanceSurfaceFacadeService facadeService;

    public PublicPlenarioGovernanceController(PublicPlenarioGovernanceSurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @GetMapping("/sessoes/{sessaoId}")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','ASSESSOR_MINISTRO','ASSESSOR_DESEMBARGADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(facadeService.detalhar(sessaoId));
    }

    @PostMapping("/sessoes/{sessaoId}/midias")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','ASSESSOR_MINISTRO','ASSESSOR_DESEMBARGADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarMidia(@PathVariable Long sessaoId,
                                                                @Valid @RequestBody PublicPlenarioMediaRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarMidia(sessaoId, request));
    }

    @PostMapping("/sessoes/{sessaoId}/esclarecimentos")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','ASSESSOR_MINISTRO','ASSESSOR_DESEMBARGADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarEsclarecimento(@PathVariable Long sessaoId,
                                                                         @Valid @RequestBody PublicPlenarioEsclarecimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarEsclarecimento(sessaoId, request));
    }

    @PostMapping("/esclarecimentos/{esclarecimentoId}/resposta")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','ASSESSOR_MINISTRO','ASSESSOR_DESEMBARGADOR')")
    public ResponseEntity<SurfaceActionResponse> responder(@PathVariable Long esclarecimentoId,
                                                           @Valid @RequestBody PublicPlenarioRespostaRequest request) {
        return ResponseEntity.ok(facadeService.responder(esclarecimentoId, request));
    }
}
