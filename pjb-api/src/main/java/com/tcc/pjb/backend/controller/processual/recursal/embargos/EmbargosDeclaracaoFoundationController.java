package com.tcc.pjb.backend.controller.processual.recursal.embargos;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoRequest;
import com.tcc.pjb.backend.service.processual.recursal.embargos.EmbargosDeclaracaoFoundationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class EmbargosDeclaracaoFoundationController {

    private final EmbargosDeclaracaoFoundationService foundationService;

    public EmbargosDeclaracaoFoundationController(EmbargosDeclaracaoFoundationService foundationService) {
        this.foundationService = foundationService;
    }

    @GetMapping(RecursalRoutes.EMBARGOS_DECLARACAO)
    public ResponseEntity<EmbargosDeclaracaoFoundationResponse> describe() {
        return ResponseEntity.ok(foundationService.describe());
    }

    @PostMapping(RecursalRoutes.EMBARGOS_DECLARACAO_PREVIEW)
    public ResponseEntity<EmbargosDeclaracaoFoundationResponse> preview(@RequestBody EmbargosDeclaracaoRequest request) {
        return ResponseEntity.ok(foundationService.preview(request));
    }
}
