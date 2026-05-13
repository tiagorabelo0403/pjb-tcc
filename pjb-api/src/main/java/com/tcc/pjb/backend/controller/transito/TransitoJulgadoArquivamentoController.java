package com.tcc.pjb.backend.controller.transito;

import com.tcc.pjb.backend.core.transito.ExecutionPanelAssemblerService;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoAtoExecutivoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoConstricaoPatrimonialRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoContingenciaConstricaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoConsolidacaoFechamentoExecutivoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoCicloLeilaoExpropriatorioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoCumprimentoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoFundamentacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoGovernancaExpropriacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoHomologacaoExpropriacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoIncidenteExecutivoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoIntegracaoConstricaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoLiquidacaoProdutoExpropriacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoMotivoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoReconciliacaoConstricaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoSatisfacaoTerminalRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.TransitoVinculoArquivamentoTerminalRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.transito.ExecutionPanelResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.transito.surface.TransitoJulgadoSurfaceFacadeService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processo/transito-julgado")
@Validated
public class TransitoJulgadoArquivamentoController {

    private final TransitoJulgadoSurfaceFacadeService facadeService;
    private final ExecutionPanelAssemblerService executionPanelAssemblerService;
    private final CapabilityRateLimiter rateLimiter;

    public TransitoJulgadoArquivamentoController(TransitoJulgadoSurfaceFacadeService facadeService,
                                                 ExecutionPanelAssemblerService executionPanelAssemblerService,
                                                 CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.executionPanelAssemblerService = executionPanelAssemblerService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/processos/{processoId}/certidao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> certificar(@PathVariable Long processoId,
                                                            @Valid @RequestBody TransitoFundamentacaoRequest request,
                                                            Authentication authentication) {
        enforce(authentication, "transito_julgado_certificar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.certificar(processoId, request.fundamentacao()));
    }

    @PostMapping("/processos/{processoId}/cumprimento")
    @PreAuthorize("hasAnyRole('ADVOGADO','SERVIDOR','SERVIDOR_FORUM','PROCURADOR')")
    public ResponseEntity<SurfaceActionResponse> iniciarCumprimento(@PathVariable Long processoId,
                                                                    @Valid @RequestBody TransitoCumprimentoRequest request,
                                                                    Authentication authentication) {
        enforce(authentication, "transito_julgado_cumprimento");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.iniciarCumprimento(processoId, request.tipoCumprimento(), request.valorExequendo()));
    }

    @PostMapping("/processos/{processoId}/incidente-executivo")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> instaurarIncidente(@PathVariable Long processoId,
                                                                    @Valid @RequestBody TransitoIncidenteExecutivoRequest request,
                                                                    Authentication authentication) {
        enforce(authentication, "transito_julgado_incidente_executivo");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.instaurarIncidente(processoId, request.incidente(), request.fundamentacao(), request.valorGarantia()));
    }

    @PostMapping("/processos/{processoId}/ato-executivo")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> praticarAtoExecutivo(@PathVariable Long processoId,
                                                                      @Valid @RequestBody TransitoAtoExecutivoRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "transito_julgado_ato_executivo");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.praticarAtoExecutivo(processoId, request.ato(), request.detalhe(), request.valorOperacao()));
    }

    @PostMapping("/processos/{processoId}/constricao-patrimonial")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> praticarConstricaoPatrimonial(@PathVariable Long processoId,
                                                                                @Valid @RequestBody TransitoConstricaoPatrimonialRequest request,
                                                                                Authentication authentication) {
        enforce(authentication, "transito_julgado_constricao_patrimonial");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.praticarConstricao(processoId, request.ato(), request.bem(), request.detalhe(), request.convenio(), request.valorOperacao()));
    }

    @PostMapping("/processos/{processoId}/integracao-constricao")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> integrarConstricao(@PathVariable Long processoId,
                                                                     @Valid @RequestBody TransitoIntegracaoConstricaoRequest request,
                                                                     Authentication authentication) {
        enforce(authentication, "transito_julgado_integracao_constricao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.integrarConstricao(processoId, request.ato(), request.bem(), request.convenio(), request.referenciaExterna(), request.valorOperacao()));
    }

    @PostMapping("/processos/{processoId}/governanca-expropriacao")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> governarExpropriacao(@PathVariable Long processoId,
                                                                       @Valid @RequestBody TransitoGovernancaExpropriacaoRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "transito_julgado_governanca_expropriacao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.governarExpropriacao(processoId, request.ato(), request.bem(), request.modalidade(), request.valorReferencia()));
    }

    @PostMapping("/processos/{processoId}/reconciliacao-constricao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO','PROCURADOR')")
    public ResponseEntity<SurfaceActionResponse> reconciliarConstricao(@PathVariable Long processoId,
                                                                        @Valid @RequestBody TransitoReconciliacaoConstricaoRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "transito_julgado_reconciliacao_constricao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.reconciliarConstricao(processoId, request.bem(), request.convenio(), request.statusExterno(), request.referenciaExterna(), request.valorOperacao()));
    }

    @PostMapping("/processos/{processoId}/ciclo-leilao-expropriatorio")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> planejarCicloLeilao(@PathVariable Long processoId,
                                                                      @Valid @RequestBody TransitoCicloLeilaoExpropriatorioRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "transito_julgado_ciclo_leilao_expropriatorio");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.planejarCicloLeilao(processoId, request.ato(), request.bem(), request.modalidade(), request.tentativa(), request.valorReferencia()));
    }

    @PostMapping("/processos/{processoId}/contingencia-constricao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO','PROCURADOR')")
    public ResponseEntity<SurfaceActionResponse> deflagrarContingenciaConstricao(@PathVariable Long processoId,
                                                                                   @Valid @RequestBody TransitoContingenciaConstricaoRequest request,
                                                                                   Authentication authentication) {
        enforce(authentication, "transito_julgado_contingencia_constricao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.deflagrarContingencia(processoId, request.bem(), request.convenio(), request.statusExterno(), request.referenciaExterna(), request.valorOperacao()));
    }

    @PostMapping("/processos/{processoId}/homologacao-expropriacao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> homologarExpropriacao(@PathVariable Long processoId,
                                                                        @Valid @RequestBody TransitoHomologacaoExpropriacaoRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "transito_julgado_homologacao_expropriacao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.homologarExpropriacao(processoId, request.ato(), request.bem(), request.modalidade(), request.adquirente(), request.valorArrematacao()));
    }

    @PostMapping("/processos/{processoId}/liquidacao-produto-expropriacao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','CONTADOR_JUDICIAL','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> liquidarProdutoExpropriacao(@PathVariable Long processoId,
                                                                               @Valid @RequestBody TransitoLiquidacaoProdutoExpropriacaoRequest request,
                                                                               Authentication authentication) {
        enforce(authentication, "transito_julgado_liquidacao_produto_expropriacao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.liquidarProdutoExpropriacao(processoId, request.bem(), request.modoProduto(), request.preferencia(), request.subrogacao(), request.valorProduto(), request.saldoExecutado(), request.saldoCredor()));
    }

    @PostMapping("/processos/{processoId}/consolidacao-fechamento-executivo")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','CONTADOR_JUDICIAL','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> consolidarFechamentoExecutivo(@PathVariable Long processoId,
                                                                                @Valid @RequestBody TransitoConsolidacaoFechamentoExecutivoRequest request,
                                                                                Authentication authentication) {
        enforce(authentication, "transito_julgado_consolidacao_fechamento_executivo");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.consolidarFechamentoExecutivo(processoId, request.modoFechamento(), request.preferencia(), request.subrogacao(), request.percentualSatisfeito(), request.saldoRemanescente(), request.motivo()));
    }

    @PostMapping("/processos/{processoId}/satisfacao-terminal")
    @PreAuthorize("hasAnyRole('ADVOGADO','PROCURADOR','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> registrarSatisfacaoTerminal(@PathVariable Long processoId,
                                                                              @Valid @RequestBody TransitoSatisfacaoTerminalRequest request,
                                                                              Authentication authentication) {
        enforce(authentication, "transito_julgado_satisfacao_terminal");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarSatisfacaoTerminal(processoId, request.modo(), request.percentualSatisfeito(), request.saldoRemanescente(), request.fundamento()));
    }

    @PostMapping("/processos/{processoId}/vinculo-arquivamento-terminal")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<SurfaceActionResponse> vincularArquivamentoTerminal(@PathVariable Long processoId,
                                                                               @Valid @RequestBody TransitoVinculoArquivamentoTerminalRequest request,
                                                                               Authentication authentication) {
        enforce(authentication, "transito_julgado_vinculo_arquivamento_terminal");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.vincularArquivamentoTerminal(processoId, request.operacao(), request.disposicaoTerminal(), request.motivo(), request.percentualSatisfeito(), request.saldoRemanescente()));
    }

    @PostMapping("/processos/{processoId}/arquivamento")
    @PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceActionResponse> arquivar(@PathVariable Long processoId,
                                                          @Valid @RequestBody TransitoMotivoRequest request,
                                                          Authentication authentication) {
        enforce(authentication, "transito_julgado_arquivamento");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.arquivar(processoId, request.motivo()));
    }

    @PostMapping("/processos/{processoId}/desarquivamento")
    @PreAuthorize("hasAnyRole('JUIZ','MAGISTRADO','SERVIDOR_FORUM')")
    public ResponseEntity<SurfaceActionResponse> desarquivar(@PathVariable Long processoId,
                                                             @Valid @RequestBody TransitoMotivoRequest request,
                                                             Authentication authentication) {
        enforce(authentication, "transito_julgado_desarquivamento");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.desarquivar(processoId, request.motivo()));
    }

    @GetMapping("/processos/{processoId}/efeitos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> efeitos(@PathVariable Long processoId,
                                                           Authentication authentication) {
        enforce(authentication, "transito_julgado_efeitos");
        return ResponseEntity.ok(facadeService.efeitos(processoId));
    }

    @GetMapping("/processos/{processoId}/malha-executiva")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> malhaExecutiva(@PathVariable Long processoId,
                                                                  Authentication authentication) {
        enforce(authentication, "transito_julgado_malha_executiva");
        return ResponseEntity.ok(facadeService.malhaExecutiva(processoId));
    }

    @GetMapping("/processos/{processoId}/snapshot-executivo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> snapshotExecutivo(@PathVariable Long processoId,
                                                                     Authentication authentication) {
        enforce(authentication, "transito_julgado_snapshot_executivo");
        return ResponseEntity.ok(facadeService.snapshotExecutivo(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-executivo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExecutionPanelResponse> painelExecutivo(@PathVariable Long processoId,
                                                                  Authentication authentication) {
        enforce(authentication, "transito_julgado_painel_executivo");
        return ResponseEntity.ok(executionPanelAssemblerService.assemble(processoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
