package com.tcc.pjb.backend.service.processual.postarchive;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalProfile;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalResolver;
import com.tcc.pjb.backend.core.transito.TransitoJulgadoArquivamentoEngine;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleRequest;
import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyEngine;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyReport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostArchiveLifecycleService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final PjbAuthorizationService authorizationService;
    private final TransitoJulgadoArquivamentoEngine transitoEngine;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final PostJudgmentOperationalResolver operationalResolver;

    public PostArchiveLifecycleService(ProcessoRepository processoRepository,
                                       WorkItemRepository workItemRepository,
                                       DocumentoProcessualRepository documentoRepository,
                                       MovimentacaoProcessualRepository movimentacaoRepository,
                                       PjbAuthorizationService authorizationService,
                                       TransitoJulgadoArquivamentoEngine transitoEngine,
                                       ProcessoLifecycleMachine lifecycleMachine,
                                       PostJudgmentOperationalResolver operationalResolver) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.transitoEngine = Objects.requireNonNull(transitoEngine);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.operationalResolver = Objects.requireNonNull(operationalResolver);
    }

    @Transactional
    public PostArchiveLifecycleResponse evaluate(PostArchiveLifecycleRequest request) {
        Objects.requireNonNull(request);
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(processo);
        List<com.tcc.pjb.backend.model.entity.workflow.WorkItem> items = workItemRepository.findAllByProcesso(processo.getId());
        int pendenciasAbertas = (int) items.stream().filter(item -> item.getStatus() != WorkItemStatus.CONCLUIDO && item.getStatus() != WorkItemStatus.CANCELADO).count();
        long totalDocumentos = documentoRepository.countByProcesso_Id(processo.getId());
        int janelaDias = Math.max(1, request.janelaDias());
        Instant cutoff = Instant.now().minus(janelaDias, java.time.temporal.ChronoUnit.DAYS);
        int movimentacoesRecentes = request.verificarMovimentacaoRecente()
                ? (int) movimentacaoRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).stream()
                        .filter(mov -> mov.getDataMovimentacao() != null && !mov.getDataMovimentacao().isBefore(cutoff))
                        .count()
                : 0;
        boolean encerrado = processo.getStatusProcesso() != null && processo.getStatusProcesso().isEncerrado();
        boolean aptoArquivamentoDefinitivo = encerrado && pendenciasAbertas == 0 && movimentacoesRecentes == 0;
        boolean desarquivamentoRecomendado = pendenciasAbertas > 0
                || (request.verificarDocumentosNovos() && totalDocumentos > 0 && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO)
                || movimentacoesRecentes > 0;
        boolean desarquivamentoSolicitado = false;

        ProcessoLifecycleDecision archiveDecision = lifecycleMachine.preview(processo, ProcessoLifecycleAction.ARQUIVAR);
        ProcessoLifecycleDecision reopenDecision = lifecycleMachine.preview(processo, ProcessoLifecycleAction.DESARQUIVAR);
        PostJudgmentOperationalProfile archiveProfile = operationalResolver.resolve(processo, ProcessoLifecycleAction.ARQUIVAR, request.motivo(), 0D);
        PostJudgmentOperationalProfile reopenProfile = operationalResolver.resolve(processo, ProcessoLifecycleAction.DESARQUIVAR, request.motivo(), 0D);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("janelaDias", janelaDias);
        metadata.put("encerrado", encerrado);
        metadata.put("ultimoStatus", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        metadata.put("numeroDocumentos", totalDocumentos);
        metadata.put("movimentacoesRecentes", movimentacoesRecentes);
        metadata.put("pendenciasAbertas", pendenciasAbertas);
        metadata.put("archivePermitted", archiveDecision.permitida());
        metadata.put("archiveTransitionKey", archiveDecision.transitionKey());
        metadata.put("reopenPermitted", reopenDecision.permitida());
        metadata.put("reopenTransitionKey", reopenDecision.transitionKey());
        metadata.put("archiveDescriptor", archiveProfile.descriptor());
        metadata.put("reopenDescriptor", reopenProfile.descriptor());
        metadata.put("archiveReviewDesk", archiveProfile.reviewDesk());
        metadata.put("reopenReviewDesk", reopenProfile.reviewDesk());
        metadata.put("archiveRetentionMode", archiveProfile.retentionMode());
        metadata.put("reopenRetentionMode", reopenProfile.retentionMode());
        metadata.put("archiveQueueCode", archiveProfile.queueCode());
        metadata.put("reopenQueueCode", reopenProfile.queueCode());
        metadata.put("archiveInboxKey", archiveProfile.inboxKey());
        metadata.put("reopenInboxKey", reopenProfile.inboxKey());
        Map<String, Object> snapshotExecutivo = transitoEngine.consultarSnapshotExecutivo(processo.getId());
        ArchivedProcessVisibilityPolicyReport visibilityPolicy = ArchivedProcessVisibilityPolicyEngine.analyze(processo, request, snapshotExecutivo);
        metadata.put("archiveProfile", archiveProfile.toMap());
        metadata.put("reopenProfile", reopenProfile.toMap());
        metadata.put("visibilityPolicy", visibilityPolicy.toMap());
        metadata.put("snapshotExecutivo", snapshotExecutivo);
        metadata.put("terminalReference", snapshotExecutivo.getOrDefault("terminalDisposition", null));
        metadata.put("closureReference", snapshotExecutivo.getOrDefault("currentClosureMode", null));
        metadata.put("closureConsistencyReference", snapshotExecutivo.getOrDefault("closureConsistencyStatus", null));
        metadata.put("preferenceReference", snapshotExecutivo.getOrDefault("currentPreferenceMode", null));
        metadata.put("subrogationReference", snapshotExecutivo.getOrDefault("subrogationStatus", null));

        if (request.reativar()) {
            String motivo = request.motivo() == null ? "" : request.motivo().trim();
            if (motivo.isBlank()) {
                throw new IllegalArgumentException("Motivo é obrigatório para reativação do processo arquivado.");
            }
            transitoEngine.abrirDesarquivamento(processo.getId(), motivo);
            desarquivamentoSolicitado = true;
        }

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        alertas.addAll(archiveDecision.alertas());
        alertas.addAll(reopenDecision.alertas());
        alertas.addAll(archiveProfile.warnings());
        alertas.addAll(reopenProfile.warnings());
        if (aptoArquivamentoDefinitivo) {
            alertas.add("Feito apto ao ciclo de guarda definitiva sem pendências abertas.");
        }
        alertas.addAll(visibilityPolicy.alerts());
        if (desarquivamentoRecomendado) {
            alertas.add("Há sinais materiais para reativação controlada do processo arquivado.");
        }
        if (pendenciasAbertas > 0) {
            alertas.add("Existem work items pendentes vinculados ao processo.");
        }
        if (movimentacoesRecentes > 0) {
            alertas.add("Foram identificadas movimentações recentes dentro da janela de controle.");
        }
        if (!archiveDecision.permitida()) {
            alertas.add("A máquina de ciclo não autoriza arquivamento imediato no estado atual.");
        }
        if (!reopenDecision.permitida() && processo.getStatusProcesso() != StatusProcesso.ARQUIVADO) {
            alertas.add("Reativação depende primeiro de baixa formal ou mudança prévia do estado processual.");
        }
        metadata.values().removeIf(Objects::isNull);
        return new PostArchiveLifecycleResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                aptoArquivamentoDefinitivo,
                desarquivamentoRecomendado,
                desarquivamentoSolicitado,
                pendenciasAbertas,
                totalDocumentos,
                movimentacoesRecentes,
                List.copyOf(alertas),
                metadata
        );
    }
}
