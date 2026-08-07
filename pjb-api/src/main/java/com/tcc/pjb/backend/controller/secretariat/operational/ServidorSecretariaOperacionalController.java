package com.tcc.pjb.backend.controller.secretariat.operational;


import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBaixaOrigemRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBalcaoVirtualMilitarRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaConclusaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaCorregedoriaEleitoralRequest;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaExecucaoTrabalhistaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaInspecaoCorregedoriaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaMandadoCitacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaMidiaProcessualRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPautaColegiadaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPesquisaEleitoralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPlantaoMilitarRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoAcordaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoPautaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaSustentacaoOralRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoMaterializacaoRequest;
import com.tcc.pjb.backend.model.dto.secretariat.oficial.SecretariaOficialCumprimentoReclassificacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaJuntadaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.secretariat.surface.SecretariatOperationalSurfaceFacadeService;
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
@RequestMapping(OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE)
@Validated
public class ServidorSecretariaOperacionalController {

    private static final String ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM')";
    private final SecretariatOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public ServidorSecretariaOperacionalController(SecretariatOperationalSurfaceFacadeService facadeService,
                                                   CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_SNAPSHOT)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "secretaria_operacional_snapshot");
        return ResponseEntity.ok(facadeService.snapshotOperacional());
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_JUNTADA)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> realizarJuntada(@PathVariable Long processoId,
                                                                 @Valid @RequestBody SecretariaJuntadaRequest request,
                                                                 Authentication authentication) {
        enforce(authentication, "secretaria_operacional_juntada");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.realizarJuntada(processoId, request.tipoDocumento(), request.descricao(), request.origem()));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_INTIMACAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> expedirIntimacao(@PathVariable Long processoId,
                                                                  @Valid @RequestBody SecretariaIntimacaoRequest request,
                                                                  Authentication authentication) {
        enforce(authentication, "secretaria_operacional_intimacao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.expedirIntimacao(
                        processoId,
                        request.destinatario(),
                        request.conteudo(),
                        request.prazo(),
                        request.oficialId(),
                        request.reativarOficial(),
                        request.origemOperacional(),
                        request.fundamentoOperacional(),
                        request.observacaoOperacional(),
                        request.manterRetornoForumAberto()
                ));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_MANDADO_CITACAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> expedirMandadoCitacao(@PathVariable Long processoId,
                                                                        @Valid @RequestBody SecretariaMandadoCitacaoRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "secretaria_operacional_mandado_citacao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.expedirMandadoCitacao(
                        processoId,
                        request.oficialId(),
                        request.enderecoCitacao(),
                        request.observacaoOperacional()
                ));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_SERVIDOR_REATRIBUICAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> reatribuirCargaPorAfastamento(@PathVariable Long servidorId,
                                                                               Authentication authentication) {
        enforce(authentication, "secretaria_operacional_reatribuir_carga_afastamento");
        return ResponseEntity.ok(facadeService.reatribuirCargaPorAfastamento(servidorId));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_CONCLUSAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> conclusaoParaDespacho(@PathVariable Long processoId,
                                                                       @Valid @RequestBody SecretariaConclusaoRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "secretaria_operacional_conclusao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.conclusaoParaDespacho(processoId, request.motivoConclusao()));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_QUEUE_SANEAMENTO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> saneamentoFila(@RequestParam String queueCode,
                                                                @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limite,
                                                                Authentication authentication) {
        enforce(authentication, "secretaria_operacional_saneamento");
        return ResponseEntity.ok(facadeService.saneamentoFila(queueCode, limite));
    }
    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURES)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> oficialCumprimentos(@RequestParam String inboxKey,
                                                                       @RequestParam(defaultValue = "24") @Min(1) @Max(80) int limit,
                                                                       Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos");
        return ResponseEntity.ok(facadeService.oficialCumprimentos(inboxKey, limit));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_RECLASSIFY)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> reclassificarOficialCumprimento(@PathVariable Long deskWorkItemId,
                                                                                  @Valid @RequestBody(required = false) SecretariaOficialCumprimentoReclassificacaoRequest request,
                                                                                  Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos_reclassificar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.reclassificarOficialCumprimento(deskWorkItemId, request));
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_NEXT_PROVIDENCE)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> proximaProvidenciaOficialCumprimento(@PathVariable Long deskWorkItemId,
                                                                                         Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos_proxima_providencia");
        return ResponseEntity.ok(facadeService.proximaProvidenciaOficialCumprimento(deskWorkItemId));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_MATERIALIZE_ACT)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> materializarAtoOficialCumprimento(@PathVariable Long deskWorkItemId,
                                                                                    @Valid @RequestBody(required = false) SecretariaOficialCumprimentoMaterializacaoRequest request,
                                                                                    Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos_materializar_ato");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.materializarAtoOficialCumprimento(deskWorkItemId, request));
    }


    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PAUTA)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> incluirEmPautaColegiada(@PathVariable Long processoId,
                                                                          @Valid @RequestBody SecretariaPautaColegiadaRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "secretaria_operacional_colegiada_pauta");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.incluirEmPautaColegiada(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> publicarPautaColegiada(@PathVariable Long julgamentoId,
                                                                        @Valid @RequestBody(required = false) SecretariaPublicacaoPautaRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "secretaria_operacional_colegiada_publicacao_pauta");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.publicarPautaColegiada(julgamentoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_SUSTENTACAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarSustentacaoOralColegiada(@PathVariable Long julgamentoId,
                                                                                    @Valid @RequestBody SecretariaSustentacaoOralRequest request,
                                                                                    Authentication authentication) {
        enforce(authentication, "secretaria_operacional_colegiada_sustentacao_oral");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarSustentacaoOralColegiada(julgamentoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_ACORDAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> publicarAcordaoColegiado(@PathVariable Long julgamentoId,
                                                                          @Valid @RequestBody(required = false) SecretariaPublicacaoAcordaoRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "secretaria_operacional_colegiada_publicacao_acordao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.publicarAcordaoColegiado(julgamentoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_BAIXA)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> baixarOrigemColegiada(@PathVariable Long julgamentoId,
                                                                       @Valid @RequestBody(required = false) SecretariaBaixaOrigemRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "secretaria_operacional_colegiada_baixa_origem");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.baixarOrigemColegiada(julgamentoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_CORREGEDORIA)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> instaurarProcedimentoCorregedoriaEleitoral(@PathVariable Long processoId,
                                                                                            @Valid @RequestBody SecretariaCorregedoriaEleitoralRequest request,
                                                                                            Authentication authentication) {
        enforce(authentication, "secretaria_operacional_eleitoral_corregedoria");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.instaurarProcedimentoCorregedoriaEleitoral(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_INSPECAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarInspecaoCorregedoriaEleitoral(@PathVariable Long processoId,
                                                                                         @Valid @RequestBody SecretariaInspecaoCorregedoriaRequest request,
                                                                                         Authentication authentication) {
        enforce(authentication, "secretaria_operacional_eleitoral_inspecao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarInspecaoCorregedoriaEleitoral(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_PESQUISA)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> validarPesquisaEleitoral(@PathVariable Long processoId,
                                                                          @Valid @RequestBody SecretariaPesquisaEleitoralRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "secretaria_operacional_eleitoral_pesquisa");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.validarPesquisaEleitoral(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_RECEBIMENTO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> receberMidiaProcessualTrabalhista(@PathVariable Long processoId,
                                                                                    @Valid @RequestBody SecretariaMidiaProcessualRequest request,
                                                                                    Authentication authentication) {
        enforce(authentication, "secretaria_operacional_trabalhista_midia_recebimento");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.receberMidiaProcessualTrabalhista(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_DISPONIBILIZACAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> disponibilizarMidiaProcessualTrabalhista(@PathVariable Long processoId,
                                                                                           @Valid @RequestBody SecretariaMidiaProcessualRequest request,
                                                                                           Authentication authentication) {
        enforce(authentication, "secretaria_operacional_trabalhista_midia_disponibilizacao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.disponibilizarMidiaProcessualTrabalhista(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_EXECUCAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> impulsionarExecucaoTrabalhista(@PathVariable Long processoId,
                                                                                 @Valid @RequestBody SecretariaExecucaoTrabalhistaRequest request,
                                                                                 Authentication authentication) {
        enforce(authentication, "secretaria_operacional_trabalhista_execucao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.impulsionarExecucaoTrabalhista(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_MILITARY_PLANTAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> receberPlantaoMilitar(@PathVariable Long processoId,
                                                                       @Valid @RequestBody SecretariaPlantaoMilitarRequest request,
                                                                       Authentication authentication) {
        enforce(authentication, "secretaria_operacional_militar_plantao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.receberPlantaoMilitar(processoId, request));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_MILITARY_BALCAO)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarBalcaoVirtualMilitar(@PathVariable Long processoId,
                                                                               @Valid @RequestBody SecretariaBalcaoVirtualMilitarRequest request,
                                                                               Authentication authentication) {
        enforce(authentication, "secretaria_operacional_militar_balcao_virtual");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarBalcaoVirtualMilitar(processoId, request));
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWERS)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> gavetasOficialCumprimento(@RequestParam String inboxKey,
                                                                              @RequestParam(defaultValue = "24") @Min(1) @Max(80) int limit,
                                                                              Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos_gavetas");
        return ResponseEntity.ok(facadeService.gavetasOficialCumprimento(inboxKey, limit));
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWER_DETAIL)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> detalheGavetaOficialCumprimento(@RequestParam String inboxKey,
                                                                                    @RequestParam String drawerKey,
                                                                                    @RequestParam(defaultValue = "32") @Min(1) @Max(120) int limit,
                                                                                    Authentication authentication) {
        enforce(authentication, "secretaria_operacional_oficial_cumprimentos_gaveta_detalhe");
        return ResponseEntity.ok(facadeService.detalheGavetaOficialCumprimento(inboxKey, drawerKey, limit));
    }


    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, capability, ApiVersion.V1);
    }
}
