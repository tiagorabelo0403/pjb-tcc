package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(ProcessoSigiloRlsEnvelopeService.class)
public class PjbProcessoSigiloRlsFilter extends OncePerRequestFilter {

    static final String ATTR_TABLE_NAME = "PJB_PROCESSO_SIGILO_TABLE_NAME";
    static final String ATTR_SIGILO_LEVEL = "PJB_PROCESSO_SIGILO_LEVEL";
    static final String ATTR_REQUIRED_SIGILO_CLEARANCE = "PJB_PROCESSO_SIGILO_REQUIRED_CLEARANCE";
    static final String ATTR_REQUIRED_UNIT_CODE = "PJB_PROCESSO_SIGILO_REQUIRED_UNIT_CODE";
    static final String ATTR_REQUIRED_TRIBUNAL_CODE = "PJB_PROCESSO_SIGILO_REQUIRED_TRIBUNAL_CODE";
    static final String ATTR_RLS_SCOPE_KEY = "PJB_PROCESSO_SIGILO_RLS_SCOPE_KEY";
    static final String ATTR_REQUIRES_STEP_UP = "PJB_PROCESSO_SIGILO_REQUIRES_STEP_UP";
    static final String ATTR_REQUIRES_QUALIFIED_CERTIFICATE = "PJB_PROCESSO_SIGILO_REQUIRES_QUALIFIED_CERTIFICATE";
    static final String ATTR_READ_ONLY_RECOMMENDED = "PJB_PROCESSO_SIGILO_READ_ONLY_RECOMMENDED";

    private static final String HEADER_TABLE_NAME = "X-PJB-Processo-Sigilo-Table";
    private static final String HEADER_SIGILO_LEVEL = "X-PJB-Processo-Sigilo-Level";
    private static final String HEADER_REQUIRED_SIGILO_CLEARANCE = "X-PJB-Processo-Sigilo-Required-Clearance";
    private static final String HEADER_REQUIRED_UNIT_CODE = "X-PJB-Processo-Sigilo-Required-Unit-Code";
    private static final String HEADER_REQUIRED_TRIBUNAL_CODE = "X-PJB-Processo-Sigilo-Required-Tribunal-Code";
    private static final String HEADER_RLS_SCOPE_KEY = "X-PJB-Processo-Sigilo-Rls-Scope";
    private static final String HEADER_REQUIRES_STEP_UP = "X-PJB-Processo-Sigilo-Requires-Step-Up";
    private static final String HEADER_REQUIRES_QUALIFIED_CERTIFICATE = "X-PJB-Processo-Sigilo-Requires-Qualified-Certificate";
    private static final String HEADER_READ_ONLY_RECOMMENDED = "X-PJB-Processo-Sigilo-Read-Only-Recommended";
    private static final String HEADER_CLASSIFICATION = "X-PJB-Processo-Sigilo-Classification";

    private static final Pattern[] PROCESS_ID_PATTERNS = new Pattern[]{
            Pattern.compile("^/api/v1/processual/unificado/(\\d+)/sigilo-inteligente$"),
            Pattern.compile("^/api/v1/processual/unificado/(\\d+)/sigilo-notificacoes$"),
            Pattern.compile("^/api/v1/processos/sigilo/zk/(\\d+)/challenge$")
    };

    private final ProcessoSigiloRlsEnvelopeService processoSigiloRlsEnvelopeService;
    private final PjbProcessoSigiloRlsContext processoSigiloRlsContext;

    public PjbProcessoSigiloRlsFilter(ProcessoSigiloRlsEnvelopeService processoSigiloRlsEnvelopeService,
                                      PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        this.processoSigiloRlsEnvelopeService = Objects.requireNonNull(processoSigiloRlsEnvelopeService, "processoSigiloRlsEnvelopeService");
        this.processoSigiloRlsContext = Objects.requireNonNull(processoSigiloRlsContext, "processoSigiloRlsContext");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveProcessoId(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Long processoId = resolveProcessoId(request.getRequestURI());
        if (processoId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            ProcessoSigiloRlsEnvelope envelope = processoSigiloRlsEnvelopeService.materialize(processoId, request.getMethod());
            PjbProcessoSigiloRlsContext.SessionSettings previous = processoSigiloRlsContext.bind(envelope);
            try {
                Map<String, String> syntheticHeaders = resolveSyntheticHeaders(envelope);
                HttpServletRequest effectiveRequest = syntheticHeaders.isEmpty() ? request : new PjbInstitutionalRoutingAugmentedRequest(request, syntheticHeaders);
                bindRequestAttributes(effectiveRequest, envelope);
                bindResponseHeaders(response, envelope);
                filterChain.doFilter(effectiveRequest, response);
            } finally {
                processoSigiloRlsContext.restore(previous);
            }
        } catch (RuntimeException ex) {
            filterChain.doFilter(request, response);
        }
    }

    private Map<String, String> resolveSyntheticHeaders(ProcessoSigiloRlsEnvelope envelope) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        put(out, HEADER_TABLE_NAME, envelope.tableName());
        put(out, HEADER_SIGILO_LEVEL, envelope.sigiloLevel());
        put(out, HEADER_REQUIRED_SIGILO_CLEARANCE, envelope.requiredSigiloClearance());
        put(out, HEADER_REQUIRED_UNIT_CODE, envelope.requiredUnitCode());
        put(out, HEADER_REQUIRED_TRIBUNAL_CODE, envelope.requiredTribunalCode());
        put(out, HEADER_RLS_SCOPE_KEY, envelope.rlsScopeKey());
        put(out, HEADER_REQUIRES_STEP_UP, Boolean.toString(envelope.requiresStepUp()));
        put(out, HEADER_REQUIRES_QUALIFIED_CERTIFICATE, Boolean.toString(envelope.requiresQualifiedCertificate()));
        put(out, HEADER_READ_ONLY_RECOMMENDED, Boolean.toString(envelope.readOnlyRecommended()));
        if (!envelope.classificationCategories().isEmpty()) {
            put(out, HEADER_CLASSIFICATION, String.join(",", envelope.classificationCategories()));
        }
        return Map.copyOf(out);
    }

    private void bindRequestAttributes(HttpServletRequest request, ProcessoSigiloRlsEnvelope envelope) {
        request.setAttribute(ATTR_TABLE_NAME, envelope.tableName());
        request.setAttribute(ATTR_SIGILO_LEVEL, envelope.sigiloLevel());
        request.setAttribute(ATTR_REQUIRED_SIGILO_CLEARANCE, envelope.requiredSigiloClearance());
        request.setAttribute(ATTR_REQUIRED_UNIT_CODE, envelope.requiredUnitCode());
        request.setAttribute(ATTR_REQUIRED_TRIBUNAL_CODE, envelope.requiredTribunalCode());
        request.setAttribute(ATTR_RLS_SCOPE_KEY, envelope.rlsScopeKey());
        request.setAttribute(ATTR_REQUIRES_STEP_UP, envelope.requiresStepUp());
        request.setAttribute(ATTR_REQUIRES_QUALIFIED_CERTIFICATE, envelope.requiresQualifiedCertificate());
        request.setAttribute(ATTR_READ_ONLY_RECOMMENDED, envelope.readOnlyRecommended());
    }

    private void bindResponseHeaders(HttpServletResponse response, ProcessoSigiloRlsEnvelope envelope) {
        putHeader(response, HEADER_TABLE_NAME, envelope.tableName());
        putHeader(response, HEADER_SIGILO_LEVEL, envelope.sigiloLevel());
        putHeader(response, HEADER_REQUIRED_SIGILO_CLEARANCE, envelope.requiredSigiloClearance());
        putHeader(response, HEADER_REQUIRED_UNIT_CODE, envelope.requiredUnitCode());
        putHeader(response, HEADER_REQUIRED_TRIBUNAL_CODE, envelope.requiredTribunalCode());
        putHeader(response, HEADER_RLS_SCOPE_KEY, envelope.rlsScopeKey());
        putHeader(response, HEADER_REQUIRES_STEP_UP, Boolean.toString(envelope.requiresStepUp()));
        putHeader(response, HEADER_REQUIRES_QUALIFIED_CERTIFICATE, Boolean.toString(envelope.requiresQualifiedCertificate()));
        putHeader(response, HEADER_READ_ONLY_RECOMMENDED, Boolean.toString(envelope.readOnlyRecommended()));
        if (!envelope.classificationCategories().isEmpty()) {
            putHeader(response, HEADER_CLASSIFICATION, String.join(",", envelope.classificationCategories()));
        }
    }

    private Long resolveProcessoId(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return null;
        }
        for (Pattern pattern : PROCESS_ID_PATTERNS) {
            Matcher matcher = pattern.matcher(requestUri);
            if (matcher.matches()) {
                return Long.valueOf(matcher.group(1));
            }
        }
        return null;
    }

    private void put(Map<String, String> target, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        target.put(name, value.trim());
    }

    private void putHeader(HttpServletResponse response, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        response.setHeader(name, value.trim());
    }
}
