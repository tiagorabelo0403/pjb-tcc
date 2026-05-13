package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import org.springframework.stereotype.Component;

@Component
public final class DreamPolicy {

    public boolean podeIniciarDreaming(MemoryStore inputStore) {
        return !inputStore.isArquivado()
                && MemoryAccessPolicy.podeEnviarParaAnthropicApi(inputStore.sigiloNivel());
    }

    public boolean podeEnviarSessionParaAnthropicApi(String sigiloNivel) {
        try {
            return MemoryAccessPolicy.podeEnviarParaAnthropicApi(MemorySigiloNivel.valueOf(sigiloNivel));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
