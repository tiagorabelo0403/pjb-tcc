package com.tcc.pjb.backend.integration.mni.domain;

public record MniTribunalCredentialView(
        String tribunal,
        String usuario,
        boolean configured
) {}
