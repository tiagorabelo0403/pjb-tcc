package com.tcc.pjb.backend.service.psicossocial;

import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialParecerRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialRelatorioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PsicossocialJudicialPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;

    public PsicossocialJudicialPainelService(PerfilDashboardContextFactory contextFactory,
                                             PainelServiceCommons commons,
                                             ProcessoRepository processoRepository,
                                             WorkItemRepository workItemRepository,
                                             InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                        InstitutionalPanelBrandingService institutionalPanelBrandingService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
    }

    public PerfilDashboardPayload.PsicossocialJudicialPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        int estudos = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "ESTUDO", "PARECER", "LAUDO")).count();
        int visitas = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "VISITA", "DOMICILIAR")).count();
        int urgentes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        List<String> casos = commons.inboxHibrido(usuario, 20).stream().limit(8).map(commons::resumo).toList();
        String etag = commons.etag("PSICOSSOCIAL", usuario.getId(), estudos, visitas, urgentes, casos, ctx.behavioralAudit());
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve("PSICOSSOCIAL", "PAINEL_PSICOSSOCIAL", usuario.getTipoUsuario());
        return new PerfilDashboardPayload.PsicossocialJudicialPayload(etag, ctx.generatedAt(), ctx.perfilAtivo(), ctx.tratamento(), ctx.pendencias(), ctx.prazoRadar(), ctx.sessionRisk(), ctx.sigiloAtivo(), ctx.plantao(), ctx.onboarding(), ctx.externalSystems(), ctx.behavioralAudit(), usuario.getTipoUsuario().name(), estudos, visitas, urgentes, casos, castMap(panelBranding.get("institutionalBranding")), castMap(panelBranding.get("panelVisualIdentity")));
    }

    public List<Map<String, Object>> listarCasosPrioritarios() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().map(commons::mapWorkItem).toList();
    }

    @Transactional
    public Map<String, Object> registrarParecer(Long processoId, PsicossocialParecerRequest request) {
        Processo processo = resolveProcesso(processoId);
        Usuario usuario = contextFactory.build().usuario();
        PsicossocialParecerRequest safe = request == null
                ? new PsicossocialParecerRequest("Parecer psicossocial", "Recomendações não informadas", List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.TRUE)
                : request;
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("PARECER_PSICOSSOCIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.PETICAO)
                .titulo("Parecer psicossocial — " + processo.getNumeroProcesso())
                .descricao(safe.parecer())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(safe.recomendacoes())
                .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        item = workItemRepository.save(item);
        commons.publishUserHistory(usuario, "PSICOSSOCIAL", "PARECER_REGISTRADO", "Parecer psicossocial registrado.", processo, item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PARECER_PSICOSSOCIAL_REGISTRADO");
        out.put("processoId", processoId);
        out.put("workItemId", item.getId());
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "PSICOSSOCIAL",
                        "PARECER_PSICOSSOCIAL",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        true
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> entregarRelatorio(Long processoId, PsicossocialRelatorioRequest request) {
        Processo processo = resolveProcesso(processoId);
        Usuario usuario = contextFactory.build().usuario();
        PsicossocialRelatorioRequest safe = request == null
                ? new PsicossocialRelatorioRequest("Relatório psicossocial", "Conclusão não informada", List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.TRUE)
                : request;
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("RELATORIO_PSICOSSOCIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.LAUDO)
                .titulo("Relatório psicossocial — " + processo.getNumeroProcesso())
                .descricao(safe.relatorio())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(safe.conclusao())
                .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        item = workItemRepository.save(item);
        commons.publishUserHistory(usuario, "PSICOSSOCIAL", "RELATORIO_REGISTRADO", "Relatório psicossocial registrado.", processo, item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RELATORIO_PSICOSSOCIAL_ENTREGUE");
        out.put("processoId", processoId);
        out.put("workItemId", item.getId());
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "PSICOSSOCIAL",
                        "RELATORIO_PSICOSSOCIAL",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        true
                )
        ));
        return out;
    }

    private Processo resolveProcesso(Long processoId) {
        return processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }




    public List<Map<String, Object>> listarAgendaTecnica() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "VISITA", "ENTREVISTA", "AUDIENCIA", "ESCUTA"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public List<Map<String, Object>> listarVisitasDomiciliares() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream()
                .filter(item -> commons.titleContains(item, "VISITA", "DOMICILIAR"))
                .map(commons::mapWorkItem)
                .toList();
    }

    public Map<String, Object> resumoSensibilidadeCasos() {
        Usuario usuario = contextFactory.build().usuario();
        long visitas = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "VISITA", "DOMICILIAR")).count();
        long estudos = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "ESTUDO", "PARECER", "LAUDO")).count();
        long sensiveis = commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "ABRIGAMENTO", "VIOLENCIA", "INFANCIA", "FAMILIA")).count();
        long urgentes = commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getPrioridade() != null && item.getPrioridade() <= 1).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("usuario", usuario.getNome());
        out.put("visitasDomiciliares", visitas);
        out.put("estudosTecnicos", estudos);
        out.put("casosSensiveis", sensiveis);
        out.put("itensUrgentes", urgentes);
        out.put("nivelCuidado", sensiveis >= 3 || urgentes >= 3 ? "INTENSIVO" : sensiveis >= 1 || urgentes >= 1 ? "REFORCADO" : "PADRAO");
        return out;
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
