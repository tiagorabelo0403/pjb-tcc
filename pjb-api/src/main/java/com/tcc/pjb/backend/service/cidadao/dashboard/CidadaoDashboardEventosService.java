package com.tcc.pjb.backend.service.cidadao.dashboard;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProximoEventoDto;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoCardMapper;
import com.tcc.pjb.backend.service.ui.UiHintService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CidadaoDashboardEventosService {

    private final AudienciaRepository audienciaRepo;
    private final JulgamentoColegiadoRepository julgamentoRepo;
    private final UiHintService ui;

    public CidadaoDashboardEventosService(AudienciaRepository audienciaRepo,
                                          JulgamentoColegiadoRepository julgamentoRepo,
                                          UiHintService ui) {
        this.audienciaRepo = Objects.requireNonNull(audienciaRepo);
        this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
        this.ui = Objects.requireNonNull(ui);
    }

    public Map<Long, Audiencia> nextAud(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, Audiencia> m = new HashMap<>();
        for (Audiencia a : audienciaRepo.findNextUpcomingByProcessoIds(idArray, LocalDateTime.now())) {
            if (a == null || a.getProcesso() == null || a.getProcesso().getId() == null) continue;
            m.put(a.getProcesso().getId(), a);
        }
        return m;
    }

    public Map<Long, JulgamentoColegiado> nextJulg(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, JulgamentoColegiado> m = new HashMap<>();
        for (JulgamentoColegiado j : julgamentoRepo.findNextPautaByProcessoIds(idArray, LocalDateTime.now())) {
            if (j == null || j.getProcesso() == null || j.getProcesso().getId() == null) continue;
            m.put(j.getProcesso().getId(), j);
        }
        return m;
    }

    public List<CidadaoProximoEventoDto> proximosEventos(List<Processo> all) {
        if (all == null || all.isEmpty()) return List.of();
        List<Long> ids = all.stream().map(Processo::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return List.of();

        long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Processo> byId = new HashMap<>();
        for (Processo p : all) {
            if (p != null && p.getId() != null) byId.put(p.getId(), p);
        }

        List<CidadaoProximoEventoDto> out = new ArrayList<>();

        for (Audiencia a : audienciaRepo.findNextUpcomingByProcessoIds(idArray, now)) {
            if (a == null || a.getProcesso() == null || a.getProcesso().getId() == null) continue;
            if (a.getDataHora() == null) continue;
            long dias = ChronoUnit.DAYS.between(now, a.getDataHora());
            if (dias > 30) continue;
            Processo p = byId.get(a.getProcesso().getId());
            List<String> tok = p != null ? ui.tokenSetForProcess(p).stream().map(Enum::name).toList() : List.of();
            Long pid = a.getProcesso().getId();
            String detalhe = (a.getTipo() != null ? a.getTipo().name() : "") +
                    (a.getModalidade() != null ? (" • " + a.getModalidade().name()) : "") +
                    (a.getLocal() != null ? (" • " + a.getLocal()) : "");

            out.add(new CidadaoProximoEventoDto(
                    "AUDIENCIA",
                    a.getDataHora(),
                    pid,
                    p != null ? p.getNumeroUnificado() : null,
                    "Audiência marcada",
                    safeShort(detalhe, 220),
                    tok,
                    CidadaoProcessoCardMapper.linksFor(pid)
            ));
        }

        for (JulgamentoColegiado j : julgamentoRepo.findNextPautaByProcessoIds(idArray, now)) {
            if (j == null || j.getProcesso() == null || j.getProcesso().getId() == null) continue;
            if (j.getPautaDataHora() == null) continue;
            long dias = ChronoUnit.DAYS.between(now, j.getPautaDataHora());
            if (dias > 30) continue;

            Processo p = byId.get(j.getProcesso().getId());
            List<String> tok = p != null ? ui.tokenSetForProcess(p).stream().map(Enum::name).toList() : List.of();
            Long pid = j.getProcesso().getId();
            String detalhe = (j.getGrau() != null ? j.getGrau().getLabel() : "") +
                    (j.getTribunalSigla() != null ? (" • " + j.getTribunalSigla()) : "") +
                    (j.getOrgaoJulgador() != null ? (" • " + j.getOrgaoJulgador()) : "");

            out.add(new CidadaoProximoEventoDto(
                    "JULGAMENTO",
                    j.getPautaDataHora(),
                    pid,
                    p != null ? p.getNumeroUnificado() : null,
                    "Julgamento na pauta",
                    safeShort(detalhe, 220),
                    tok,
                    CidadaoProcessoCardMapper.linksFor(pid)
            ));
        }

        out.sort(Comparator.comparing(CidadaoProximoEventoDto::quando));
        return out.size() > 10 ? List.copyOf(out.subList(0, 10)) : List.copyOf(out);
    }

    private static String safeShort(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }
}
