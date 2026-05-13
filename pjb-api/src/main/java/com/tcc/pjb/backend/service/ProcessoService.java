package com.tcc.pjb.backend.service;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class ProcessoService {

    private final ProcessoRepository repository;
    private final ProcessoResponseAssemblerService processoResponseAssemblerService;
    private final AuditoriaService auditoriaService;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;

    public ProcessoService(ProcessoRepository repository,
                           ProcessoResponseAssemblerService processoResponseAssemblerService,
                           AuditoriaService auditoriaService,
                           CurrentUserService currentUserService,
                           PjbAuthorizationService authorizationService) {
        this.repository = repository;
        this.processoResponseAssemblerService = processoResponseAssemblerService;
        this.auditoriaService = auditoriaService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    public ProcessoResponse consultar(String numero, String usuario) {

        Processo processo = repository.findByNumeroProcesso(numero)
                .orElseThrow(() -> new RuntimeException("Processo inexistente"));

        
        authorizationService.requireReadProcesso(processo);

        
        String actor = currentUserService.getOptional()
                .map(u -> u.getCpf() != null ? u.getCpf() : u.getEmail())
                .orElse(usuario);
        auditoriaService.registrar(actor, "CONSULTA_PROCESSO", numero);

        return processoResponseAssemblerService.toResponse(processo);
    }
}
