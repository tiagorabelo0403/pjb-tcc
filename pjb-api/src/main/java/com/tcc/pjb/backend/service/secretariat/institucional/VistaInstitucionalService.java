package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VistaInstitucionalService {

    private final ProcessoRepository processoRepository;
    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService;

    public VistaInstitucionalService(ProcessoRepository processoRepository,
                                     SecretariaInstitucionalEnfileiramentoService enfileiramentoService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.enfileiramentoService = Objects.requireNonNull(enfileiramentoService);
    }

    @Transactional
    public void determinarVista(Long processoId, TipoUnidadeInstitucional tipoInstituicaoAlvo, int prazoBaseDias) {
        if (tipoInstituicaoAlvo == TipoUnidadeInstitucional.FORUM) {
            throw new IllegalArgumentException("Vista institucional não se aplica ao próprio Fórum — o processo já está lá.");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        enfileiramentoService.enfileirar(processo.getId(), processo.getComarca(), tipoInstituicaoAlvo,
                MotivoEnfileiramentoInstitucional.DESPACHO_VISTA, prazoBaseDias);
    }
}
