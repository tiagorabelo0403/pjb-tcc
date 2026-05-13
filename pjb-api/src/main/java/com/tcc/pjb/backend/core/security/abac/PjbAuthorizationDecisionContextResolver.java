package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.model.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class PjbAuthorizationDecisionContextResolver {

    private final CurrentUserService currentUserService;
    private final GovBrAssuranceExtractor govBrAssuranceExtractor;

    PjbAuthorizationDecisionContextResolver(CurrentUserService currentUserService,
                                           GovBrAssuranceExtractor govBrAssuranceExtractor) {
        this.currentUserService = currentUserService;
        this.govBrAssuranceExtractor = govBrAssuranceExtractor;
    }

    PjbAuthorizationDecisionContext resolve() {
        return resolve(null);
    }

    PjbAuthorizationDecisionContext resolve(Usuario actorOverride) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PjbAuthorizationDecisionContext(
                actorOverride != null ? actorOverride : currentUserService.getOptional().orElse(null),
                RequestContext.getJustificativa().orElse(null),
                RequestContext.getRequestId().orElse(null),
                govBrAssuranceExtractor.extract(authentication)
        );
    }
}
