package com.tcc.pjb.backend.core.processo.tese;

import com.tcc.pjb.backend.core.events.TeseVinculanteDecididaEvent;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class SobrestamentoVinculanteApplicationService implements TeseVinculanteListenerPort {

    private final SobrestamentoVinculanteRetomadaPort retomadaPort;

    public SobrestamentoVinculanteApplicationService(SobrestamentoVinculanteRetomadaPort retomadaPort) {
        this.retomadaPort = Objects.requireNonNull(retomadaPort);
    }

    @Override
    @EventListener
    public void onDecisaoPublicada(TeseVinculanteDecisao decisao) {
        if (decisao == null || !decisao.aplicacaoImediata()) {
            return;
        }
        retomadaPort.retomar(decisao.teseId(), decisao.resultado(), decisao.ementa());
    }

    @EventListener
    public void onTeseVinculanteDecidida(TeseVinculanteDecididaEvent event) {
        if (!event.aplicacaoImediata()) {
            return;
        }
        TeseVinculanteDecisao decisao = new TeseVinculanteDecisao(
                event.teseVinculanteId(),
                event.tribunalOrigem(),
                event.ementa(),
                event.resultado(),
                event.julgadaEm(),
                true
        );
        onDecisaoPublicada(decisao);
    }
}
