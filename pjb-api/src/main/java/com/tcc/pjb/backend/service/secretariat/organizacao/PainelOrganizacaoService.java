package com.tcc.pjb.backend.service.secretariat.organizacao;

import com.tcc.pjb.backend.model.dto.organizacao.FiltroOrganizacaoRequest;
import com.tcc.pjb.backend.model.dto.organizacao.GrupoProcessualDto;
import com.tcc.pjb.backend.model.dto.organizacao.ProcessoResumoDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PainelOrganizacaoService {

    private static final int PAGINA_MAX = 500;

    private final ProcessoRepository processoRepository;
    private final AudienciaRepository audienciaRepository;

    public PainelOrganizacaoService(ProcessoRepository processoRepository,
                                     AudienciaRepository audienciaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
    }

    @Transactional(readOnly = true)
    public List<GrupoProcessualDto> organizarPorFiltro(FiltroOrganizacaoRequest filtro) {
        Page<Processo> pagina = processoRepository.findParaPainelOrganizacao(
                filtro.vara(), filtro.comarca(), filtro.uf(),
                filtro.rito(), filtro.faseAtual(), filtro.statusProcesso(),
                PageRequest.of(0, PAGINA_MAX)
        );

        Function<Processo, String> chaveGrupo = resolverChaveGrupo(filtro.agruparPorEfetivo());

        Map<String, List<Processo>> grupos = pagina.getContent().stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            String chave = chaveGrupo.apply(p);
                            return chave != null ? chave : "—";
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);
        long[] todosIds = pagina.getContent().stream().mapToLong(Processo::getId).toArray();

        Map<Long, Long> audienciasHoje = audienciaRepository
                .findCalendarRowsByProcessoIds(todosIds, inicioHoje, fimHoje)
                .stream()
                .collect(Collectors.groupingBy(row -> ((Number) row[1]).longValue(), Collectors.counting()));

        return grupos.entrySet().stream().map(entry -> {
            List<Processo> lista = entry.getValue();
            long ativos = lista.stream()
                    .filter(p -> p.getStatusProcesso() != null && p.getStatusProcesso() != StatusProcesso.ARQUIVADO)
                    .count();
            long comAudienciaHoje = lista.stream()
                    .filter(p -> audienciasHoje.containsKey(p.getId()))
                    .count();
            List<ProcessoResumoDto> resumos = lista.stream()
                    .map(ProcessoResumoDto::de)
                    .toList();
            return new GrupoProcessualDto(
                    entry.getKey(),
                    entry.getKey(),
                    lista.size(),
                    ativos,
                    comAudienciaHoje,
                    0L,
                    resumos
            );
        }).toList();
    }

    private Function<Processo, String> resolverChaveGrupo(String agruparPor) {
        return switch (agruparPor.toLowerCase()) {
            case "rito"         -> p -> p.getRito() != null ? p.getRito().name() : null;
            case "fase"         -> p -> p.getFaseAtual() != null ? p.getFaseAtual().name() : null;
            case "status"       -> p -> p.getStatusProcesso() != null ? p.getStatusProcesso().name() : null;
            case "comarca"      -> Processo::getComarca;
            case "ramo"         -> p -> p.getRamoDireito() != null ? p.getRamoDireito().name() : null;
            default             -> Processo::getVara;
        };
    }
}
