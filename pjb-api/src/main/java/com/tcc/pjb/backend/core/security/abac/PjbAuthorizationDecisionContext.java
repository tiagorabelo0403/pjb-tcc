package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.entity.Usuario;

record PjbAuthorizationDecisionContext(Usuario usuario, String justificativa, String requestId, String govBrAssuranceLevel) {
}
