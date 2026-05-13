package com.tcc.pjb.backend.service.processual.comunicacao.institutional.access;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalRequestContextKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceAssemblerSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class InstitutionalRequestAccessContextFacadeService {

    private static final String ATTR_CACHED_RESPONSE = "PJB_INSTITUTIONAL_ACCESS_CONTEXT_RESPONSE";

    private final InstitutionalAccessContextMaterializationApplicationService materializationApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public InstitutionalRequestAccessContextFacadeService(InstitutionalAccessContextMaterializationApplicationService materializationApplicationService,
                                                          NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.materializationApplicationService = Objects.requireNonNull(materializationApplicationService);
        this.governanceAssemblerSupport = Objects.requireNonNull(governanceAssemblerSupport);
    }

    public NationalCommunicationInstitutionalAccessContextResponse atual() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object cached = request.getAttribute(ATTR_CACHED_RESPONSE);
        if (cached instanceof NationalCommunicationInstitutionalAccessContextResponse response) {
            return response;
        }
        String affiliationId = firstNonBlank(attribute(request, InstitutionalRequestContextKeys.ATTR_AFFILIATION_ID), request.getParameter("affiliationId"));
        String nominationId = firstNonBlank(attribute(request, InstitutionalRequestContextKeys.ATTR_NOMINATION_ID), request.getParameter("nominationId"));
        if (affiliationId == null || affiliationId.isBlank()) {
            return null;
        }
        InstitutionalAccessContextSnapshot snapshot = materializationApplicationService.materializar(affiliationId, nominationId);
        NationalCommunicationInstitutionalAccessContextResponse response = governanceAssemblerSupport.toResponse(snapshot);
        request.setAttribute(ATTR_CACHED_RESPONSE, response);
        return response;
    }

    public InstitutionalAccessDigest digest() {
        NationalCommunicationInstitutionalAccessContextResponse current = atual();
        if (current == null) {
            HttpServletRequest request = currentRequest();
            return new InstitutionalAccessDigest(
                    request == null ? null : attribute(request, InstitutionalRequestContextKeys.ATTR_DATA_PLANE_KEY),
                    request == null ? null : attribute(request, InstitutionalRequestContextKeys.ATTR_RLS_SCOPE_KEY),
                    request == null ? null : attribute(request, InstitutionalRequestContextKeys.ATTR_COVERAGE_MODE),
                    request != null && booleanAttribute(request, InstitutionalRequestContextKeys.ATTR_ACCESS_READ_ONLY),
                    request != null && booleanAttribute(request, InstitutionalRequestContextKeys.ATTR_ACCESS_REQUIRES_STEP_UP),
                    request != null && booleanAttribute(request, InstitutionalRequestContextKeys.ATTR_ACCESS_REQUIRES_QUALIFIED_CERTIFICATE));
        }
        return new InstitutionalAccessDigest(current.horizontalDataPlaneKey(), current.rlsScopeKey(), current.coverageMode(), current.readOnly(), current.requiresStepUp(), current.requiresQualifiedCertificate());
    }

    private String attribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value).trim();
    }

    private boolean booleanAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
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

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    public record InstitutionalAccessDigest(String horizontalDataPlaneKey,
                                            String rlsScopeKey,
                                            String coverageMode,
                                            boolean readOnly,
                                            boolean requiresStepUp,
                                            boolean requiresQualifiedCertificate) {
    }
}