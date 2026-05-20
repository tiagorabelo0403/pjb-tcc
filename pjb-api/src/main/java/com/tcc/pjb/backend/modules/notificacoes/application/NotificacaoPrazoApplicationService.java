package com.tcc.pjb.backend.modules.notificacoes.application;

import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoDispatchResult;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoPort;
import com.tcc.pjb.backend.modules.notificacoes.domain.NotificacaoPrazoPolicy;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoResult;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoPrazoApplicationService {

    private final NotificacaoPrazoPort port;
    private final NotificacaoPrazoPolicy policy;

    @Autowired
    public NotificacaoPrazoApplicationService(NotificacaoPrazoPort port) {
        this(port, new NotificacaoPrazoPolicy());
    }

    NotificacaoPrazoApplicationService(NotificacaoPrazoPort port,
                                       NotificacaoPrazoPolicy policy) {
        this.port = Objects.requireNonNull(port);
        this.policy = Objects.requireNonNull(policy);
    }

    @Transactional
    public NotificacaoPrazoDispatchResult publicarAlertaPrazo(NotificacaoPrazoCommand command) {
        Objects.requireNonNull(command);
        var normalizada = policy.normalizar(
                command.usuarioId(),
                command.processoId(),
                command.processoNumero(),
                command.vencimentoForense(),
                command.notificarEm(),
                command.titulo(),
                command.corpo(),
                command.urlDetalhes(),
                command.prioridade(),
                command.origemModulo(),
                command.notificationKey()
        );
        return port.publicarAlertaPrazo(new NotificacaoPrazoCommand(
                normalizada.usuarioId(),
                normalizada.processoId(),
                normalizada.processoNumero(),
                normalizada.vencimentoForense(),
                normalizada.notificarEm(),
                normalizada.titulo(),
                normalizada.corpo(),
                normalizada.urlDetalhes(),
                normalizada.prioridade().name(),
                normalizada.origemModulo(),
                normalizada.notificationKey()
        ));
    }

    @Transactional
    public NotificacaoPrazoDispatchResult notificarPrazoCalculado(Long usuarioId,
                                                                  Long processoId,
                                                                  String processoNumero,
                                                                  String origemModulo,
                                                                  String urlDetalhes,
                                                                  PrazoProcessualCalculoResult prazo) {
        Objects.requireNonNull(prazo);
        var prioridade = policy.prioridadeParaPrazo(
                prazo.vencimentoForense(),
                prazo.conferenciaManualRecomendada(),
                prazo.marcoInicialDiaUtil()
        );
        return publicarAlertaPrazo(new NotificacaoPrazoCommand(
                usuarioId,
                processoId,
                processoNumero,
                prazo.vencimentoForense(),
                null,
                policy.tituloPadrao(prazo.tipoPrazo()),
                policy.corpoPadrao(prazo.vencimentoForense(), prazo.conferenciaManualRecomendada()),
                urlDetalhes,
                prioridade.name(),
                origemModulo,
                null
        ));
    }
}
