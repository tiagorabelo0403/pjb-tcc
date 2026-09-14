package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoAggregate;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: eixo de proteção do processo --
 * sigilo declarado, hardening final (endurecimento pós-decisão), sigilo inteligente
 * (classificação avaliada) e notificações de sigilo (planejar/disparar).
 */
@Service
public class ProcessoSurfaceSigiloOrchestrator {

    private final ProcessoSigiloApplicationService processoSigiloApplicationService;
    private final ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService;
    private final ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService;
    private final ProcessoSigiloNotificacaoApplicationService processoSigiloNotificacaoApplicationService;

    public ProcessoSurfaceSigiloOrchestrator(ProcessoSigiloApplicationService processoSigiloApplicationService,
                                              ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService,
                                              ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService,
                                              ProcessoSigiloNotificacaoApplicationService processoSigiloNotificacaoApplicationService) {
        this.processoSigiloApplicationService = Objects.requireNonNull(processoSigiloApplicationService);
        this.processoHardeningFinalApplicationService = Objects.requireNonNull(processoHardeningFinalApplicationService);
        this.processoSigiloInteligenteApplicationService = Objects.requireNonNull(processoSigiloInteligenteApplicationService);
        this.processoSigiloNotificacaoApplicationService = Objects.requireNonNull(processoSigiloNotificacaoApplicationService);
    }

    public ProcessoSigiloAggregate sigilo(Long processoId) {
        return processoSigiloApplicationService.detalhar(processoId);
    }

    public ProcessoHardeningAggregate hardening(Long processoId) {
        return processoHardeningFinalApplicationService.detalhar(processoId);
    }

    public ProcessoSigiloInteligenteAggregate sigiloInteligente(Long processoId) {
        return processoSigiloInteligenteApplicationService.avaliar(processoId);
    }

    public ProcessoSigiloNotificacaoAggregate planejarSigiloNotificacoes(Long processoId) {
        return processoSigiloNotificacaoApplicationService.planejar(processoId);
    }

    public ProcessoSigiloNotificacaoAggregate dispararSigiloNotificacoes(Long processoId) {
        return processoSigiloNotificacaoApplicationService.notificar(processoId);
    }
}
