package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.core.servidor.api.dto.UnidadeCandidataResponse;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unidades judiciárias de uma comarca que podem receber a designação de um servidor. Separado de
 * {@link FuncaoServidorApplicationService} de propósito: aquele responde pelo ciclo de vida da função
 * do servidor — designar, encerrar, verificar se pode executar — e este responde por quais unidades
 * entram na lista de escolha, que é pergunta sobre a malha judiciária e não sobre a função.
 */
@Service
public class UnidadesCandidatasParaDesignacaoService {

    private final UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;

    public UnidadesCandidatasParaDesignacaoService(
            UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository) {
        this.unidadeJudiciariaCompetenciaRepository =
                Objects.requireNonNull(unidadeJudiciariaCompetenciaRepository);
    }

    @Transactional(readOnly = true)
    public List<UnidadeCandidataResponse> naComarca(String comarcaUf, String comarcaNome) {
        return unidadeJudiciariaCompetenciaRepository
                .findAllByUfIgnoreCaseAndComarcaIgnoreCase(comarcaUf, comarcaNome).stream()
                .map(UnidadeCandidataResponse::from)
                .toList();
    }
}
