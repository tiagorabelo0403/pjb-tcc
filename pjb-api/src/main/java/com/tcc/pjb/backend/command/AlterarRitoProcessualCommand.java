package com.tcc.pjb.backend.command;

import com.tcc.pjb.backend.model.dto.event.ProcessoRitoAlteradoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlterarRitoProcessualCommand {

    private final ProcessoRepository processoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PjbTransactionalBudget(operation = "rito.alterar.command", maxMillis = 1500, critical = false)
    public Processo execute(Long processoId, RitoProcessual novoRito, String fundamentacao) {
        Objects.requireNonNull(processoId, "O processoId é obrigatório");
        Objects.requireNonNull(novoRito, "O novoRito é obrigatório");

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        RitoProcessual ritoAtual = processo.getRito();
        if (novoRito.equals(ritoAtual)) {
            return processo;
        }

        processo.setRito(novoRito);
        Processo salvo = processoRepository.save(processo);

        try {
            eventPublisher.publishEvent(ProcessoRitoAlteradoEvent.from(salvo, ritoAtual, novoRito, fundamentacao));
        } catch (Exception e) {
            log.warn("Falha ao publicar evento ProcessoRitoAlteradoEvent (não bloqueante): {}", e.getMessage());
        }

        return salvo;
    }
}
