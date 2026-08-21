package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.processo.atoordinatorio.application.AtoOrdinatorioServidorApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioRequest;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/processo/ato-ordinatorio")
public class AtoOrdinatorioServidorController {

    private static final String SERVIDOR_ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM')";

    private final AtoOrdinatorioServidorApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AtoOrdinatorioServidorController(AtoOrdinatorioServidorApplicationService applicationService,
                                            ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping
    @PreAuthorize(SERVIDOR_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> proferir(@Valid @RequestBody AtoOrdinatorioRequest request) {
        AtoOrdinatorioResponse response = applicationService.proferir(
                request.processoId(), request.tipo(), request.complemento());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponseFactory.commandOk("ato ordinatório proferido e assinado", response, List.of()));
    }
}
