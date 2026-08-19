package com.tcc.pjb.backend.controller.secretariat.malote;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.service.secretariat.ingest.ProcessoExternoCargaService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secretariat/malote")
public class SecretariatMaloteDigitalController {

    private static final String SECRETARIA_ROLES =
            "hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','DIRETOR_SECRETARIA','CHEFE_SECRETARIA')";

    private final ProcessoExternoCargaService cargaService;

    public SecretariatMaloteDigitalController(ProcessoExternoCargaService cargaService) {
        this.cargaService = Objects.requireNonNull(cargaService);
    }

    @PostMapping("/processar")
    @PreAuthorize(SECRETARIA_ROLES)
    public ResponseEntity<PjbApiResponseEnvelope<ProcessoExternoCargaService.CargaResultado>> processar(
            @RequestBody List<ProcessoExternoCargaService.CargaItem> itens) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(cargaService.processarLote(itens)));
    }
}
