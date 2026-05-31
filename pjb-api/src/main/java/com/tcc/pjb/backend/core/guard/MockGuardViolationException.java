package com.tcc.pjb.backend.core.guard;

public final class MockGuardViolationException extends IllegalStateException {

    private final MockGuardViolation violation;

    public MockGuardViolationException(MockGuardViolation violation) {
        super("[MOCK-GUARD] Violação de segurança: " + violation.propriedade()
                + "=true não é permitido em ambiente " + violation.perfil().name()
                + ". Serviço: " + violation.servico()
                + ". ViolationId: " + violation.violationId());
        this.violation = violation;
    }

    public MockGuardViolation getViolation() {
        return violation;
    }
}
