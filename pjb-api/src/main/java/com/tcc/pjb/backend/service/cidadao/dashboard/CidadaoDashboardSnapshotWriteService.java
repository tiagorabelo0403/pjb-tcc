package com.tcc.pjb.backend.service.cidadao.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelBadgesDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProximoEventoDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoWidgetDto;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardItem;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardSnapshot;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.cidadao.CidadaoDashboardItemRepository;
import com.tcc.pjb.backend.repository.cidadao.CidadaoDashboardSnapshotRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.cidadao.CidadaoGovHubService;
import com.tcc.pjb.backend.service.cidadao.CidadaoPendenciasService;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoCardMapper;

@Service
public class CidadaoDashboardSnapshotWriteService {

    private final UsuarioRepository usuarioRepo;
    private final ProcessoRepository processoRepo;
    private final MovimentacaoProcessualRepository movRepo;
    private final DocumentoProcessualRepository docRepo;
    private final CidadaoDashboardEventosService cidadaoDashboardEventosService;
    private final CidadaoProcessoCardMapper cardMapper;
    private final CidadaoPendenciasService pendenciasService;
    private final CidadaoGovHubService govHubService;
    private final CidadaoDashboardSnapshotRepository snapshotRepo;
    private final CidadaoDashboardItemRepository itemRepo;
    private final ObjectMapper mapper;

    public CidadaoDashboardSnapshotWriteService(
            UsuarioRepository usuarioRepo,
            ProcessoRepository processoRepo,
            MovimentacaoProcessualRepository movRepo,
            DocumentoProcessualRepository docRepo,
            CidadaoDashboardEventosService cidadaoDashboardEventosService,
            CidadaoProcessoCardMapper cardMapper,
            CidadaoPendenciasService pendenciasService,
            CidadaoGovHubService govHubService,
            CidadaoDashboardSnapshotRepository snapshotRepo,
            CidadaoDashboardItemRepository itemRepo,
            ObjectMapper mapper) {
        this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
        this.processoRepo = Objects.requireNonNull(processoRepo);
        this.movRepo = Objects.requireNonNull(movRepo);
        this.docRepo = Objects.requireNonNull(docRepo);
        this.cidadaoDashboardEventosService = Objects.requireNonNull(cidadaoDashboardEventosService);
        this.cardMapper = Objects.requireNonNull(cardMapper);
        this.pendenciasService = Objects.requireNonNull(pendenciasService);
        this.govHubService = Objects.requireNonNull(govHubService);
        this.snapshotRepo = Objects.requireNonNull(snapshotRepo);
        this.itemRepo = Objects.requireNonNull(itemRepo);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Transactional
    public void refreshForCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return;
        Usuario u = usuarioRepo.findByCpf(cpf.trim()).orElse(null);
        if (u == null || u.getId() == null) return;
        if (u.getTipoUsuario() == null || !"CIDADAO".equals(u.getTipoUsuario().name())) return;
        if (!u.isAtivo()) return;
        refreshForUser(u);
    }

    @Transactional
    public CidadaoDashboardSnapshot refreshForUser(Usuario u) {
        Objects.requireNonNull(u);
        Long userId = u.getId();
        String cpf = u.getCpf();
        if (userId == null || cpf == null || cpf.isBlank()) {
            return upsertEmpty(userId, cpf);
        }

        String cpfNorm = cpf.trim();
        String cpfHash = Hashes.sha256Hex(cpfNorm);

        List<CidadaoPendenciaDto> pendencias = pendenciasService.pendenciasForCpf(cpfNorm);
        Page<Processo> page = processoRepo.searchQuick(cpfNorm, null, null, PageRequest.of(0, 200));
        List<Processo> processos = page.getContent();

        List<Long> ids = processos.stream().map(Processo::getId).filter(Objects::nonNull).toList();
        Map<Long, MovimentacaoProcessual> lastMov = lastMov(ids);
        Map<Long, Long> docCount = docCount(ids);
        Map<Long, Audiencia> nextAud = cidadaoDashboardEventosService.nextAud(ids);
        Map<Long, JulgamentoColegiado> nextJulg = cidadaoDashboardEventosService.nextJulg(ids);

        List<CidadaoProcessoCardDto> cards = new ArrayList<>(processos.size());
        for (Processo p : processos) {
            if (p == null || p.getId() == null) continue;
            cards.add(cardMapper.toCard(
                    p,
                    lastMov.get(p.getId()),
                    docCount.getOrDefault(p.getId(), 0L),
                    nextAud.get(p.getId()),
                    nextJulg.get(p.getId())
            ));
        }

        List<CidadaoProcessoCardDto> recentes = cards.size() > 10 ? cards.subList(0, 10) : cards;
        List<Processo> processosAll = processoRepo.findAllByPartesCpf(cpfNorm);

        List<CidadaoProximoEventoDto> proximosEventos = cidadaoDashboardEventosService.proximosEventos(processosAll);
        CidadaoPainelBadgesDto badges = badges(processosAll, pendencias, proximosEventos);

        List<CidadaoWidgetDto> widgets = widgets(processosAll, pendencias);
        CidadaoGovHubDto govHub = govHubService.hubForUf(u.getUf());

        upsertItems(userId, processos, cards);

        CidadaoDashboardSnapshot prev = snapshotRepo.findById(userId).orElse(null);
        long nextVersion = prev != null ? (prev.getVersion() + 1L) : 1L;
        Instant now = Instant.now();

        CidadaoDashboardSnapshot snap = CidadaoDashboardSnapshot.builder()
                .cidadaoUserId(userId)
                .cpfHash(cpfHash)
                .version(nextVersion)
                .updatedAt(now)
                .badgesJson(toJson(badges))
                .widgetsJson(toJson(widgets))
                .pendenciasJson(toJson(pendencias))
                .proximosEventosJson(toJson(proximosEventos))
                .recentesJson(toJson(recentes))
                .govHubJson(toJson(govHub))
                .build();

        return snapshotRepo.save(snap);
    }

    private CidadaoDashboardSnapshot upsertEmpty(Long userId, String cpf) {
        if (userId == null) {
            return null;
        }
        String cpfHash = Hashes.sha256Hex(cpf == null ? "" : cpf);
        Instant now = Instant.now();
        CidadaoDashboardSnapshot prev = snapshotRepo.findById(userId).orElse(null);
        long nextVersion = prev != null ? (prev.getVersion() + 1L) : 1L;

        CidadaoDashboardSnapshot snap = CidadaoDashboardSnapshot.builder()
                .cidadaoUserId(userId)
                .cpfHash(cpfHash)
                .version(nextVersion)
                .updatedAt(now)
                .badgesJson("{}")
                .widgetsJson("[]")
                .pendenciasJson("[]")
                .proximosEventosJson("[]")
                .recentesJson("[]")
                .govHubJson("{}")
                .build();
        return snapshotRepo.save(snap);
    }

    private void upsertItems(Long userId, List<Processo> processos, List<CidadaoProcessoCardDto> cards) {
        if (userId == null || processos == null || cards == null) return;
        Map<Long, CidadaoProcessoCardDto> byId = new HashMap<>();
        for (CidadaoProcessoCardDto c : cards) {
            if (c == null || c.processoId() == null) continue;
            byId.put(c.processoId(), c);
        }
        Instant now = Instant.now();
        List<CidadaoDashboardItem> items = new ArrayList<>();
        for (Processo p : processos) {
            if (p == null || p.getId() == null) continue;
            CidadaoProcessoCardDto card = byId.get(p.getId());
            if (card == null) continue;
            long sortKey = sortKey(p);
            String flagsJson = flagsFor(p);
            items.add(CidadaoDashboardItem.builder()
                    .cidadaoUserId(userId)
                    .processoId(p.getId())
                    .lastUpdateAt(now)
                    .sortKey(sortKey)
                    .cardJson(toJson(card))
                    .flagsJson(flagsJson)
                    .build());
        }
        if (!items.isEmpty()) {
            itemRepo.saveAll(items);
        }
    }

    private long sortKey(Processo p) {
        try {
            if (p.getDataUltimaMovimentacao() != null) {
                return p.getDataUltimaMovimentacao().toInstant(ZoneOffset.UTC).toEpochMilli();
            }
        } catch (Exception ignored) {
        }
        return p.getId() != null ? p.getId() : 0L;
    }

    private String flagsFor(Processo p) {
        try {
            ObjectNode n = mapper.createObjectNode();
            n.put("sigiloso", p.getNivelSigilo() != null && p.getNivelSigilo().exigeCredencial());
            n.put("status", p.getStatusProcesso() != null ? p.getStatusProcesso().name() : null);
            n.put("ramo", p.getRamoDireito() != null ? p.getRamoDireito().name() : null);
            return mapper.writeValueAsString(n);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<Long, MovimentacaoProcessual> lastMov(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, MovimentacaoProcessual> m = new HashMap<>();
        for (MovimentacaoProcessual x : movRepo.findLatestByProcessoIds(ids)) {
            if (x == null || x.getProcesso() == null || x.getProcesso().getId() == null) continue;
            m.put(x.getProcesso().getId(), x);
        }
        return m;
    }

    private Map<Long, Long> docCount(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, Long> m = new HashMap<>();
        for (DocumentoProcessualRepository.ProcessoDocCount row : docRepo.countDocsByProcessoIds(ids)) {
            if (row == null || row.getProcessoId() == null) continue;
            m.put(row.getProcessoId(), row.getCnt());
        }
        return m;
    }

    private CidadaoPainelBadgesDto badges(List<Processo> all, List<CidadaoPendenciaDto> pendencias, List<CidadaoProximoEventoDto> proximosEventos) {
        long total = all != null ? all.size() : 0;
        int sig = 0;
        if (all != null) {
            for (Processo p : all) {
                NivelSigilo sigilo = p == null || p.getNivelSigilo() == null ? NivelSigilo.PUBLICO : p.getNivelSigilo();
                if (sigilo.exigeCredencial()) sig++;
            }
        }
        int pend = pendencias != null ? pendencias.size() : 0;
        int ev = proximosEventos != null ? proximosEventos.size() : 0;
        return new CidadaoPainelBadgesDto(total, pend, ev, sig);
    }

    private List<CidadaoWidgetDto> widgets(List<Processo> all, List<CidadaoPendenciaDto> pendencias) {
        List<CidadaoWidgetDto> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        ObjectNode prazos = mapper.createObjectNode();
        ObjectNode comunicacoes = mapper.createObjectNode();
        int emDia = 0;
        int vencendo = 0;
        int vencido = 0;
        int citacoes = 0;
        int intimacoes = 0;
        int pendentesCiencia = 0;
        int editais = 0;
        if (pendencias != null) {
            for (CidadaoPendenciaDto p : pendencias) {
                if (p == null) continue;
                if ("PRAZO".equalsIgnoreCase(p.tipo())) {
                    if (p.quando() == null) continue;
                    long dias = ChronoUnit.DAYS.between(now, p.quando());
                    if (dias < 0) vencido++;
                    else if (dias <= 3) vencendo++;
                    else emDia++;
                    continue;
                }
                if (p.tipo() != null && p.tipo().contains("JUDICIAL")) {
                    String titulo = p.titulo() == null ? "" : p.titulo().toLowerCase(Locale.ROOT);
                    String resumo = p.resumo() == null ? "" : p.resumo().toLowerCase(Locale.ROOT);
                    if (titulo.contains("cita")) {
                        citacoes++;
                    } else if (titulo.contains("intima")) {
                        intimacoes++;
                    }
                    if (resumo.contains("aguardando ciência") || resumo.contains("entrega confirmada") || resumo.contains("presunção")) {
                        pendentesCiencia++;
                    }
                    if (resumo.contains("edital")) {
                        editais++;
                    }
                }
            }
        }
        prazos.put("emDia", emDia);
        prazos.put("vencendo", vencendo);
        prazos.put("vencido", vencido);
        out.add(new CidadaoWidgetDto("prazos_status", "donut", prazos));
        comunicacoes.put("citacoes", citacoes);
        comunicacoes.put("intimacoes", intimacoes);
        comunicacoes.put("pendentesCiencia", pendentesCiencia);
        comunicacoes.put("editais", editais);
        out.add(new CidadaoWidgetDto("comunicacoes_judiciais", "summary", comunicacoes));

        Map<String, Integer> areas = new HashMap<>();
        if (all != null) {
            for (Processo p : all) {
                if (p == null) continue;
                String k = areaKey(p.getRamoDireito());
                areas.put(k, areas.getOrDefault(k, 0) + 1);
            }
        }
        ObjectNode areaNode = mapper.createObjectNode();
        for (Map.Entry<String, Integer> kv : areas.entrySet()) {
            areaNode.put(kv.getKey(), kv.getValue());
        }
        out.add(new CidadaoWidgetDto("areas", "pie", areaNode));

        if (all == null || all.isEmpty()) {
            out.add(new CidadaoWidgetDto("movs_12m", "line", emptySeries()));
            out.add(new CidadaoWidgetDto("docs_12m", "bar", emptySeries()));
            return List.copyOf(out);
        }

        List<Long> processoIds = all.stream().map(Processo::getId).filter(Objects::nonNull).toList();
        if (processoIds.isEmpty()) {
            out.add(new CidadaoWidgetDto("movs_12m", "line", emptySeries()));
            out.add(new CidadaoWidgetDto("docs_12m", "bar", emptySeries()));
            return List.copyOf(out);
        }

        LocalDate firstMonth = LocalDate.now().withDayOfMonth(1).minusMonths(11);
        Instant fromInstant = firstMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDateTime fromDate = firstMonth.atStartOfDay();

        Map<String, Long> movSeries = new HashMap<>();
        for (MovimentacaoProcessualRepository.MonthCount r : movRepo.countByMonthForProcessoIds(processoIds, fromInstant)) {
            if (r == null || r.getMonthKey() == null) continue;
            movSeries.put(r.getMonthKey(), r.getCnt());
        }

        Map<String, Long> docSeries = new HashMap<>();
        for (DocumentoProcessualRepository.MonthCount r : docRepo.countCreatedByMonthForProcessoIds(processoIds, fromDate)) {
            if (r == null || r.getMonthKey() == null) continue;
            docSeries.put(r.getMonthKey(), r.getCnt());
        }

        out.add(new CidadaoWidgetDto("movs_12m", "line", fillSeries(firstMonth, movSeries)));
        out.add(new CidadaoWidgetDto("docs_12m", "bar", fillSeries(firstMonth, docSeries)));
        return List.copyOf(out);
    }

    private ObjectNode emptySeries() {
        ObjectNode n = mapper.createObjectNode();
        n.set("labels", mapper.createArrayNode());
        n.set("values", mapper.createArrayNode());
        return n;
    }

    private ObjectNode fillSeries(LocalDate startMonth, Map<String, Long> series) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode labels = mapper.createArrayNode();
        ArrayNode values = mapper.createArrayNode();
        LocalDate m = startMonth;
        for (int i = 0; i < 12; i++) {
            String k = String.format(Locale.ROOT, "%04d-%02d", m.getYear(), m.getMonthValue());
            labels.add(k);
            values.add(series.getOrDefault(k, 0L));
            m = m.plusMonths(1);
        }
        n.set("labels", labels);
        n.set("values", values);
        return n;
    }

    private static String areaKey(RamoDireito ramo) {
        if (ramo == null) return "outros";
        String r = ramo.name().toLowerCase(Locale.ROOT);
        if (r.contains("penal") || r.contains("criminal")) return "criminal";
        if (r.contains("trabal")) return "trabalhista";
        if (r.contains("trib")) return "tributario";
        if (r.contains("eleit")) return "eleitoral";
        if (r.contains("milit")) return "militar";
        if (r.contains("agr")) return "agrario";
        if (r.contains("prev")) return "previdenciario";
        return "civel";
    }


    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception ex) {
            return o instanceof List ? "[]" : "{}";
        }
    }
}
