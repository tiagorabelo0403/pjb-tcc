package com.tcc.pjb.backend.controller.ajuizamento.federal;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoEventoRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoHeartbeatRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoLedgerResponse;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoNodeResponse;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoNodeUpsertRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoOutboxResponse;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;

@RestController
@RequestMapping("/api/v1/admin/federalismo")
@Validated
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class FederalismoJudicialController {

    private final FederalismoJudicialEngine federalismoJudicialEngine;

    public FederalismoJudicialController(FederalismoJudicialEngine federalismoJudicialEngine) {
        this.federalismoJudicialEngine = federalismoJudicialEngine;
    }

    @PostMapping("/nos")
    public ResponseEntity<FederalismoNodeResponse> upsertNo(@Valid @RequestBody FederalismoNodeUpsertRequest request) {
        return ResponseEntity.ok(federalismoJudicialEngine.upsertNo(request));
    }

    @PostMapping("/nos/heartbeat")
    public ResponseEntity<FederalismoNodeResponse> heartbeat(@Valid @RequestBody FederalismoHeartbeatRequest request) {
        return ResponseEntity.ok(federalismoJudicialEngine.registrarHeartbeat(request));
    }

    @GetMapping("/nos")
    public ResponseEntity<List<FederalismoNodeResponse>> listarNos() {
        return ResponseEntity.ok(federalismoJudicialEngine.listarNos());
    }

    @GetMapping("/nos/{codigoTribunal}")
    public ResponseEntity<FederalismoNodeResponse> buscarNo(@PathVariable String codigoTribunal) {
        return federalismoJudicialEngine.buscarNo(codigoTribunal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<FederalismoJudicialEngine.FederacaoHealth> health() {
        return ResponseEntity.ok(federalismoJudicialEngine.healthFederacao());
    }

    @PostMapping("/eventos")
    public ResponseEntity<FederalismoLedgerResponse> registrarEvento(@Valid @RequestBody FederalismoEventoRequest request) {
        return ResponseEntity.ok(federalismoJudicialEngine.registrarEventoFederado(request));
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<FederalismoLedgerResponse>> consultarLedger(@RequestParam String nupn) {
        return ResponseEntity.ok(federalismoJudicialEngine.consultarLedgerPorNupn(nupn));
    }

    @GetMapping("/ledger/integridade")
    public ResponseEntity<Long> verificarIntegridade() {
        return ResponseEntity.ok(federalismoJudicialEngine.verificarIntegridadeLedger());
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<FederalismoOutboxResponse>> listarOutbox(@RequestParam(required = false) String codigoTribunal) {
        return ResponseEntity.ok(federalismoJudicialEngine.listarPendencias(codigoTribunal));
    }

    @PostMapping("/outbox/processar")
    public ResponseEntity<FederalismoOutboxResponse> processarOutbox(@RequestParam(required = false) String codigoTribunal) {
        return federalismoJudicialEngine.processarProximoEventoPendente(codigoTribunal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
