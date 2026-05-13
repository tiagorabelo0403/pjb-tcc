package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalExecutionOrchestrator {

    private final PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService;
    private final ObjectProvider<com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService> commandCenterServiceProvider;
    private final PjbSubstituicaoTribunalHomologacaoProbeService homologacaoProbeService;
    private final PjbSubstituicaoMigracaoIndustrialBatchService migracaoIndustrialBatchService;
    private final PjbSubstituicaoComunicacaoNacionalSyncService comunicacaoNacionalSyncService;
    private final PjbSubstituicaoNacionalExecutionTransactionCoordinator tx;

    public PjbSubstituicaoNacionalExecutionOrchestrator(PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService,
                                                        ObjectProvider<com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService> commandCenterServiceProvider,
                                                        PjbSubstituicaoTribunalHomologacaoProbeService homologacaoProbeService,
                                                        PjbSubstituicaoMigracaoIndustrialBatchService migracaoIndustrialBatchService,
                                                        PjbSubstituicaoComunicacaoNacionalSyncService comunicacaoNacionalSyncService,
                                                        PjbSubstituicaoNacionalExecutionTransactionCoordinator tx) {
        this.programaApplicationService = Objects.requireNonNull(programaApplicationService);
        this.commandCenterServiceProvider = Objects.requireNonNull(commandCenterServiceProvider);
        this.homologacaoProbeService = Objects.requireNonNull(homologacaoProbeService);
        this.migracaoIndustrialBatchService = Objects.requireNonNull(migracaoIndustrialBatchService);
        this.comunicacaoNacionalSyncService = Objects.requireNonNull(comunicacaoNacionalSyncService);
        this.tx = Objects.requireNonNull(tx);
    }

    public void executar(Long execucaoId, JobExecutionContext ctx) {
        PjbSubstituicaoNacionalExecutionTransactionCoordinator.ExecutionSnapshot snapshot = tx.carregar(execucaoId);
        PjbSubstituicaoGateSnapshot gate = avaliarGate(snapshot.tribunalCodigo());
        tx.iniciar(execucaoId, gate);
        ctx.progress(1, 4);
        switch (snapshot.acao()) {
            case HOMOLOGAR_TRIBUNAL -> homologar(snapshot, gate, ctx);
            case INICIAR_MIGRACAO_SOMBRA -> migracaoSombra(snapshot, gate, ctx);
            case SINCRONIZAR_COMUNICACOES_NACIONAIS -> sincronizarComunicacoes(snapshot, gate, ctx);
            case CONFIRMAR_CUTOVER -> confirmarCutover(execucaoId, snapshot.dryRun(), gate, ctx);
            case ACIONAR_ROLLBACK -> rollback(execucaoId, snapshot.dryRun(), gate, ctx);
        }
    }

    private void homologar(PjbSubstituicaoNacionalExecutionTransactionCoordinator.ExecutionSnapshot snapshot, PjbSubstituicaoGateSnapshot gate, JobExecutionContext ctx) {
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.HOMOLOGACAO, null, "HOMOLOGACAO_PROBES", "INFO", "Probes de homologação federativa preparados.", gate.probeMap());
        ctx.progress(2, 4);
        if (gate.blockedFor(PjbSubstituicaoExecucaoAcao.HOMOLOGAR_TRIBUNAL) && !snapshot.dryRun()) {
            tx.bloquear(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.HOMOLOGACAO, gate, "Homologação bloqueada pelos guardrails nacionais.");
            return;
        }
        var probeResult = homologacaoProbeService.executar(snapshot.execucaoId(), snapshot.tribunalCodigo(), snapshot.dryRun(), snapshot.payloadJson(), gate);
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.HOMOLOGACAO, null, "HOMOLOGACAO_EVIDENCIA_PERSISTIDA", "INFO", "Evidência de homologação persistida por probe.", probeResult.details());
        Map<String, Object> resultado = new LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", snapshot.dryRun() ? "DRY_RUN_APROVADO" : "PRONTO_PARA_HOMOLOGACAO_ASSISTIDA");
        resultado.put("evidencias", List.of("Conectores tribunal-ready presentes", "Sem bloqueio criptográfico impeditivo", "Gate institucional acima do mínimo operacional"));
        resultado.put("probes", probeResult.details());
        tx.concluir(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.FINALIZACAO, gate, "Homologação do tribunal consolidada com evidência institucional.", resultado);
        ctx.progress(4, 4);
    }

    private void migracaoSombra(PjbSubstituicaoNacionalExecutionTransactionCoordinator.ExecutionSnapshot snapshot, PjbSubstituicaoGateSnapshot gate, JobExecutionContext ctx) {
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.MIGRACAO_SOMBRA, null, "MIGRACAO_SHADOW_BASELINE", "INFO", "Baseline da onda de sombra preparada.", Map.of("ondaAlvo", "shadow-mode-governado"));
        ctx.progress(2, 4);
        if (gate.blockedFor(PjbSubstituicaoExecucaoAcao.INICIAR_MIGRACAO_SOMBRA) && !snapshot.dryRun()) {
            tx.bloquear(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.MIGRACAO_SOMBRA, gate, "Migração sombra bloqueada por prontidão insuficiente.");
            return;
        }
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "MIGRACAO_SHADOW_RECONCILIACAO", "INFO", "Reconciliação de sombra preparada.", Map.of("rollbackReversivel", gate.rollbackReversivel()));
        ctx.progress(3, 4);
        var migrationResult = migracaoIndustrialBatchService.executar(snapshot.execucaoId(), snapshot.tribunalCodigo(), snapshot.dryRun(), snapshot.requestHash(), snapshot.payloadJson(), gate);
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "MIGRACAO_SHADOW_LOTES_PERSISTIDOS", "INFO", "Lotes industriais persistidos com checksum e reconciliação.", migrationResult.details());
        Map<String, Object> resultado = new LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", snapshot.dryRun() ? "DRY_RUN_SHADOW_APROVADO" : "SHADOW_READY");
        resultado.put("controles", List.of("Checksum por lote habilitado", "Reconciliação PJB x legado exigida antes da ampliação", "Rollback overlay reversível preservado"));
        resultado.put("lotes", migrationResult.details());
        tx.concluir(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.FINALIZACAO, gate, "Migração industrial em modo sombra preparada.", resultado);
        ctx.progress(4, 4);
    }

    private void sincronizarComunicacoes(PjbSubstituicaoNacionalExecutionTransactionCoordinator.ExecutionSnapshot snapshot, PjbSubstituicaoGateSnapshot gate, JobExecutionContext ctx) {
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.COMUNICACOES, null, "COMUNICACOES_SYNC_PREP", "INFO", "Sync nacional de comunicações preparado.", gate.communicationMap());
        ctx.progress(2, 4);
        if (gate.blockedFor(PjbSubstituicaoExecucaoAcao.SINCRONIZAR_COMUNICACOES_NACIONAIS) && !snapshot.dryRun()) {
            tx.bloquear(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.COMUNICACOES, gate, "Sincronização nacional bloqueada por ausência de conectores saudáveis.");
            return;
        }
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "COMUNICACOES_SYNC_CORRELACAO", "INFO", "Correlação e deduplicação nacional armadas.", Map.of("dedupe", "NACIONAL", "janelaHoras", 12));
        ctx.progress(3, 4);
        var syncResult = comunicacaoNacionalSyncService.executar(snapshot.execucaoId(), snapshot.tribunalCodigo(), snapshot.dryRun(), snapshot.payloadJson(), gate);
        tx.atualizarFase(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "COMUNICACOES_SYNC_CURSOR_PERSISTIDO", "INFO", "Cursores e itens de sync persistidos com correlação e deduplicação.", syncResult.details());
        Map<String, Object> resultado = new LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", snapshot.dryRun() ? "DRY_RUN_SYNC_APROVADO" : "SYNC_READY");
        resultado.put("controles", List.of("Deduplicação nacional habilitada", "Correlação tribunal-processo armada", "Reprocessamento controlado disponível"));
        resultado.put("sync", syncResult.details());
        tx.concluir(snapshot.execucaoId(), PjbSubstituicaoExecucaoFase.FINALIZACAO, gate, "Comunicações nacionais sincronizáveis com trilha governada.", resultado);
        ctx.progress(4, 4);
    }

    private void confirmarCutover(Long execucaoId, boolean dryRun, PjbSubstituicaoGateSnapshot gate, JobExecutionContext ctx) {
        tx.atualizarFase(execucaoId, PjbSubstituicaoExecucaoFase.CUTOVER, null, "CUTOVER_ARMADO", "INFO", "Janela de corte assistido preparada.", gate.cutoverMap());
        ctx.progress(2, 4);
        if (gate.blockedFor(PjbSubstituicaoExecucaoAcao.CONFIRMAR_CUTOVER) && !dryRun) {
            tx.bloquear(execucaoId, PjbSubstituicaoExecucaoFase.CUTOVER, gate, "Cutover nacional bloqueado por gate ou bloqueadores do tribunal.");
            return;
        }
        tx.atualizarFase(execucaoId, PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "CUTOVER_RECONCILIACAO", "INFO", "Reconciliação pós-corte preparada.", Map.of("reversivel", gate.rollbackReversivel()));
        ctx.progress(3, 4);
        Map<String, Object> resultado = new LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", dryRun ? "DRY_RUN_CUTOVER_APROVADO" : "CUTOVER_READY");
        resultado.put("controles", List.of("Build gate aprovado", "Cutover nacional permitido pelo programa", "Sem bloqueio crítico de observabilidade/criptografia"));
        tx.concluir(execucaoId, PjbSubstituicaoExecucaoFase.FINALIZACAO, gate, "Cutover assistido pronto para execução governada.", resultado);
        ctx.progress(4, 4);
    }

    private void rollback(Long execucaoId, boolean dryRun, PjbSubstituicaoGateSnapshot gate, JobExecutionContext ctx) {
        tx.atualizarFase(execucaoId, PjbSubstituicaoExecucaoFase.ROLLBACK, null, "ROLLBACK_PREP", "WARN", "Plano de reversão preparado.", gate.rollbackMap());
        ctx.progress(2, 4);
        if (!gate.rollbackReversivel()) {
            tx.bloquear(execucaoId, PjbSubstituicaoExecucaoFase.ROLLBACK, gate, "Rollback governado bloqueado por ausência de reversibilidade mínima.");
            return;
        }
        tx.atualizarFase(execucaoId, PjbSubstituicaoExecucaoFase.RECONCILIACAO, null, "ROLLBACK_RECONCILIACAO", "WARN", "Reconciliação de retorno ao legado armada.", Map.of("legadoPreferido", gate.tribunal().connectorPreferido().name()));
        ctx.progress(3, 4);
        Map<String, Object> resultado = new LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", dryRun ? "DRY_RUN_ROLLBACK_APROVADO" : "ROLLBACK_READY");
        resultado.put("controles", List.of("Overlay reversível disponível", "Reconciliação com legado prevista", "Isolamento por tribunal preservado"));
        tx.concluir(execucaoId, PjbSubstituicaoExecucaoFase.FINALIZACAO, gate, "Rollback governado preparado com trilha probatória.", resultado);
        ctx.progress(4, 4);
    }

    public void falhar(Long execucaoId, String descricao, Throwable throwable) {
        tx.falhar(execucaoId, descricao, throwable);
    }

    private PjbSubstituicaoGateSnapshot avaliarGate(String tribunalCodigo) {
        return PjbSubstituicaoGateSnapshot.of(tribunalCodigo, programaApplicationService.avaliar(), commandCenterServiceProvider.getIfAvailable());
    }
}
