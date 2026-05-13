package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.panel;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalExecutiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelNotificationResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalPanelController {

    private final NationalCommunicationInstitutionalPanelFacadeService facadeService;

    public NationalCommunicationInstitutionalPanelController(NationalCommunicationInstitutionalPanelFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PAINEL_EXECUTIVO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalExecutiveDashboardResponse> painelExecutivo(@RequestParam(required = false) String unidadeCodigo,
                                                                                                         @RequestParam(required = false) String uf,
                                                                                                         @RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind,
                                                                                                         @RequestParam(required = false) Long processoId,
                                                                                                         @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(facadeService.painelExecutivo(unidadeCodigo, uf, destinatarioKind, processoId, expedicaoUuid));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_NOTIFICACOES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalPanelNotificationResponse>> notificacoes(@RequestParam(required = false) String unidadeCodigo,
                                                                                                           @RequestParam(required = false) String uf,
                                                                                                           @RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind,
                                                                                                           @RequestParam(required = false) Long processoId,
                                                                                                           @RequestParam(required = false) String expedicaoUuid) {
        return ResponseEntity.ok(facadeService.notificacoes(unidadeCodigo, uf, destinatarioKind, processoId, expedicaoUuid));
    }
}
