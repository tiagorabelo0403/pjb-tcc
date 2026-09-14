package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.service.processual.peticionamento.leitura.PecaInicialLeituraResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.leitura.PeticaoInicialLeituraService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Superfície de leitura da peça inicial publicada. Devolve o corpo da petição já renderizado como
 * HTML seguro (mesma fonte de verdade sanitizada usada na escrita), gateado pelo ABAC/sigilo do
 * processo em {@link PeticaoInicialLeituraService}. Complementa o painel de leitura documental: a
 * peça inicial não é materializada como {@code DocumentoProcessual}, então esta rota é o ponto por
 * onde juiz, servidor, parte e público autorizado leem o que foi efetivamente peticionado.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PeticaoInicialLeituraController {

    private final PeticaoInicialLeituraService leituraService;

    @GetMapping("/processos/{processoId}/peticao-inicial/leitura")
    @PreAuthorize("isAuthenticated()")
    public PecaInicialLeituraResponse ler(@PathVariable Long processoId) {
        return leituraService.lerPorProcesso(processoId);
    }
}
