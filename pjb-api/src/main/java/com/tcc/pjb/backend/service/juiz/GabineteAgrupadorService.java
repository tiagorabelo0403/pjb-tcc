package com.tcc.pjb.backend.service.juiz;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.dto.juiz.GabineteAgrupadorItemDto;
import com.tcc.pjb.backend.model.dto.juiz.GabineteAgrupadorResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;

@Service
public class GabineteAgrupadorService {

    private final CurrentUserService currentUserService;
    private final PainelServiceCommons commons;

    public GabineteAgrupadorService(CurrentUserService currentUserService,
                                    PainelServiceCommons commons) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.commons = Objects.requireNonNull(commons);
    }

    @Transactional(readOnly = true)
    public GabineteAgrupadorResponse agrupar() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null || !tipo.isMagistratura()) {
            throw new AccessDeniedPjbException("Acesso restrito à magistratura");
        }

        Instant now = Instant.now();
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 200);
        List<GabineteAgrupadorItemDto> itens = inbox.stream()
                .map(item -> toDto(item, now))
                .sorted(Comparator
                        .comparing((GabineteAgrupadorItemDto dto) -> urgencyOrder(dto.urgencyBand()))
                        .thenComparing(GabineteAgrupadorItemDto::prioridade, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(GabineteAgrupadorItemDto::dueAt, Comparator.nullsLast(Instant::compareTo)))
                .toList();

        LinkedHashMap<String, List<GabineteAgrupadorItemDto>> porUrgencia = new LinkedHashMap<>();
        porUrgencia.put("CRITICA", new ArrayList<>());
        porUrgencia.put("ALTA", new ArrayList<>());
        porUrgencia.put("MEDIA", new ArrayList<>());
        porUrgencia.put("BAIXA", new ArrayList<>());

        LinkedHashMap<String, List<GabineteAgrupadorItemDto>> porMateria = new LinkedHashMap<>();
        List<GabineteAgrupadorItemDto> riscoNulidade = new ArrayList<>();
        List<GabineteAgrupadorItemDto> pendentesCriticos = new ArrayList<>();

        for (GabineteAgrupadorItemDto item : itens) {
            porUrgencia.computeIfAbsent(item.urgencyBand(), ignored -> new ArrayList<>()).add(item);
            porMateria.computeIfAbsent(normalizeMateria(item.ramoDireito()), ignored -> new ArrayList<>()).add(item);
            if (!item.riskFlags().isEmpty()) {
                riscoNulidade.add(item);
            }
            if ("CRITICA".equals(item.urgencyBand()) || ("ALTA".equals(item.urgencyBand()) && !item.riskFlags().isEmpty())) {
                pendentesCriticos.add(item);
            }
        }

        LinkedHashMap<String, Long> contadores = new LinkedHashMap<>();
        contadores.put("total", (long) itens.size());
        contadores.put("critica", (long) porUrgencia.getOrDefault("CRITICA", List.of()).size());
        contadores.put("alta", (long) porUrgencia.getOrDefault("ALTA", List.of()).size());
        contadores.put("comRiscoNulidade", (long) riscoNulidade.size());
        contadores.put("pendentesCriticos", (long) pendentesCriticos.size());
        contadores.put("materias", (long) porMateria.size());

        return new GabineteAgrupadorResponse(
                now,
                usuario.getId(),
                usuario.getNome(),
                itens.size(),
                immutableMapOfLists(porUrgencia),
                immutableMapOfLists(porMateria),
                List.copyOf(riscoNulidade),
                List.copyOf(pendentesCriticos),
                Map.copyOf(contadores)
        );
    }

    private GabineteAgrupadorItemDto toDto(WorkItem item, Instant now) {
        Processo processo = item.getProcesso();
        List<String> riskFlags = detectRiskFlags(item, processo);
        return new GabineteAgrupadorItemDto(
                item.getId(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                item.getTitulo(),
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo != null ? processo.getClasseProcessual() : null,
                urgencyBand(item, now),
                item.getPrioridade(),
                item.getDueAt(),
                item.isBlocking(),
                riskFlags
        );
    }

    private List<String> detectRiskFlags(WorkItem item, Processo processo) {
        ArrayList<String> flags = new ArrayList<>(4);
        if (item.isBlocking() && item.getDueAt() == null) {
            flags.add("BLOQUEIO_SEM_PRAZO");
        }
        if (item.getBaseLegal() == null || item.getBaseLegal().isBlank()) {
            flags.add("BASE_LEGAL_AUSENTE");
        }
        if (processo == null || processo.getClasseProcessual() == null || processo.getClasseProcessual().isBlank()) {
            flags.add("CLASSE_PROCESSUAL_AUSENTE");
        }
        if (processo == null || processo.getRamoDireito() == null) {
            flags.add("RAMO_DIREITO_NAO_CLASSIFICADO");
        }
        NivelSigilo sigilo = processo != null && processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        if (sigilo.exigeCredencial() && (item.getDescricao() == null || item.getDescricao().isBlank())) {
            flags.add("ATO_SIGILOSO_SEM_DESCRICAO_OPERACIONAL");
        }
        if (item.getFaseOrigem() == null) {
            flags.add("FASE_ORIGEM_AUSENTE");
        }
        return List.copyOf(flags);
    }

    private String urgencyBand(WorkItem item, Instant now) {
        if (item.isBlocking()) {
            return "CRITICA";
        }
        Integer prioridade = item.getPrioridade();
        Instant dueAt = item.getDueAt();
        if (dueAt != null) {
            if (!dueAt.isAfter(now.plus(24, ChronoUnit.HOURS))) {
                return "CRITICA";
            }
            if (!dueAt.isAfter(now.plus(3, ChronoUnit.DAYS))) {
                return "ALTA";
            }
            if (!dueAt.isAfter(now.plus(7, ChronoUnit.DAYS))) {
                return "MEDIA";
            }
        }
        if (prioridade != null && prioridade <= 1) {
            return "CRITICA";
        }
        if (prioridade != null && prioridade == 2) {
            return "ALTA";
        }
        if (prioridade != null && prioridade == 3) {
            return "MEDIA";
        }
        return "BAIXA";
    }

    private int urgencyOrder(String urgency) {
        return switch (urgency == null ? "BAIXA" : urgency) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private String normalizeMateria(String ramoDireito) {
        if (ramoDireito == null || ramoDireito.isBlank()) {
            return "NAO_CLASSIFICADO";
        }
        return ramoDireito.trim().toUpperCase(Locale.ROOT);
    }

    private Map<String, List<GabineteAgrupadorItemDto>> immutableMapOfLists(Map<String, List<GabineteAgrupadorItemDto>> source) {
        LinkedHashMap<String, List<GabineteAgrupadorItemDto>> out = new LinkedHashMap<>();
        source.forEach((key, value) -> out.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(out);
    }
}
