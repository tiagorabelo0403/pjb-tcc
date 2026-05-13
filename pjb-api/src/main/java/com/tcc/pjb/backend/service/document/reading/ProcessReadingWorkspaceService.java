package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingContentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingEcosystemResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPresetCatalogResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSearchHitResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import com.tcc.pjb.backend.service.intelligence.StructuredProcessSummaryService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProcessReadingWorkspaceService {

    private final ProcessReadingWorkspaceFacade facade;
    private final StructuredProcessSummaryService structuredProcessSummaryService;

    public ProcessReadingWorkspaceService(ProcessoRepository processoRepository,
                                          DocumentoProcessualRepository documentoRepository,
                                          DocumentoPaginaRepository paginaRepository,
                                          MovimentacaoProcessualRepository movimentacaoRepository,
                                          EventoProcessualRepository eventoRepository,
                                          PjbAuthorizationService authorizationService,
                                          RecursalEffectiveSecrecyService secrecyService,
                                          CurrentUserService currentUserService,
                                          ProcessReadingModeResolver modeResolver,
                                          ProcessReadingPresetResolver presetResolver,
                                          ProcessReadingNavigationResolver navigationResolver,
                                          ProcessReadingFlowResolver flowResolver,
                                          ProcessReadingProceduralContextResolver proceduralContextResolver,
                                          ProcessReadingSpecializationResolver specializationResolver,
                                          ProcessReadingEcosystemResolver ecosystemResolver,
                                          ProcessReadingSurfaceResolver surfaceResolver,
                                          ProcessReadingContentAssemblerService contentAssemblerService,
                                          StructuredProcessSummaryService structuredProcessSummaryService) {
        this.facade = new ProcessReadingWorkspaceFacade(
                processoRepository,
                documentoRepository,
                paginaRepository,
                movimentacaoRepository,
                eventoRepository,
                authorizationService,
                secrecyService,
                currentUserService,
                modeResolver,
                presetResolver,
                navigationResolver,
                flowResolver,
                proceduralContextResolver,
                specializationResolver,
                ecosystemResolver,
                surfaceResolver,
                contentAssemblerService
        );
        this.structuredProcessSummaryService = structuredProcessSummaryService;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingWorkspaceResponse assembleProcesso(Long processoId) {
        return facade.assembleProcesso(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingWorkspaceResponse assembleDocumento(UUID documentoId) {
        return facade.assembleDocumento(documentoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingNavigationResponse navigation(Long processoId) {
        return facade.navigation(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingFlowResponse flow(Long processoId) {
        return facade.flow(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse contentDocumento(UUID documentoId) {
        return facade.contentDocumento(documentoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse contentFluxo(Long processoId, String entryId) {
        return facade.contentFluxo(processoId, entryId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingPresetCatalogResponse presetCatalog(Long processoId) {
        return facade.presetCatalog(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingProceduralContextResponse proceduralContext(Long processoId) {
        return facade.proceduralContext(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingSpecializationResponse specialization(Long processoId) {
        return facade.specialization(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingEcosystemResponse ecosystem(Long processoId) {
        return facade.ecosystem(processoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProcessReadingSearchHitResponse> search(Long processoId, String query) {
        return facade.search(processoId, query);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public StructuredProcessSummaryResponse structuredSummary(Long processoId) {
        return structuredProcessSummaryService.summarize(processoId);
    }
}

