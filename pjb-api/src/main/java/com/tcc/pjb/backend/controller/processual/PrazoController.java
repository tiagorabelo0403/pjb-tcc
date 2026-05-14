package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.service.prazo.PrazoDecursoCertidaoService;
import com.tcc.pjb.backend.service.prazo.PrazoProcessualEngine;
import com.tcc.pjb.backend.service.prazo.PrazoVencimentoAlertService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prazos")
public class PrazoController {

    private final PrazoProcessualEngine engine;
    private final PrazoVencimentoAlertService alertService;
    private final PrazoDecursoCertidaoService certidaoService;

    public PrazoController(PrazoProcessualEngine engine,
            PrazoVencimentoAlertService alertService,
            PrazoDecursoCertidaoService certidaoService) {
        this.engine = engine;
        this.alertService = alertService;
        this.certidaoService = certidaoService;
    }

    @PostMapping("/calcular")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','MAGISTRADO')")
    public ResponseEntity<PjbApiResponseEnvelope<PrazoProcessualEngine.PrazoSnapshot>>
            calcular(@RequestBody PrazoProcessualEngine.PrazoInput input) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(engine.calcular(input)));
    }

    @PostMapping("/calcular-lote")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR')")
    public ResponseEntity<PjbApiResponseEnvelope<List<PrazoProcessualEngine.PrazoSnapshot>>>
            calcularLote(@RequestBody List<PrazoProcessualEngine.PrazoInput> inputs) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(engine.calcularLote(inputs)));
    }

    @PostMapping("/alertas")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','DIRETOR_SECRETARIA')")
    public ResponseEntity<PjbApiResponseEnvelope<List<PrazoVencimentoAlertService.AlertaPrazo>>>
            alertas(@RequestBody List<PrazoProcessualEngine.PrazoSnapshot> prazos) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                alertService.gerarAlertas(prazos)));
    }

    @PostMapping("/certidao-decurso")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','CHEFE_SECRETARIA')")
    public ResponseEntity<PjbApiResponseEnvelope<PrazoDecursoCertidaoService.CertidaoDecurso>>
            certidaoDecurso(
                    @RequestBody PrazoDecursoCertidaoService.CertidaoDecursoInput input) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                certidaoService.avaliar(input)));
    }
}
