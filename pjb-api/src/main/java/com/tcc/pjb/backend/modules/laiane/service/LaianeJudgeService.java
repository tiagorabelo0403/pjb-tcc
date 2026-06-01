package com.tcc.pjb.backend.modules.laiane.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeMovimentacaoLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeAgendaEventDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeAgendaResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeInitialDispatchRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeInitialDispatchResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeOnePagerResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeQueueBucketDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeQueuePanelResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeSanitationChecklistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeUrgencyPanelResponse;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaianeJudgeService {

    private final LaianeRoleGuard guard;
    private final WorkItemRepository workItemRepository;
    private final PjbTimeService timeService;
    private final EventoProcessualRepository eventoProcessualRepository;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoProcessualRepository;
    private final PrecedenteRepository precedenteRepository;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;

    public LaianeJudgeUrgencyPanelResponse urgencies() {
        var juiz = guard.requireMagistratura();
        Page<WorkItem> page = workItemRepository.inboxByUser(juiz.getId(), PageRequest.of(0, 30));
        List<LaianeWorkItemLiteDto> items = page.getContent().stream().map(this::toLite).toList();
        return LaianeJudgeUrgencyPanelResponse.builder()
                .items(items)
                .hint("Painel priorizado por prazo, sigilo, bloqueio e risco operacional.")
                .build();
    }

    public LaianeJudgeSanitationChecklistResponse sanitationChecklist(Long processoId) {
        guard.requireMagistratura();
        return LaianeJudgeSanitationChecklistResponse.builder()
                .processoId(processoId)
                .checklist(List.of(
                        "Pontos controvertidos e teses relevantes",
                        "Provas admitidas, lacunas e contradições aparentes",
                        "Competência, nulidades e prevenção",
                        "Saneamento, instrução e atos pendentes críticos"
                ))
                .build();
    }

    public LaianeJudgeInitialDispatchResponse initialDispatch(LaianeJudgeInitialDispatchRequest req) {
        guard.requireMagistratura();
        String rito = (req.getRito() == null || req.getRito().isBlank()) ? "DESCONHECIDO" : req.getRito().trim();
        String minuta = "Vistos.\n\n1) Recebo a inicial.\n2) Cite-se/intime-se conforme o rito " + rito + ".\n3) Defiro as providências preparatórias necessárias.\n4) Cumpra-se.";
        return LaianeJudgeInitialDispatchResponse.builder()
                .processId(req.getProcessoId())
                .rito(rito)
                .minuta(minuta)
                .travas(List.of("TRAVA_COMPETENCIA", "TRAVA_CITACAO", "TRAVA_SIGILO"))
                .build();
    }

    @Transactional(readOnly = true)
    public LaianeJudgeAgendaResponse agenda(LocalDateTime inicio, LocalDateTime fim) {
        var juiz = guard.requireMagistratura();
        LocalDateTime safeInicio = (inicio != null) ? inicio : LocalDate.now().atStartOfDay();
        LocalDateTime safeFim = (fim != null) ? fim : safeInicio.plusDays(7);
        if (!safeFim.isAfter(safeInicio)) {
            throw new IllegalArgumentException("Intervalo inválido: fim deve ser depois de inicio.");
        }

        List<EventoProcessual> eventos = eventoProcessualRepository.findEventosNaAgenda(juiz.getId(), safeInicio, safeFim)
                .stream()
                .filter(e -> e.getStatus() == null || e.getStatus() == StatusEvento.PENDENTE || e.getStatus() == StatusEvento.CONCLUIDO)
                .sorted(Comparator.comparing(EventoProcessual::getDataInicio))
                .toList();

        int n = eventos.size();
        int[] counts = new int[n];
        boolean[] conflict = new boolean[n];
        for (int i = 0; i < n; i++) {
            EventoProcessual a = eventos.get(i);
            for (int j = i + 1; j < n; j++) {
                EventoProcessual b = eventos.get(j);
                if (b.getDataInicio() == null || a.getDataFim() == null) {
                    continue;
                }
                if (!b.getDataInicio().isBefore(a.getDataFim())) {
                    break;
                }
                if (b.getDataFim() != null && a.getDataInicio() != null && a.getDataInicio().isBefore(b.getDataFim())) {
                    counts[i]++;
                    counts[j]++;
                    conflict[i] = true;
                    conflict[j] = true;
                }
            }
        }

        List<LaianeAgendaEventDto> dtos = new ArrayList<>();
        int totalConflitos = 0;
        for (int i = 0; i < n; i++) {
            EventoProcessual e = eventos.get(i);
            if (conflict[i]) {
                totalConflitos++;
            }
            dtos.add(LaianeAgendaEventDto.builder()
                    .id(e.getId())
                    .processoId(e.getProcesso() != null ? e.getProcesso().getId() : null)
                    .tipo(e.getTipo() != null ? e.getTipo().name() : null)
                    .status(e.getStatus() != null ? e.getStatus().name() : null)
                    .titulo(e.getTitulo())
                    .descricao(e.getDescricao())
                    .dataInicio(toOffset(e.getDataInicio()))
                    .dataFim(toOffset(e.getDataFim()))
                    .conflict(conflict[i])
                    .conflictCount(counts[i])
                    .build());
        }

        return LaianeJudgeAgendaResponse.builder()
                .inicio(toOffset(safeInicio))
                .fim(toOffset(safeFim))
                .total(dtos.size())
                .totalConflitos(totalConflitos)
                .events(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public LaianeJudgeQueuePanelResponse queuePanel(int limit) {
        var juiz = guard.requireMagistratura();
        int safeLimit = Math.max(5, Math.min(limit, 100));

        Instant now = timeService.nowUtc();
        Instant d0 = now;
        Instant d1 = now.plus(1, ChronoUnit.DAYS);
        Instant d3 = now.plus(3, ChronoUnit.DAYS);
        Instant d7 = now.plus(7, ChronoUnit.DAYS);

        List<WorkItem> meus = workItemRepository.inboxByUser(juiz.getId(), PageRequest.of(0, safeLimit)).getContent();
        List<WorkItem> unidade = workItemRepository.inboxByRoleAndTerritory(juiz.getTipoUsuario(), juiz.getUf(), juiz.getComarca(), PageRequest.of(0, safeLimit)).getContent();
        List<WorkItem> meusPorPrazo = workItemRepository.findDueByAssignedUser(juiz.getId(), d7, PageRequest.of(0, safeLimit));
        List<WorkItem> unidadePorPrazo = workItemRepository.findDueByRoleAndTerritory(juiz.getTipoUsuario(), juiz.getUf(), juiz.getComarca(), d7, PageRequest.of(0, safeLimit));

        List<WorkItem> merged = new ArrayList<>();
        merged.addAll(meus);
        merged.addAll(unidade);
        merged.addAll(meusPorPrazo);
        merged.addAll(unidadePorPrazo);

        List<WorkItem> deduplicados = new ArrayList<>(new LinkedHashSet<>(merged));
        java.util.Map<String, List<WorkItem>> buckets = new java.util.LinkedHashMap<>();
        buckets.put("VENCIDOS", new ArrayList<>());
        buckets.put("HOJE", new ArrayList<>());
        buckets.put("ATE_3_DIAS", new ArrayList<>());
        buckets.put("ATE_7_DIAS", new ArrayList<>());
        buckets.put("SEM_PRAZO", new ArrayList<>());

        for (WorkItem w : deduplicados) {
            if (w.getDueAt() == null) {
                buckets.get("SEM_PRAZO").add(w);
            } else if (w.getDueAt().isBefore(d0)) {
                buckets.get("VENCIDOS").add(w);
            } else if (w.getDueAt().isBefore(d1)) {
                buckets.get("HOJE").add(w);
            } else if (w.getDueAt().isBefore(d3)) {
                buckets.get("ATE_3_DIAS").add(w);
            } else {
                buckets.get("ATE_7_DIAS").add(w);
            }
        }

        List<LaianeJudgeQueueBucketDto> out = new ArrayList<>();
        int total = 0;
        for (var entry : buckets.entrySet()) {
            List<LaianeWorkItemLiteDto> items = entry.getValue().stream()
                    .sorted(Comparator.comparing(WorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(safeLimit)
                    .map(this::toLite)
                    .toList();
            out.add(LaianeJudgeQueueBucketDto.builder()
                    .nome(entry.getKey())
                    .count(entry.getValue().size())
                    .items(items)
                    .build());
            total += entry.getValue().size();
        }

        return LaianeJudgeQueuePanelResponse.builder()
                .generatedAt(now)
                .uf(juiz.getUf())
                .comarca(juiz.getComarca())
                .total(total)
                .buckets(out)
                .build();
    }

    @Transactional(readOnly = true)
    public LaianeJudgeOnePagerResponse onePager(Long processoId) {
        guard.requireMagistratura();
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório");
        }

        Processo p = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new NoSuchElementException("Processo não encontrado"));

        List<WorkItem> wItems = workItemRepository.findAllByProcesso(processoId);
        List<LaianeWorkItemLiteDto> proximos = wItems.stream()
                .filter(w -> w.getDueAt() != null)
                .sorted(Comparator.comparing(WorkItem::getDueAt))
                .limit(12)
                .map(this::toLite)
                .toList();

        List<MovimentacaoProcessual> movs = movimentacaoProcessualRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        List<LaianeMovimentacaoLiteDto> movDtos = movs.stream().limit(12).map(this::toMovLite).toList();

        var ritoSnapshot = processoRitoSnapshotService.resolve(p, null);
        var precPage = precedenteRepository.search(null, null, p.getRamoDireito(), ritoSnapshot.rito(), null, PageRequest.of(0, 5));
        List<LaianePrecedenteLiteDto> precDtos = precPage.getContent().stream().map(this::toPrecedenteLite).toList();

        return LaianeJudgeOnePagerResponse.builder()
                .processoId(p.getId())
                .numeroUnificado(p.getNumeroUnificado())
                .classeProcessual(p.getClasseProcessual())
                .assunto(p.getAssunto())
                .rito(ritoSnapshot.ritoCode())
                .faseAtual(p.getFaseAtual() != null ? p.getFaseAtual().name() : null)
                .status(resolveStatus(p))
                .ultimaMovimentacao(toOffset(p.getDataUltimaMovimentacao()))
                .resumoProcessual(firstNonBlank(p.getResumoIA(), p.getObjetoProcessual(), p.getPedidoPrincipal(), p.getAssunto()))
                .fatosRelevantes(firstNonBlank(p.getObjetoProcessual(), p.getResumoIA(), p.getAssunto()))
                .pedidosCentrais(splitNarrative(p.getPedidosConsolidados(), p.getPedidoPrincipal()))
                .provasChave(splitNarrative(p.getMaterialProbatorioResumo(), p.getMaterialProbatorioHash()))
                .tesesProvaveis(buildTesesProvaveis(p, precDtos))
                .badgeSigilo(p.getNivelSigilo() != null ? p.getNivelSigilo().name() : "PUBLICO")
                .scoreComplexidade(p.getScoreComplexidade())
                .proximosPrazos(proximos)
                .ultimasMovimentacoes(movDtos)
                .precedentesRelacionados(precDtos)
                .build();
    }

    private String resolveStatus(Processo processo) {
        if (processo.getStatusProcesso() != null) {
            return processo.getStatusProcesso().name();
        }
        return null;
    }

    private List<String> buildTesesProvaveis(Processo processo, List<LaianePrecedenteLiteDto> precedentes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) {
            out.add("Enquadramento principal: " + processo.getRamoDireito().name());
        }
        if (processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            out.add("Classe processual relevante: " + processo.getClasseProcessual().trim());
        }
        precedentes.stream()
                .map(LaianePrecedenteLiteDto::tema)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(3)
                .forEach(tema -> out.add("Precedente convergente: " + tema));
        return List.copyOf(out);
    }

    private List<String> splitNarrative(String primary, String fallback) {
        String base = firstNonBlank(primary, fallback);
        if (base == null || base.isBlank()) {
            return List.of();
        }
        String normalized = base.replace('\r', '\n');
        String[] raw = normalized.split("(?:\\n|;|•|-\\s+)");
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw) {
            String value = part == null ? null : part.trim();
            if (value == null || value.isBlank()) {
                continue;
            }
            values.add(limit(value, 280));
        }
        if (values.isEmpty()) {
            return List.of(limit(base.trim(), 280));
        }
        return values.stream().limit(6).toList();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private LaianeWorkItemLiteDto toLite(WorkItem w) {
        return LaianeWorkItemLiteDto.builder()
                .id(w.getId())
                .processoId(w.getProcesso() != null ? w.getProcesso().getId() : null)
                .titulo(w.getTitulo())
                .status(w.getStatus() != null ? w.getStatus().name() : null)
                .prioridade(w.getPrioridade())
                .blocking(w.isBlocking())
                .dueAt(w.getDueAt())
                .build();
    }

    private LaianeMovimentacaoLiteDto toMovLite(MovimentacaoProcessual m) {
        return LaianeMovimentacaoLiteDto.builder()
                .id(m.getId())
                .dataMovimentacao(m.getDataMovimentacao() != null ? m.getDataMovimentacao().atZone(timeService.legalZone()).toOffsetDateTime() : null)
                .tipo(m.getFasePara() != null ? m.getFasePara().name() : (m.getFaseDe() != null ? m.getFaseDe().name() : null))
                .descricao(m.getDescricao())
                .build();
    }

    private LaianePrecedenteLiteDto toPrecedenteLite(com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente p) {
        String tribunal = p.getFonte() != null ? p.getFonte().name() : null;
        String tema = p.getIdentificador() != null ? p.getIdentificador() : (p.getTipo() != null ? p.getTipo().name() : null);
        return LaianePrecedenteLiteDto.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .tribunal(tribunal)
                .tema(tema)
                .ementa(p.getEmentaResumo())
                .build();
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(timeService.legalZone()).toOffsetDateTime();
    }
}
