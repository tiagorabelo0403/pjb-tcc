package com.tcc.pjb.backend.modules.custas.infrastructure;

import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaContexto;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProcessoCustaAdapter implements ProcessoCustaPort {

    private final ProcessoRepository processoRepository;

    public ProcessoCustaAdapter(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Override
    public Optional<ProcessoCustaContexto> obterContexto(Long processoId) {
        return processoRepository.findById(processoId)
                .map(processo -> new ProcessoCustaContexto(processo.getId(), processo.getUf(), processo.getRamoDireito(), processo.getRito()));
    }
}
