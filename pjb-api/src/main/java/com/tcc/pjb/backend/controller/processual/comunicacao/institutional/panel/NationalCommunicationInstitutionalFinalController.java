package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.panel;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalNoReadRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalFinalController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalFinalController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PANEL_ORGAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalPanelSummaryResponse>> painelOrgao(@RequestParam(required = false) String unidadeCodigo) {
        return ResponseEntity.ok(facadeService.painelOrgao(unidadeCodigo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_FILAS_UNIDADE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalUnitQueueResponse>> filasUnidade(@RequestParam(required = false) String unidadeCodigo) {
        return ResponseEntity.ok(facadeService.filasUnidade(unidadeCodigo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AVISOS_EXTERNOS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalNoticeChannelResponse>> avisosExternos() {
        return ResponseEntity.ok(facadeService.avisosExternos());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PENDENTES_NAO_LEITURA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalInboxItemResponse>> pendentesNaoLeitura(@RequestParam(required = false) String unidadeCodigo) {
        return ResponseEntity.ok(facadeService.pendentesNaoLeitura(unidadeCodigo));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_CERTIFICAR_NAO_LEITURA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalActionResponse> certificarNaoLeitura(@Valid @RequestBody NationalCommunicationInstitutionalNoReadRequest request) {
        return ResponseEntity.ok(facadeService.certificarNaoLeitura(request));
    }
}