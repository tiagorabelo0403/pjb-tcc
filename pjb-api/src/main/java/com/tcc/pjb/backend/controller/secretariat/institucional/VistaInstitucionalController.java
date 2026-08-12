package com.tcc.pjb.backend.controller.secretariat.institucional;

import com.tcc.pjb.backend.model.dto.secretariat.VistaInstitucionalRequest;
import com.tcc.pjb.backend.service.secretariat.institucional.VistaInstitucionalService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VistaInstitucionalController {

    private final VistaInstitucionalService service;

    public VistaInstitucionalController(VistaInstitucionalService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping("/api/v1/processos/{processoId}/vista-institucional")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<Void> determinarVista(@PathVariable Long processoId,
                                                 @Valid @RequestBody VistaInstitucionalRequest request) {
        service.determinarVista(processoId, request.tipoInstituicaoAlvo(), request.prazoBaseDias());
        return ResponseEntity.accepted().build();
    }
}
