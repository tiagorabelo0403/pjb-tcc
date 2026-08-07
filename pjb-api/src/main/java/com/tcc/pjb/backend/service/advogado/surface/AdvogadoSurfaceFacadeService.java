package com.tcc.pjb.backend.service.advogado.surface;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoDashboardDto;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoClienteAnaliticoItemResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCockpitSnapshotResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoDashboardSummaryResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoHonorariosResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOfficeBulkApproveResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOperacaoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoHonorariosCalculoRequest;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.modules.advocacia.office.dto.BulkApproveRequest;
import com.tcc.pjb.backend.modules.advocacia.office.dto.DecisionRequest;
import com.tcc.pjb.backend.modules.advocacia.office.dto.DelegacaoRegraDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficePolicyDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficePolicyService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeSignatureQueueService;
import com.tcc.pjb.backend.service.advogado.AdvogadoCockpitService;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.advogado.CockpitSnapshot;
import com.tcc.pjb.backend.service.advogado.AdvogadoDashboardService;
import com.tcc.pjb.backend.service.exception.EquipeNaoSelecionadaException;

@Service
public class AdvogadoSurfaceFacadeService {

    private final AdvogadoDashboardService advogadoDashboardService;
    private final AdvogadoCockpitService advogadoCockpitService;
    private final CurrentUserService currentUserService;
    private final OfficePolicyService officePolicyService;
    private final OfficeSignatureQueueService officeSignatureQueueService;
    private final UserCalendarPanelService calendarPanelService;

    public AdvogadoSurfaceFacadeService(AdvogadoDashboardService advogadoDashboardService,
                                        AdvogadoCockpitService advogadoCockpitService,
                                        CurrentUserService currentUserService,
                                        OfficePolicyService officePolicyService,
                                        OfficeSignatureQueueService officeSignatureQueueService,
                                        UserCalendarPanelService calendarPanelService) {
        this.advogadoDashboardService = Objects.requireNonNull(advogadoDashboardService);
        this.advogadoCockpitService = Objects.requireNonNull(advogadoCockpitService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officePolicyService = Objects.requireNonNull(officePolicyService);
        this.officeSignatureQueueService = Objects.requireNonNull(officeSignatureQueueService);
        this.calendarPanelService = Objects.requireNonNull(calendarPanelService);
    }

    public AdvogadoDashboardSummaryResponse dashboardSummary(int horizonDays, int maxItems) {
        AdvogadoDashboardDto.SummaryResponse source = advogadoDashboardService.summary(horizonDays, maxItems);
        return new AdvogadoDashboardSummaryResponse(
                source.getGeneratedAt(),
                source.getClientesAtivos(),
                source.getClientesArquivados(),
                source.getWorkItemsOverdue(),
                source.getWorkItemsDueSoon(),
                source.getAgendaProxima().stream()
                        .map(item -> new AdvogadoDashboardSummaryResponse.AgendaEventLiteResponse(
                                item.getId(),
                                item.getTipo(),
                                item.getTitulo(),
                                item.getDataInicio(),
                                item.getDataFim(),
                                item.getProcessoId(),
                                item.getProcessoNumero()
                        ))
                        .toList(),
                source.getPrazosCriticos().stream()
                        .map(item -> new AdvogadoDashboardSummaryResponse.WorkItemLiteResponse(
                                item.getId(),
                                item.getProcessoId(),
                                item.getProcessoNumero(),
                                item.getTitulo(),
                                item.getDueAt(),
                                item.getStatus(),
                                item.getPrioridade()
                        ))
                        .toList(),
                calendarPanelService.panel(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(Math.max(horizonDays, 31)), null)
        );
    }

    public AdvogadoCockpitSnapshotResponse cockpitSnapshot() {
        CockpitSnapshot source = advogadoCockpitService.bootstrapCockpit();
        return new AdvogadoCockpitSnapshotResponse(
                source.generatedAt(),
                source.perfilAtivo(),
                source.tratamento(),
                source.escritorio(),
                source.carteiraTotalProcessos(),
                source.prazosCriticos().stream()
                        .map(item -> new AdvogadoCockpitSnapshotResponse.PrazoCriticoItemResponse(
                                item.workItemId(),
                                item.titulo(),
                                item.dueAt(),
                                item.horasRestantes(),
                                item.numeroProcesso()
                        ))
                        .toList(),
                source.intimacoesPendentes(),
                source.peticoesPendentes(),
                source.audienciasProximas(),
                source.intimacoesNaoLidas(),
                source.recursosVencendo(),
                source.prazoRadar(),
                source.sessionRisk()
        );
    }

    public AdvogadoOperacaoResponse protocolizarPeticao(Long processoId, String tipoPeticao, String conteudo, String fundamentacao) {
        return toOperacaoResponse(advogadoCockpitService.protocolizarPeticao(processoId, tipoPeticao, conteudo, fundamentacao));
    }

    public AdvogadoOperacaoResponse darCienciaEmLote(List<Long> workItemIds) {
        return toOperacaoResponse(advogadoCockpitService.darCienciaIntimacaoEmLote(workItemIds));
    }

    public AdvogadoOperacaoResponse prorrogarPrazoEmLote(List<Long> processoIds, String justificativa) {
        return toOperacaoResponse(advogadoCockpitService.prorrogarPrazoEmLote(processoIds, justificativa));
    }

    public AdvogadoHonorariosResponse calcularHonorarios(Long processoId, AdvogadoHonorariosCalculoRequest request) {
        return advogadoCockpitService.calcularHonorarios(processoId, request);
    }

    public List<com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCustaItemResponse> listarCustas(Long processoId) {
        return advogadoCockpitService.listarCustas(processoId);
    }

    public com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOabRegularidadeResponse consultarRegularidadeOab() {
        return advogadoCockpitService.consultarRegularidadeOab();
    }

    public com.tcc.pjb.backend.model.dto.jurisprudencia.JurisprudenceContextualSearchResponse buscarJurisprudenciaDoProcesso(
            Long processoId, String query, int topK) {
        return advogadoCockpitService.buscarJurisprudenciaDoProcesso(processoId, query, topK);
    }

    public com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoPainelFinanceiroResponse consultarPainelFinanceiro(Long processoId) {
        return advogadoCockpitService.consultarPainelFinanceiro(processoId);
    }

    public List<AdvogadoClienteAnaliticoItemResponse> analiticoPorCliente(String clienteCpfCnpj) {
        return advogadoCockpitService.analiticoPorCliente(clienteCpfCnpj).stream()
                .map(this::toAnaliticoItem)
                .toList();
    }

    public OfficePolicyDto getPolicy(Long equipeId) {
        return officePolicyService.getOrCreatePolicy(currentUserId(), resolveEquipeId(equipeId));
    }

    public OfficePolicyDto updatePolicy(Long equipeId, OfficePolicyDto req) {
        return officePolicyService.updatePolicy(currentUserId(), resolveEquipeId(equipeId), req);
    }

    public DelegacaoRegraDto upsertRegra(Long equipeId, Long usuarioId, DelegacaoRegraDto req) {
        return officePolicyService.upsertRegra(currentUserId(), resolveEquipeId(equipeId), usuarioId, req);
    }

    public DelegacaoRegraDto getRegra(Long equipeId, Long usuarioId) {
        return officePolicyService.getRegra(currentUserId(), resolveEquipeId(equipeId), usuarioId);
    }

    public List<DelegacaoRegraDto> listRegras(Long equipeId) {
        return officePolicyService.listarRegras(currentUserId(), resolveEquipeId(equipeId));
    }

    public Page<OfficeQueueItemDto> listQueue(OfficeQueueStatus status, Pageable pageable) {
        return officeSignatureQueueService.listarPorSigner(currentUserId(), status, pageable);
    }

    public OfficeQueueItemDto approve(Long id, DecisionRequest req) {
        return officeSignatureQueueService.aprovar(currentUserId(), id, req != null ? req.getReason() : null);
    }

    public OfficeQueueItemDto reject(Long id, DecisionRequest req) {
        return officeSignatureQueueService.rejeitar(currentUserId(), id, req != null ? req.getReason() : null);
    }

    public AdvogadoOfficeBulkApproveResponse bulkApprove(BulkApproveRequest req) {
        OfficeSignatureQueueService.BulkResult result = officeSignatureQueueService.bulkApprove(currentUserId(), req.getIds(), req.getReason());
        return new AdvogadoOfficeBulkApproveResponse(result.approved(), result.rejected(), result.errors());
    }

    private Long currentUserId() {
        return currentUserService.currentUserIdOrZero();
    }

    private Long resolveEquipeId(Long equipeId) {
        if (equipeId != null) {
            return equipeId;
        }
        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        if (membro == null || membro.getEquipe() == null || membro.getEquipe().getId() == null) {
            throw new EquipeNaoSelecionadaException("Equipe não selecionada. Informe o header X-Equipe-ID.", "X-Equipe-ID", java.util.List.of());
        }
        return membro.getEquipe().getId();
    }

    private AdvogadoOperacaoResponse toOperacaoResponse(Map<String, Object> source) {
        Map<String, Object> safe = source == null ? Map.of() : new java.util.LinkedHashMap<>(source);
        return new AdvogadoOperacaoResponse(
                asString(safe.get("status")),
                asString(safe.get("tipo")),
                asLong(safe.get("processoId")),
                asLong(safe.get("workItemId")),
                asString(safe.get("dedupKey")),
                asInteger(safe.get("total")),
                asInteger(safe.get("processados")),
                asLongList(safe.get("ids")),
                safe
        );
    }

    private AdvogadoClienteAnaliticoItemResponse toAnaliticoItem(Map<String, Object> source) {
        return new AdvogadoClienteAnaliticoItemResponse(
                asLong(source.get("processoId")),
                asString(source.get("numero")),
                asString(source.get("fase")),
                asString(source.get("rito")),
                asString(source.get("tribunal"))
        );
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static List<Long> asLongList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(AdvogadoSurfaceFacadeService::asLong).toList();
    }
}
