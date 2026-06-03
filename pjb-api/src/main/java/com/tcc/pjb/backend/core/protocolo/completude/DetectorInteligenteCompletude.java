package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.core.protocolo.completude.domain.DocumentoAnalisavel;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import java.util.List;

public interface DetectorInteligenteCompletude {

    List<ViolacaoCompletude> analisar(ContextoValidacaoCompletude contexto, List<DocumentoAnalisavel> documentos);

    boolean disponivel();
}
