package com.tcc.pjb.backend.service.conciliacao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;

@Service
public class ConciliadorMediadorPainelService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final JudgeAgreementApprovalService judgeAgreementApprovalService;

    public ConciliadorMediadorPainelService(PerfilDashboardContextFactory contextFactory,
                                            PainelServiceCommons commons,
                                            ProcessoRepository processoRepository,
                                            WorkItemRepository workItemRepository,
                                            InstitutionalActorRoutingService institutionalActorRoutingService,
                                            JudgeAgreementApprovalService judgeAgreementApprovalService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.judgeAgreementApprovalService = judgeAgreementApprovalService;
    }

    public PerfilDashboardPayload.ConciliadorMediadorPayload bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<CalendarEventDto> agenda = commons.agenda(LocalDate.now(), LocalDate.now().plusDays(7));
        List<CalendarEventDto> sessoes = agenda.stream().filter(this::isSessao).toList();
        int pendentes = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "ACORDO", "SESSAO", "CEJUSC", "MEDIACAO")).count();
        int hoje = (int) sessoes.stream().filter(event -> event.at() != null && event.at().toLocalDate().equals(LocalDate.now())).count();
        int homologados = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "HOMOLOGADO", "ACORDO HOMOLOGADO")).count();
        int homologacaoPendente = (int) commons.inboxHibrido(usuario, 20).stream().filter(item -> commons.titleContains(item, "HOMOLOGAR", "ACORDO")).count();
        List<PerfilDashboardPayload.ConciliadorMediadorPayload.SessaoResumo> proximas = sessoes.stream().limit(8).map(event -> new PerfilDashboardPayload.ConciliadorMediadorPayload.SessaoResumo(String.valueOf(event.eventId()), event.eventType(), event.at() != null ? event.at().toString() : null, event.color(), event.processoNumero(), List.of("parte autora", "parte ré"))).toList();
        double taxa = homologados + pendentes == 0 ? 0d : ((double) homologados * 100d) / (double) (homologados + pendentes);
        String etag = commons.etag("CONCILIACAO", usuario.getId(), pendentes, hoje, homologados, homologacaoPendente, proximas, taxa, ctx.behavioralAudit());
        return new PerfilDashboardPayload.ConciliadorMediadorPayload(
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
                usuario.getComarca(),
                usuario.getTipoUsuario().name(),
                pendentes,
                hoje,
                homologados,
                homologacaoPendente,
                proximas,
                Math.round(taxa * 100.0) / 100.0
        );
    }

    public List<PerfilDashboardPayload.ConciliadorMediadorPayload.SessaoResumo> listarSessoesHoje() {
        return commons.agenda(LocalDate.now(), LocalDate.now()).stream().filter(this::isSessao).map(event -> new PerfilDashboardPayload.ConciliadorMediadorPayload.SessaoResumo(String.valueOf(event.eventId()), event.eventType(), event.at() != null ? event.at().toString() : null, event.color(), event.processoNumero(), List.of("parte autora", "parte ré"))).toList();
    }

    public List<Map<String, Object>> listarSessoesPendentes() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> commons.titleContains(item, "SESSAO", "CEJUSC", "MEDIACAO", "CONCILIACAO")).map(commons::mapWorkItem).toList();
    }

    @Transactional
    public Map<String, Object> registrarAcordo(String sessaoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        Processo processo = resolveProcesso(request);
        InstitutionalActorRoutingService.InstitutionalRoute route = processo != null
                ? institutionalActorRoutingService.gabineteDecision(processo.getId(), "HOMOLOGACAO_ACORDO")
                : null;
        WorkItem homologar = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("HOMOLOGAR_ACORDO:" + sessaoId)
                .type(WorkItemType.MANIFESTACAO)
                .titulo("Homologar acordo da sessão " + sessaoId)
                .descricao(String.valueOf(request))
                .queueCode(route == null ? "HOMOLOGACAO_ACORDO" : route.queueCode())
                .inboxKey(route == null ? "HOMOLOGACAO_ACORDO" : route.inboxKey())
                .assignedRole(route == null ? TipoUsuario.JUIZ : route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .build();
        homologar = workItemRepository.save(homologar);
        if (processo != null) {
            judgeAgreementApprovalService.requestApproval(processo, null, null, null, String.valueOf(request));
        }
        commons.publishTerritoryHistory(usuario, "ASSESSOR", "ACORDO_AGUARDANDO_HOMOLOGACAO", "Acordo enviado para homologação.", processo, homologar.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ACORDO_REGISTRADO");
        out.put("workItemId", homologar.getId());
        return out;
    }

    @Transactional
    public Map<String, Object> encerrarSessaoSemAcordo(String sessaoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        commons.publishUserHistory(usuario, "CONCILIACAO", "SESSAO_ENCERRADA_SEM_ACORDO", "Sessão encerrada sem acordo.", resolveProcesso(request), sessaoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ENCERRADA");
        out.put("sessaoId", sessaoId);
        out.put("resultado", "SEM_ACORDO");
        return out;
    }

    public List<Map<String, Object>> listarAcordosPendentesHomologacao() {
        return commons.inboxHibrido(contextFactory.build().usuario(), 20).stream().filter(item -> commons.titleContains(item, "HOMOLOGAR", "ACORDO")).map(commons::mapWorkItem).toList();
    }

    public Map<String, Object> carregarMetricasMes() {
        int homologados = listarAcordosPendentesHomologacao().size();
        int sessoes = listarSessoesPendentes().size() + listarSessoesHoje().size();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("sessoes", sessoes);
        out.put("homologacoesPendentes", homologados);
        out.put("taxaAcordoEstimada", sessoes == 0 ? 0d : (double) homologados / (double) sessoes);
        return out;
    }

    private boolean isSessao(CalendarEventDto event) {
        String t = event.title() == null ? "" : event.title().toUpperCase();
        return t.contains("SESS") || t.contains("CEJUSC") || t.contains("MEDIACAO") || t.contains("CONCILIACAO");
    }

    private Processo resolveProcesso(Object request) {
        if (request instanceof Map<?, ?> map) {
            Object v = map.get("processoId");
            if (v instanceof Number n) {
                return processoRepository.findById(n.longValue()).orElse(null);
            }
            if (v != null) {
                try {
                    return processoRepository.findById(Long.parseLong(String.valueOf(v))).orElse(null);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
