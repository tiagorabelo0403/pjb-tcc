package com.tcc.pjb.backend.controller.juiz;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.juiz.CertidaoTJResponse;
import com.tcc.pjb.backend.model.dto.juiz.GabineteAgrupadorResponse;
import com.tcc.pjb.backend.model.dto.juiz.GabineteKanbanResponse;
import com.tcc.pjb.backend.model.dto.juiz.JuizGabineteActionResponse;
import com.tcc.pjb.backend.model.dto.juiz.JuizGabineteSnapshotResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizAudienciaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoDecisaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoRejeicaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizDespachoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizDecisaoInterlocutoriaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizOrdemCumprimentoOficialRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizSentencaRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.juiz.CertidaoTransitoJulgadoService;
import com.tcc.pjb.backend.service.juiz.GabineteAgrupadorService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.surface.JuizGabineteSurfaceFacadeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.JUDGE_GABINETE_DECISOES_BASE)
@Validated
public class JuizGabineteDecisionalController {

    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";
    private final JuizGabineteDecisionalService service;
    private final JuizGabineteSurfaceFacadeService juizGabineteSurfaceFacadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final CertidaoTransitoJulgadoService certidaoTransitoJulgadoService;
    private final GabineteAgrupadorService gabineteAgrupadorService;

    public JuizGabineteDecisionalController(JuizGabineteDecisionalService service,
                                            JuizGabineteSurfaceFacadeService juizGabineteSurfaceFacadeService,
                                            CapabilityRateLimiter rateLimiter,
                                            CertidaoTransitoJulgadoService certidaoTransitoJulgadoService,
                                            GabineteAgrupadorService gabineteAgrupadorService) {
        this.service = service;
        this.juizGabineteSurfaceFacadeService = juizGabineteSurfaceFacadeService;
        this.rateLimiter = rateLimiter;
        this.certidaoTransitoJulgadoService = certidaoTransitoJulgadoService;
        this.gabineteAgrupadorService = gabineteAgrupadorService;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizGabineteSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "juiz_gabinete_snapshot");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.snapshot());
    }

    @GetMapping("/painel/kanban")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<GabineteKanbanResponse> kanban(Authentication authentication) {
        enforce(authentication, "juiz_gabinete_kanban");
        return ResponseEntity.ok(service.kanban());
    }

    @GetMapping("/painel/agrupadores")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<GabineteAgrupadorResponse> agrupadores(Authentication authentication) {
        enforce(authentication, "juiz_gabinete_agrupadores");
        return ResponseEntity.ok(gabineteAgrupadorService.agrupar());
    }

    @GetMapping("/fila")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceCollectionResponse> fila(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                            Authentication authentication) {
        enforce(authentication, "juiz_gabinete_fila");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.fila(page, size));
    }


    @GetMapping("/processos/{processoId}/matriz")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> matriz(@PathVariable Long processoId,
                                                          Authentication authentication) {
        enforce(authentication, "juiz_gabinete_matriz");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.matriz(processoId));
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> malha(@PathVariable Long processoId,
                                                         Authentication authentication) {
        enforce(authentication, "juiz_gabinete_malha");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.malha(processoId));
    }

    @GetMapping("/processos/{processoId}/topologia")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> topologia(@PathVariable Long processoId,
                                                             Authentication authentication) {
        enforce(authentication, "juiz_gabinete_topologia");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.topologia(processoId));
    }

    @GetMapping("/processos/{processoId}/isolamento-fila")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> isolamentoFila(@PathVariable Long processoId,
                                                                  Authentication authentication) {
        enforce(authentication, "juiz_gabinete_isolamento_fila");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.isolamentoFila(processoId));
    }


    @GetMapping("/processos/{processoId}/handoff")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> handoff(@PathVariable Long processoId,
                                                           Authentication authentication) {
        enforce(authentication, "juiz_gabinete_handoff");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.handoff(processoId));
    }

    @PostMapping("/processos/{processoId}/encaminhar-assessoria")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> encaminharParaAssessoria(@PathVariable Long processoId,
                                                                          @RequestParam(required = false) String tarefa,
                                                                          @RequestParam(required = false) String observacao,
                                                                          Authentication authentication) {
        enforce(authentication, "juiz_gabinete_encaminhar_assessoria");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.encaminharParaAssessoria(processoId, tarefa, observacao));
    }

    @PostMapping("/processos/{processoId}/capturar")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> capturar(@PathVariable Long processoId,
                                                          Authentication authentication) {
        enforce(authentication, "juiz_gabinete_capturar");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.capturar(processoId));
    }

    @PostMapping("/processos/{processoId}/liberar")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> liberar(@PathVariable Long processoId,
                                                         @RequestParam(required = false) String destino,
                                                         Authentication authentication) {
        enforce(authentication, "juiz_gabinete_liberar");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.liberar(processoId, destino));
    }

    @GetMapping("/processos/{processoId}/guardrails")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse> guardrails(@PathVariable Long processoId,
                                                                                                            Authentication authentication) {
        enforce(authentication, "juiz_gabinete_guardrails");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.guardrails(processoId));
    }

    @PostMapping("/processos/{processoId}/despacho")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizGabineteActionResponse> assinarDespacho(@PathVariable Long processoId,
                                                                      @Valid @RequestBody JuizDespachoRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "juiz_gabinete_despacho");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.assinarDespacho(processoId, request.conteudo(), request.fundamentacao()));
    }

    @PostMapping("/processos/{processoId}/sentenca")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizGabineteActionResponse> proferirSentenca(@PathVariable Long processoId,
                                                                       @Valid @RequestBody JuizSentencaRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "juiz_gabinete_sentenca");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.proferirSentenca(processoId, request.dispositivo(), request.fundamentacao(), request.tipoSentenca()));
    }

    @PostMapping("/processos/{processoId}/decisao-interlocutoria")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizGabineteActionResponse> proferirDecisaoInterlocutoria(@PathVariable Long processoId,
                                                                                     @Valid @RequestBody JuizDecisaoInterlocutoriaRequest request,
                                                                                     Authentication authentication) {
        enforce(authentication, "juiz_gabinete_decisao_interlocutoria");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.proferirDecisaoInterlocutoria(processoId, request.dispositivo(), request.fundamentacao(), request.tipoDecisao()));
    }

    @PostMapping("/processos/{processoId}/audiencia")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizGabineteActionResponse> designarAudiencia(@PathVariable Long processoId,
                                                                        @Valid @RequestBody JuizAudienciaRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "juiz_gabinete_audiencia");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.designarAudiencia(processoId, request.dataHora().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(), request.tipo(), request.local()));
    }


    @GetMapping(OperationalApiRoutes.PATH_JUDGE_OFFICIAL_RETURN_SUGGESTION)
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> sugestaoRetornoOficial(@PathVariable Long gabineteWorkItemId,
                                                                          Authentication authentication) {
        enforce(authentication, "juiz_gabinete_oficial_retorno_sugestao");
        return ResponseEntity.ok(juizGabineteSurfaceFacadeService.sugestaoRetornoOficial(gabineteWorkItemId));
    }

    @PostMapping(OperationalApiRoutes.PATH_JUDGE_OFFICIAL_RETURN_APPROVE_MINUTA)
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> aprovarMinutaRetornoOficial(@PathVariable Long gabineteWorkItemId,
                                                                             @Valid @RequestBody(required = false) JuizGabineteOficialRetornoDecisaoRequest request,
                                                                             Authentication authentication) {
        enforce(authentication, "juiz_gabinete_oficial_retorno_aprovar_minuta");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.aprovarMinutaRetornoOficial(gabineteWorkItemId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_JUDGE_OFFICIAL_RETURN_APPROVE_REEXPEDICAO)
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> aprovarReexpedicaoRetornoOficial(@PathVariable Long gabineteWorkItemId,
                                                                                   @Valid @RequestBody(required = false) JuizGabineteOficialRetornoDecisaoRequest request,
                                                                                   Authentication authentication) {
        enforce(authentication, "juiz_gabinete_oficial_retorno_aprovar_reexpedicao");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.aprovarReexpedicaoRetornoOficial(gabineteWorkItemId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_JUDGE_OFFICIAL_RETURN_REJECT)
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> rejeitarRetornoOficial(@PathVariable Long gabineteWorkItemId,
                                                                        @Valid @RequestBody(required = false) JuizGabineteOficialRetornoRejeicaoRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "juiz_gabinete_oficial_retorno_rejeitar");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.rejeitarRetornoOficial(gabineteWorkItemId, request));
    }


    @PostMapping("/processos/{processoId}/ordem-cumprimento-oficial")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<SurfaceActionResponse> ordenarCumprimentoOficial(@PathVariable Long processoId,
                                                                           @Valid @RequestBody JuizOrdemCumprimentoOficialRequest request,
                                                                           Authentication authentication) {
        enforce(authentication, "juiz_gabinete_ordem_cumprimento_oficial");
        return ResponseEntity.status(HttpStatus.CREATED).body(juizGabineteSurfaceFacadeService.ordenarCumprimentoOficial(processoId, request));
    }

    @PostMapping("/processos/{processoId}/certidao-tj")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<CertidaoTJResponse> gerarCertidaoTJ(@PathVariable Long processoId,
                                                              Authentication authentication) {
        enforce(authentication, "juiz_gabinete_certidao_tj");
        return ResponseEntity.status(HttpStatus.CREATED).body(certidaoTransitoJulgadoService.gerarAutomatica(processoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
