package com.tcc.pjb.backend.controller.advogado;

import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialEstruturarRequest;
import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialProtocolarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.advogado.surface.LaianePeticaoInicialDraftSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advogado/laiane/peticao-inicial")
public class LaianePeticaoInicialDraftController {

    private final LaianePeticaoInicialDraftSurfaceFacadeService facadeService;

    public LaianePeticaoInicialDraftController(LaianePeticaoInicialDraftSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/estruturar")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> estruturar(@Valid @RequestBody LaianePeticaoInicialEstruturarRequest request) {
        return ResponseEntity.ok(facadeService.estruturar(request));
    }

    @PostMapping("/salvar")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> salvar(@Valid @RequestBody LaianePeticaoInicialEstruturarRequest request) {
        return ResponseEntity.ok(facadeService.salvar(request));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> listarMinhas() {
        return ResponseEntity.ok(facadeService.listarMinhas());
    }

    @GetMapping("/{draftId}")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable Long draftId) {
        return ResponseEntity.ok(facadeService.detalhar(draftId));
    }

    @PostMapping("/{draftId}/protocolar")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> protocolar(@PathVariable Long draftId,
                                                              @RequestBody(required = false) LaianePeticaoInicialProtocolarRequest request) {
        return ResponseEntity.ok(facadeService.protocolar(draftId, request));
    }
}
