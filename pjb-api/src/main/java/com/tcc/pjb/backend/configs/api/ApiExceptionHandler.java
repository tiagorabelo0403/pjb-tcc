package com.tcc.pjb.backend.configs.api;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import com.tcc.pjb.backend.core.moderation.ContentBlockedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.adapter.factory.PJeAdapterNotFoundException;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitExceededException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalConstraintViolationException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRevisionConflictException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionRejectedException;
import com.tcc.pjb.backend.service.exception.ErroDeTetoException;
import com.tcc.pjb.backend.service.exception.ErroTerritorialException;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.sse.TooManySseConnectionsException;
import com.tcc.pjb.backend.service.upload.TooManyUploadsException;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoTosNotAcceptedException;
import com.tcc.pjb.backend.service.exception.EquipeNaoSelecionadaException;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialDomainSupport;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialUnsupportedDomainException;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialFrontendContractService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialApiObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialApiObservabilityService observabilityService;

    public ApiExceptionHandler(ObjectProvider<CalculoJudicialFrontendContractService> frontendContractService,
                               ObjectProvider<CalculoJudicialApiObservabilityService> observabilityService) {
        this.frontendContractService = frontendContractService.getIfAvailable();
        this.observabilityService = observabilityService.getIfAvailable();
    }

    @ExceptionHandler(CapabilityRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleCapabilityRateLimit(CapabilityRateLimitExceededException ex, HttpServletRequest request) {
        var d = ex.getDecision();

        Map<String, Object> extra = new HashMap<>();
        extra.put("error", "RATE_LIMIT_EXCEEDED");
        extra.put("domain", ex.getDomain().name());
        extra.put("capability", ex.getCapability());
        extra.put("version", ex.getVersion() != null ? ex.getVersion().name() : null);
        extra.put("windowSeconds", d.windowSeconds());
        extra.put("costTokens", d.costTokens());
        extra.put("retryAfterSeconds", d.retryAfterSeconds());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições. Tente novamente em alguns instantes.");
        pd.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        pd.setType(URI.create("https://pjb.local/problems/rate_limit"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
        extra.forEach(pd::setProperty);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", String.valueOf(d.limitTokens()));
        headers.set("X-RateLimit-Remaining", String.valueOf(d.remainingTokens()));
        headers.set("Retry-After", String.valueOf(Math.max(1L, d.retryAfterSeconds())));

        return problemResponse(HttpStatus.TOO_MANY_REQUESTS, pd, request, headers);
    }

    @ExceptionHandler(com.tcc.pjb.backend.core.security.device.download.DownloadBudgetExceededException.class)
    public ResponseEntity<ProblemDetail> handleDownloadBudget(com.tcc.pjb.backend.core.security.device.download.DownloadBudgetExceededException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, safeMessage(ex));
        pd.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        pd.setType(URI.create("https://pjb.local/problems/download_budget"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
        pd.setProperty("error", "DOWNLOAD_BUDGET_EXCEEDED");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(Math.max(1L, ex.getRetryAfterSeconds())));
        return problemResponse(HttpStatus.TOO_MANY_REQUESTS, pd, request, headers);
    }


    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurity(SecurityException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleSpringAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = resolveHttpStatus(ex.getStatusCode());
        String detail = safeResponseStatusReason(ex);
        if (status == HttpStatus.FORBIDDEN) {
            detail = "Acesso negado.";
        } else if (status.is5xxServerError()) {
            detail = "Erro interno.";
        }
        return build(status, normalizeType(status), detail, request, null);
    }

    @ExceptionHandler(AccessDeniedPjbException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedPjbException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.", request, null);
    }

    @ExceptionHandler(CalculoJudicialUnsupportedDomainException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedCalculoDomain(CalculoJudicialUnsupportedDomainException ex, HttpServletRequest request) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("error", "UNSUPPORTED_DOMAIN");
        extra.put("requestedDomain", ex.getRequestedDomain());
        extra.put("normalizedDomain", ex.getNormalizedDomain());
        extra.put("supportedDomains", ex.getSupportedDomains());
        if (ex.getSuggestedDomain() != null) {
            extra.put("domainHint", ex.getSuggestedDomain());
            extra.put("frontendBootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(ex.getSuggestedDomain()));
            extra.put("frontendDomainCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(ex.getSuggestedDomain()));
            if (frontendContractService != null) {
                extra.put("profileCapabilities", frontendContractService.profileCapabilities(null));
            }
        }
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported_domain", "Domínio de cálculo não suportado para esta operação.", request, calculoFrontendExtra(request, extra, ex.getSuggestedDomain()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", safeMessage(ex), request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(ErroDeValidacaoException.class)
    public ResponseEntity<ProblemDetail> handleValidacao(ErroDeValidacaoException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "validation_error", safeMessage(ex), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> extra = calculoFrontendExtra(request, null);
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        extra.put("fieldErrors", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "validation_error", "Dados inválidos.", request, extra);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "constraint_violation", "Dados inválidos.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler({java.util.NoSuchElementException.class, RecursoNaoEncontradoException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "not_found", "Recurso não encontrado.", request, null);
    }

    @ExceptionHandler(RecursoJaExistenteException.class)
    public ResponseEntity<ProblemDetail> handleConflict(RecursoJaExistenteException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "conflict", safeMessage(ex), request, null);
    }

    @ExceptionHandler(ErroDeTetoException.class)
    public ResponseEntity<ProblemDetail> handleErroDeTeto(ErroDeTetoException ex, HttpServletRequest request) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("error", "PROCESSUAL_TETO_VIOLATION");
        extra.put("incidentId", ex.getIdIncidente());
        extra.put("tipo", ex.getTipo() != null ? ex.getTipo().name() : null);
        extra.put("codigo", ex.getCodigoTipo());
        extra.put("fundamentacaoLegal", ex.getFundamentacaoLegal());
        extra.put("competenciaSugerida", ex.getCompetenciaSugerida());
        extra.put("ritoSugerido", ex.getRitoSugerido());
        extra.put("anoReferencia", ex.getAnoReferencia());
        extra.put("salarioMinimoReferencia", ex.getSalarioMinimoReferencia());
        extra.put("limiteLegal", ex.getLimiteLegal());
        extra.put("valorInformado", ex.getValorInformado());
        extra.put("excedente", ex.getExcedente());
        extra.put("percentualExcedente", ex.getPercentualExcedente());
        extra.put("bloqueante", ex.isBloqueante());
        extra.put("calculoDetalhado", ex.getCalculoDetalhado());
        extra.put("sugestaoCorrecao", ex.getSugestaoCorrecao());
        extra.put("proof", ex.getProvaIntegridade());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "processual_teto_violation", safeMessage(ex), request, extra);
    }


    @ExceptionHandler(ErroTerritorialException.class)
    public ResponseEntity<ProblemDetail> handleErroTerritorial(ErroTerritorialException ex, HttpServletRequest request) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("error", "PROCESSUAL_TERRITORIAL_VIOLATION");
        extra.put("incidentId", ex.getIdIncidente());
        extra.put("tipo", ex.getTipo() != null ? ex.getTipo().name() : null);
        extra.put("codigo", ex.getCodigoTipo());
        extra.put("fundamentacaoLegal", ex.getFundamentacaoLegal());
        extra.put("territorialMode", ex.getTerritorialMode());
        extra.put("tribunalCodigoSugerido", ex.getTribunalCodigoSugerido());
        extra.put("unidadeJudiciariaSugerida", ex.getUnidadeJudiciariaSugerida());
        extra.put("comarcaInformada", ex.getComarcaInformada());
        extra.put("comarcaSugerida", ex.getComarcaSugerida());
        extra.put("ufInformada", ex.getUfInformada());
        extra.put("ufSugerida", ex.getUfSugerida());
        extra.put("varaInformada", ex.getVaraInformada());
        extra.put("varaSugerida", ex.getVaraSugerida());
        extra.put("foroInformado", ex.getForoInformado());
        extra.put("foroSugerido", ex.getForoSugerido());
        extra.put("bloqueante", ex.isBloqueante());
        extra.put("alertas", ex.getAlertas());
        extra.put("reviewChecklist", ex.getChecklist());
        extra.put("sugestaoCorrecao", ex.getSugestaoCorrecao());
        extra.put("proof", ex.getProvaIntegridade());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "processual_territorial_violation", safeMessage(ex), request, extra);
    }

    @ExceptionHandler(com.tcc.pjb.backend.configs.security.PasskeyRequiredException.class)
    public ResponseEntity<ProblemDetail> handlePasskeyRequired(
            com.tcc.pjb.backend.configs.security.PasskeyRequiredException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "passkey_required",
                "Autenticação forte por passkey obrigatória para este perfil.", request, null);
    }

    @ExceptionHandler(com.tcc.pjb.backend.integration.serpro.datavalid.CpfSituacaoBloqueadaException.class)
    public ResponseEntity<ProblemDetail> handleCpfBloqueado(com.tcc.pjb.backend.integration.serpro.datavalid.CpfSituacaoBloqueadaException ex, HttpServletRequest request) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("errorCode", ex.getCodigoPjb());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "cpf_situacao_bloqueada",
                "Peticionamento não permitido para o CPF informado.", request, extra);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(RegraNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "business_rule", safeMessage(ex), request, null);
    }

    @ExceptionHandler(PJeAdapterNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleIntegration(PJeAdapterNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_IMPLEMENTED, "integration_not_configured", safeMessage(ex), request, null);
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyInProgress(IdempotencyInProgressException ex, HttpServletRequest request) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("error", "IDEMPOTENCY_IN_PROGRESS");
        extra.put("action", ex.getAction());
        extra.put("requestHash", ex.getRequestHash());
        return build(HttpStatus.CONFLICT, "idempotency_in_progress", "Requisição idêntica em processamento.", request, extra);
    }

    @ExceptionHandler(TooManyUploadsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyUploads(TooManyUploadsException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, safeMessage(ex));
        pd.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        pd.setType(URI.create("https://pjb.local/problems/uploads_busy"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "1");
        return problemResponse(HttpStatus.TOO_MANY_REQUESTS, pd, request, headers);
    }

    @ExceptionHandler(TooManySseConnectionsException.class)
    public ResponseEntity<ProblemDetail> handleTooManySseConnections(TooManySseConnectionsException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Muitas conexões SSE ativas. Feche outras abas e tente novamente.");
        pd.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        pd.setType(URI.create("https://pjb.local/problems/sse_busy"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "1");
        return problemResponse(HttpStatus.TOO_MANY_REQUESTS, pd, request, headers);
    }

  @ExceptionHandler(ContentBlockedException.class)
  public ResponseEntity<ProblemDetail> handleContentBlocked(ContentBlockedException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Mensagem bloqueada pelas regras de uso.");
    pd.setType(URI.create("urn:pjb:content_blocked"));
    pd.setTitle("Conteúdo bloqueado");
    pd.setProperty("reason", ex.reason());
    pd.setProperty("path", request.getRequestURI());
    return problemResponse(HttpStatus.BAD_REQUEST, pd, request, null);
  }

  @ExceptionHandler(AtendimentoTosNotAcceptedException.class)
  public ResponseEntity<ProblemDetail> handleTos(AtendimentoTosNotAcceptedException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, "Aceite os termos do atendimento para continuar.");
    pd.setType(URI.create("urn:pjb:atendimento_tos_required"));
    pd.setTitle("Termos obrigatórios");
    pd.setProperty("requiredVersion", ex.requiredVersion());
    pd.setProperty("tosUrl", ex.tosUrl());
    pd.setProperty("path", request.getRequestURI());
    return problemResponse(HttpStatus.PRECONDITION_REQUIRED, pd, request, null);
  }

  @ExceptionHandler(EquipeNaoSelecionadaException.class)
  public ResponseEntity<ProblemDetail> handleEquipeNaoSelecionada(EquipeNaoSelecionadaException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, safeMessage(ex));
    pd.setType(URI.create("urn:pjb:equipe_required"));
    pd.setTitle("Equipe obrigatória");
    pd.setProperty("error", "EQUIPE_REQUIRED");
    pd.setProperty("requiredHeader", ex.getRequiredHeader());
    if (ex.getCandidateEquipeIds() != null && !ex.getCandidateEquipeIds().isEmpty()) {
      pd.setProperty("candidates", ex.getCandidateEquipeIds());
    }
    pd.setProperty("path", request.getRequestURI());
    RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
    pd.setProperty("timestamp", Instant.now().toString());
    return problemResponse(HttpStatus.PRECONDITION_REQUIRED, pd, request, null);
  }


    @ExceptionHandler(RecursalConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleRecursalConstraint(RecursalConstraintViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "recursal_constraint", safeMessage(ex), request, null);
    }

    @ExceptionHandler(RecursalTransitionRejectedException.class)
    public ResponseEntity<ProblemDetail> handleRecursalTransitionRejected(RecursalTransitionRejectedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "recursal_transition_rejected", safeMessage(ex), request, null);
    }

    @ExceptionHandler(RecursalRevisionConflictException.class)
    public ResponseEntity<ProblemDetail> handleRecursalRevisionConflict(RecursalRevisionConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "recursal_revision_conflict", safeMessage(ex), request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "missing_parameter", "Parametro obrigatorio ausente.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ProblemDetail> handleServletRequestBinding(ServletRequestBindingException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "request_binding_error", "Cabecalho ou parametro obrigatorio ausente.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "Metodo HTTP nao suportado para este recurso.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", "Tipo de conteudo nao suportado.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "malformed_request_body", "Corpo da requisicao invalido.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleAsyncTimeout(AsyncRequestTimeoutException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "request_timeout", "Tempo limite da requisicao excedido.", request, calculoFrontendExtra(request, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        if (response.isCommitted()) {
            return null;
        }
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        String ct = response.getContentType();
        if (ct != null && !ct.startsWith("application/json") && !ct.startsWith("application/problem")) {
            response.reset();
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Erro interno.", request, calculoFrontendExtra(request, null));
    }

    private Map<String, Object> calculoFrontendExtra(HttpServletRequest request, Map<String, Object> seed) {
        return calculoFrontendExtra(request, seed, null);
    }

    private Map<String, Object> calculoFrontendExtra(HttpServletRequest request, Map<String, Object> seed, String domainOverride) {
        Map<String, Object> extra = seed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(seed);
        if (request == null || request.getRequestURI() == null || !request.getRequestURI().startsWith("/api/v1/processual/calculos")) {
            return extra;
        }
        extra.putIfAbsent("frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute());
        extra.putIfAbsent("frontendOfficialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute());
        extra.putIfAbsent("supportedDomains", CalculoJudicialDomainSupport.supportedDomains());
        extra.putIfAbsent("transport", "application/problem+json");
        if (frontendContractService != null) {
            extra.putIfAbsent("contractVersion", frontendContractService.version());
            extra.putIfAbsent("contractFingerprint", frontendContractService.fingerprint());
        }
        String domain = domainOverride != null && !domainOverride.isBlank() ? domainOverride : CalculoJudicialDomainSupport.fromPath(request.getRequestURI());
        if (domain != null) {
            extra.putIfAbsent("calculoDomain", domain);
            extra.putIfAbsent("frontendBootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(domain));
            extra.putIfAbsent("frontendDomainCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(domain));
            if (frontendContractService != null) {
                extra.putIfAbsent("httpStatusMap", frontendContractService.apiContract(domain));
                extra.putIfAbsent("profileCapabilities", frontendContractService.profileCapabilities(null));
            }
        }
        return extra;
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String type, String detail, HttpServletRequest request, Map<String, Object> extra) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create("https://pjb.local/problems/" + type));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        RequestContext.getRequestId().ifPresent(id -> pd.setProperty("requestId", id));
        if (extra != null) {
            extra.forEach(pd::setProperty);
        }
        return problemResponse(status, pd, request, null);
    }


    private ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail pd, HttpServletRequest request, HttpHeaders baseHeaders) {
        HttpHeaders headers = baseHeaders == null ? new HttpHeaders() : new HttpHeaders(baseHeaders);
        if (observabilityService != null) {
            observabilityService.problemHeaders(request).forEach((name, values) -> values.forEach(value -> headers.add(name, value)));
        }
        return ResponseEntity.status(status).headers(headers).body(pd);
    }

    private HttpStatus resolveHttpStatus(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String normalizeType(HttpStatus status) {
        if (status == null) {
            return "internal_error";
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return "bad_request";
        }
        if (status == HttpStatus.FORBIDDEN) {
            return "forbidden";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "not_found";
        }
        if (status == HttpStatus.CONFLICT) {
            return "conflict";
        }
        if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            return "method_not_allowed";
        }
        if (status == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
            return "unsupported_media_type";
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return "rate_limited";
        }
        if (status == HttpStatus.PRECONDITION_REQUIRED) {
            return "precondition_required";
        }
        if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
            return "business_rule";
        }
        if (status.is5xxServerError()) {
            return "internal_error";
        }
        return status.name().toLowerCase(Locale.ROOT);
    }

    private String safeResponseStatusReason(ResponseStatusException ex) {
        if (ex == null) {
            return "Solicitacao invalida.";
        }
        String reason = ex.getReason();
        if (reason == null || reason.isBlank()) {
            HttpStatus status = resolveHttpStatus(ex.getStatusCode());
            if (status == HttpStatus.NOT_FOUND) {
                return "Recurso nao encontrado.";
            }
            if (status == HttpStatus.BAD_REQUEST) {
                return "Solicitacao invalida.";
            }
            if (status == HttpStatus.METHOD_NOT_ALLOWED) {
                return "Metodo HTTP nao suportado para este recurso.";
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                return "Muitas requisicoes. Tente novamente em alguns instantes.";
            }
            return status.is5xxServerError() ? "Erro interno." : status.getReasonPhrase();
        }
        return safeMessage(new IllegalArgumentException(reason));
    }

    private String safeMessage(Exception ex) {
        if (ex == null) return "Solicitação inválida.";
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Solicitação inválida.";
        }
        String normalized = msg.replaceAll("[\r\n\t]+", " ").replaceAll("\\s+", " ").trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        String[] sensitiveMarkers = {
                "exception", "stacktrace", "org.springframework", "java.", "jakarta.",
                "jdbc", "hibernate", "sql", "select ", "insert ", "update ", "delete ",
                "drop ", "alter ", "constraint", "password", "secret", "token", "bearer",
                "authorization", "classpath:", "file:", "/tmp/", "c:\\", " at "
        };
        for (String marker : sensitiveMarkers) {
            if (lower.contains(marker)) {
                return "Solicitação inválida.";
            }
        }
        if (normalized.length() > 200) {
            normalized = normalized.substring(0, 200);
        }
        return normalized;
    }
}
