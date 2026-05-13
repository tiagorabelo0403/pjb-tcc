package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalRequestContextKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundle;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbInstitutionalDataPlaneFilter extends OncePerRequestFilter {

    static final String ATTR_PANEL_CODE = InstitutionalRequestContextKeys.ATTR_PANEL_CODE;
    static final String ATTR_LANDING_PATH = InstitutionalRequestContextKeys.ATTR_LANDING_PATH;
    static final String ATTR_DATA_PLANE_KEY = InstitutionalRequestContextKeys.ATTR_DATA_PLANE_KEY;
    static final String ATTR_READ_REPLICA = InstitutionalRequestContextKeys.ATTR_READ_REPLICA;
    static final String ATTR_NOMINATION_ID = InstitutionalRequestContextKeys.ATTR_NOMINATION_ID;
    static final String ATTR_READY_FOR_PANEL = InstitutionalRequestContextKeys.ATTR_READY_FOR_PANEL;
    static final String ATTR_PROFILE_STATE = InstitutionalRequestContextKeys.ATTR_PROFILE_STATE;
    static final String ATTR_TARGET_ENVIRONMENT = InstitutionalRequestContextKeys.ATTR_TARGET_ENVIRONMENT;
    static final String ATTR_PANEL_PROVISIONING_COMPLETE = InstitutionalRequestContextKeys.ATTR_PANEL_PROVISIONING_COMPLETE;
    static final String ATTR_SHARED_EXPERIENCE_READY = InstitutionalRequestContextKeys.ATTR_SHARED_EXPERIENCE_READY;
    static final String ATTR_AFFILIATION_ID = InstitutionalRequestContextKeys.ATTR_AFFILIATION_ID;
    static final String ATTR_UNIDADE_CODIGO = InstitutionalRequestContextKeys.ATTR_UNIDADE_CODIGO;
    static final String ATTR_CAIXA_CODIGO = InstitutionalRequestContextKeys.ATTR_CAIXA_CODIGO;
    static final String ATTR_ACCESS_READ_ONLY = InstitutionalRequestContextKeys.ATTR_ACCESS_READ_ONLY;
    static final String ATTR_RLS_SCOPE_KEY = InstitutionalRequestContextKeys.ATTR_RLS_SCOPE_KEY;
    static final String ATTR_COVERAGE_MODE = InstitutionalRequestContextKeys.ATTR_COVERAGE_MODE;
    static final String ATTR_ACCESS_REQUIRES_STEP_UP = InstitutionalRequestContextKeys.ATTR_ACCESS_REQUIRES_STEP_UP;
    static final String ATTR_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE = InstitutionalRequestContextKeys.ATTR_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE;
    static final String ATTR_JUDICIAL_FLOW_SENSITIVE = InstitutionalRequestContextKeys.ATTR_JUDICIAL_FLOW_SENSITIVE;
    private static final String RESPONSE_HEADER_PANEL_CODE = "X-PJB-Institutional-Panel-Code";
    private static final String RESPONSE_HEADER_LANDING_PATH = "X-PJB-Institutional-Landing-Path";
    private static final String RESPONSE_HEADER_DATA_PLANE_KEY = "X-PJB-Institutional-Data-Plane-Key";
    private static final String RESPONSE_HEADER_READ_REPLICA = "X-PJB-Institutional-Read-Replica";
    private static final String RESPONSE_HEADER_READY_FOR_PANEL = "X-PJB-Institutional-Ready-For-Panel";
    private static final String RESPONSE_HEADER_PROFILE_STATE = "X-PJB-Institutional-Profile-State";
    private static final String RESPONSE_HEADER_TARGET_ENVIRONMENT = "X-PJB-Institutional-Target-Environment";
    private static final String RESPONSE_HEADER_PANEL_PROVISIONING_COMPLETE = "X-PJB-Institutional-Panel-Provisioning-Complete";
    private static final String RESPONSE_HEADER_SHARED_EXPERIENCE_READY = "X-PJB-Institutional-Shared-Experience-Ready";
    private static final String RESPONSE_HEADER_UNIT_CODE = "X-PJB-Institutional-Unit-Code";
    private static final String RESPONSE_HEADER_BOX_CODE = "X-PJB-Institutional-Box-Code";
    private static final String RESPONSE_HEADER_RLS_SCOPE = "X-PJB-Institutional-Rls-Scope";
    private static final String RESPONSE_HEADER_ACCESS_READ_ONLY = "X-PJB-Institutional-Access-Read-Only";
    private static final String RESPONSE_HEADER_COVERAGE_MODE = "X-PJB-Institutional-Coverage-Mode";
    private static final String RESPONSE_HEADER_ACCESS_REQUIRES_STEP_UP = "X-PJB-Institutional-Requires-Step-Up";
    private static final String RESPONSE_HEADER_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE = "X-PJB-Institutional-Requires-Qualified-Certificate";
    private static final String RESPONSE_HEADER_JUDICIAL_FLOW_SENSITIVE = "X-PJB-Institutional-Judicial-Flow-Sensitive";

    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;
    private final InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService;
    private final PjbDataSourceRoutingProperties routingProperties;

    public PjbInstitutionalDataPlaneFilter(NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService,
                                           InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService,
                                           PjbDataSourceRoutingProperties routingProperties) {
        this.stateBundleFacadeService = Objects.requireNonNull(stateBundleFacadeService, "stateBundleFacadeService");
        this.accessContextMaterializationApplicationService = Objects.requireNonNull(accessContextMaterializationApplicationService, "accessContextMaterializationApplicationService");
        this.routingProperties = Objects.requireNonNull(routingProperties, "routingProperties");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !routingProperties.getRegionalSelection().isEnabled()
                || !InstitutionalApiRoutes.isInstitutionalPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String affiliationId = resolveAffiliationId(request);
            String nominationId = resolveNominationId(request);
            var entryActivationDecisionApplicationService = stateBundleFacadeService;
            entryActivationDecisionApplicationService.avaliarEntradaAtual(affiliationId, nominationId);
            NationalCommunicationInstitutionalStateBundle stateBundle = stateBundleFacadeService.carregar(affiliationId, nominationId);
            InstitutionalHorizontalDataPlanePlan plan = stateBundle.horizontalDataPlanePlan();
            InstitutionalOperationalProfileProjection profileProjection = stateBundle.operationalProfile();
            InstitutionalEntryActivationBundle activationBundle = stateBundle.entryActivationBundle();
            InstitutionalAccessContextSnapshot accessContext = accessContextMaterializationApplicationService.materializar(affiliationId, nominationId);
            Map<String, String> syntheticHeaders = resolveSyntheticHeaders(request, plan, accessContext);
            HttpServletRequest effectiveRequest = syntheticHeaders.isEmpty() ? request : new PjbInstitutionalRoutingAugmentedRequest(request, syntheticHeaders);
            bindRequestAttributes(effectiveRequest, plan, profileProjection, activationBundle, accessContext);
            bindResponseHeaders(response, plan, profileProjection, activationBundle, accessContext);
            filterChain.doFilter(effectiveRequest, response);
        } catch (RuntimeException ex) {
            filterChain.doFilter(request, response);
        }
    }


    private String resolveAffiliationId(HttpServletRequest request) {
        String explicit = request.getParameter("affiliationId");
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        return InstitutionalApiRoutes.extractAffiliationId(request.getRequestURI());
    }

    private String resolveNominationId(HttpServletRequest request) {
        String explicit = request.getParameter("nominationId");
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        return InstitutionalApiRoutes.extractNominationId(request.getRequestURI());
    }

    private Map<String, String> resolveSyntheticHeaders(HttpServletRequest request,
                                                        InstitutionalHorizontalDataPlanePlan plan,
                                                        InstitutionalAccessContextSnapshot accessContext) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (plan != null) {
            plan.routingHeaders().forEach((name, value) -> {
                if (name == null || name.isBlank() || value == null || value.isBlank()) {
                    return;
                }
                String current = request.getHeader(name);
                if (current == null || current.isBlank()) {
                    out.put(name, value.trim());
                }
            });
        }
        if (accessContext != null && !accessContext.sessionVariables().isEmpty()) {
            accessContext.sessionVariables().forEach((name, value) -> {
                if (name == null || name.isBlank() || value == null || value.isBlank()) {
                    return;
                }
                String current = request.getHeader(name);
                if (current == null || current.isBlank()) {
                    out.put(name, value.trim());
                }
            });
        }
        return out;
    }

    private void bindRequestAttributes(HttpServletRequest request,
                                       InstitutionalHorizontalDataPlanePlan plan,
                                       InstitutionalOperationalProfileProjection profileProjection,
                                       InstitutionalEntryActivationBundle activationBundle,
                                       InstitutionalAccessContextSnapshot accessContext) {
        if (plan == null && accessContext == null) {
            return;
        }
        putAttribute(request, ATTR_PANEL_CODE, plan == null ? null : plan.panelCode());
        putAttribute(request, ATTR_LANDING_PATH, plan == null ? null : plan.landingPath());
        putAttribute(request, ATTR_DATA_PLANE_KEY, firstNonBlank(plan == null ? null : plan.horizontalDataPlaneKey(), accessContext == null ? null : accessContext.horizontalDataPlaneKey()));
        putAttribute(request, ATTR_READ_REPLICA, firstNonBlank(plan == null ? null : plan.readReplicaCode(), accessContext == null ? null : accessContext.readReplicaCode()));
        putAttribute(request, ATTR_NOMINATION_ID, firstNonBlank(plan == null ? null : plan.nominationId(), accessContext == null ? null : accessContext.nominationId()));
        boolean readyForPanel = activationBundle != null && activationBundle.decision() != null
                ? activationBundle.decision().panelProvisioningComplete() && !activationBundle.decision().requiresPanelProvisioningReview()
                : accessContext != null ? accessContext.readyForInstitutionalPanel() : plan != null && plan.readyForInstitutionalPanel();
        request.setAttribute(ATTR_READY_FOR_PANEL, readyForPanel);
        if (profileProjection != null && profileProjection.profileState() != null && !profileProjection.profileState().isBlank()) {
            request.setAttribute(ATTR_PROFILE_STATE, profileProjection.profileState());
        }
        if (accessContext != null) {
            putAttribute(request, ATTR_AFFILIATION_ID, accessContext.affiliationId());
            putAttribute(request, ATTR_UNIDADE_CODIGO, accessContext.primaryUnitCode());
            putAttribute(request, ATTR_CAIXA_CODIGO, accessContext.primaryBoxCode());
            putAttribute(request, ATTR_RLS_SCOPE_KEY, accessContext.rlsScopeKey());
            putAttribute(request, ATTR_COVERAGE_MODE, accessContext.coverageMode());
            request.setAttribute(ATTR_ACCESS_READ_ONLY, accessContext.readOnly());
            request.setAttribute(ATTR_ACCESS_REQUIRES_STEP_UP, accessContext.requiresStepUp());
            request.setAttribute(ATTR_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE, accessContext.requiresQualifiedCertificate());
            request.setAttribute(ATTR_JUDICIAL_FLOW_SENSITIVE, accessContext.judicialFlowSensitive());
        }
        if (activationBundle != null && activationBundle.decision() != null) {
            request.setAttribute(ATTR_PANEL_PROVISIONING_COMPLETE, activationBundle.decision().panelProvisioningComplete());
            request.setAttribute(ATTR_SHARED_EXPERIENCE_READY, activationBundle.decision().sharedExperienceReady());
            if (activationBundle.decision().targetEnvironment() != null && !activationBundle.decision().targetEnvironment().isBlank()) {
                request.setAttribute(ATTR_TARGET_ENVIRONMENT, activationBundle.decision().targetEnvironment());
            }
        }
    }

    private void bindResponseHeaders(HttpServletResponse response,
                                     InstitutionalHorizontalDataPlanePlan plan,
                                     InstitutionalOperationalProfileProjection profileProjection,
                                     InstitutionalEntryActivationBundle activationBundle,
                                     InstitutionalAccessContextSnapshot accessContext) {
        if (plan == null && accessContext == null) {
            return;
        }
        putHeader(response, RESPONSE_HEADER_PANEL_CODE, plan == null ? null : plan.panelCode());
        putHeader(response, RESPONSE_HEADER_LANDING_PATH, plan == null ? null : plan.landingPath());
        putHeader(response, RESPONSE_HEADER_DATA_PLANE_KEY, firstNonBlank(plan == null ? null : plan.horizontalDataPlaneKey(), accessContext == null ? null : accessContext.horizontalDataPlaneKey()));
        putHeader(response, RESPONSE_HEADER_READ_REPLICA, firstNonBlank(plan == null ? null : plan.readReplicaCode(), accessContext == null ? null : accessContext.readReplicaCode()));
        boolean readyForPanel = activationBundle != null && activationBundle.decision() != null
                ? activationBundle.decision().panelProvisioningComplete() && !activationBundle.decision().requiresPanelProvisioningReview()
                : accessContext != null ? accessContext.readyForInstitutionalPanel() : plan != null && plan.readyForInstitutionalPanel();
        response.setHeader(RESPONSE_HEADER_READY_FOR_PANEL, Boolean.toString(readyForPanel));
        if (profileProjection != null && profileProjection.profileState() != null && !profileProjection.profileState().isBlank()) {
            response.setHeader(RESPONSE_HEADER_PROFILE_STATE, profileProjection.profileState());
        }
        if (accessContext != null) {
            putHeader(response, RESPONSE_HEADER_UNIT_CODE, accessContext.primaryUnitCode());
            putHeader(response, RESPONSE_HEADER_BOX_CODE, accessContext.primaryBoxCode());
            putHeader(response, RESPONSE_HEADER_RLS_SCOPE, accessContext.rlsScopeKey());
            putHeader(response, RESPONSE_HEADER_COVERAGE_MODE, accessContext.coverageMode());
            response.setHeader(RESPONSE_HEADER_ACCESS_READ_ONLY, Boolean.toString(accessContext.readOnly()));
            response.setHeader(RESPONSE_HEADER_ACCESS_REQUIRES_STEP_UP, Boolean.toString(accessContext.requiresStepUp()));
            response.setHeader(RESPONSE_HEADER_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE, Boolean.toString(accessContext.requiresQualifiedCertificate()));
            response.setHeader(RESPONSE_HEADER_JUDICIAL_FLOW_SENSITIVE, Boolean.toString(accessContext.judicialFlowSensitive()));
        }
        if (activationBundle != null && activationBundle.decision() != null) {
            response.setHeader(RESPONSE_HEADER_PANEL_PROVISIONING_COMPLETE, Boolean.toString(activationBundle.decision().panelProvisioningComplete()));
            response.setHeader(RESPONSE_HEADER_SHARED_EXPERIENCE_READY, Boolean.toString(activationBundle.decision().sharedExperienceReady()));
            putHeader(response, RESPONSE_HEADER_TARGET_ENVIRONMENT, activationBundle.decision().targetEnvironment());
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void putAttribute(HttpServletRequest request, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        request.setAttribute(name, value.trim());
    }

    private void putHeader(HttpServletResponse response, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        response.setHeader(name, value.trim());
    }
}
