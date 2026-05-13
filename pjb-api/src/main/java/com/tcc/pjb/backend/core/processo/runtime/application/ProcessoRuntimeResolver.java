package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoRuntimeResolver {

    private final ProcessoRepository processoRepository;

    public ProcessoRuntimeResolver(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public ProcessoRuntimeContext resolver(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = processo.getUsuario();
        TipoUsuario papelPrincipal = usuario == null || usuario.getTipoUsuario() == null ? TipoUsuario.SERVIDOR_FORUM : usuario.getTipoUsuario();
        return new ProcessoRuntimeContext(
                processo,
                processo.getId(),
                processo.getNumero(),
                processo.getNumeroUnificado(),
                processo.getRamoDireito(),
                processo.getRito(),
                papelPrincipal,
                processo.getTribunal(),
                processo.getVara(),
                processo.getComarca(),
                processo.getUf(),
                processo.getNivelSigilo() != null && processo.getNivelSigilo().nivel() > 0
        );
    }
}
