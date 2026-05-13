package com.tcc.pjb.backend.integration.mni.domain;
public record MniTribunalHealthResult(String tribunalCodigo, boolean enabled, boolean healthy, long remessasConhecidas, long recepcoesConhecidas) {}
