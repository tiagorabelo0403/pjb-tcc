package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGratuidadeAvaliacaoRequest;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoSolicitacaoAjgRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.defensor.DefensoriaPublicaOperacionalService;
import com.tcc.pjb.backend.service.processual.gratuidade.JusticaGratuidaVerificadorService;
import com.tcc.pjb.backend.service.processual.gratuidade.JusticaGratuidaVerificadorService.GratuidadeInput;
import com.tcc.pjb.backend.service.processual.gratuidade.JusticaGratuidaVerificadorService.GratuidadeSnapshot;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cidadao/gratuidade")
public class CidadaoGratuidadeController {

    private final JusticaGratuidaVerificadorService verificadorService;
    private final DefensoriaPublicaOperacionalService defensoriaPublicaOperacionalService;
    private final CapabilityRateLimiter rateLimiter;

    public CidadaoGratuidadeController(JusticaGratuidaVerificadorService verificadorService,
                                       DefensoriaPublicaOperacionalService defensoriaPublicaOperacionalService,
                                       CapabilityRateLimiter rateLimiter) {
        this.verificadorService = Objects.requireNonNull(verificadorService);
        this.defensoriaPublicaOperacionalService = Objects.requireNonNull(defensoriaPublicaOperacionalService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/avaliacao")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<GratuidadeSnapshot> avaliar(@Valid @RequestBody CidadaoGratuidadeAvaliacaoRequest request,
                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_gratuidade_avaliacao", ApiVersion.V1);
        GratuidadeInput input = new GratuidadeInput(
                request.processoId(),
                request.parteId(),
                request.declaracaoHipossuficiencia(),
                request.rendaMensalDeclarada(),
                request.representadoPorDefensoria(),
                request.beneficioSocial(),
                request.impugnadaPelaParteContraria());
        return ResponseEntity.ok(verificadorService.avaliar(input));
    }

    @PostMapping("/processos/{processoId}/solicitar-ajg")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Map<String, Object>> solicitarAjg(@PathVariable Long processoId,
                                                              @Valid @RequestBody CidadaoSolicitacaoAjgRequest request,
                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_gratuidade_solicitar_ajg", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                defensoriaPublicaOperacionalService.solicitarAssistenciaJudiciariaGratuitaComoParte(
                        processoId, request.renda(), request.justificativa()));
    }
}
