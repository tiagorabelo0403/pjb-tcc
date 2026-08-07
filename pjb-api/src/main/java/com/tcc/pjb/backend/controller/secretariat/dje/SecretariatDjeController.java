package com.tcc.pjb.backend.controller.secretariat.dje;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.core.dje.DjeApplicationService;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import com.tcc.pjb.backend.core.dje.domain.DjeProcessoPublicationView;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secretariat/dje")
public class SecretariatDjeController {

    private static final String SECRETARIA_ROLES = "hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','DIRETOR_SECRETARIA','CHEFE_SECRETARIA')";
    private static final String SECRETARIA_OU_MAGISTRATURA_ROLES =
            "hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','DIRETOR_SECRETARIA','CHEFE_SECRETARIA','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final DjeApplicationService applicationService;

    public SecretariatDjeController(DjeApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    @PostMapping("/lifecycle/run")
    @PreAuthorize(SECRETARIA_ROLES)
    public ResponseEntity<PjbApiResponseEnvelope<DjeLifecycleExecutionSummary>> lifecycleRun(
            @RequestParam(value = "hoje", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hoje,
            @RequestParam(value = "batchSize", required = false) Integer batchSize) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(applicationService.lifecycleRun(hoje, batchSize)));
    }

    @GetMapping("/processos/{processoId}/publicacao")
    @PreAuthorize(SECRETARIA_OU_MAGISTRATURA_ROLES)
    public ResponseEntity<PjbApiResponseEnvelope<DjeProcessoPublicationView>> publicacaoDoProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(applicationService.processoPublication(processoId)));
    }
}
