package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadPolicy;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadPolicyRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Bloqueia o envio de mensagens/typing por um cidadão durante uma janela de suspensão definida
 * pela política da thread. Extraído de {@link AtendimentoChatService} porque
 * {@code policyRepository} é usado exclusivamente por essa checagem (chamada de
 * {@code sendMessage} e {@code typing}).
 */
@Service
public class AtendimentoCidadaoSendWindowGuardService {

    private final AtendimentoThreadPolicyRepository policyRepository;
    private final Clock clock;

    public AtendimentoCidadaoSendWindowGuardService(AtendimentoThreadPolicyRepository policyRepository, Clock clock) {
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public void enforce(Usuario actor, Long threadId) {
        if (actor.getTipoUsuario() != TipoUsuario.CIDADAO) {
            return;
        }
        AtendimentoThreadPolicy policy = policyRepository.findById(threadId).orElse(null);
        Instant disabledUntil = policy != null ? policy.getCidadaoSendDisabledUntil() : null;
        if (disabledUntil != null && Instant.now(clock).isBefore(disabledUntil)) {
            throw new AccessDeniedException("cidadao_send_disabled");
        }
    }
}
