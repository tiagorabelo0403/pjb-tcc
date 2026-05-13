package com.tcc.pjb.backend.controller.expediente;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.expediente.SemInteresseRequest;
import com.tcc.pjb.backend.model.dto.expediente.SemInteresseResponse;
import com.tcc.pjb.backend.service.expediente.SemInteresseService;

@RestController
@RequestMapping("/api/v1/expediente")
public class ExpedienteSemInteresseController {

    private static final String ROLES = "hasAnyRole('PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','PROCURADOR_GERAL_REPUBLICA','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA')";

    private final SemInteresseService semInteresseService;

    public ExpedienteSemInteresseController(SemInteresseService semInteresseService) {
        this.semInteresseService = semInteresseService;
    }

    @PostMapping("/{id}/sem-interesse")
    @PreAuthorize(ROLES)
    public ResponseEntity<SemInteresseResponse> registrar(@PathVariable Long id,
                                                          @Valid @RequestBody(required = false) SemInteresseRequest request) {
        String justificativa = request != null ? request.justificativa() : null;
        return ResponseEntity.ok(semInteresseService.registrar(id, justificativa));
    }
}
