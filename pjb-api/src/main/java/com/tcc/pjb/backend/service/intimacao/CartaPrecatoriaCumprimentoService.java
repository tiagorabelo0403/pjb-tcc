package com.tcc.pjb.backend.service.intimacao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CartaPrecatoriaCumprimentoService {

    public record CartaPrecatoriaInput(
            UUID processoOrigemId,
            UUID cartaId,
            String juizoDelegante,
            String juizoDelegado,
            String uf,
            boolean competenciaDelegada,
            boolean pecasInstruidas,
            boolean despachoExpedicaoExarado,
            boolean protocoladaNoJuizoDelegado,
            boolean cumprida,
            boolean devolvida
    ) {}

    public record CartaCumprimentoResult(
            boolean aptaExpedicao,
            boolean cumprimentoConfirmado,
            List<String> pendencias,
            String fundamentacao
    ) {}

    public CartaCumprimentoResult avaliar(CartaPrecatoriaInput input) {
        List<String> pendencias = new ArrayList<>();

        if (!input.competenciaDelegada()) {
            pendencias.add("Competência delegada não verificada — juízo delegado deve ter competência territorial.");
        }
        if (!input.pecasInstruidas()) {
            pendencias.add("Peças necessárias ao cumprimento não instruídas na carta (art. 262, CPC).");
        }
        if (!input.despachoExpedicaoExarado()) {
            pendencias.add("Despacho de expedição da carta precatória não exarado pelo juízo delegante.");
        }

        boolean aptaExpedicao = pendencias.isEmpty();

        if (aptaExpedicao && !input.protocoladaNoJuizoDelegado()) {
            pendencias.add("Carta não protocolada no juízo delegado.");
        }

        boolean cumprimentoConfirmado = input.cumprida() && input.devolvida();
        if (!cumprimentoConfirmado && aptaExpedicao && input.protocoladaNoJuizoDelegado()) {
            pendencias.add("Cumprimento da carta pendente no juízo delegado.");
        }

        return new CartaCumprimentoResult(aptaExpedicao, cumprimentoConfirmado,
                List.copyOf(pendencias),
                cumprimentoConfirmado ? "Carta precatória cumprida e devolvida ao juízo delegante."
                        : aptaExpedicao ? "Carta expedida — aguardando cumprimento pelo juízo delegado."
                        : "Carta com " + pendencias.size() + " pendência(s) para expedição.");
    }
}
