package com.tcc.pjb.backend.service.casefile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class CaseContinuityConsistencyService {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(48);

    private final CaseContinuityOrchestratorService orchestratorService;
    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityConsistencyService(CaseContinuityOrchestratorService orchestratorService,
                                            ProcessoRepository processoRepository,
                                            AuditLedgerService auditLedgerService,
                                            CaseContinuityObservabilityMetrics metrics) {
        this.orchestratorService = Objects.requireNonNull(orchestratorService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityConsistencyResponse snapshot(Long processoId) {
        CaseContinuitySnapshot snapshot = orchestratorService.inspect(processoId);
        Processo processo = processoRepository.findById(processoId).orElse(null);
        Instant reference = Instant.now();
        Set<String> keys = new LinkedHashSet<>();
        snapshot.proceedings().stream()
                .map(CaseContinuityProceedingNode::proceedingKey)
                .filter(Objects::nonNull)
                .forEach(keys::add);

        long rootProceedingCount = snapshot.proceedings().stream().filter(CaseContinuityProceedingNode::isRootLike).count();
        long orphanParentCount = snapshot.proceedings().stream()
                .filter(node -> node.parentProceedingKey() != null && !node.parentProceedingKey().isBlank())
                .filter(node -> !keys.contains(node.parentProceedingKey()))
                .count();
        long incompatibleRoleTrackCount = snapshot.proceedings().stream()
                .filter(node -> node.role() == null || node.continuityTrack() == null || !node.role().acceptsTrack(node.continuityTrack()))
                .count();
        long incompatibleStateCount = snapshot.proceedings().stream()
                .filter(node -> node.continuityTrack() != null)
                .filter(node -> !node.continuityTrack().supportsFase(node.sourceFaseProcessual()) || !node.continuityTrack().supportsStatus(node.sourceStatusProcesso()))
                .count();
        long recursalBranchesWithoutEdge = snapshot.proceedings().stream()
                .filter(CaseContinuityProceedingNode::isRecursalBranch)
                .filter(node -> snapshot.edges().stream().noneMatch(edge -> Objects.equals(edge.toProceedingKey(), node.proceedingKey()) || Objects.equals(edge.fromProceedingKey(), node.proceedingKey())))
                .count();
        long executoryBranchesWithoutParent = snapshot.proceedings().stream()
                .filter(CaseContinuityProceedingNode::isExecutoryBranch)
                .filter(node -> node.parentProceedingKey() == null || node.parentProceedingKey().isBlank())
                .count();
        long staleProceedings = snapshot.proceedings().stream()
                .filter(node -> node.isStale(reference, STALE_THRESHOLD))
                .count();

        List<String> warnings = new ArrayList<>(snapshot.warnings());
        List<String> inconsistencies = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();

        if (rootProceedingCount != 1) {
            inconsistencies.add("O caso raiz materializado não possui exatamente um proceeding âncora com papel de raiz/vínculo principal.");
            recommendedActions.add("Reconciliar anchor proceeding e consolidar o root canônico do caso unificado.");
        }
        if (orphanParentCount > 0) {
            inconsistencies.add("Existem ramificações com parentProceedingKey sem correspondente materializado no grafo do caso.");
            recommendedActions.add("Reconstruir encadeamento entre proceeding pai e proceeding filho antes de novas transições críticas.");
        }
        if (incompatibleRoleTrackCount > 0) {
            inconsistencies.add("Existem ramificações com incompatibilidade entre papel institucional do proceeding e continuity track persistido.");
            recommendedActions.add("Recalibrar role/track pelo orquestrador de continuidade antes de publicar novos atos estruturais.");
        }
        if (incompatibleStateCount > 0) {
            inconsistencies.add("Existem ramificações com continuity track incompatível com a fase ou o status processual de origem.");
            recommendedActions.add("Sincronizar fase/status de origem com o snapshot canônico do processo materializado.");
        }
        if (recursalBranchesWithoutEdge > 0) {
            inconsistencies.add("Há ramificações recursais sem edge estrutural correspondente na malha do caso unificado.");
            recommendedActions.add("Refazer ligação recursal para preservar o encadeamento entre origem e instância ad quem.");
        }
        if (executoryBranchesWithoutParent > 0) {
            inconsistencies.add("Há ramificações executórias sem parentProceedingKey válido para continuidade do caso.");
            recommendedActions.add("Materializar vínculo de cumprimento/execução ao proceeding de origem antes de seguir para baixa ou arquivamento.");
        }
        if (staleProceedings > 0) {
            warnings.add("Existem ramificações do caso raiz com sincronização defasada acima do limiar operacional endurecido.");
            recommendedActions.add("Reexecutar sincronização do caso raiz antes de nova movimentação sensível.");
        }

        if (processo != null) {
            CaseContinuityTrack expectedTrack = CaseContinuityTrack.resolve(null, processo.getFaseAtual(), processo.getStatusProcesso());
            if (expectedTrack != null && snapshot.dominantTrack() != null && expectedTrack != snapshot.dominantTrack()) {
                inconsistencies.add("A trilha dominante do caso unificado diverge do estado processual atual do processo solicitado.");
                recommendedActions.add("Revalidar snapshot e aplicar sincronização do lifecycle para alinhar o track dominante ao processo real.");
            }
            if (expectedTrack != null && expectedTrack.isExecutory() && !snapshot.hasExecutoryBranch()) {
                inconsistencies.add("O processo já exige trilha executória, mas o caso unificado ainda não materializou branch de cumprimento/execução.");
                recommendedActions.add("Materializar proceeding executório antes de seguir com atos de cobrança, baixa ou extinção.");
            }
            if (expectedTrack != null && expectedTrack.isRecursalState() && !snapshot.hasRecursalBranch()) {
                inconsistencies.add("O processo está em trilha recursal, mas o caso unificado não possui branch recursal materializado.");
                recommendedActions.add("Abrir branch recursal no caso raiz e reconciliar edges com a malha recursal nacional.");
            }
        }

        boolean healthy = inconsistencies.isEmpty();
        CaseContinuityConsistencyResponse response = new CaseContinuityConsistencyResponse(
                reference,
                snapshot.caseFileId(),
                processoId,
                snapshot.dominantTrack(),
                healthy,
                snapshot.proceedingCount(),
                rootProceedingCount,
                orphanParentCount,
                incompatibleRoleTrackCount,
                incompatibleStateCount,
                recursalBranchesWithoutEdge,
                executoryBranchesWithoutParent,
                staleProceedings,
                warnings,
                inconsistencies,
                recommendedActions
        );
        metrics.recordConsistency(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_CONSISTENCY_INSPECT",
                "CASE_FILE",
                String.valueOf(snapshot.caseFileId()),
                String.join("|",
                        String.valueOf(snapshot.caseFileId()),
                        String.valueOf(processoId),
                        snapshot.dominantTrack() == null ? "-" : snapshot.dominantTrack().name(),
                        String.valueOf(inconsistencies.size()),
                        String.valueOf(staleProceedings))
        );
        return response;
    }
}
