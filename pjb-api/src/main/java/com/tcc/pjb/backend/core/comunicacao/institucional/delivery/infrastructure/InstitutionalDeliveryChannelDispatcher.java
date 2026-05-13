package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;

@Component
public class InstitutionalDeliveryChannelDispatcher {

    private final List<InstitutionalDeliveryHandler> handlers;

    public InstitutionalDeliveryChannelDispatcher(List<InstitutionalDeliveryHandler> handlers) {
        this.handlers = List.copyOf(Objects.requireNonNull(handlers));
    }

    public InstitutionalDeliveryDispatchResult dispatch(InstitutionalDeliveryJob job) {
        return handlers.stream()
                .filter(handler -> handler.supports(job.currentChannel()))
                .findFirst()
                .map(handler -> handler.dispatch(job))
                .orElseGet(() -> InstitutionalDeliveryDispatchResult.falhaTerminal(
                        MotivoFalhaEntregaInstitucional.CANAL_NAO_SUPORTADO,
                        "NO_HANDLER",
                        "Nenhum handler institucional registrado para o canal " + job.currentChannel().name()
                ));
    }
}
