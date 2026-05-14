package com.tcc.pjb.backend.service.citacao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CitacaoReadinessService {

    public record CitacaoInput(
            UUID processoId,
            ModalidadeCitacao modalidade,
            boolean enderecoConfirmado,
            boolean domicilioEletronicoValidado,
            boolean parteFazendaPublica,
            boolean mandadoAssinado,
            boolean custasPagas,
            boolean despachoPublicado
    ) {}

    public record CitacaoReadiness(
            UUID processoId,
            ModalidadeCitacao modalidade,
            boolean apta,
            List<String> bloqueios,
            List<String> orientacoes
    ) {}

    public CitacaoReadiness avaliar(CitacaoInput input) {
        List<String> bloqueios = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();
        if (!input.despachoPublicado()) {
            bloqueios.add("Despacho inicial não publicado — publicar antes de expedir.");
        }
        if (!input.custasPagas() && !input.parteFazendaPublica()) {
            bloqueios.add("Custas de citação não recolhidas.");
        }
        if (input.modalidade().requerOfficialDeJustica() && !input.mandadoAssinado()) {
            bloqueios.add("Mandado não assinado pelo magistrado.");
        }
        if (!input.enderecoConfirmado() && input.modalidade() != ModalidadeCitacao.EDITAL) {
            orientacoes.add("Confirmar endereço do réu antes de expedir.");
        }
        if (input.modalidade().eEletronica() && !input.domicilioEletronicoValidado()) {
            bloqueios.add("Domicílio eletrônico judicial não validado.");
        }
        if (input.parteFazendaPublica()
                && input.modalidade() != ModalidadeCitacao.PESSOAL_FAZENDA) {
            orientacoes.add("Fazenda Pública requer citação pessoal — verificar modalidade.");
        }
        return new CitacaoReadiness(input.processoId(), input.modalidade(),
                bloqueios.isEmpty(), List.copyOf(bloqueios), List.copyOf(orientacoes));
    }
}
