package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import java.time.LocalDate;

public class ProtocoloPendenteException extends RuntimeException {

    private final Long protocoloId;
    private final ResultadoValidacao resultado;
    private final LocalDate prazoRegularizacao;

    public ProtocoloPendenteException(Long protocoloId, ResultadoValidacao resultado, LocalDate prazoRegularizacao) {
        super("Protocolo " + protocoloId + " pendente de documentação: "
                + resultado.bloqueantes().size() + " bloqueante(s)");
        this.protocoloId = protocoloId;
        this.resultado = resultado;
        this.prazoRegularizacao = prazoRegularizacao;
    }

    public Long protocoloId() { return protocoloId; }
    public ResultadoValidacao resultado() { return resultado; }
    public LocalDate prazoRegularizacao() { return prazoRegularizacao; }
}
