package com.tcc.pjb.backend.service.pericia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.pericia.PeritoDisponibilidadeRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoDisponibilidadeResponse;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioAuditView;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoDisponibilidade;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.entity.pericia.PeritoSorteioAudit;
import com.tcc.pjb.backend.model.repository.PeritoDisponibilidadeRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.PeritoSorteioAuditRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;

@Service
public class PeritoDisponibilidadeService {

    private final PeritoDisponibilidadeRepository disponibilidadeRepository;
    private final PeritoNomeacaoRepository nomeacaoRepository;
    private final PeritoSorteioAuditRepository sorteioAuditRepository;
    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final ComarcaResolutionService comarcaResolutionService;

    public PeritoDisponibilidadeService(PeritoDisponibilidadeRepository disponibilidadeRepository,
                                        PeritoNomeacaoRepository nomeacaoRepository,
                                        PeritoSorteioAuditRepository sorteioAuditRepository,
                                        ProcessoRepository processoRepository,
                                        CurrentUserService currentUserService,
                                        ObjectMapper objectMapper,
                                        ComarcaResolutionService comarcaResolutionService) {
        this.disponibilidadeRepository = Objects.requireNonNull(disponibilidadeRepository);
        this.nomeacaoRepository = Objects.requireNonNull(nomeacaoRepository);
        this.sorteioAuditRepository = Objects.requireNonNull(sorteioAuditRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.comarcaResolutionService = Objects.requireNonNull(comarcaResolutionService);
    }

    @Transactional
    public PeritoDisponibilidadeResponse registrar(PeritoDisponibilidadeRequest request) {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.isPerito()) {
            throw new AccessDeniedPjbException("Apenas peritos podem registrar disponibilidade");
        }
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("dataFim não pode ser anterior à dataInicio");
        }
        String comarca = normalizeNullable(request.comarca(), usuario.getComarca());
        PeritoDisponibilidade entity = PeritoDisponibilidade.builder()
                .perito(usuario)
                .especialidadeCodigo(normalize(request.especialidadeCodigo()))
                .comarca(comarca)
                .comarcaEntidade(resolverComarca(comarca))
                .dataInicio(request.dataInicio())
                .dataFim(request.dataFim())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .disponivel(request.disponivel())
                .motivoIndisponibilidade(trimToNull(request.motivoIndisponibilidade()))
                .build();
        return toResponse(disponibilidadeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PeritoDisponibilidadeResponse> listarMinhas() {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.isPerito()) {
            throw new AccessDeniedPjbException("Apenas peritos podem consultar disponibilidade própria");
        }
        return disponibilidadeRepository.findTop100ByPerito_IdOrderByDataInicioDesc(usuario.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PeritoSorteioResponse sortear(PeritoSorteioRequest request) {
        Usuario actor = currentUserService.getRequired();
        TipoUsuario tipo = actor.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isMagistratura() || tipo.isAdmin() || tipo.isServidorJudiciario());
        if (!permitido) {
            throw new AccessDeniedPjbException("Apenas magistratura, administração ou secretaria podem sortear perito");
        }
        List<PeritoDisponibilidade> candidatos = disponibilidadeRepository.findDisponiveis(
                normalize(request.especialidadeCodigo()),
                trimToNull(request.comarca()),
                request.data() != null ? request.data() : LocalDate.now()
        );
        if (candidatos.isEmpty()) {
            throw new IllegalStateException("Nenhum perito disponível encontrado para os filtros informados");
        }
        List<PeritoSorteioCandidate> avaliados = candidatos.stream()
                .map(this::avaliarInterno)
                .sorted(Comparator.comparingDouble(PeritoSorteioCandidate::score).reversed()
                        .thenComparing(PeritoSorteioCandidate::nomeacoesAtivas)
                        .thenComparing(PeritoSorteioCandidate::peritoId))
                .toList();
        PeritoSorteioCandidate vencedor = avaliados.get(0);
        registrarAuditoria(actor, request, vencedor, avaliados.size());
        return vencedor.response();
    }

    @Transactional(readOnly = true)
    public List<PeritoSorteioAuditView> listarHistoricoPorProcesso(Long processoId) {
        Usuario actor = currentUserService.getRequired();
        TipoUsuario tipo = actor.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isMagistratura() || tipo.isAdmin() || tipo.isServidorJudiciario());
        if (!permitido) {
            throw new AccessDeniedPjbException("Apenas magistratura, administração ou secretaria podem consultar histórico de sorteio");
        }
        return sorteioAuditRepository.findTop100ByProcesso_IdOrderByCreatedAtDesc(processoId).stream()
                .map(this::toAuditView)
                .toList();
    }

    private PeritoSorteioCandidate avaliarInterno(PeritoDisponibilidade disponibilidade) {
        return new PeritoSorteioCandidate(disponibilidade, avaliar(disponibilidade));
    }

    private PeritoSorteioResponse avaliar(PeritoDisponibilidade disponibilidade) {
        Usuario perito = disponibilidade.getPerito();
        long nomeacoesAtivas = nomeacaoRepository.countByPerito_IdAndStatusIn(
                perito.getId(),
                EnumSet.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO)
        );
        double especialidade = perito.possuiEspecialidade(disponibilidade.getEspecialidadeCodigo()) ? 45d : 20d;
        double territorial = disponibilidade.getComarca() == null || disponibilidade.getComarca().isBlank() ? 10d : 20d;
        double disponibilidadeBase = disponibilidade.isDisponivel() ? 30d : -100d;
        double carga = Math.max(0d, 25d - Math.min(25d, nomeacoesAtivas * 4d));
        double score = disponibilidadeBase + especialidade + territorial + carga;
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Especialidade compatível: " + disponibilidade.getEspecialidadeCodigo());
        fundamentos.add("Carga ativa do perito: " + nomeacoesAtivas + " nomeações");
        if (disponibilidade.getComarca() != null && !disponibilidade.getComarca().isBlank()) {
            fundamentos.add("Cobertura territorial: " + disponibilidade.getComarca());
        }
        return new PeritoSorteioResponse(
                perito.getId(),
                perito.getNome(),
                disponibilidade.getEspecialidadeCodigo(),
                disponibilidade.getComarca(),
                disponibilidade.getDataInicio(),
                score,
                nomeacoesAtivas,
                List.copyOf(fundamentos)
        );
    }

    private void registrarAuditoria(Usuario actor,
                                    PeritoSorteioRequest request,
                                    PeritoSorteioCandidate vencedor,
                                    int candidatosElegiveis) {
        PeritoSorteioAudit audit = new PeritoSorteioAudit();
        audit.setActor(actor);
        audit.setProcesso(resolveProcesso(request.processoId()));
        audit.setDisponibilidade(vencedor.disponibilidade());
        audit.setPerito(vencedor.disponibilidade().getPerito());
        audit.setEspecialidadeCodigo(vencedor.response().especialidadeCodigo());
        audit.setComarca(vencedor.response().comarca());
        audit.setComarcaEntidade(resolverComarca(vencedor.response().comarca()));
        audit.setDataReferencia(String.valueOf(request.data() != null ? request.data() : LocalDate.now()));
        audit.setScore(vencedor.response().score());
        audit.setNomeacoesAtivas(vencedor.response().nomeacoesAtivas());
        audit.setCandidatosElegiveis(candidatosElegiveis);
        audit.setFundamentosJson(writeList(vencedor.response().fundamentos()));
        audit.setHashIntegridade(Hashes.sha256Hex(String.join("|",
                String.valueOf(actor.getId()),
                String.valueOf(request.processoId()),
                String.valueOf(vencedor.response().peritoId()),
                String.valueOf(vencedor.response().score()),
                String.valueOf(candidatosElegiveis),
                String.valueOf(vencedor.response().fundamentos())
        )));
        sorteioAuditRepository.save(audit);
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findById(processoId).orElse(null);
    }

    private PeritoSorteioAuditView toAuditView(PeritoSorteioAudit audit) {
        return new PeritoSorteioAuditView(
                audit.getId(),
                audit.getProcesso() != null ? audit.getProcesso().getId() : null,
                audit.getActor() != null ? audit.getActor().getId() : null,
                audit.getActor() != null ? audit.getActor().getNome() : null,
                audit.getPerito() != null ? audit.getPerito().getId() : null,
                audit.getPerito() != null ? audit.getPerito().getNome() : null,
                audit.getEspecialidadeCodigo(),
                audit.getComarca(),
                audit.getDataReferencia(),
                audit.getScore() == null ? 0.0d : audit.getScore(),
                audit.getNomeacoesAtivas() == null ? 0L : audit.getNomeacoesAtivas(),
                audit.getCandidatosElegiveis() == null ? 0 : audit.getCandidatosElegiveis(),
                readList(audit.getFundamentosJson()),
                audit.getHashIntegridade(),
                audit.getCreatedAt()
        );
    }

    private PeritoDisponibilidadeResponse toResponse(PeritoDisponibilidade entity) {
        Usuario perito = entity.getPerito();
        return new PeritoDisponibilidadeResponse(
                entity.getId(),
                perito != null ? perito.getId() : null,
                perito != null ? perito.getNome() : null,
                entity.getEspecialidadeCodigo(),
                entity.getComarca(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getHoraInicio(),
                entity.getHoraFim(),
                entity.isDisponivel(),
                entity.getMotivoIndisponibilidade()
        );
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar histórico de sorteio do perito", e);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception e) {
            return List.of();
        }
    }

    private record PeritoSorteioCandidate(PeritoDisponibilidade disponibilidade,
                                          PeritoSorteioResponse response) {
        double score() {
            return response.score();
        }

        long nomeacoesAtivas() {
            return response.nomeacoesAtivas();
        }

        Long peritoId() {
            return response.peritoId();
        }
    }

    private Comarca resolverComarca(String comarca) {
        if (comarca == null || comarca.isBlank()) {
            return null;
        }
        return comarcaResolutionService.resolver(comarca, null).orElse(null);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value, String fallback) {
        String source = value == null || value.isBlank() ? fallback : value;
        return source == null ? null : source.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
