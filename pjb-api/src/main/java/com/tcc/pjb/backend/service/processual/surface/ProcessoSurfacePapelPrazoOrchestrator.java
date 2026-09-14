package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: papéis processuais e prazos --
 * catálogo de perfis com capacidades por papel e cálculo de marcos temporais
 * (agregado ou específico).
 */
@Service
public class ProcessoSurfacePapelPrazoOrchestrator {

    private final ProcessoPapelApplicationService processoPapelApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;

    public ProcessoSurfacePapelPrazoOrchestrator(ProcessoPapelApplicationService processoPapelApplicationService,
                                                  ProcessoPrazoApplicationService processoPrazoApplicationService) {
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
    }

    public ProcessoPapelAggregate papeis(Long processoId) {
        return processoPapelApplicationService.detalhar(processoId);
    }

    public ProcessoPapelPerfil perfil(Long processoId, String profileCode) {
        return processoPapelApplicationService.detalharPerfil(processoId, profileCode);
    }

    public ProcessoPrazoAggregate prazos(Long processoId) {
        return processoPrazoApplicationService.detalhar(processoId);
    }

    public ProcessoPrazoMarco calcularPrazo(Long processoId, NationalPrazoEngine.TipoPrazo tipoPrazo) {
        return processoPrazoApplicationService.calcular(processoId, tipoPrazo);
    }
}
