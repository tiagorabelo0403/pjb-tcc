package com.tcc.pjb.backend.service.institutional.movimentacao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MovimentacaoProcessualRegistrar {

    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final ProcessoRepository processoRepository;

    public MovimentacaoProcessualRegistrar(MovimentacaoProcessualRepository movimentacaoRepository,
                                           ProcessoRepository processoRepository) {
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional
    public MovimentacaoProcessual registrar(Processo processo, Usuario ator, FaseProcessual faseOrigem, String descricao) {
        Instant agora = Instant.now();
        MovimentacaoProcessual movimentacao = movimentacaoRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(faseOrigem)
                .fasePara(processo.getFaseAtual())
                .descricao(descricao)
                .ator(ator)
                .dataMovimentacao(agora)
                .build());
        processo.setDataUltimaMovimentacao(agora);
        processoRepository.save(processo);
        return movimentacao;
    }
}
