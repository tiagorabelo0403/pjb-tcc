package com.tcc.pjb.backend.core.comunicacao.judicial;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoComunicacoesJudiciaisResponse;

@RestController
@RequestMapping("/api/v1/comunicacoes/judiciais")
public class ComunicacaoJudicialInboxController {

    private final ComunicacaoJudicialInboxService service;

    public ComunicacaoJudicialInboxController(ComunicacaoJudicialInboxService service) {
        this.service = service;
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA')")
    public CidadaoComunicacoesJudiciaisResponse minhas() {
        return service.minhasComunicacoes();
    }
}
