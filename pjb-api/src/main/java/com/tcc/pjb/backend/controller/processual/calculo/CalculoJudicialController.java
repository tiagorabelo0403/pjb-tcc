package com.tcc.pjb.backend.controller.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAssistenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendBootstrapResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialTabelaOficialResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialIaFinanceiraCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialIaFinanceiraResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAjuizamentoSignalRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAjuizamentoSignalResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialEconomicReferenceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperiencePreferenceRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperiencePreferenceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CustasProcessuaisCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.TrabalhistaCalculoAvancadoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialAssistenciaService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialDomainSupport;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFacadeService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFrontendCatalogService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFrontendContractService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialPdfDocument;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialWorkspaceService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialTabelaOficialService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialApiObservabilityService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialIaFinanceiraService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialAjuizamentoSignalService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialEconomicReferenceService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialExperiencePreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(value = "/api/v1/processual/calculos", produces = MediaType.APPLICATION_JSON_VALUE)
public class CalculoJudicialController {

    private static final String AUTH_WORKSPACE = "hasAnyRole('CIDADAO','ADVOGADO','DEFENSOR_PUBLICO','PROCURADOR','PROCURADORIA','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','SERVIDOR','SERVIDOR_FORUM','TECNICO_INSTITUCIONAL','CONTADOR_JUDICIAL','CONTADOR','CALCULISTA','PERITO_CONTABIL','JUIZ','MAGISTRADO','DESEMBARGADOR','MINISTRO')";
    private static final String AUTH_TRABALHISTA = "hasAnyRole('CIDADAO','ADVOGADO','DEFENSOR_PUBLICO','PROCURADOR','PROCURADORIA','SERVIDOR','SERVIDOR_FORUM','TECNICO_INSTITUCIONAL','CONTADOR_JUDICIAL','CONTADOR','CALCULISTA','PERITO_CONTABIL','JUIZ','MAGISTRADO','DESEMBARGADOR','MINISTRO')";

    private final CalculoJudicialFacadeService facadeService;
    private final CalculoJudicialWorkspaceService workspaceService;
    private final CalculoJudicialAssistenciaService assistenciaService;
    private final CalculoJudicialFrontendCatalogService frontendCatalogService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialTabelaOficialService tabelaOficialService;
    private final CalculoJudicialIaFinanceiraService iaFinanceiraService;
    private final CalculoJudicialAjuizamentoSignalService ajuizamentoSignalService;
    private final CalculoJudicialEconomicReferenceService economicReferenceService;
    private final CalculoJudicialExperiencePreferenceService experiencePreferenceService;
    private final CapabilityRateLimiter rateLimiter;
    private final CalculoJudicialApiObservabilityService observabilityService;

    public CalculoJudicialController(CalculoJudicialFacadeService facadeService,
                                     CalculoJudicialWorkspaceService workspaceService,
                                     CalculoJudicialAssistenciaService assistenciaService,
                                     CalculoJudicialFrontendCatalogService frontendCatalogService,
                                     CalculoJudicialFrontendContractService frontendContractService,
                                     CalculoJudicialTabelaOficialService tabelaOficialService,
                                     CalculoJudicialIaFinanceiraService iaFinanceiraService,
                                     CalculoJudicialAjuizamentoSignalService ajuizamentoSignalService,
                                     CalculoJudicialEconomicReferenceService economicReferenceService,
                                     CalculoJudicialExperiencePreferenceService experiencePreferenceService,
                                     CapabilityRateLimiter rateLimiter,
                                     CalculoJudicialApiObservabilityService observabilityService) {
        this.facadeService = facadeService;
        this.workspaceService = workspaceService;
        this.assistenciaService = assistenciaService;
        this.frontendCatalogService = frontendCatalogService;
        this.frontendContractService = frontendContractService;
        this.tabelaOficialService = tabelaOficialService;
        this.iaFinanceiraService = iaFinanceiraService;
        this.ajuizamentoSignalService = ajuizamentoSignalService;
        this.economicReferenceService = economicReferenceService;
        this.experiencePreferenceService = experiencePreferenceService;
        this.rateLimiter = rateLimiter;
        this.observabilityService = observabilityService;
    }



    @GetMapping("/catalogo")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialFrontendCatalogResponse> catalogo(@RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                           @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                           @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                           @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                           @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                           @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                           @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_catalogo_frontend", ApiVersion.V1);
        CalculoJudicialFrontendCatalogResponse response = frontendCatalogService.catalog(authentication, perfil, null, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "catalogo", null, response.perfilResolvido());
    }

    @GetMapping("/catalogo/{dominio}")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialFrontendCatalogResponse> catalogoDominio(@PathVariable String dominio,
                                                                                  @RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                                  @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                                  @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                                  @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                                  @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                                  @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                                  @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_catalogo_frontend_dominio", ApiVersion.V1);
        String dominioCanonico = supportedDomain(dominio);
        CalculoJudicialFrontendCatalogResponse response = frontendCatalogService.catalog(authentication, perfil, dominioCanonico, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "catalogo_dominio", dominioCanonico, response.perfilResolvido());
    }

    @GetMapping("/catalogo/{dominio}/bootstrap")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialFrontendBootstrapResponse> bootstrapDominio(@PathVariable String dominio,
                                                                                     @RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                                     @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                                     @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                                     @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                                     @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                                     @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                                     @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_catalogo_frontend_bootstrap", ApiVersion.V1);
        String dominioCanonico = supportedDomain(dominio);
        CalculoJudicialFrontendBootstrapResponse response = frontendCatalogService.bootstrap(authentication, perfil, dominioCanonico, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "bootstrap", dominioCanonico, response.perfilResolvido());
    }

    @GetMapping("/tabelas/oficiais")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialTabelaOficialResponse> tabelasOficiais(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_tabelas_oficiais", ApiVersion.V1);
        return cachedJsonResponse(tabelaOficialService.catalog(null), "tabelas_oficiais", null, null);
    }

    @GetMapping("/tabelas/oficiais/{dominio}")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialTabelaOficialResponse> tabelasOficiaisDominio(@PathVariable String dominio,
                                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_tabelas_oficiais_dominio", ApiVersion.V1);
        String dominioCanonico = supportedDomain(dominio);
        return cachedJsonResponse(tabelaOficialService.catalog(dominioCanonico), "tabelas_oficiais_dominio", dominioCanonico, null);
    }


    @GetMapping("/referencias/economicas")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialEconomicReferenceResponse> referenciasEconomicas(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_referencias_economicas", ApiVersion.V1);
        return cachedJsonResponse(economicReferenceService.current(), "referencias_economicas", null, null);
    }

    @GetMapping("/experiencia/preferencia")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialExperiencePreferenceResponse> carregarPreferenciaExperiencia(@RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                                                      @RequestParam(name = "dominio", required = false) String dominio,
                                                                                                      @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                                                      @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                                                      @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                                                      @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                                                      @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                                                      @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_experience_preference_load", ApiVersion.V1);
        String dominioCanonico = dominio == null || dominio.isBlank() ? null : supportedDomain(dominio);
        return cachedJsonResponse(experiencePreferenceService.resolve(authentication, perfil, dominioCanonico, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem)), "experience_preference", dominioCanonico, perfil);
    }

    @PutMapping(value = "/experiencia/preferencia", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialExperiencePreferenceResponse> salvarPreferenciaExperiencia(@RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                                                     @Valid @RequestBody CalculoJudicialExperiencePreferenceRequest request,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_experience_preference_save", ApiVersion.V1);
        String dominioCanonico = request.domainCode() == null || request.domainCode().isBlank() ? null : supportedDomain(request.domainCode());
        CalculoJudicialExperiencePreferenceRequest normalized = new CalculoJudicialExperiencePreferenceRequest(request.experienceMode(), dominioCanonico, request.ramoDireito(), request.classeProcessual(), request.tipoCausa(), request.perfilEquipe(), request.tribunal(), request.sistemaOrigem(), request.persistForTeam(), request.institutionalPolicy());
        return canonicalJsonResponse(experiencePreferenceService.save(authentication, perfil, normalized), "experience_preference", dominioCanonico);
    }

    @GetMapping("/workspace")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialWorkspaceResponse> workspace(@RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                      @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                      @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                      @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                      @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                      @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                      @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_workspace", ApiVersion.V1);
        CalculoJudicialWorkspaceResponse response = workspaceService.workspace(authentication, perfil, null, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "workspace", null, response.perfilResolvido());
    }

    @GetMapping("/workspace/{dominio}")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialWorkspaceResponse> workspaceDominio(@PathVariable String dominio,
                                                                             @RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                             @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                             @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                             @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                             @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                             @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                             @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_workspace_dominio", ApiVersion.V1);
        String dominioCanonico = supportedDomain(dominio);
        CalculoJudicialWorkspaceResponse response = workspaceService.workspace(authentication, perfil, dominioCanonico, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "workspace_dominio", dominioCanonico, response.perfilResolvido());
    }

    @GetMapping("/workspace/{dominio}/ajuda")
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialWorkspaceCardResponse> ajudaDominio(@PathVariable String dominio,
                                                                             @RequestParam(name = "perfil", required = false) CalculoJudicialSolicitantePerfil perfil,
                                                                             @RequestParam(name = "ramoDireito", required = false) String ramoDireito,
                                                                             @RequestParam(name = "classeProcessual", required = false) String classeProcessual,
                                                                             @RequestParam(name = "tipoCausa", required = false) String tipoCausa,
                                                                             @RequestParam(name = "perfilEquipe", required = false) String perfilEquipe,
                                                                             @RequestParam(name = "tribunal", required = false) String tribunal,
                                                                             @RequestParam(name = "sistemaOrigem", required = false) String sistemaOrigem,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_workspace_ajuda", ApiVersion.V1);
        String dominioCanonico = supportedDomain(dominio);
        CalculoJudicialWorkspaceCardResponse response = workspaceService.workspaceCard(authentication, perfil, dominioCanonico, experienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem));
        return cachedJsonResponse(response, "workspace_ajuda", dominioCanonico, perfil);
    }

    @PostMapping(value = "/assistente/trabalhista-clt", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_TRABALHISTA)
    public ResponseEntity<CalculoJudicialAssistenciaResponse> orientarTrabalhista(@RequestBody(required = false) TrabalhistaCalculoAvancadoRequest request,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_assistente_trabalhista", ApiVersion.V1);
        return canonicalJsonResponse(assistenciaService.orientarTrabalhista(request, authentication), "assistente", "TRABALHISTA_CLT");
    }

    @PostMapping(value = "/assistente/fazenda-tributario", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialAssistenciaResponse> orientarFazenda(@RequestBody(required = false) FazendaTributarioCalculoAvancadoRequest request,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_assistente_fazenda", ApiVersion.V1);
        return canonicalJsonResponse(assistenciaService.orientarFazenda(request, authentication), "assistente", "FAZENDA_TRIBUTARIO");
    }


    @PostMapping(value = "/assistente/custas-processuais", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialAssistenciaResponse> orientarCustas(@RequestBody(required = false) CustasProcessuaisCalculoAvancadoRequest request,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_assistente_custas", ApiVersion.V1);
        return canonicalJsonResponse(assistenciaService.orientarCustas(request, authentication), "assistente", "CUSTAS_PROCESSUAIS");
    }


    @PostMapping(value = "/assistente/federal-previdenciario-cjf", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialAssistenciaResponse> orientarFederalPrevidenciario(@RequestBody(required = false) FederalPrevidenciarioCjfCalculoAvancadoRequest request,
                                                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_assistente_federal_previdenciario", ApiVersion.V1);
        return canonicalJsonResponse(assistenciaService.orientarFederalPrevidenciario(request, authentication), "assistente", "FEDERAL_PREVIDENCIARIO_CJF");
    }



    @PostMapping(value = "/ia/financeira/executar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialIaFinanceiraResponse> executarIaFinanceira(@Valid @RequestBody CalculoJudicialIaFinanceiraCommandRequest request,
                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_executar", ApiVersion.V1);
        return canonicalJsonResponse(iaFinanceiraService.executar(request, authentication), "ia_financeira", request.dominio());
    }



    @PostMapping(value = "/ia/financeira/sinalizar-ajuizamento", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialAjuizamentoSignalResponse> sinalizarAjuizamento(@RequestBody(required = false) CalculoJudicialAjuizamentoSignalRequest request,
                                                                                          Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_sinalizar_ajuizamento", ApiVersion.V1);
        return canonicalJsonResponse(ajuizamentoSignalService.analisar(request, authentication), "ia_financeira_live_signal", null);
    }

    @PostMapping(value = "/ia/financeira/trabalhista-clt", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_TRABALHISTA)
    public ResponseEntity<CalculoJudicialIaFinanceiraResponse> executarIaFinanceiraTrabalhista(@RequestBody(required = false) TrabalhistaCalculoAvancadoRequest request,
                                                                                               Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_trabalhista", ApiVersion.V1);
        return canonicalJsonResponse(iaFinanceiraService.executarTrabalhista(request, authentication), "ia_financeira", "TRABALHISTA_CLT");
    }

    @PostMapping(value = "/ia/financeira/fazenda-tributario", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialIaFinanceiraResponse> executarIaFinanceiraFazenda(@RequestBody(required = false) FazendaTributarioCalculoAvancadoRequest request,
                                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_fazenda", ApiVersion.V1);
        return canonicalJsonResponse(iaFinanceiraService.executarFazenda(request, authentication), "ia_financeira", "FAZENDA_TRIBUTARIO");
    }

    @PostMapping(value = "/ia/financeira/custas-processuais", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialIaFinanceiraResponse> executarIaFinanceiraCustas(@RequestBody(required = false) CustasProcessuaisCalculoAvancadoRequest request,
                                                                                          Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_custas", ApiVersion.V1);
        return canonicalJsonResponse(iaFinanceiraService.executarCustas(request, authentication), "ia_financeira", "CUSTAS_PROCESSUAIS");
    }

    @PostMapping(value = "/ia/financeira/federal-previdenciario-cjf", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialIaFinanceiraResponse> executarIaFinanceiraFederalPrevidenciario(@RequestBody(required = false) FederalPrevidenciarioCjfCalculoAvancadoRequest request,
                                                                                                         Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_ia_financeira_federal_previdenciario", ApiVersion.V1);
        return canonicalJsonResponse(iaFinanceiraService.executarFederalPrevidenciario(request, authentication), "ia_financeira", "FEDERAL_PREVIDENCIARIO_CJF");
    }

    @PostMapping(value = "/trabalhista-clt", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_TRABALHISTA)
    public ResponseEntity<CalculoJudicialResumoResponse> calcularTrabalhista(@Valid @RequestBody TrabalhistaCalculoAvancadoRequest request,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_trabalhista_clt", ApiVersion.V1);
        return canonicalJsonResponse(facadeService.calcularTrabalhista(request, authentication), "json", "TRABALHISTA_CLT");
    }

    @PostMapping(value = "/trabalhista-clt/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(AUTH_TRABALHISTA)
    public ResponseEntity<byte[]> calcularTrabalhistaPdf(@Valid @RequestBody TrabalhistaCalculoAvancadoRequest request,
                                                         Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_trabalhista_clt_pdf", ApiVersion.V1);
        CalculoJudicialPdfDocument pdf = facadeService.calcularTrabalhistaPdf(request, authentication);
        return pdfResponse(pdf.bytes(), pdf.filename(), "TRABALHISTA_CLT");
    }

    @PostMapping(value = "/fazenda-tributario", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialResumoResponse> calcularFazenda(@Valid @RequestBody FazendaTributarioCalculoAvancadoRequest request,
                                                                         Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_fazenda_tributario", ApiVersion.V1);
        return canonicalJsonResponse(facadeService.calcularFazenda(request, authentication), "json", "FAZENDA_TRIBUTARIO");
    }

    @PostMapping(value = "/fazenda-tributario/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<byte[]> calcularFazendaPdf(@Valid @RequestBody FazendaTributarioCalculoAvancadoRequest request,
                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_fazenda_tributario_pdf", ApiVersion.V1);
        CalculoJudicialPdfDocument pdf = facadeService.calcularFazendaPdf(request, authentication);
        return pdfResponse(pdf.bytes(), pdf.filename(), "FAZENDA_TRIBUTARIO");
    }


    @PostMapping(value = "/custas-processuais", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialResumoResponse> calcularCustas(@Valid @RequestBody CustasProcessuaisCalculoAvancadoRequest request,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_custas_processuais", ApiVersion.V1);
        return canonicalJsonResponse(facadeService.calcularCustas(request, authentication), "json", "CUSTAS_PROCESSUAIS");
    }

    @PostMapping(value = "/custas-processuais/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<byte[]> calcularCustasPdf(@Valid @RequestBody CustasProcessuaisCalculoAvancadoRequest request,
                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_custas_processuais_pdf", ApiVersion.V1);
        CalculoJudicialPdfDocument pdf = facadeService.calcularCustasPdf(request, authentication);
        return pdfResponse(pdf.bytes(), pdf.filename(), "CUSTAS_PROCESSUAIS");
    }



    @PostMapping(value = "/federal-previdenciario-cjf", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<CalculoJudicialResumoResponse> calcularFederalPrevidenciario(@Valid @RequestBody FederalPrevidenciarioCjfCalculoAvancadoRequest request,
                                                                                       Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_federal_previdenciario_cjf", ApiVersion.V1);
        return canonicalJsonResponse(facadeService.calcularFederalPrevidenciario(request, authentication), "json", "FEDERAL_PREVIDENCIARIO_CJF");
    }

    @PostMapping(value = "/federal-previdenciario-cjf/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(AUTH_WORKSPACE)
    public ResponseEntity<byte[]> calcularFederalPrevidenciarioPdf(@Valid @RequestBody FederalPrevidenciarioCjfCalculoAvancadoRequest request,
                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "pjb_calculo_federal_previdenciario_cjf_pdf", ApiVersion.V1);
        CalculoJudicialPdfDocument pdf = facadeService.calcularFederalPrevidenciarioPdf(request, authentication);
        return pdfResponse(pdf.bytes(), pdf.filename(), "FEDERAL_PREVIDENCIARIO_CJF");
    }


    private <T> ResponseEntity<T> cachedJsonResponse(T body, String scope, String dominio, CalculoJudicialSolicitantePerfil perfil) {
        HttpHeaders headers = new HttpHeaders();
        frontendContractService.frontendResponseHeaders(scope, dominio, perfil).forEach(headers::add);
        observabilityService.apply(headers, observabilityService.canonical(scope, dominio));
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private <T> ResponseEntity<T> canonicalJsonResponse(T body, String operation, String dominio) {
        HttpHeaders headers = new HttpHeaders();
        observabilityService.apply(headers, observabilityService.canonical(operation, dominio));
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename, String dominio) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        headers.setCacheControl("no-store, no-cache, max-age=0, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-PJB-Calculation-File-Name", filename);
        observabilityService.apply(headers, observabilityService.canonical("pdf", dominio));
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-Robots-Tag", "noindex, nofollow, noarchive");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private CalculoJudicialExperienceContext experienceContext(String ramoDireito,
                                                               String classeProcessual,
                                                               String tipoCausa,
                                                               String perfilEquipe,
                                                               String tribunal,
                                                               String sistemaOrigem) {
        return new CalculoJudicialExperienceContext(ramoDireito, classeProcessual, tipoCausa, perfilEquipe, tribunal, sistemaOrigem);
    }

    private String supportedDomain(String dominio) {
        return CalculoJudicialDomainSupport.requireSupported(dominio);
    }
}
