package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalAutuacaoDestinoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalAutuacaoDestinoService;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence/recursal")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class RecursalIntelligenceController {

    private final RecursalFactIdempotentIngestService idempotentIngest;
    private final RecursalIntelligenceFacadeService facade;
    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final PjbAuthorizationService authorizationService;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final RecursalAutuacaoDestinoService autuacaoDestinoService;

    @PostMapping("/processo/{processoId}/facts")
    @CacheEvict(cacheNames = "timeline_processo", key = "#processoId + ':' + @currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public ResponseEntity<RecursalFactIngestResponse> ingestFact(@PathVariable Long processoId,
                                                                 @Valid @RequestBody RecursalFactIngestRequest req) {
        Processo proc = processoAccessApplicationService.load(processoId);
        authorizationService.requireWriteProcesso(proc);
        String proceedingNumber = normalizeProceedingNumber(req.sourceProceedingNumber(), proc);
        CanonicalFact fact = new CanonicalFact(
                null,
                req.type(),
                req.sourceSystem(),
                req.externalId(),
                proceedingNumber,
                req.payload(),
                req.observedAt() == null ? Instant.now() : req.observedAt()
        );
        return ResponseEntity.ok(idempotentIngest.ingest(processoId, fact, req, proceedingNumber));
    }

    @PostMapping("/processo/{processoId}/autuacao-destino")
    @CacheEvict(cacheNames = "timeline_processo", key = "#processoId + ':' + @currentUserService.currentUserIdOrZero()", condition = "@cacheRuntime.redisEnabled()")
    public ResponseEntity<java.util.Map<String, Object>> registrarAutuacaoDestino(@PathVariable Long processoId,
                                                                                  @Valid @RequestBody RecursalAutuacaoDestinoRequest request) {
        Processo proc = processoAccessApplicationService.load(processoId);
        authorizationService.requireWriteProcesso(proc);
        return ResponseEntity.ok(autuacaoDestinoService.registrar(
                processoId,
                request.numeroAutuacaoDestino(),
                request.instanciaDestino(),
                request.tribunalDestino(),
                request.unidadeDistribuicao(),
                request.observacoes()
        ));
    }

    @GetMapping("/processo/{processoId}/graph")
    public ResponseEntity<RecursalGraphResponse> graph(@PathVariable Long processoId) {
        Processo proc = processoAccessApplicationService.load(processoId);
        authorizationService.requireReadProcesso(proc);
        authorizationService.requireReadProcessoAtSecrecy(proc, secrecyService.effectiveSecrecyForProcesso(processoId));
        return ResponseEntity.ok(facade.graph(processoId));
    }

    private static String normalizeProceedingNumber(String provided, Processo p) {
        String s = provided == null ? "" : provided.trim();
        if (!s.isBlank()) {
            return s;
        }
        String unificado = p.getNumeroUnificado();
        if (unificado != null && !unificado.isBlank()) {
            return unificado.trim();
        }
        String legacy = p.getNumeroProcesso();
        if (legacy != null && !legacy.isBlank()) {
            return legacy.trim();
        }
        return "PROCESSO_ID:" + p.getId();
    }
}
