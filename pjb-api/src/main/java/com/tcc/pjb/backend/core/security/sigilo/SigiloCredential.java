package com.tcc.pjb.backend.core.security.sigilo;

import java.util.UUID;

public record SigiloCredential(UUID requestId, String password) {
}
