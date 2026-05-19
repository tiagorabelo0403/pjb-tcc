package com.tcc.pjb.backend.core.processo.autuacao.application;

import com.tcc.pjb.backend.core.processo.autuacao.domain.NumeracaoCnjService;
import com.tcc.pjb.backend.model.entity.processo.SequencialNumeracaoCnj;
import com.tcc.pjb.backend.model.repository.SequencialNumeracaoCnjRepository;
import jakarta.inject.Inject;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeracaoCnjApplicationService {

    private final SequencialNumeracaoCnjRepository sequencialRepository;
    private final NumeracaoCnjService numeracaoService;

    @Inject
    public NumeracaoCnjApplicationService(SequencialNumeracaoCnjRepository sequencialRepository) {
        this.sequencialRepository = sequencialRepository;
        this.numeracaoService = new NumeracaoCnjService();
    }

    @Transactional
    public String gerarProximo(int segmento, int tribunal, int unidade) {
        int ano = LocalDate.now().getYear();
        SequencialNumeracaoCnj seq = sequencialRepository
                .findAndLock(segmento, tribunal, unidade, ano)
                .orElseGet(() -> {
                    SequencialNumeracaoCnj novo = new SequencialNumeracaoCnj(segmento, tribunal, unidade, ano);
                    return sequencialRepository.save(novo);
                });
        long proximoSeq = seq.incrementar();
        sequencialRepository.save(seq);
        return numeracaoService.gerar((int) proximoSeq, ano, segmento, tribunal, unidade);
    }

    @Transactional(readOnly = true)
    public boolean validar(String numeroUnificado) {
        return numeracaoService.validar(numeroUnificado);
    }
}
