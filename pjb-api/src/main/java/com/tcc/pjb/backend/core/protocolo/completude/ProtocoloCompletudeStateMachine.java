package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import java.util.Map;
import java.util.Set;

public final class ProtocoloCompletudeStateMachine {

    private static final Map<ProtocoloCompletudeStatus, Set<ProtocoloCompletudeStatus>> TRANSICOES_PERMITIDAS =
            Map.of(
                    ProtocoloCompletudeStatus.RECEBIDO,
                            Set.of(ProtocoloCompletudeStatus.EM_VALIDACAO,
                                   ProtocoloCompletudeStatus.CANCELADO),
                    ProtocoloCompletudeStatus.EM_VALIDACAO,
                            Set.of(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO,
                                   ProtocoloCompletudeStatus.COMPLETO,
                                   ProtocoloCompletudeStatus.CANCELADO),
                    ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO,
                            Set.of(ProtocoloCompletudeStatus.EM_VALIDACAO,
                                   ProtocoloCompletudeStatus.DISPENSADO,
                                   ProtocoloCompletudeStatus.CANCELADO),
                    ProtocoloCompletudeStatus.COMPLETO,
                            Set.of(ProtocoloCompletudeStatus.DISTRIBUIDO,
                                   ProtocoloCompletudeStatus.CANCELADO),
                    ProtocoloCompletudeStatus.DISPENSADO,
                            Set.of(ProtocoloCompletudeStatus.DISTRIBUIDO,
                                   ProtocoloCompletudeStatus.CANCELADO),
                    ProtocoloCompletudeStatus.DISTRIBUIDO,
                            Set.of(),
                    ProtocoloCompletudeStatus.CANCELADO,
                            Set.of()
            );

    private ProtocoloCompletudeStateMachine() {
    }

    public static void validar(ProtocoloCompletudeStatus origem, ProtocoloCompletudeStatus destino) {
        Set<ProtocoloCompletudeStatus> permitidos = TRANSICOES_PERMITIDAS.getOrDefault(origem, Set.of());
        if (!permitidos.contains(destino)) {
            throw new TransicaoInvalidaException(origem, destino);
        }
    }

    public static boolean transicaoValida(ProtocoloCompletudeStatus origem, ProtocoloCompletudeStatus destino) {
        return TRANSICOES_PERMITIDAS.getOrDefault(origem, Set.of()).contains(destino);
    }

    public static final class TransicaoInvalidaException extends RuntimeException {
        private final ProtocoloCompletudeStatus origem;
        private final ProtocoloCompletudeStatus destino;

        public TransicaoInvalidaException(ProtocoloCompletudeStatus origem, ProtocoloCompletudeStatus destino) {
            super("Transição inválida: " + origem + " → " + destino);
            this.origem = origem;
            this.destino = destino;
        }

        public ProtocoloCompletudeStatus origem() { return origem; }
        public ProtocoloCompletudeStatus destino() { return destino; }
    }
}
