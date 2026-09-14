package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Checagens de elegibilidade para abrir uma thread de atendimento entre cidadão e advogado.
 * Extraído de {@link AtendimentoChatService} porque {@code clienteRepository} e
 * {@code procuracaoRepository} são usados exclusivamente por {@code createThread()}.
 */
@Service
public class AtendimentoThreadCreationEligibilityService {

    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;

    public AtendimentoThreadCreationEligibilityService(LaianeProcuracaoRepository procuracaoRepository,
                                                        ClienteRepository clienteRepository) {
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    public boolean cidadaoEhClienteDoAdvogado(String cpfHash, Long advogadoId) {
        return clienteRepository.existsByCpfHashAndAdvogado_Id(cpfHash, advogadoId);
    }

    public boolean advogadoTemProcuracaoAtivaNoProcesso(Long advogadoId, Long processoId) {
        return procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatus(advogadoId, processoId, LaianeProcuracaoStatus.ATIVA);
    }
}
