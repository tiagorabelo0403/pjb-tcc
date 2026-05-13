package com.tcc.pjb.backend.core.financeiro.trabalhista;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.AcordoHomologadoHealthView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.AcordoHomologadoResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalHealthQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalHealthResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalWindowView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GerarGruTrabalhistaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaConsultaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaConsultaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.HomologarAcordoTrabalhistaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.RegistrarDepositoRecursalCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaAcordoHealthResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaConsultaTimelineCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaConsultaTimelineResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaDepositoConsistencyView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaExecucaoHealthView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaExecucaoResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaFluxoStatusQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaFluxoStatusResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruConsistencyAuditView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruConsistencyView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruHealthQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruHealthResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaHealthQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaOwnershipView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineAuditSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineWindowView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrabalhistaApplicationService {

    private final WorkflowTrabalhistaService workflowTrabalhistaService;
    private final AuditLedgerService auditLedgerService;

    public TrabalhistaApplicationService(WorkflowTrabalhistaService workflowTrabalhistaService,
                                         AuditLedgerService auditLedgerService) {
        this.workflowTrabalhistaService = Objects.requireNonNull(workflowTrabalhistaService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public GruTrabalhistaResult gerarGru(Long processoId, String tipo, BigDecimal valor) {
        GruTrabalhistaResult result = workflowTrabalhistaService.gerarGruRecursal(new GerarGruTrabalhistaCommand(requireId(processoId), normalizeText(tipo, "tipo obrigatorio"), Objects.requireNonNull(valor, "valor obrigatorio")));
        auditLedgerService.appendSafely("TRABALHISTA_SURFACE_GRU", "PROCESSO", String.valueOf(processoId), null, "gruId=" + result.gruId());
        return result;
    }

    @Transactional
    public DepositoRecursalResult registrarDeposito(Long processoId, String instancia, BigDecimal valorDepositado, String comprovanteHash) {
        DepositoRecursalResult result = workflowTrabalhistaService.registrarDepositoRecursal(new RegistrarDepositoRecursalCommand(requireId(processoId), normalizeText(instancia, "instancia obrigatoria"), Objects.requireNonNull(valorDepositado, "valorDepositado obrigatorio"), comprovanteHash));
        auditLedgerService.appendSafely("TRABALHISTA_SURFACE_DEPOSITO", "PROCESSO", String.valueOf(processoId), null, "depositoId=" + result.depositoId() + " status=" + result.status());
        return result;
    }

    @Transactional
    public AcordoHomologadoResult homologarAcordo(Long processoId, String resumo) {
        AcordoHomologadoResult result = workflowTrabalhistaService.homologarAcordo(new HomologarAcordoTrabalhistaCommand(requireId(processoId), normalizeText(resumo, "resumo obrigatorio")));
        auditLedgerService.appendSafely("TRABALHISTA_SURFACE_ACORDO", "PROCESSO", String.valueOf(processoId), null, result.statusProcesso());
        return result;
    }

    @Transactional(readOnly = true)
    public TrabalhistaProcessoResult processo(Long processoId) {
        return workflowTrabalhistaService.processoResult(new TrabalhistaProcessoQuery(requireId(processoId)));
    }

    @Transactional(readOnly = true)
    public TrabalhistaFluxoStatusResult fluxoStatus(Long processoId) {
        return workflowTrabalhistaService.fluxoStatus(new TrabalhistaFluxoStatusQuery(requireId(processoId)));
    }

    @Transactional(readOnly = true)
    public GruTrabalhistaConsultaResult gru(Long gruId) {
        return workflowTrabalhistaService.consultarGru(new GruTrabalhistaConsultaCommand(requireId(gruId)));
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruResult gruView(Long gruId) {
        var consulta = workflowTrabalhistaService.consultarGru(new GruTrabalhistaConsultaCommand(requireId(gruId)));
        return new TrabalhistaGruResult(consulta.gruId(), consulta.tipo(), consulta.linhaDigitavel(), consulta.status(), null);
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruHealthResult gruHealth(Long gruId) {
        return workflowTrabalhistaService.gruHealth(new TrabalhistaGruHealthQuery(requireId(gruId)));
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruConsistencyView gruConsistency(Long gruId) {
        TrabalhistaGruHealthResult health = gruHealth(gruId);
        return new TrabalhistaGruConsistencyView(String.valueOf(gruId), health.status(), Instant.now());
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruConsistencyAuditView gruConsistencyAudit(Long gruId) {
        TrabalhistaGruHealthResult health = gruHealth(gruId);
        auditLedgerService.appendSafely("TRABALHISTA_GRU_CONSISTENCY_QUERY", "TRABALHISTA_GRU", String.valueOf(gruId), null, health.status());
        return new TrabalhistaGruConsistencyAuditView(String.valueOf(gruId), health.status(), health.pendente() ? "pendente" : "consistente");
    }

    @Transactional(readOnly = true)
    public DepositoRecursalHealthResult depositoHealth(Long depositoId) {
        return workflowTrabalhistaService.depositoHealth(new DepositoRecursalHealthQuery(requireId(depositoId)));
    }

    @Transactional(readOnly = true)
    public TrabalhistaDepositoConsistencyView depositoConsistency(Long depositoId) {
        DepositoRecursalHealthResult health = depositoHealth(depositoId);
        return new TrabalhistaDepositoConsistencyView(String.valueOf(depositoId), health.status(), Instant.now());
    }

    @Transactional(readOnly = true)
    public DepositoRecursalWindowView depositoWindow(Long depositoId) {
        DepositoRecursalHealthResult health = depositoHealth(depositoId);
        return new DepositoRecursalWindowView(String.valueOf(depositoId), health.status(), health.confirmado() ? "deposito confirmado" : "deposito pendente");
    }

    @Transactional(readOnly = true)
    public TrabalhistaConsultaTimelineResult timeline(Long processoId) {
        Long requiredId = requireId(processoId);
        TrabalhistaConsultaTimelineResult result = workflowTrabalhistaService.timeline(new TrabalhistaConsultaTimelineCommand(requiredId));
        auditLedgerService.appendSafely("TRABALHISTA_TIMELINE_QUERY", "PROCESSO", String.valueOf(requiredId), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public TrabalhistaTimelineAuditSnapshot timelineAudit(Long processoId) {
        Long requiredId = requireId(processoId);
        TrabalhistaTimelineAuditSnapshot snapshot = workflowTrabalhistaService.timelineAudit(requiredId);
        auditLedgerService.appendSafely("TRABALHISTA_TIMELINE_AUDIT_QUERY", "PROCESSO", String.valueOf(requiredId), null, "entries=" + snapshot.totalEventos());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public TrabalhistaTimelineWindowView timelineWindow(Long processoId) {
        TrabalhistaTimelineAuditSnapshot snapshot = workflowTrabalhistaService.timelineAudit(requireId(processoId));
        return new TrabalhistaTimelineWindowView(String.valueOf(snapshot.processoId()), snapshot.totalEventos() > 0 ? "ATIVO" : "VAZIO", Instant.now());
    }

    @Transactional(readOnly = true)
    public TrabalhistaExecucaoResult execucao(Long processoId) {
        TrabalhistaProcessoResult processoResult = processo(requireId(processoId));
        var processo = processoResult.processo();
        boolean emExecucao = processo != null && "EXECUCAO_TRABALHISTA".equalsIgnoreCase(processo.statusProcesso());
        return new TrabalhistaExecucaoResult(requireId(processoId), processo == null ? null : processo.statusProcesso(), processo == null ? null : processo.tribunal(), emExecucao);
    }

    @Transactional(readOnly = true)
    public TrabalhistaExecucaoHealthView execucaoHealth(Long processoId) {
        TrabalhistaExecucaoResult result = execucao(processoId);
        return new TrabalhistaExecucaoHealthView(String.valueOf(result.processoId()), result.emExecucao() ? "EM_EXECUCAO" : "FORA_EXECUCAO", result.status());
    }

    @Transactional(readOnly = true)
    public TrabalhistaOwnershipView ownership(Long processoId) {
        TrabalhistaProcessoResult processoResult = processo(requireId(processoId));
        var processo = processoResult.processo();
        return new TrabalhistaOwnershipView(String.valueOf(processoId), processo == null ? null : processo.statusProcesso(), processo == null ? null : processo.tribunal());
    }

    @Transactional(readOnly = true)
    public TrabalhistaAcordoHealthResult acordoHealth(Long processoId) {
        var snapshot = workflowTrabalhistaService.acordoHealth(requireId(processoId));
        return new TrabalhistaAcordoHealthResult(snapshot.homologado(), snapshot.status(), Instant.now());
    }

    @Transactional(readOnly = true)
    public AcordoHomologadoHealthView acordoHealthView(Long processoId) {
        TrabalhistaAcordoHealthResult result = acordoHealth(processoId);
        return new AcordoHomologadoHealthView(String.valueOf(requireId(processoId)), result.ok() ? "HOMOLOGADO" : "PENDENTE", result.mensagem());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoView processOverview(Long processoId) {
        return workflowTrabalhistaService.health(new TrabalhistaHealthQuery(requireId(processoId)));
    }

    private Long requireId(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("id obrigatorio");
        }
        return value;
    }

    private String normalizeText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
