package com.tcc.pjb.backend.service.assessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.assessor.guardrails.AssessorGabineteGuardRailService;
import com.tcc.pjb.backend.service.juiz.handoff.JuizGabineteHandoffService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologyCoordinationMatrixService;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;

@Service
public class AssessorPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final AssessorGabineteGuardRailService assessorGabineteGuardRailService;
    private final JuizGabineteHandoffService handoffService;
    private final JudicialTopologyCoordinationMatrixService coordinationMatrixService;

    public AssessorPainelService(PerfilDashboardContextFactory contextFactory,
                                 PainelServiceCommons commons,
                                 ProcessoRepository processoRepository,
                                 AssessorGabineteGuardRailService assessorGabineteGuardRailService,
                                 JuizGabineteHandoffService handoffService,
                                 JudicialTopologyCoordinationMatrixService coordinationMatrixService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.assessorGabineteGuardRailService = assessorGabineteGuardRailService;
        this.handoffService = handoffService;
        this.coordinationMatrixService = coordinationMatrixService;
    }

    public PerfilDashboardPayload.AssessorPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = assessorGabineteGuardRailService.filtrarInboxCompativel(usuario, commons.inboxHibrido(usuario, 24));
        List<String> minutas = inbox.stream().filter(this::isMinuta).limit(8).map(commons::resumo).toList();
        List<String> drafts = inbox.stream().filter(this::isDecisionDraft).limit(8).map(commons::resumo).toList();
        List<String> audienciasHoje = listarAgendaHoje().stream().map(CalendarEventDto::title).limit(8).toList();
        int pendentesDespacho = (int) inbox.stream().filter(this::isDespacho).count();
        int pendentesAssinatura = (int) inbox.stream().filter(this::isAssinatura).count();
        long acervo = processoRepository.findForMagistradoDashboard(usuario.getUf(), usuario.getComarca(), PageRequest.of(0, 1)).getTotalElements();
        String etag = commons.etag("ASSESSOR", usuario.getId(), minutas, drafts, audienciasHoje, pendentesDespacho, pendentesAssinatura, acervo, ctx.behavioralAudit());
        return new PerfilDashboardPayload.AssessorPayload(
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
                ctx.persona().displayPerfil(),
                usuario.getUf() + ":" + usuario.getComarca(),
                minutas,
                drafts,
                audienciasHoje,
                pendentesDespacho,
                pendentesAssinatura,
                ctx.persona().delegacaoAtiva(),
                resolveGabineteKey(usuario, inbox)
        );
    }

    public List<Map<String, Object>> listarMinutasPendentesGabinete() {
        return assessorGabineteGuardRailService.listarMinutasCompativeis();
    }

    public List<CalendarEventDto> listarAgendaHoje() {
        return commons.agenda(LocalDate.now(), LocalDate.now());
    }

    public List<Map<String, Object>> listarProcessosPendentesDespacho() {
        return assessorGabineteGuardRailService.listarDespachosCompativeis();
    }

    public AssessorGabineteGuardRailService.AssessorProcessoGuardRailSnapshot guardrailsProcesso(Long processoId) {
        return assessorGabineteGuardRailService.avaliarProcesso(processoId);
    }


    public JuizGabineteHandoffService.HandoffSnapshot handoffProcesso(Long processoId) {
        return handoffService.snapshot(processoId);
    }

    public JuizGabineteHandoffService.HandoffAction devolverParaGabinete(Long processoId, String observacao) {
        return handoffService.devolverParaGabinete(processoId, observacao);
    }

    public JudicialTopologyCoordinationMatrixService.JudicialTopologyCoordinationMatrixSnapshot matrizProcesso(Long processoId) {
        return coordinationMatrixService.snapshot(processoId);
    }


    private String resolveGabineteKey(Usuario usuario, List<WorkItem> inbox) {
        if (inbox != null) {
            for (WorkItem item : inbox) {
                if (item == null) {
                    continue;
                }
                String inboxKey = item.getInboxKey();
                if (inboxKey != null && inboxKey.startsWith("GAB:")) {
                    return normalizeKey("GABINETE_" + inboxKey.substring(4));
                }
                String queueCode = item.getQueueCode();
                if (queueCode != null && !queueCode.isBlank()) {
                    String normalizedQueue = queueCode.trim().toUpperCase();
                    if (normalizedQueue.contains("GABINETE") || normalizedQueue.contains("ASSESSORIA")) {
                        return normalizeKey(normalizedQueue);
                    }
                }
            }
        }
        String uf = usuario == null || usuario.getUf() == null || usuario.getUf().isBlank() ? "XX" : usuario.getUf().trim().toUpperCase();
        String comarca = usuario == null || usuario.getComarca() == null || usuario.getComarca().isBlank() ? "BASE" : usuario.getComarca().trim().toUpperCase();
        return normalizeKey("GABINETE_" + uf + '_' + comarca);
    }

    private String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "GABINETE_BASE";
        }
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private boolean isMinuta(WorkItem item) {
        return commons.titleContains(item, "MINUTA", "DESPACHO", "DECISAO", "GABINETE");
    }

    private boolean isDecisionDraft(WorkItem item) {
        return commons.titleContains(item, "DECISAO", "SENTENCA", "ASSINATURA", "ACORDAO");
    }

    private boolean isDespacho(WorkItem item) {
        return commons.titleContains(item, "DESPACHO", "CONCLUSAO", "MINUTA");
    }

    private boolean isAssinatura(WorkItem item) {
        return commons.titleContains(item, "ASSINATURA", "PUBLICACAO", "ACORDAO");
    }
}
