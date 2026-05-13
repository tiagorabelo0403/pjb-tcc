package com.tcc.pjb.backend.controller.publico;

import com.tcc.pjb.backend.model.dto.publico.AdvogadoPublicoProcessoIntegralResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicPessoaProcessualSearchResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoSearchResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.publico.ProcessoPesquisaIdentidadePublicaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public/processos-pessoas")
@Validated
public class PublicProcessoPessoaController {

    private final ProcessoPesquisaIdentidadePublicaService service;
    private final CapabilityRateLimiter rateLimiter;

    public PublicProcessoPessoaController(ProcessoPesquisaIdentidadePublicaService service,
                                          CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/candidatos")
    public ResponseEntity<PublicPessoaProcessualSearchResponse> candidatos(@RequestParam("nome") @NotBlank String nome,
                                                                           @RequestParam(value = "uf", required = false) String uf,
                                                                           @RequestParam(value = "comarca", required = false) String comarca,
                                                                           @RequestParam(value = "forum", required = false) String forum,
                                                                           @RequestParam(value = "page", defaultValue = "0") int page,
                                                                           @RequestParam(value = "size", defaultValue = "20") int size) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_PROCESSO_PESSOA_CANDIDATOS", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate().mustRevalidate())
                .body(service.buscarPessoasPorNome(nome, uf, comarca, forum, page, size));
    }

    @GetMapping("/candidatos/{identityKey}/processos")
    public ResponseEntity<PublicProcessoResumoSearchResponse> processosPorCandidato(@PathVariable String identityKey,
                                                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                                                     @RequestParam(value = "size", defaultValue = "20") int size) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_PROCESSO_PESSOA_PROCESSOS", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate().mustRevalidate())
                .body(service.buscarProcessosPorIdentidade(identityKey, page, size));
    }

    @GetMapping("/cpf/{cpf}/processos")
    public ResponseEntity<PublicProcessoResumoSearchResponse> processosPorCpf(@PathVariable String cpf,
                                                                              @RequestParam(value = "page", defaultValue = "0") int page,
                                                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_PROCESSO_PESSOA_CPF", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(12)).cachePrivate().mustRevalidate())
                .body(service.buscarProcessosPublicosPorCpf(cpf, page, size));
    }

    @GetMapping("/processos/{numero}/resumo")
    public ResponseEntity<PublicProcessoResumoCardDto> resumo(@PathVariable @NotBlank String numero) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_PROCESSO_RESUMO", ApiVersion.latest());
        return ResponseEntity.ok(service.resumirProcessoPublico(numero));
    }

    @GetMapping("/advocacia/processos/{numero}/integral")
    @PreAuthorize("hasRole('ADVOGADO')")
    public ResponseEntity<AdvogadoPublicoProcessoIntegralResponse> integral(@PathVariable @NotBlank String numero,
                                                                            HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, SecurityContextHolder.getContext().getAuthentication(), "ADVOGADO_PROCESSO_PUBLICO_INTEGRAL", ApiVersion.latest());
        if (request != null) {
            request.setAttribute("PJB_PROCESSO_NUMERO", numero);
        }
        return ResponseEntity.ok(service.acessoProfissionalIntegral(numero));
    }
}
