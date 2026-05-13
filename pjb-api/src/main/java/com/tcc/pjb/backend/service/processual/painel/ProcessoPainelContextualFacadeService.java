package com.tcc.pjb.backend.service.processual.painel;

import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelBndtApplicationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelContextualApplicationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelFonteOficialApplicationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelPrevidenciarioTrilhoApplicationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelRotaTaticaApplicationService;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelTelemetriaConectorApplicationService;
import com.tcc.pjb.backend.model.dto.processual.painel.contextual.ProcessoPainelContextualResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.fonte.ProcessoPainelFonteOficialResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.previdenciario.ProcessoPainelPrevidenciarioTrilhoResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.rota.ProcessoPainelRotaTaticaResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.telemetria.ProcessoPainelTelemetriaConectorResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.trabalhista.ProcessoPainelBndtResponse;
import com.tcc.pjb.backend.service.processual.painel.contextual.ProcessoPainelContextualWorkspaceAssembler;
import com.tcc.pjb.backend.service.processual.painel.fonte.ProcessoPainelFonteOficialAssembler;
import com.tcc.pjb.backend.service.processual.painel.previdenciario.ProcessoPainelPrevidenciarioTrilhoAssembler;
import com.tcc.pjb.backend.service.processual.painel.rota.ProcessoPainelRotaTaticaAssembler;
import com.tcc.pjb.backend.service.processual.painel.telemetria.ProcessoPainelTelemetriaConectorAssembler;
import com.tcc.pjb.backend.service.processual.painel.trabalhista.ProcessoPainelBndtAssembler;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelContextualFacadeService {

    private final ProcessoPainelContextualApplicationService processoPainelContextualApplicationService;
    private final ProcessoPainelTelemetriaConectorApplicationService processoPainelTelemetriaConectorApplicationService;
    private final ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService;
    private final ProcessoPainelBndtApplicationService processoPainelBndtApplicationService;
    private final ProcessoPainelPrevidenciarioTrilhoApplicationService processoPainelPrevidenciarioTrilhoApplicationService;
    private final ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService;
    private final ProcessoPainelContextualWorkspaceAssembler processoPainelContextualWorkspaceAssembler;
    private final ProcessoPainelTelemetriaConectorAssembler processoPainelTelemetriaConectorAssembler;
    private final ProcessoPainelFonteOficialAssembler processoPainelFonteOficialAssembler;
    private final ProcessoPainelBndtAssembler processoPainelBndtAssembler;
    private final ProcessoPainelPrevidenciarioTrilhoAssembler processoPainelPrevidenciarioTrilhoAssembler;
    private final ProcessoPainelRotaTaticaAssembler processoPainelRotaTaticaAssembler;

    public ProcessoPainelContextualFacadeService(ProcessoPainelContextualApplicationService processoPainelContextualApplicationService,
                                                 ProcessoPainelTelemetriaConectorApplicationService processoPainelTelemetriaConectorApplicationService,
                                                 ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService,
                                                 ProcessoPainelBndtApplicationService processoPainelBndtApplicationService,
                                                 ProcessoPainelPrevidenciarioTrilhoApplicationService processoPainelPrevidenciarioTrilhoApplicationService,
                                                 ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService,
                                                 ProcessoPainelContextualWorkspaceAssembler processoPainelContextualWorkspaceAssembler,
                                                 ProcessoPainelTelemetriaConectorAssembler processoPainelTelemetriaConectorAssembler,
                                                 ProcessoPainelFonteOficialAssembler processoPainelFonteOficialAssembler,
                                                 ProcessoPainelBndtAssembler processoPainelBndtAssembler,
                                                 ProcessoPainelPrevidenciarioTrilhoAssembler processoPainelPrevidenciarioTrilhoAssembler,
                                                 ProcessoPainelRotaTaticaAssembler processoPainelRotaTaticaAssembler) {
        this.processoPainelContextualApplicationService = processoPainelContextualApplicationService;
        this.processoPainelTelemetriaConectorApplicationService = processoPainelTelemetriaConectorApplicationService;
        this.processoPainelFonteOficialApplicationService = processoPainelFonteOficialApplicationService;
        this.processoPainelBndtApplicationService = processoPainelBndtApplicationService;
        this.processoPainelPrevidenciarioTrilhoApplicationService = processoPainelPrevidenciarioTrilhoApplicationService;
        this.processoPainelRotaTaticaApplicationService = processoPainelRotaTaticaApplicationService;
        this.processoPainelContextualWorkspaceAssembler = processoPainelContextualWorkspaceAssembler;
        this.processoPainelTelemetriaConectorAssembler = processoPainelTelemetriaConectorAssembler;
        this.processoPainelFonteOficialAssembler = processoPainelFonteOficialAssembler;
        this.processoPainelBndtAssembler = processoPainelBndtAssembler;
        this.processoPainelPrevidenciarioTrilhoAssembler = processoPainelPrevidenciarioTrilhoAssembler;
        this.processoPainelRotaTaticaAssembler = processoPainelRotaTaticaAssembler;
    }

    public ProcessoPainelContextualResponse painel(Long processoId, String profileCode) {
        return processoPainelContextualWorkspaceAssembler.toResponse(
                processoPainelContextualApplicationService.detalhar(processoId, profileCode)
        );
    }

    public ProcessoPainelTelemetriaConectorResponse telemetria(Long processoId) {
        return processoPainelTelemetriaConectorAssembler.toResponse(
                processoPainelTelemetriaConectorApplicationService.detalhar(processoId)
        );
    }

    public ProcessoPainelFonteOficialResponse fontesOficiais(Long processoId) {
        return processoPainelFonteOficialAssembler.toResponse(
                processoPainelFonteOficialApplicationService.detalhar(processoId)
        );
    }

    public ProcessoPainelBndtResponse bndt(Long processoId) {
        return processoPainelBndtAssembler.toResponse(
                processoPainelBndtApplicationService.detalhar(processoId)
        );
    }

    public ProcessoPainelPrevidenciarioTrilhoResponse trilhoPrevidenciario(Long processoId) {
        return processoPainelPrevidenciarioTrilhoAssembler.toResponse(
                processoPainelPrevidenciarioTrilhoApplicationService.detalhar(processoId)
        );
    }

    public ProcessoPainelRotaTaticaResponse rotaTatica(Long processoId) {
        return processoPainelRotaTaticaAssembler.toResponse(
                processoPainelRotaTaticaApplicationService.detalhar(processoId)
        );
    }
}
