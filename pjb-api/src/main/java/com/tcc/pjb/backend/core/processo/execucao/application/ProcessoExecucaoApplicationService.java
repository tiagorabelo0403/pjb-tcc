package com.tcc.pjb.backend.core.processo.execucao.application;

import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.transito.ExecutionClosureGovernanceProfile;
import com.tcc.pjb.backend.core.transito.ExecutionClosureGovernanceResolver;
import com.tcc.pjb.backend.core.transito.ExecutionEnforcementProfile;
import com.tcc.pjb.backend.core.transito.ExecutionEnforcementResolver;
import com.tcc.pjb.backend.core.transito.ExecutionIncidentProfile;
import com.tcc.pjb.backend.core.transito.ExecutionIncidentResolver;
import com.tcc.pjb.backend.core.transito.ExecutionSatisfactionProfile;
import com.tcc.pjb.backend.core.transito.ExecutionSatisfactionResolver;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoExecucaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final ExecutionIncidentResolver incidentResolver;
    private final ExecutionEnforcementResolver enforcementResolver;
    private final ExecutionSatisfactionResolver satisfactionResolver;
    private final ExecutionClosureGovernanceResolver closureGovernanceResolver;

    public ProcessoExecucaoApplicationService(ProcessoRepository processoRepository,
                                              WorkItemRepository workItemRepository,
                                              ExecutionIncidentResolver incidentResolver,
                                              ExecutionEnforcementResolver enforcementResolver,
                                              ExecutionSatisfactionResolver satisfactionResolver,
                                              ExecutionClosureGovernanceResolver closureGovernanceResolver) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.incidentResolver = Objects.requireNonNull(incidentResolver);
        this.enforcementResolver = Objects.requireNonNull(enforcementResolver);
        this.satisfactionResolver = Objects.requireNonNull(satisfactionResolver);
        this.closureGovernanceResolver = Objects.requireNonNull(closureGovernanceResolver);
    }

    public ProcessoExecucaoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processoId);
        ExecutionIncidentProfile incident = incidentResolver.resolve(processo, inferIncidentType(processo), "execucao institucional", inferValorGarantia(processo));
        ExecutionEnforcementProfile enforcement = enforcementResolver.resolve(processo, inferEnforcementAct(processo), inferEnforcementDetail(processo), inferValorGarantia(processo));
        ExecutionSatisfactionProfile satisfaction = satisfactionResolver.resolve(processo, inferSatisfactionMode(processo), inferPercentualSatisfeito(processo), inferSaldoRemanescente(processo), "satisfacao executiva monitorada");
        ExecutionClosureGovernanceProfile closure = closureGovernanceResolver.resolve(processo, inferClosureMode(processo), inferPreferencia(processo), inferSubrogacao(processo), inferPercentualSatisfeito(processo), inferSaldoRemanescente(processo), "fechamento executivo nacional");
        ArrayList<ProcessoExecucaoTrilha> trilhas = new ArrayList<>();
        trilhas.add(toIncidentTrail(incident, workItems));
        trilhas.add(toEnforcementTrail(enforcement, workItems, processo));
        trilhas.add(toSatisfactionTrail(satisfaction, workItems));
        trilhas.add(toClosureTrail(closure, workItems));
        if (supportsCustody(processo)) {
            trilhas.add(toCustodyTrail(processo, workItems));
        }
        List<ProcessoExecucaoTrilha> ordered = trilhas.stream()
                .sorted(Comparator.comparing(ProcessoExecucaoTrilha::bloqueante).reversed()
                        .thenComparing(ProcessoExecucaoTrilha::itensAbertosRelacionados).reversed()
                        .thenComparing(ProcessoExecucaoTrilha::prioridade).reversed()
                        .thenComparing(ProcessoExecucaoTrilha::codigo))
                .toList();
        long totalMandados = ordered.stream().mapToLong(item -> item.mandados().size()).sum();
        long totalCustodia = ordered.stream().mapToLong(item -> item.operacoesCustodia().size()).sum();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (!isProcessoExecutivo(processo)) {
            alertas.add("A malha executiva foi antecipada para prevenir falhas quando o processo migrar de fase.");
        }
        if (workItems.stream().anyMatch(this::vencido)) {
            alertas.add("Há item executivo vencido ou em risco imediato de atraso operacional.");
        }
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isArquivadoOuBaixado()) {
            alertas.add("Processo arquivado ou baixado exige revisão rigorosa antes de novo ato constritivo, executivo ou custodial.");
        }
        LinkedHashSet<String> proximoPasso = new LinkedHashSet<>();
        ordered.stream().limit(4).forEach(trilha -> proximoPasso.add(trilha.eixo() + ':' + trilha.titulo()));
        return new ProcessoExecucaoAggregate(
                identity(processo),
                isProcessoExecutivo(processo),
                ordered.size(),
                ordered.stream().filter(ProcessoExecucaoTrilha::bloqueante).count(),
                totalMandados,
                totalCustodia,
                ordered,
                List.copyOf(alertas),
                List.copyOf(proximoPasso),
                Instant.now()
        );
    }

    private ProcessoExecucaoTrilha toIncidentTrail(ExecutionIncidentProfile profile, List<WorkItem> workItems) {
        return new ProcessoExecucaoTrilha(
                "TRILHA_INCIDENTE_EXECUTIVO",
                "Incidentes e impugnações executivas",
                "INCIDENTE",
                profile.queueCode(),
                profile.inboxKey(),
                profile.assignedRole().name(),
                profile.priority(),
                profile.blocking(),
                countByQueue(workItems, profile.queueCode(), profile.inboxKey()),
                profile.incidentMode(),
                profile.executionImpact(),
                List.of(),
                List.of(),
                merge(profile.reviewChecklist(), profile.warnings()),
                merge(profile.fundamentos(), List.of(profile.baseLegal()))
        );
    }

    private ProcessoExecucaoTrilha toEnforcementTrail(ExecutionEnforcementProfile profile, List<WorkItem> workItems, Processo processo) {
        return new ProcessoExecucaoTrilha(
                "TRILHA_ATOS_EXECUTIVOS",
                "Atos executivos, mandados e constrições",
                "EXECUCAO",
                profile.queueCode(),
                profile.inboxKey(),
                profile.assignedRole().name(),
                profile.priority(),
                profile.blocking(),
                countByQueue(workItems, profile.queueCode(), profile.inboxKey()),
                profile.actMode(),
                profile.executionImpact(),
                mandadosFor(processo, profile),
                List.of(),
                merge(profile.reviewChecklist(), profile.warnings()),
                merge(profile.fundamentos(), List.of(profile.baseLegal()))
        );
    }

    private ProcessoExecucaoTrilha toSatisfactionTrail(ExecutionSatisfactionProfile profile, List<WorkItem> workItems) {
        return new ProcessoExecucaoTrilha(
                "TRILHA_SATISFACAO",
                "Satisfação, baixa e saldo remanescente",
                "SATISFACAO",
                profile.queueCode(),
                profile.inboxKey(),
                profile.assignedRole().name(),
                profile.priority(),
                profile.blocking(),
                countByQueue(workItems, profile.queueCode(), profile.inboxKey()),
                profile.satisfactionMode(),
                profile.terminalDisposition(),
                List.of(),
                List.of(),
                merge(profile.reviewChecklist(), profile.warnings()),
                merge(profile.fundamentos(), List.of(profile.baseLegal()))
        );
    }

    private ProcessoExecucaoTrilha toClosureTrail(ExecutionClosureGovernanceProfile profile, List<WorkItem> workItems) {
        return new ProcessoExecucaoTrilha(
                "TRILHA_FECHAMENTO_GOVERNANCA",
                "Fechamento, consistência e guarda terminal",
                "GOVERNANCA",
                profile.queueCode(),
                profile.inboxKey(),
                profile.assignedRole().name(),
                profile.priority(),
                profile.blocking(),
                countByQueue(workItems, profile.queueCode(), profile.inboxKey()),
                profile.closureMode(),
                profile.archiveReadiness(),
                List.of(),
                List.of(),
                merge(profile.reviewChecklist(), profile.warnings()),
                merge(profile.fundamentos(), List.of(profile.baseLegal()))
        );
    }

    private ProcessoExecucaoTrilha toCustodyTrail(Processo processo, List<WorkItem> workItems) {
        return new ProcessoExecucaoTrilha(
                "TRILHA_CUSTODIA_E_APRESENTACAO",
                "Custódia, apresentação e certificação material",
                "CUSTODIA",
                processo.getRito() == null || processo.getRito().name().contains("PENAL") ? "CUSTODIA_PENAL_PRISIONAL" : "CUSTODIA_GERAL",
                "inbox.custodia.unidade.prisional",
                "POLICIAL_PENAL",
                99,
                true,
                countByToken(workItems, "CUSTODIA", "APRESENTACAO", "AUDIENCIA_CUSTODIA"),
                supportsCustody(processo) ? "CUSTODIA_ATIVA" : "NAO_APLICAVEL",
                "EXECUCAO_MATERIAL_DE_ORDEM_JUDICIAL",
                mandadosCustodia(processo),
                operacoesCustodia(processo),
                List.of("CERTIFICACAO_OPERACIONAL_OBRIGATORIA", "TRILHA_FORENSE_DE_APRESENTACAO", "CHECAGEM_DE_UNIDADE_CUSTODIANTE"),
                List.of("Fluxo custodial unifica unidade prisional, polícia penal, audiência e certidão material.")
        );
    }

    private ProcessoExecucaoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) marcadores.add(processo.getRamoDireito().name());
        if (processo.getRito() != null) marcadores.add(processo.getRito().name());
        if (processo.getFaseAtual() != null) marcadores.add(processo.getFaseAtual().name());
        if (processo.getStatusProcesso() != null) marcadores.add(processo.getStatusProcesso().name());
        if (supportsCustody(processo)) marcadores.add("CUSTODIA");
        return new ProcessoExecucaoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                List.copyOf(marcadores)
        );
    }

    private boolean isProcessoExecutivo(Processo processo) {
        return processo.getFaseAtual() != null && processo.getFaseAtual().isExecutionLike()
                || processo.getRito() != null && (processo.getRito().isExecucaoFiscalEstrita() || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.EXECUCAO_PENAL || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.EXECUCAO_TITULO_JUDICIAL || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.CUMPRIMENTO_SENTENCA || processo.getRito() == com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.CUMPRIMENTO_PROVISORIO)
                || processo.getStatusProcesso() == StatusProcesso.CUMPRIMENTO_SENTENCA;
    }

    private boolean supportsCustody(Processo processo) {
        return processo.getFaseAtual() == FaseProcessual.AUDIENCIA_CUSTODIA
                || processo.getRito() != null && (processo.getRito().name().contains("PENAL") || processo.getRito().name().contains("JURI") || processo.getRito().name().contains("EXECUCAO_PENAL"));
    }

    private long countByQueue(List<WorkItem> workItems, String queueCode, String inboxKey) {
        return workItems.stream()
                .filter(this::aberto)
                .filter(item -> matches(item.getQueueCode(), queueCode) || matches(item.getInboxKey(), inboxKey))
                .count();
    }

    private long countByToken(List<WorkItem> workItems, String... tokens) {
        return workItems.stream()
                .filter(this::aberto)
                .filter(item -> {
                    String haystack = normalize(item.getQueueCode()) + ' ' + normalize(item.getInboxKey()) + ' ' + normalize(item.getTemplateCode()) + ' ' + normalize(item.getTitulo());
                    for (String token : tokens) {
                        if (haystack.contains(normalize(token))) {
                            return true;
                        }
                    }
                    return false;
                })
                .count();
    }

    private List<String> mandadosFor(Processo processo, ExecutionEnforcementProfile profile) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (profile.actType().equals("PENHORA") || profile.actType().equals("BLOQUEIO_ATIVOS")) {
            out.add("MANDADO_PENHORA_E_AVALIACAO");
        }
        if (profile.actType().equals("ALIENACAO_JUDICIAL") || profile.actType().equals("HASTA_PUBLICA")) {
            out.add("EDITAL_EXPROPRIATORIO");
            out.add("MANDADO_INTIMACAO_EXPROPRIACAO");
        }
        if (profile.speciesCode().equals("ENTREGA_COISA")) {
            out.add("MANDADO_BUSCA_APREENSAO_E_ENTREGA");
        }
        if (supportsCustody(processo)) {
            out.addAll(mandadosCustodia(processo));
        }
        return List.copyOf(out);
    }

    private List<String> mandadosCustodia(Processo processo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("MANDADO_APRESENTACAO_CUSTODIADO");
        if (processo.getStatusProcesso() == StatusProcesso.CITACAO_REALIZADA || processo.getFaseAtual() == FaseProcessual.AUDIENCIA_CUSTODIA) {
            out.add("ALVARA_APRESENTACAO_AUDIENCIA");
        }
        if (processo.getRito() != null && processo.getRito().name().contains("EXECUCAO_PENAL")) {
            out.add("GUIA_RECOLHIMENTO_E_MOVIMENTACAO_PRISIONAL");
        }
        return List.copyOf(out);
    }

    private List<String> operacoesCustodia(Processo processo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("CONFIRMAR_CUSTODIA");
        out.add("REGISTRAR_APRESENTACAO");
        out.add("EMITIR_CERTIDAO_OPERACIONAL");
        if (processo.getRito() != null && processo.getRito().name().contains("EXECUCAO_PENAL")) {
            out.add("ATUALIZAR_MOVIMENTACAO_PRISIONAL");
        }
        return List.copyOf(out);
    }

    private String inferIncidentType(Processo processo) {
        if (processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita()) {
            return "EMBARGOS_EXECUCAO_FISCAL";
        }
        if (supportsCustody(processo)) {
            return "HABILITACAO_CREDITO";
        }
        if (processo.getStatusProcesso() == StatusProcesso.CUMPRIMENTO_SENTENCA) {
            return "IMPUGNACAO_CUMPRIMENTO";
        }
        return "EXCECAO_PRE_EXECUTIVIDADE";
    }

    private String inferEnforcementAct(Processo processo) {
        if (supportsCustody(processo)) {
            return "SATISFACAO_FINAL";
        }
        if (processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita()) {
            return "BLOQUEIO_ATIVOS";
        }
        if (processo.getFaseAtual() == FaseProcessual.PENHORA || processo.getFaseAtual() == FaseProcessual.EXECUCAO) {
            return "PENHORA";
        }
        return "ALIENACAO_JUDICIAL";
    }

    private String inferEnforcementDetail(Processo processo) {
        if (supportsCustody(processo)) {
            return "ENTREGA_COISA";
        }
        if (processo.getRito() != null && processo.getRito().isTrabalhista()) {
            return "FAZER TRABALHISTA";
        }
        return processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita() ? "QUANTIA FISCAL" : "QUANTIA";
    }

    private String inferSatisfactionMode(Processo processo) {
        if (processo.getStatusProcesso() == StatusProcesso.TRANSITO_EM_JULGADO && !isProcessoExecutivo(processo)) {
            return "SATISFACAO_FRUSTRADA";
        }
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO) {
            return "EXTINCAO_POR_PAGAMENTO";
        }
        return isProcessoExecutivo(processo) ? "SATISFACAO_PARCIAL" : "SUSPENSAO_SEM_BENS";
    }

    private String inferClosureMode(Processo processo) {
        return processo.getStatusProcesso() != null && processo.getStatusProcesso().isArquivadoOuBaixado()
                ? "FECHAMENTO_INTEGRAL"
                : supportsCustody(processo) ? "FECHAMENTO_CONTROLADO_CUSTODIA" : "FECHAMENTO_PARCIAL";
    }

    private String inferPreferencia(Processo processo) {
        if (processo.getRito() != null && processo.getRito().isTrabalhista()) {
            return "TRABALHISTA";
        }
        if (processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita()) {
            return "FISCAL";
        }
        return "ORDINARIA";
    }

    private String inferSubrogacao(Processo processo) {
        return processo.getAssunto() != null && normalize(processo.getAssunto()).contains("SUBROG") ? "SIM" : "NAO";
    }

    private double inferPercentualSatisfeito(Processo processo) {
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO || processo.getStatusProcesso() == StatusProcesso.BAIXADO) {
            return 100D;
        }
        if (isProcessoExecutivo(processo)) {
            return 60D;
        }
        return 0D;
    }

    private double inferSaldoRemanescente(Processo processo) {
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO || processo.getStatusProcesso() == StatusProcesso.BAIXADO) {
            return 0D;
        }
        return supportsCustody(processo) ? 0D : 1D;
    }

    private double inferValorGarantia(Processo processo) {
        return processo.getValorCausa() == null ? 0D : processo.getValorCausa().doubleValue();
    }

    private boolean aberto(WorkItem item) {
        return item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO;
    }

    private boolean vencido(WorkItem item) {
        return aberto(item) && item.getDueAt() != null && item.getDueAt().isBefore(Instant.now());
    }

    private boolean matches(String current, String expected) {
        return current != null && expected != null && normalize(current).equals(normalize(expected));
    }

    private List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) merged.addAll(first);
        if (second != null) merged.addAll(second);
        return List.copyOf(merged);
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
