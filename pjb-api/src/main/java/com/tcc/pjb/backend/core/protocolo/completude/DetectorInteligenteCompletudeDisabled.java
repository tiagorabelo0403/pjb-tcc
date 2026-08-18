package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.core.protocolo.completude.domain.DocumentoAnalisavel;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "pjb.runtime.barrier.integrations",
        name = "digitalizacao",
        havingValue = "false")
public class DetectorInteligenteCompletudeDisabled implements DetectorInteligenteCompletude {

    @Override
    public List<ViolacaoCompletude> analisar(ContextoValidacaoCompletude contexto,
                                              List<DocumentoAnalisavel> documentos) {
        return List.of();
    }

    @Override
    public boolean disponivel() {
        return false;
    }
}
