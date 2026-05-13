package com.tcc.pjb.backend.service.processo;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAccessApplicationService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;

    public ProcessoAccessApplicationService(ProcessoRepository processoRepository,
                                            PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public Processo load(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    public Processo loadCompleto(Long processoId) {
        return processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    public Processo loadAndRequireRead(Long processoId) {
        Processo processo = load(processoId);
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    public Processo loadCompletoAndRequireRead(Long processoId) {
        Processo processo = loadCompleto(processoId);
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    public Processo loadAndRequireCitizenPartyRead(Long processoId) {
        Processo processo = load(processoId);
        authorizationService.requireReadProcessoAsCidadaoParte(processo);
        return processo;
    }
}
