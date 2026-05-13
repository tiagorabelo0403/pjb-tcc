package com.tcc.pjb.backend.service.cidadao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoAudienciaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoAudienciasResponse;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class CidadaoAudienciasService {

    private final CurrentUserService currentUser;
    private final ProcessoRepository processoRepository;
    private final AudienciaRepository audienciaRepository;

    public CidadaoAudienciasService(CurrentUserService currentUser,
                                   ProcessoRepository processoRepository,
                                   AudienciaRepository audienciaRepository) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
    }

    public CidadaoAudienciasResponse listar(LocalDate from, LocalDate to) {
        Usuario u = currentUser.getRequired();
        String cpf = u.getCpf();
        if (cpf == null || cpf.isBlank()) {
            return new CidadaoAudienciasResponse(from, to, 0, List.of(), links());
        }

        List<Processo> processos = processoRepository.findAllByPartesCpf(cpf);
        if (processos.isEmpty()) {
            return new CidadaoAudienciasResponse(from, to, 0, List.of(), links());
        }

        long[] ids = processos.stream().map(Processo::getId).filter(Objects::nonNull).mapToLong(Long::longValue).toArray();
        Map<Long, Processo> processoById = processos.stream().filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Processo::getId, p -> p, (a, b) -> a));

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Audiencia> audiencias = audienciaRepository.findUpcomingByProcessoIds(ids, fromDt, toDt);
        if (audiencias.isEmpty()) {
            return new CidadaoAudienciasResponse(from, to, 0, List.of(), links());
        }

        Map<LocalDate, List<CidadaoAudienciaDto>> grouped = new TreeMap<>();
        for (Audiencia a : audiencias) {
            Processo p = a.getProcesso() != null && a.getProcesso().getId() != null
                    ? processoById.get(a.getProcesso().getId())
                    : null;

            LocalDate dia = a.getDataHora().toLocalDate();
            grouped.computeIfAbsent(dia, k -> new ArrayList<>()).add(new CidadaoAudienciaDto(
                    a.getId(),
                    p != null ? p.getId() : null,
                    p != null ? p.getNumeroUnificado() : null,
                    a.getTipo() != null ? a.getTipo().name() : null,
                    a.getModalidade() != null ? a.getModalidade().name() : null,
                    a.getStatus() != null ? a.getStatus().name() : null,
                    a.getDataHora(),
                    a.getDuracaoMin(),
                    a.getLocal(),
                    a.getLinkVideo(),
                    a.getPauta()
            ));
        }

        List<CidadaoAudienciasResponse.DiaAudiencias> dias = grouped.entrySet().stream()
                .map(e -> new CidadaoAudienciasResponse.DiaAudiencias(e.getKey(), List.copyOf(e.getValue())))
                .toList();

        return new CidadaoAudienciasResponse(from, to, audiencias.size(), dias, links());
    }

    private static AreaLinks links() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }
}
