package com.tcc.pjb.backend.service.defensor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;

@Service
public class DefensorPublicoPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final PainelSharedExperienceService sharedExperienceService;
    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;

    public DefensorPublicoPainelService(PerfilDashboardContextFactory contextFactory,
                                        PainelServiceCommons commons,
                                        ProcessoRepository processoRepository,
                                        RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService,
                                        InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                        InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                        PainelSharedExperienceService sharedExperienceService,
                                        PainelSignalReflectionService signalReflectionService,
                                        PainelNativeCollectionCompositionService collectionCompositionService,
                                        PainelActionSurfaceCompositionService actionSurfaceCompositionService,
                                        PainelExecutionSurfaceCompositionService executionSurfaceCompositionService,
                                        InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.recursalPeticionamentoFacadeService = recursalPeticionamentoFacadeService;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.sharedExperienceService = sharedExperienceService;
        this.signalReflectionService = signalReflectionService;
        this.collectionCompositionService = collectionCompositionService;
        this.actionSurfaceCompositionService = actionSurfaceCompositionService;
        this.executionSurfaceCompositionService = executionSurfaceCompositionService;
        this.institutionalMaterialActionGuardService = institutionalMaterialActionGuardService;
    }

    public PerfilDashboardPayload.DefensorPublicoPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<com.tcc.pjb.backend.model.entity.workflow.WorkItem> inbox = commons.inboxHibrido(usuario, 30);
        long assistidos = processoRepository.findForMagistradoDashboard(usuario.getUf(), usuario.getComarca(), PageRequest.of(0, 1)).getTotalElements();
        List<CalendarEventDto> audienciasHoje = listarAudienciasHoje();
        int peticoes = (int) inbox.stream().filter(item -> commons.titleContains(item, "PETICAO", "INICIAL", "DEFESA")).count();
        int recursos = (int) inbox.stream().filter(item -> commons.titleContains(item, "RECURSO", "APELACAO", "AGRAVO")).count();
        int prazos48h = (int) inbox.stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS))).count();
        List<String> prioridade = inbox.stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).limit(8).map(commons::resumo).toList();
        String etag = commons.etag("DEFENSOR", usuario.getId(), assistidos, audienciasHoje.size(), peticoes, recursos, prazos48h, prioridade, ctx.behavioralAudit());
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve("DEFENSORIA", "PAINEL_DEFENSORIA", usuario.getTipoUsuario());
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("DEFENSOR_PUBLICO");
        Map<String, Object> operationalSignals = signalReflectionService.deriveSignals("DEFENSOR_PUBLICO", sharedExperience, peticoes + recursos, prazos48h, "ATUACAO_ASSISTENCIAL");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("DEFENSOR_PUBLICO", operationalSignals);
        prioridade = collectionCompositionService.composeList("DEFENSOR_PUBLICO", "PROCESSOS_PRIORIDADE_ALTA", prioridade, operationalSignals, nativeComposition);
        Map<String, Object> collectionComposition = collectionCompositionService.buildCollectionComposition("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, Map.of(
                "processosPrioridadeAlta", prioridade
        ));
        Map<String, Object> actionSurface = actionSurfaceCompositionService.buildActionSurface("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, collectionComposition);
        Map<String, Object> executionSurface = executionSurfaceCompositionService.buildExecutionSurface("DEFENSOR_PUBLICO", operationalSignals, nativeComposition, collectionComposition, actionSurface);
        Map<String, Object> panelVisualIdentity = signalReflectionService.reflectInBlock("DEFENSOR_PUBLICO", "VISUAL_IDENTITY", castMap(panelBranding.get("panelVisualIdentity")), operationalSignals);
        panelVisualIdentity = collectionCompositionService.decorateBlock("DEFENSOR_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, operationalSignals, nativeComposition);
        panelVisualIdentity = actionSurfaceCompositionService.decorateBlock("DEFENSOR_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, actionSurface, nativeComposition);
        panelVisualIdentity = executionSurfaceCompositionService.decorateBlock("DEFENSOR_PUBLICO", "VISUAL_IDENTITY", panelVisualIdentity, executionSurface, nativeComposition);
        return new PerfilDashboardPayload.DefensorPublicoPayload(
                etag,
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                ctx.pendencias(),
                ctx.prazoRadar(),
                ctx.sessionRisk(),
                ctx.sigiloAtivo(),
                ctx.plantao(),
                ctx.onboarding(),
                ctx.externalSystems(),
                ctx.behavioralAudit(),
                usuario.getUf() + ":" + usuario.getComarca(),
                usuario.getTipoUsuario().name().contains("FEDERAL"),
                (int) assistidos,
                audienciasHoje.size(),
                peticoes,
                recursos,
                prazos48h,
                prioridade,
                true,
                castMap(panelBranding.get("institutionalBranding")),
                panelVisualIdentity,
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface,
                executionSurface,
                sharedExperience
        );
    }

    public List<Map<String, Object>> listarAssistidosAtivos() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().map(commons::mapWorkItem).toList();
    }

    public Map<String, Object> listarProcessosDoAssistido(Long assistidoId) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("assistidoId", assistidoId);
        out.put("processos", commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().map(commons::mapWorkItem).toList());
        return out;
    }

    @Transactional
    public Map<String, Object> registrarPeticao(Long processoId, Object request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_PETICAO);
        Usuario usuario = contextFactory.build().usuario();
        commons.publishUserHistory(usuario, "DEFENSOR", "PETICAO_REGISTRADA", "Petição registrada na defensoria.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PETICAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("payload", String.valueOf(request));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "DEFENSORIA",
                        "PETICAO_INSTITUCIONAL",
                        processoId,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    public List<CalendarEventDto> listarAudienciasHoje() {
        return commons.agenda(LocalDate.now(), LocalDate.now());
    }

    @Transactional
    public Map<String, Object> interporRecurso(Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_RECURSO);
        return recursalPeticionamentoFacadeService.interporRecurso(
                processoId,
                tipoRecurso,
                razoes,
                fundamentacao,
                pedidoEfeitoSuspensivo,
                preparoDispensado,
                observacoes
        );
    }

    public List<Map<String, Object>> listarPrazosCriticos() {
        Instant now = Instant.now().plus(72, ChronoUnit.HOURS);
        return commons.inboxHibrido(contextFactory.build().usuario(), 30).stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(now)).map(commons::mapWorkItem).toList();
    }

    @Transactional
    public Map<String, Object> registrarRequerimentoGratuidade(Long processoId, Object request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_GRATUIDADE);
        Usuario usuario = contextFactory.build().usuario();
        commons.publishUserHistory(usuario, "DEFENSOR", "GRATUIDADE_REQUERIDA", "Requerimento de gratuidade protocolado.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REQUERIDO");
        out.put("processoId", processoId);
        out.put("fundamento", WorkItemType.MANIFESTACAO.name());
        out.put("payload", String.valueOf(request));
        return out;
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
