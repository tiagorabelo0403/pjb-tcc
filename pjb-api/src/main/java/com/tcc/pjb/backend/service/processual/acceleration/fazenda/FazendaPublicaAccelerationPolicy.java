package com.tcc.pjb.backend.service.processual.acceleration.fazenda;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;

public final class FazendaPublicaAccelerationPolicy {

    private FazendaPublicaAccelerationPolicy() {}

    public record FazendaInput(
            RitoProcessual rito,
            boolean prazoFazendaEmDobro,
            boolean intimacaoPessoalRealizada,
            boolean remessaNecessariaIndicada,
            boolean precatorioEmitido,
            boolean requisicaoPequenoValorEmitida
    ) {}

    public record AceleradorFazenda(
            String acao,
            String mensagem,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorFazenda> sugerir(FazendaInput input) {
        List<AceleradorFazenda> acoes = new ArrayList<>();
        if (!input.intimacaoPessoalRealizada()) {
            acoes.add(new AceleradorFazenda(
                    "EXPEDIR_INTIMACAO_PESSOAL",
                    "Fazenda Pública exige intimação pessoal — não publicar no Diário sem certificar.",
                    false));
        }
        if (input.remessaNecessariaIndicada()) {
            acoes.add(new AceleradorFazenda(
                    "VERIFICAR_REMESSA_NECESSARIA",
                    "Remessa necessária indicada — verificar valor e isenção antes de baixar.",
                    true));
        }
        if (input.precatorioEmitido()) {
            acoes.add(new AceleradorFazenda(
                    "ACOMPANHAR_PRECATORIO",
                    "Precatório emitido — monitorar inclusão no orçamento.",
                    false));
        }
        return acoes;
    }
}
