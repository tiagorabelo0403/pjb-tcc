package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.topology;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.NationalCommunicationInstitutionalTopologyResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR_FORUM','ROLE_MAGISTRADO','ROLE_JUIZ','ROLE_MINISTERIO_PUBLICO','ROLE_PROCURADOR','ROLE_DEFENSOR_PUBLICO','ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class NationalCommunicationInstitutionalTopologyController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalTopologyController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_TOPOLOGIA_DESTINATARIOS)
    public ResponseEntity<List<NationalCommunicationInstitutionalTopologyResponse>> list() {
        return ResponseEntity.ok(facadeService.topologiaDestinatarios());
    }
}
