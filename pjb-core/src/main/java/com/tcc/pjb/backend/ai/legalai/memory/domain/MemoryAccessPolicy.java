package com.tcc.pjb.backend.ai.legalai.memory.domain;

public final class MemoryAccessPolicy {

    public enum AccessType {
        READ_WRITE,
        READ_ONLY,
        DENIED
    }

    private MemoryAccessPolicy() {}

    public static AccessType computarAccessType(MemorySigiloNivel nivel) {
        return switch (nivel) {
            case PUBLIC, INSTITUCIONAL -> AccessType.READ_WRITE;
            case SIGILOSO -> AccessType.READ_ONLY;
            case CRITICO -> AccessType.DENIED;
        };
    }

    public static boolean podeEnviarParaAnthropicApi(MemorySigiloNivel nivel) {
        return nivel == MemorySigiloNivel.PUBLIC || nivel == MemorySigiloNivel.INSTITUCIONAL;
    }

    public static boolean requerRevisaoHumana(MemorySigiloNivel nivel) {
        return nivel == MemorySigiloNivel.SIGILOSO || nivel == MemorySigiloNivel.CRITICO;
    }
}
