package com.tcc.pjb.backend.service.perito;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.pericia.PeritoNomeacaoService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;

@Service
public class PeritoPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final PeritoNomeacaoService peritoNomeacaoService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final CalendarInstitutionalBridgeService institutionalBridgeService;

    public PeritoPainelService(PerfilDashboardContextFactory contextFactory,
                               PainelServiceCommons commons,
                               PeritoNomeacaoRepository peritoNomeacaoRepository,
                               PeritoNomeacaoService peritoNomeacaoService,
                               InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                               InstitutionalPanelBrandingService institutionalPanelBrandingService,
                               CalendarInstitutionalBridgeService institutionalBridgeService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.peritoNomeacaoRepository = peritoNomeacaoRepository;
        this.peritoNomeacaoService = peritoNomeacaoService;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalPanelBrandingService = institutionalPanelBrandingService;
        this.institutionalBridgeService = institutionalBridgeService;
    }

    public PerfilDashboardPayload.PeritoPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<PeritoNomeacao> ativas = peritoNomeacaoRepository.findTop100ByPerito_IdAndStatusInOrderByNomeadoEmDesc(usuario.getId(), List.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO));
        int aguardandoAceite = (int) ativas.stream().filter(n -> n.getStatus() == PeritoNomeacaoStatus.NOMEADO).count();
        int laudosPendentes = (int) commons.inboxHibrido(usuario, 30).stream().filter(item -> commons.titleContains(item, "LAUDO", "PERICIA", "PARECER")).count();
        int laudosAtrasados = (int) commons.inboxHibrido(usuario, 30).stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(Instant.now())).count();
        List<PerfilDashboardPayload.PeritoPayload.NomeacaoResumo> nomeacoes = ativas.stream().limit(10).map(n -> new PerfilDashboardPayload.PeritoPayload.NomeacaoResumo(n.getId(), n.getProcesso() != null ? n.getProcesso().getNumeroProcesso() : null, n.getProcesso() != null ? n.getProcesso().getAssunto() : null, n.getNomeadoEm() != null ? n.getNomeadoEm().plusDays(5).toString() : null, n.getStatus().name())).toList();
        List<String> prazos = commons.inboxHibrido(usuario, 20).stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(Instant.now().plus(7, ChronoUnit.DAYS))).map(commons::resumo).toList();
        CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridgeForUser(usuario, LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC).plusDays(14), null);
        var institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
        String etag = commons.etag("PERITO", usuario.getId(), aguardandoAceite, laudosPendentes, laudosAtrasados, nomeacoes, prazos, ctx.behavioralAudit(), institutionalBridge);
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve("PERICIA", "PAINEL_PERICIA", usuario.getTipoUsuario());
        return new PerfilDashboardPayload.PeritoPayload(
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
                usuario.getTipoUsuario().name(),
                laudosPendentes,
                laudosAtrasados,
                aguardandoAceite,
                nomeacoes,
                prazos,
                usuario.getRegistroProfissional() != null && !usuario.getRegistroProfissional().isBlank(),
                usuario.getTipoUsuario().name(),
                usuario.getRegistroProfissional(),
                castMap(panelBranding.get("institutionalBranding")),
                castMap(panelBranding.get("panelVisualIdentity")),
                institutionalFocus,
                institutionalBridge
        );
    }

    public List<Map<String, Object>> listarNomeacoesAtivas() {
        return peritoNomeacaoRepository.findTop100ByPerito_IdAndStatusInOrderByNomeadoEmDesc(contextFactory.build().usuario().getId(), List.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO)).stream().map(this::mapNomeacao).toList();
    }

    @Transactional
    public Map<String, Object> aceitarNomeacao(Long nomeacaoId) {
        return mapResponse(peritoNomeacaoService.aceitarNomeacao(nomeacaoId));
    }

    @Transactional
    public Map<String, Object> recusarNomeacao(Long nomeacaoId, Object request) {
        return mapResponse(peritoNomeacaoService.recusarNomeacao(nomeacaoId, String.valueOf(request)));
    }

    public List<Map<String, Object>> listarLaudosPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> commons.titleContains(item, "LAUDO", "PERICIA")).map(commons::mapWorkItem).toList();
    }

    @Transactional
    public Map<String, Object> entregarLaudo(Long processoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        commons.publishUserHistory(usuario, "PERITO", "LAUDO_ENTREGUE", "Laudo entregue pelo perito.", null, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ENTREGUE");
        out.put("processoId", processoId);
        out.put("payload", String.valueOf(request));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "PERICIA",
                        "LAUDO_PERICIAL",
                        processoId,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        true
                )
        ));
        return out;
    }

    public List<String> listarPrazosVencendo() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(Instant.now().plus(7, ChronoUnit.DAYS))).map(commons::resumo).toList();
    }

    private Map<String, Object> mapNomeacao(PeritoNomeacao n) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", n.getId());
        out.put("status", n.getStatus());
        out.put("processoId", n.getProcesso() != null ? n.getProcesso().getId() : null);
        out.put("processoNumero", n.getProcesso() != null ? n.getProcesso().getNumeroProcesso() : null);
        out.put("assunto", n.getProcesso() != null ? n.getProcesso().getAssunto() : null);
        out.put("nomeadoEm", n.getNomeadoEm());
        out.put("respondidoEm", n.getRespondidoEm());
        return out;
    }

    private Map<String, Object> mapResponse(com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoResponse response) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", response.getId());
        out.put("processoId", response.getProcessoId());
        out.put("peritoId", response.getPeritoId());
        out.put("status", response.getStatus());
        out.put("nomeadoEm", response.getNomeadoEm());
        out.put("respondidoEm", response.getRespondidoEm());
        out.put("observacao", response.getObservacao());
        return out;
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
