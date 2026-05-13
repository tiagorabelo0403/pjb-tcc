package com.tcc.pjb.backend.core.processo.trabalho.application;

import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoCard;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoFila;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProcessoTrabalhoApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoTrabalhoApplicationService(ProcessoRepository processoRepository,
                                              WorkItemRepository workItemRepository,
                                              ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoTrabalhoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processoId);
        ProcessoUnificadoAggregate agregado = processoUnificadoApplicationService.detalhar(processoId);
        List<ProcessoTrabalhoFila> filas = agruparFilas(workItems, agregado);
        long pendentes = workItems.stream().filter(item -> item.getStatus() == WorkItemStatus.PENDENTE).count();
        long emExecucao = workItems.stream().filter(item -> item.getStatus() == WorkItemStatus.EM_EXECUCAO).count();
        long bloqueantes = workItems.stream().filter(WorkItem::isBlocking).filter(this::aberto).count();
        long vencidos = workItems.stream().filter(this::vencido).count();
        long semResponsavelNominal = workItems.stream().filter(this::aberto).filter(item -> item.getAssignedUser() == null).count();
        return new ProcessoTrabalhoAggregate(
                identity(processo),
                workItems.size(),
                pendentes,
                emExecucao,
                bloqueantes,
                vencidos,
                semResponsavelNominal,
                faixaOperacional(workItems, agregado),
                filas,
                gates(processo, workItems, agregado),
                proximoMelhorFluxo(workItems, agregado),
                Instant.now()
        );
    }

    private List<ProcessoTrabalhoFila> agruparFilas(List<WorkItem> workItems, ProcessoUnificadoAggregate agregado) {
        Map<String, List<WorkItem>> porFila = workItems.stream()
                .collect(Collectors.groupingBy(this::resolveFila, java.util.LinkedHashMap::new, Collectors.toList()));
        List<ProcessoTrabalhoFila> filas = porFila.entrySet().stream()
                .map(entry -> toFila(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ProcessoTrabalhoFila::bloqueantes).reversed()
                        .thenComparing(ProcessoTrabalhoFila::vencidos).reversed()
                        .thenComparing(ProcessoTrabalhoFila::pendentes).reversed()
                        .thenComparing(ProcessoTrabalhoFila::codigo))
                .toList();
        if (!filas.isEmpty()) {
            return filas;
        }
        List<ProcessoTrabalhoCard> sinteticos = agregado.atosPermitidos().stream()
                .limit(6)
                .map(this::toSyntheticCard)
                .toList();
        return List.of(new ProcessoTrabalhoFila(
                "TRILHA_SUGERIDA",
                "Trilha sugerida pelo motor processual",
                sinteticos.size(),
                sinteticos.size(),
                sinteticos.stream().filter(ProcessoTrabalhoCard::bloqueante).count(),
                0,
                null,
                sinteticos.stream().map(ProcessoTrabalhoCard::papel).distinct().toList(),
                sinteticos
        ));
    }

    private ProcessoTrabalhoFila toFila(String codigo, List<WorkItem> items) {
        List<ProcessoTrabalhoCard> cards = items.stream()
                .sorted(Comparator.comparing(this::vencido).reversed()
                        .thenComparing(item -> item.getDueAt() == null ? Instant.MAX : item.getDueAt())
                        .thenComparing(WorkItem::getPrioridade)
                        .thenComparing(WorkItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCard)
                .toList();
        Instant proximoVencimento = items.stream()
                .map(WorkItem::getDueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new ProcessoTrabalhoFila(
                codigo,
                tituloFila(codigo),
                items.size(),
                items.stream().filter(item -> item.getStatus() == WorkItemStatus.PENDENTE).count(),
                items.stream().filter(WorkItem::isBlocking).filter(this::aberto).count(),
                items.stream().filter(this::vencido).count(),
                proximoVencimento,
                items.stream().map(this::resolvePapel).distinct().toList(),
                cards
        );
    }

    private ProcessoTrabalhoCard toCard(WorkItem item) {
        return new ProcessoTrabalhoCard(
                item.getId(),
                item.getTitulo(),
                item.getTemplateCode(),
                item.getType() == null ? "OUTRO" : item.getType().name(),
                resolveFila(item),
                item.getInboxKey(),
                resolvePapel(item),
                item.getStatus() == null ? "PENDENTE" : item.getStatus().name(),
                item.getPrioridade() == null ? 3 : item.getPrioridade(),
                item.isBlocking(),
                vencido(item),
                venceEmAte48h(item),
                item.getDueAt(),
                etiquetas(item)
        );
    }

    private ProcessoTrabalhoCard toSyntheticCard(ProcessoUnificadoAto ato) {
        return new ProcessoTrabalhoCard(
                null,
                ato.titulo(),
                ato.transitionKey(),
                ato.workItemType(),
                ato.filaPadrao(),
                ato.inboxPadrao(),
                ato.responsavelSugerido(),
                ato.permitido() ? "SUGERIDO" : "BLOQUEADO",
                ato.sensivel() ? 1 : 2,
                ato.exigeSegurancaElevada(),
                false,
                false,
                null,
                List.of(ato.eixoOperacional(), ato.categoria(), ato.recursal() ? "RECURSAL" : "TRILHA_BASE")
        );
    }

    private ProcessoTrabalhoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) {
            marcadores.add(processo.getRamoDireito().name());
        }
        if (processo.getRito() != null) {
            marcadores.add(processo.getRito().name());
        }
        if (processo.getFaseAtual() != null) {
            marcadores.add(processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            marcadores.add(processo.getStatusProcesso().name());
        }
        if (processo.getNivelSigilo() != null) {
            marcadores.add(processo.getNivelSigilo().name());
        }
        return new ProcessoTrabalhoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                processo.getTribunal(),
                processo.getVara(),
                List.copyOf(marcadores)
        );
    }

    private String faixaOperacional(List<WorkItem> workItems, ProcessoUnificadoAggregate agregado) {
        long sensiveis = agregado.atosPermitidos().stream().filter(ProcessoUnificadoAto::sensivel).count();
        if (workItems.stream().anyMatch(this::vencido) || workItems.stream().anyMatch(item -> item.isBlocking() && aberto(item))) {
            return "CRITICA";
        }
        if (sensiveis > 0 || workItems.stream().anyMatch(item -> item.getStatus() == WorkItemStatus.EM_EXECUCAO)) {
            return "ELEVADA";
        }
        return "CONTROLADA";
    }

    private List<String> gates(Processo processo, List<WorkItem> workItems, ProcessoUnificadoAggregate agregado) {
        LinkedHashSet<String> gates = new LinkedHashSet<>();
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isArquivadoOuBaixado()) {
            gates.add("TRAVA_DE_ATOS_MODIFICADORES_EM_PROCESSO_ENCERRADO");
        }
        if (workItems.stream().anyMatch(item -> item.isBlocking() && aberto(item))) {
            gates.add("EXISTE_BLOQUEANTE_ABERTO_NA_FILA");
        }
        if (workItems.stream().anyMatch(this::vencido)) {
            gates.add("EXISTE_ITEM_VENCIDO_COM_REORDENACAO_OBRIGATORIA");
        }
        if (agregado.diagnostico().blockingFindings() > 0) {
            gates.add("MOTOR_DE_COERENCIA_COM_ACHADOS_BLOQUEANTES");
        }
        if (workItems.stream().filter(this::aberto).allMatch(item -> item.getAssignedUser() == null)) {
            gates.add("CAIXA_SEM_RESPONSAVEL_NOMINAL_ATIVO");
        }
        return List.copyOf(gates);
    }

    private List<String> proximoMelhorFluxo(List<WorkItem> workItems, ProcessoUnificadoAggregate agregado) {
        if (workItems.stream().anyMatch(this::vencido)) {
            return workItems.stream()
                    .filter(this::vencido)
                    .sorted(Comparator.comparing(item -> item.getDueAt() == null ? Instant.MIN : item.getDueAt()))
                    .limit(4)
                    .map(item -> "REGULARIZAR:" + item.getTitulo())
                    .toList();
        }
        if (workItems.stream().anyMatch(item -> item.isBlocking() && aberto(item))) {
            return workItems.stream()
                    .filter(item -> item.isBlocking() && aberto(item))
                    .sorted(Comparator.comparing(item -> item.getDueAt() == null ? Instant.MAX : item.getDueAt()))
                    .limit(4)
                    .map(item -> "DESTRAVAR:" + item.getTitulo())
                    .toList();
        }
        return agregado.proximoMelhorAto();
    }

    private List<String> etiquetas(WorkItem item) {
        LinkedHashSet<String> etiquetas = new LinkedHashSet<>();
        etiquetas.add(resolveFila(item));
        etiquetas.add(resolvePapel(item));
        if (item.isBlocking()) {
            etiquetas.add("BLOQUEANTE");
        }
        if (venceEmAte48h(item)) {
            etiquetas.add("48H");
        }
        if (vencido(item)) {
            etiquetas.add("VENCIDO");
        }
        if (item.getType() != null) {
            etiquetas.add(item.getType().name());
        }
        return List.copyOf(etiquetas);
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private boolean aberto(WorkItem item) {
        return item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO;
    }

    private boolean vencido(WorkItem item) {
        return aberto(item) && item.getDueAt() != null && item.getDueAt().isBefore(Instant.now());
    }

    private boolean venceEmAte48h(WorkItem item) {
        return aberto(item)
                && item.getDueAt() != null
                && !item.getDueAt().isBefore(Instant.now())
                && Duration.between(Instant.now(), item.getDueAt()).toHours() <= 48;
    }

    private String resolveFila(WorkItem item) {
        if (item.getQueueCode() != null && !item.getQueueCode().isBlank()) {
            return item.getQueueCode();
        }
        if (item.getType() != null) {
            return item.getType().name();
        }
        return "FILA_GERAL";
    }

    private String tituloFila(String codigo) {
        return codigo.replace('_', ' ');
    }

    private String resolvePapel(WorkItem item) {
        if (item.getAssignedRole() != null) {
            return item.getAssignedRole().name();
        }
        if (item.getAssignedUser() != null) {
            return "RESPONSAVEL_NOMINAL";
        }
        return "SEM_PAPEL_DEFINIDO";
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
