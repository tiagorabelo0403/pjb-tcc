package com.tcc.pjb.backend.service.profile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalAnalyticsResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;

@Service
public class DiligenceOperationalAnalyticsService {

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final DiligenciaOperadorTelemetriaRepository telemetriaRepository;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository;
    private final DiligenciaOperadorJuntadaProcessualRepository juntadaRepository;
    private final DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository;
    private final DiligenceReferenceResolverService referenceResolverService;
    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessEventRepository processEventRepository;
    private final JdbcTemplate jdbcTemplate;

    public DiligenceOperationalAnalyticsService(CurrentUserService currentUserService,
                                                PjbAuthorizationService authorizationService,
                                                DiligenciaOperadorTelemetriaRepository telemetriaRepository,
                                                DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                                DiligenciaOperadorCertidaoRepository certidaoRepository,
                                                DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                                DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository,
                                                DiligenciaOperadorJuntadaProcessualRepository juntadaRepository,
                                                DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository,
                                                DiligenceReferenceResolverService referenceResolverService,
                                                ProcessoRepository processoRepository,
                                                DocumentoProcessualRepository documentoRepository,
                                                ProcessEventRepository processEventRepository,
                                                JdbcTemplate jdbcTemplate) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.telemetriaRepository = Objects.requireNonNull(telemetriaRepository);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.formalizacaoRepository = Objects.requireNonNull(formalizacaoRepository);
        this.juntadaRepository = Objects.requireNonNull(juntadaRepository);
        this.anexacaoRepository = Objects.requireNonNull(anexacaoRepository);
        this.referenceResolverService = Objects.requireNonNull(referenceResolverService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processEventRepository = Objects.requireNonNull(processEventRepository);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Transactional(readOnly = true)
    public DiligenceOperationalAnalyticsResponse analytics(TelemetriaOperacionalCanal canal,
                                                           String diligenceReference) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        List<DiligenciaOperadorCheckpointEvento> checkpoints = checkpointRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorCertidao> certidoes = certidaoRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorEncerramento> encerramentos = encerramentoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes = formalizacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorJuntadaProcessual> juntadas = juntadaRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorAnexacaoInstitucional> anexacoes = anexacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        Long processoId = resolveProcessoId(checkpoints, certidoes, formalizacoes, juntadas, anexacoes, canal, normalizedReference);
        String processoNumero = resolveProcessoNumero(checkpoints, certidoes, formalizacoes, juntadas, anexacoes, canal, normalizedReference);
        if (processoId != null) {
            Processo processo = processoRepository.findById(processoId).orElse(null);
            if (processo != null) {
                authorizationService.requireReadProcesso(processo);
                processoNumero = processo.getNumeroProcesso() != null ? processo.getNumeroProcesso() : processoNumero;
            }
        }

        long telemetrias = telemetriaRepository.countByOperatorUserIdAndCanal(actor.getId(), canal);
        long checkpointsCount = checkpointRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long certidoesCount = certidaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long encerramentosCount = encerramentoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long formalizacoesCount = formalizacaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long juntadasCount = juntadaRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long anexacoesCount = anexacaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference);
        long documentos = processoId != null ? documentoRepository.countByProcesso_Id(processoId) : 0L;
        long eventos = processoId != null ? processEventRepository.countByProcessoId(processoId) : 0L;
        Instant firstAt = minInstant(checkpoints, certidoes, encerramentos, formalizacoes, juntadas, anexacoes);
        Instant lastAt = maxInstant(checkpoints, certidoes, encerramentos, formalizacoes, juntadas, anexacoes);
        Long cycleSeconds = andLast(firstAt, lastAt) ? Duration.between(firstAt, lastAt).getSeconds() : null;
        double hitRateCheckpoint = checkpointsCount == 0 ? 0d : checkpoints.stream().filter(DiligenciaOperadorCheckpointEvento::isInsideGeofence).count() / (double) checkpoints.size();
        double annexationRate = juntadasCount == 0 ? 0d : anexacoesCount / (double) juntadasCount;
        var processScope = new DiligenceOperationalAnalyticsResponse.ProcessScope(
                processoId,
                processoNumero,
                currentStage(anexacoesCount, juntadasCount, formalizacoesCount, encerramentosCount, certidoesCount, checkpointsCount),
                telemetrias,
                checkpointsCount,
                certidoesCount,
                encerramentosCount,
                formalizacoesCount,
                juntadasCount,
                anexacoesCount,
                documentos,
                eventos,
                checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getTentativaSequencia).filter(Objects::nonNull).mapToLong(Integer::longValue).max().orElse(0L),
                firstAt,
                lastAt,
                cycleSeconds,
                scale(hitRateCheckpoint),
                scale(annexationRate),
                processHighlights(checkpointsCount, certidoesCount, juntadasCount, anexacoesCount, documentos, eventos)
        );

        Instant recentCut = Instant.now().minus(Duration.ofDays(30));
        long operatorTelemetrias = telemetriaRepository.countByOperatorUserIdAndCanalAndCapturadoEmAfter(actor.getId(), canal, recentCut);
        long operatorCheckpoints = checkpointRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        long operatorCertidoes = certidaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        long operatorEncerramentos = encerramentoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        long operatorFormalizacoes = formalizacaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        long operatorJuntadas = juntadaRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        long operatorAnexacoes = anexacaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(actor.getId(), canal, recentCut);
        Instant operatorLastAt = lastNonNull(
                telemetriaRepository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(actor.getId(), canal).map(DiligenciaOperadorTelemetria::getCapturadoEm).orElse(null),
                checkpointRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, normalizedReference).map(DiligenciaOperadorCheckpointEvento::getOccurredAt).orElse(null),
                certidaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference).map(DiligenciaOperadorCertidao::getCreatedAt).orElse(null),
                anexacoes.isEmpty() ? null : anexacoes.getFirst().getCreatedAt());
        var operatorScope = new DiligenceOperationalAnalyticsResponse.OperatorScope(
                actor.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                unitLabel(actor),
                operatorTelemetrias,
                operatorCheckpoints,
                operatorCertidoes,
                operatorEncerramentos,
                operatorFormalizacoes,
                operatorJuntadas,
                operatorAnexacoes,
                operatorEncerramentos,
                scale(operatorJuntadas == 0 ? 0d : operatorAnexacoes / (double) operatorJuntadas),
                operatorLastAt,
                operatorHighlights(operatorCheckpoints, operatorEncerramentos, operatorJuntadas, operatorAnexacoes)
        );

        var unitScope = new DiligenceOperationalAnalyticsResponse.UnitScope(
                unitLabel(actor),
                countByUnit("tb_diligencia_operador_telemetria", "capturado_em", canal, actor),
                countByUnit("tb_diligencia_operador_checkpoint", "created_at", canal, actor),
                countByUnit("tb_diligencia_operador_certidao", "created_at", canal, actor),
                countByUnit("tb_diligencia_operador_encerramento", "created_at", canal, actor),
                countByUnit("tb_diligencia_operador_formalizacao_processual", "created_at", canal, actor),
                countByUnit("tb_diligencia_operador_juntada_processual", "created_at", canal, actor),
                countByUnit("tb_diligencia_operador_anexacao_institucional", "created_at", canal, actor),
                countDistinctOperatorsByUnit(canal, actor),
                countDistinctProcessosByUnit(canal, actor),
                latestByUnit(canal, actor),
                unitHighlights(canal, actor)
        );

        return new DiligenceOperationalAnalyticsResponse(canal.name(), normalizedReference, processScope, operatorScope, unitScope);
    }

    private Long resolveProcessoId(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                   List<DiligenciaOperadorCertidao> certidoes,
                                   List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                                   List<DiligenciaOperadorJuntadaProcessual> juntadas,
                                   List<DiligenciaOperadorAnexacaoInstitucional> anexacoes,
                                   TelemetriaOperacionalCanal canal,
                                   String diligenceReference) {
        return firstNonNull(
                checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null),
                certidoes.stream().map(DiligenciaOperadorCertidao::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null),
                formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null),
                juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null),
                anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null),
                referenceResolverService.resolve(canal, diligenceReference).map(DiligenceReferenceResolverService.ResolvedDiligenceReference::processoId).orElse(null)
        );
    }

    private String resolveProcessoNumero(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                         List<DiligenciaOperadorCertidao> certidoes,
                                         List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                                         List<DiligenciaOperadorJuntadaProcessual> juntadas,
                                         List<DiligenciaOperadorAnexacaoInstitucional> anexacoes,
                                         TelemetriaOperacionalCanal canal,
                                         String diligenceReference) {
        return firstNonBlank(
                checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null),
                certidoes.stream().map(DiligenciaOperadorCertidao::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null),
                formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null),
                juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null),
                anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null),
                referenceResolverService.resolve(canal, diligenceReference).map(DiligenceReferenceResolverService.ResolvedDiligenceReference::processoNumero).orElse(null)
        );
    }

    private String currentStage(long anexacoes,
                                long juntadas,
                                long formalizacoes,
                                long encerramentos,
                                long certidoes,
                                long checkpoints) {
        if (anexacoes > 0) {
            return "ANEXACAO_INSTITUCIONAL";
        }
        if (juntadas > 0) {
            return "JUNTADA_AUTOMATICA";
        }
        if (formalizacoes > 0) {
            return "FORMALIZACAO_PROCESSUAL";
        }
        if (encerramentos > 0) {
            return "ENCERRAMENTO_OPERACIONAL";
        }
        if (certidoes > 0) {
            return "CERTIDAO_OPERACIONAL";
        }
        if (checkpoints > 0) {
            return "CHECKPOINT_OPERACIONAL";
        }
        return "TRILHA_INICIAL";
    }

    private List<String> processHighlights(long checkpoints,
                                           long certidoes,
                                           long juntadas,
                                           long anexacoes,
                                           long documentos,
                                           long eventos) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (checkpoints > 1) {
            out.add("MULTIPLAS_TENTATIVAS_REGISTRADAS");
        }
        if (certidoes > 0) {
            out.add("CERTIFICACAO_AUTOMATICA_ATIVA");
        }
        if (juntadas > 0) {
            out.add("JUNTADA_INSTITUCIONAL_CONCLUIDA");
        }
        if (anexacoes > 0) {
            out.add("ANEXACAO_EXTERNA_CONFIRMADA");
        }
        if (documentos > 3) {
            out.add("PASTA_DIGITAL_ESTRUTURADA");
        }
        if (eventos > 5) {
            out.add("EVENT_STORE_RICO");
        }
        return List.copyOf(out);
    }

    private List<String> operatorHighlights(long checkpoints,
                                            long encerramentos,
                                            long juntadas,
                                            long anexacoes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (checkpoints > 0) {
            out.add("CAPTURA_CAMPO_ATIVA");
        }
        if (encerramentos > 0) {
            out.add("CICLOS_OPERACIONAIS_FECHADOS");
        }
        if (juntadas > 0) {
            out.add("FORMALIZACAO_EM_PRODUCAO");
        }
        if (anexacoes > 0) {
            out.add("MALHA_EXTERNA_OPERANTE");
        }
        return List.copyOf(out);
    }

    private List<String> unitHighlights(TelemetriaOperacionalCanal canal,
                                        Usuario actor) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (countByUnit("tb_diligencia_operador_checkpoint", "created_at", canal, actor) > 0) {
            out.add("UNIDADE_COM_TRILHA_GEOFENCE");
        }
        if (countByUnit("tb_diligencia_operador_anexacao_institucional", "created_at", canal, actor) > 0) {
            out.add("UNIDADE_COM_ANEXACAO_INSTITUCIONAL");
        }
        if (countDistinctOperatorsByUnit(canal, actor) > 1) {
            out.add("OPERACAO_COLABORATIVA");
        }
        return List.copyOf(out);
    }

    private long countByUnit(String table,
                             String instantColumn,
                             TelemetriaOperacionalCanal canal,
                             Usuario actor) {
        String sql = "select count(*) from " + table + " t join tb_usuario u on u.id = t.operator_user_id where t.canal = ? and coalesce(u.uf, '') = ? and coalesce(u.comarca, '') = ? and u.tipo_usuario = ? and t." + instantColumn + " >= ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class,
                canal.name(),
                safe(actor.getUf()),
                safe(actor.getComarca()),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(30))));
        return value == null ? 0L : value;
    }

    private long countDistinctOperatorsByUnit(TelemetriaOperacionalCanal canal,
                                              Usuario actor) {
        String sql = "select count(distinct t.operator_user_id) from tb_diligencia_operador_checkpoint t join tb_usuario u on u.id = t.operator_user_id where t.canal = ? and coalesce(u.uf, '') = ? and coalesce(u.comarca, '') = ? and u.tipo_usuario = ? and t.created_at >= ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class,
                canal.name(),
                safe(actor.getUf()),
                safe(actor.getComarca()),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(30))));
        return value == null ? 0L : value;
    }

    private long countDistinctProcessosByUnit(TelemetriaOperacionalCanal canal,
                                              Usuario actor) {
        String sql = "select count(distinct t.processo_id) from tb_diligencia_operador_checkpoint t join tb_usuario u on u.id = t.operator_user_id where t.canal = ? and coalesce(u.uf, '') = ? and coalesce(u.comarca, '') = ? and u.tipo_usuario = ? and t.created_at >= ? and t.processo_id is not null";
        Long value = jdbcTemplate.queryForObject(sql, Long.class,
                canal.name(),
                safe(actor.getUf()),
                safe(actor.getComarca()),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(30))));
        return value == null ? 0L : value;
    }

    private Instant latestByUnit(TelemetriaOperacionalCanal canal,
                                 Usuario actor) {
        String sql = "select max(t.created_at) from tb_diligencia_operador_anexacao_institucional t join tb_usuario u on u.id = t.operator_user_id where t.canal = ? and coalesce(u.uf, '') = ? and coalesce(u.comarca, '') = ? and u.tipo_usuario = ?";
        java.sql.Timestamp value = jdbcTemplate.queryForObject(sql, java.sql.Timestamp.class,
                canal.name(),
                safe(actor.getUf()),
                safe(actor.getComarca()),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil());
        return value == null ? null : value.toInstant();
    }

    private Instant minInstant(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                               List<DiligenciaOperadorCertidao> certidoes,
                               List<DiligenciaOperadorEncerramento> encerramentos,
                               List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                               List<DiligenciaOperadorJuntadaProcessual> juntadas,
                               List<DiligenciaOperadorAnexacaoInstitucional> anexacoes) {
        List<Instant> instants = new ArrayList<>();
        checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getOccurredAt).filter(Objects::nonNull).forEach(instants::add);
        certidoes.stream().map(DiligenciaOperadorCertidao::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        encerramentos.stream().map(DiligenciaOperadorEncerramento::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        return instants.stream().min(Instant::compareTo).orElse(null);
    }

    private Instant maxInstant(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                               List<DiligenciaOperadorCertidao> certidoes,
                               List<DiligenciaOperadorEncerramento> encerramentos,
                               List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                               List<DiligenciaOperadorJuntadaProcessual> juntadas,
                               List<DiligenciaOperadorAnexacaoInstitucional> anexacoes) {
        List<Instant> instants = new ArrayList<>();
        checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getOccurredAt).filter(Objects::nonNull).forEach(instants::add);
        certidoes.stream().map(DiligenciaOperadorCertidao::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        encerramentos.stream().map(DiligenciaOperadorEncerramento::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getCreatedAt).filter(Objects::nonNull).forEach(instants::add);
        return instants.stream().max(Instant::compareTo).orElse(null);
    }

    private boolean andLast(Instant firstAt,
                            Instant lastAt) {
        return firstAt != null && lastAt != null && !lastAt.isBefore(firstAt);
    }

    private Instant lastNonNull(Instant... values) {
        Instant max = null;
        for (Instant value : values) {
            if (value != null && (max == null || value.isAfter(max))) {
                max = value;
            }
        }
        return max;
    }

    private String unitLabel(Usuario actor) {
        return (safe(actor.getComarca()).isBlank() ? "CENTRAL" : actor.getComarca().trim()) + "/" + (safe(actor.getUf()).isBlank() ? "BR" : actor.getUf().trim().toUpperCase());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Double scale(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
