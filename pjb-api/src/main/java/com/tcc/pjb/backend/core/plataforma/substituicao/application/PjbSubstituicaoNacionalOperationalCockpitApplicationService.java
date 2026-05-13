package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoHomologacaoProbeSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoMigracaoLoteSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncCursorEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncItemEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoMigracaoLoteEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEventoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoTribunalHomologacaoProbeEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoComunicacaoSyncCursorRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoComunicacaoSyncItemRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoMigracaoLoteRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoEventoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoTribunalHomologacaoProbeRepository;
import com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao.PjbSubstituicaoComunicacaoSyncCursorResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao.PjbSubstituicaoComunicacaoSyncItemResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.homologacao.PjbSubstituicaoHomologacaoProbeResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.migracao.PjbSubstituicaoMigracaoLoteResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitOndaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalOperacionalResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalEvidenciaExportavelResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalReconciliacaoResponse;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.platform.hash.Fingerprint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalOperationalCockpitApplicationService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PjbSubstituicaoNacionalExecutionQueryApplicationService executionQueryApplicationService;
    private final PjbSubstituicaoTribunalHomologacaoProbeRepository probeRepository;
    private final PjbSubstituicaoMigracaoLoteRepository migracaoLoteRepository;
    private final PjbSubstituicaoComunicacaoSyncCursorRepository syncCursorRepository;
    private final PjbSubstituicaoComunicacaoSyncItemRepository syncItemRepository;
    private final PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository;
    private final PjbSubstituicaoNacionalExecucaoRepository execucaoRepository;
    private final CanonicalJsonHasher canonicalJsonHasher;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoNacionalOperationalCockpitApplicationService(
            PjbSubstituicaoNacionalExecutionQueryApplicationService executionQueryApplicationService,
            PjbSubstituicaoTribunalHomologacaoProbeRepository probeRepository,
            PjbSubstituicaoMigracaoLoteRepository migracaoLoteRepository,
            PjbSubstituicaoComunicacaoSyncCursorRepository syncCursorRepository,
            PjbSubstituicaoComunicacaoSyncItemRepository syncItemRepository,
            PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository,
            PjbSubstituicaoNacionalExecucaoRepository execucaoRepository,
            CanonicalJsonHasher canonicalJsonHasher,
            ObjectMapper objectMapper) {
        this.executionQueryApplicationService = Objects.requireNonNull(executionQueryApplicationService);
        this.probeRepository = Objects.requireNonNull(probeRepository);
        this.migracaoLoteRepository = Objects.requireNonNull(migracaoLoteRepository);
        this.syncCursorRepository = Objects.requireNonNull(syncCursorRepository);
        this.syncItemRepository = Objects.requireNonNull(syncItemRepository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.execucaoRepository = Objects.requireNonNull(execucaoRepository);
        this.canonicalJsonHasher = Objects.requireNonNull(canonicalJsonHasher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoNacionalExecucaoOperacionalResponse detalharOperacional(Long execucaoId) {
        PjbSubstituicaoNacionalExecucaoResponse execucao = toExecucaoResponse(executionQueryApplicationService.detalhar(execucaoId));
        List<PjbSubstituicaoHomologacaoProbeResponse> probes = probeRepository.findByExecucaoIdOrderByProbeCodigoAsc(execucaoId).stream()
                .map(this::mapProbe)
                .toList();
        List<PjbSubstituicaoMigracaoLoteResponse> lotes = migracaoLoteRepository.findByExecucaoIdOrderByLoteOrdemAsc(execucaoId).stream()
                .map(this::mapLote)
                .toList();
        List<PjbSubstituicaoComunicacaoSyncCursorResponse> cursores = mapCursores(execucaoId);
        PjbSubstituicaoNacionalOperacionalResumoResponse resumo = new PjbSubstituicaoNacionalOperacionalResumoResponse(
                execucao.eventos().size(),
                probes.size(),
                countProbes(probes, PjbSubstituicaoHomologacaoProbeSituacao.APROVADA),
                countProbes(probes, PjbSubstituicaoHomologacaoProbeSituacao.BLOQUEADA),
                countProbes(probes, PjbSubstituicaoHomologacaoProbeSituacao.SIMULADA),
                lotes.size(),
                countLotes(lotes, List.of(PjbSubstituicaoMigracaoLoteSituacao.CHECKSUM_VALIDADO, PjbSubstituicaoMigracaoLoteSituacao.RECONCILIADO)),
                countLotes(lotes, List.of(PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO)),
                lotes.stream().mapToInt(PjbSubstituicaoMigracaoLoteResponse::divergencias).sum(),
                cursores.size(),
                cursores.stream().mapToInt(cursor -> cursor.itens().size()).sum(),
                cursores.stream().mapToInt(cursor -> cursor.itens().stream().mapToInt(item -> item.situacao() == PjbSubstituicaoComunicacaoSyncSituacao.CORRELACIONADO ? 1 : 0).sum()).sum(),
                cursores.stream().mapToInt(cursor -> cursor.itens().stream().mapToInt(item -> item.situacao() == PjbSubstituicaoComunicacaoSyncSituacao.DEDUPLICADO ? 1 : 0).sum()).sum(),
                cursores.stream().mapToInt(cursor -> cursor.itens().stream().mapToInt(item -> item.reprocessavel() ? 1 : 0).sum()).sum()
        );
        return new PjbSubstituicaoNacionalExecucaoOperacionalResponse(
                execucao,
                resumo,
                probes,
                lotes,
                cursores,
                reconciliarTribunal(execucao.tribunalCodigo())
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoNacionalCockpitResponse cockpit(String tribunalCodigo) {
        String normalizedTribunal = normalizeOptionalTribunal(tribunalCodigo);
        List<PjbSubstituicaoNacionalExecucaoResponse> execucoes = execucaoRepository.list(normalizedTribunal, null, null).stream()
                .map(executionQueryApplicationService::map)
                .map(this::toExecucaoResponse)
                .toList();
        Map<String, List<PjbSubstituicaoNacionalExecucaoResponse>> porTribunal = execucoes.stream()
                .collect(Collectors.groupingBy(PjbSubstituicaoNacionalExecucaoResponse::tribunalCodigo, LinkedHashMap::new, Collectors.toList()));
        List<PjbSubstituicaoNacionalCockpitTribunalResponse> tribunais = porTribunal.values().stream()
                .map(this::mapCockpitTribunal)
                .sorted(Comparator.comparing(PjbSubstituicaoNacionalCockpitTribunalResponse::tribunalCodigo))
                .toList();
        List<PjbSubstituicaoNacionalCockpitOndaResponse> ondas = buildOndas(execucoes);
        int totalCutoversProntos = (int) tribunais.stream().filter(PjbSubstituicaoNacionalCockpitTribunalResponse::cutoverPronto).count();
        int totalRollbacksReversiveis = (int) tribunais.stream().filter(PjbSubstituicaoNacionalCockpitTribunalResponse::rollbackReversivel).count();
        int totalHomologacoesBloqueadas = (int) tribunais.stream().filter(item -> "BLOQUEADA".equals(item.homologacaoStatus())).count();
        int totalMigracoesBloqueadas = (int) tribunais.stream().filter(item -> "BLOQUEADA".equals(item.migracaoStatus())).count();
        int totalComunicacoesReprocessaveis = tribunais.stream().mapToInt(item -> item.bloqueadores().stream().anyMatch(block -> block.startsWith("REPROCESSAVEL=")) ? 1 : 0).sum();
        return new PjbSubstituicaoNacionalCockpitResponse(
                new PjbSubstituicaoNacionalCockpitResumoResponse(
                        tribunais.size(),
                        execucoes.size(),
                        totalCutoversProntos,
                        totalRollbacksReversiveis,
                        totalHomologacoesBloqueadas,
                        totalMigracoesBloqueadas,
                        totalComunicacoesReprocessaveis
                ),
                ondas,
                tribunais,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoTribunalReconciliacaoResponse reconciliarTribunal(String tribunalCodigo) {
        String normalizedTribunal = requireTribunal(tribunalCodigo);
        List<PjbSubstituicaoNacionalExecucaoResponse> execucoes = execucaoRepository.list(normalizedTribunal, null, null).stream()
                .map(executionQueryApplicationService::map)
                .map(this::toExecucaoResponse)
                .toList();
        if (execucoes.isEmpty()) {
            throw new IllegalArgumentException("Tribunal não encontrado para reconciliação de substituição nacional: " + normalizedTribunal);
        }
        List<PjbSubstituicaoNacionalExecucaoEventoEntity> eventos = eventoRepository.findByExecucaoTribunalCodigoOrderByCreatedAtAsc(normalizedTribunal);
        List<PjbSubstituicaoTribunalHomologacaoProbeEntity> probes = probeRepository.findByTribunalCodigoOrderByUpdatedAtDesc(normalizedTribunal);
        List<PjbSubstituicaoMigracaoLoteEntity> lotes = migracaoLoteRepository.findByTribunalCodigoOrderByUpdatedAtDesc(normalizedTribunal);
        List<PjbSubstituicaoComunicacaoSyncCursorEntity> cursores = syncCursorRepository.findByTribunalCodigoOrderByUpdatedAtDesc(normalizedTribunal);
        List<PjbSubstituicaoComunicacaoSyncItemEntity> itens = syncItemRepository.findByCursorExecucaoTribunalCodigoOrderByCreatedAtAsc(normalizedTribunal);
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        int divergencias = lotes.stream().mapToInt(PjbSubstituicaoMigracaoLoteEntity::getDivergencias).sum();
        long reprocessaveis = itens.stream().filter(PjbSubstituicaoComunicacaoSyncItemEntity::isReprocessavel).count();
        if (probes.stream().anyMatch(item -> item.getSituacao() == PjbSubstituicaoHomologacaoProbeSituacao.BLOQUEADA)) {
            bloqueadores.add("HOMOLOGACAO_BLOQUEADA");
        }
        if (lotes.stream().anyMatch(item -> item.getSituacao() == PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO)) {
            bloqueadores.add("MIGRACAO_BLOQUEADA");
        }
        if (divergencias > 0) {
            bloqueadores.add("DIVERGENCIAS_MIGRACAO=" + divergencias);
        }
        if (reprocessaveis > 0) {
            bloqueadores.add("REPROCESSAVEL=" + reprocessaveis);
        }
        if (execucoes.stream().anyMatch(item -> item.situacao() == PjbSubstituicaoExecucaoSituacao.FALHA)) {
            bloqueadores.add("EXECUCAO_COM_FALHA");
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", normalizedTribunal);
        payload.put("tribunalNome", execucoes.get(0).tribunalNome());
        payload.put("geradoEm", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        payload.put("resumo", buildResumoPayload(execucoes, eventos, probes, lotes, cursores, itens, divergencias, (int) reprocessaveis, List.copyOf(bloqueadores)));
        payload.put("execucoes", execucoes.stream().map(this::execucaoPayload).toList());
        payload.put("probes", probes.stream().map(this::probePayload).toList());
        payload.put("lotesMigracao", lotes.stream().map(this::lotePayload).toList());
        payload.put("cursoresComunicacao", cursores.stream().map(this::cursorPayload).toList());
        payload.put("itensComunicacao", itens.stream().map(this::itemPayload).toList());
        Fingerprint fingerprint = canonicalJsonHasher.fingerprint(payload);
        String verdict = bloqueadores.isEmpty() ? "ESTAVEL" : divergencias > 0 || reprocessaveis > 0 ? "ATENCAO" : "CRITICO";
        PjbSubstituicaoTribunalEvidenciaExportavelResponse evidencia = new PjbSubstituicaoTribunalEvidenciaExportavelResponse(
                normalizedTribunal,
                "pjb-substituicao-" + normalizedTribunal.toLowerCase() + "-evidencia.json",
                fingerprint.sha256(),
                fingerprint.jsonBytes(),
                fingerprint.gzipBytes(),
                fingerprint.generatedAt(),
                payload
        );
        return new PjbSubstituicaoTribunalReconciliacaoResponse(
                normalizedTribunal,
                execucoes.get(0).tribunalNome(),
                execucoes.size(),
                eventos.size(),
                probes.size(),
                lotes.size(),
                cursores.size(),
                itens.size(),
                (int) itens.stream().filter(item -> item.getSituacao() == PjbSubstituicaoComunicacaoSyncSituacao.CORRELACIONADO).count(),
                (int) itens.stream().filter(item -> item.getSituacao() == PjbSubstituicaoComunicacaoSyncSituacao.DEDUPLICADO).count(),
                (int) reprocessaveis,
                divergencias,
                verdict,
                List.copyOf(bloqueadores),
                evidencia,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoTribunalEvidenciaExportavelResponse evidenciaExportavelTribunal(String tribunalCodigo) {
        return reconciliarTribunal(tribunalCodigo).evidenciaExportavel();
    }

    private PjbSubstituicaoNacionalCockpitTribunalResponse mapCockpitTribunal(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes) {
        List<PjbSubstituicaoNacionalExecucaoResponse> sorted = execucoes.stream()
                .sorted(Comparator.comparing(PjbSubstituicaoNacionalExecucaoResponse::atualizadoEm).reversed())
                .toList();
        PjbSubstituicaoNacionalExecucaoResponse ultima = sorted.get(0);
        String tribunalCodigo = ultima.tribunalCodigo();
        int reprocessaveis = syncItemRepository.findByCursorExecucaoTribunalCodigoOrderByCreatedAtAsc(tribunalCodigo).stream()
                .mapToInt(item -> item.isReprocessavel() ? 1 : 0)
                .sum();
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(resolveBlockers(sorted));
        if (reprocessaveis > 0) {
            bloqueadores.add("REPROCESSAVEL=" + reprocessaveis);
        }
        return new PjbSubstituicaoNacionalCockpitTribunalResponse(
                ultima.tribunalCodigo(),
                ultima.tribunalNome(),
                Optional.ofNullable(ultima.ondaAlvo()).filter(value -> !value.isBlank()).orElse("SEM_ONDA"),
                ultima.acao(),
                ultima.situacao(),
                ultima.faseAtual(),
                averageGateScore(sorted),
                resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.CONFIRMAR_CUTOVER).map(PjbSubstituicaoNacionalExecucaoResponse::gateAprovado).orElse(false),
                sorted.stream().anyMatch(PjbSubstituicaoNacionalExecucaoResponse::rollbackReversivel),
                renderStatus(resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.HOMOLOGAR_TRIBUNAL)),
                renderStatus(resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.INICIAR_MIGRACAO_SOMBRA)),
                renderStatus(resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.SINCRONIZAR_COMUNICACOES_NACIONAIS)),
                renderStatus(resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.CONFIRMAR_CUTOVER)),
                renderStatus(resolveStatus(sorted, PjbSubstituicaoExecucaoAcao.ACIONAR_ROLLBACK)),
                List.copyOf(bloqueadores),
                ultima.atualizadoEm()
        );
    }

    private List<PjbSubstituicaoNacionalCockpitOndaResponse> buildOndas(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes) {
        Map<String, List<PjbSubstituicaoNacionalExecucaoResponse>> porOnda = execucoes.stream()
                .collect(Collectors.groupingBy(item -> Optional.ofNullable(item.ondaAlvo()).filter(value -> !value.isBlank()).orElse("SEM_ONDA"), LinkedHashMap::new, Collectors.toList()));
        return porOnda.entrySet().stream()
                .map(entry -> {
                    List<PjbSubstituicaoNacionalExecucaoResponse> items = entry.getValue();
                    int cutoversProntos = (int) items.stream().filter(item -> item.acao() == PjbSubstituicaoExecucaoAcao.CONFIRMAR_CUTOVER && item.gateAprovado() && item.situacao() == PjbSubstituicaoExecucaoSituacao.CONCLUIDA).count();
                    int rollbacksReversiveis = (int) items.stream().filter(PjbSubstituicaoNacionalExecucaoResponse::rollbackReversivel).count();
                    int tribunais = (int) items.stream().map(PjbSubstituicaoNacionalExecucaoResponse::tribunalCodigo).distinct().count();
                    return new PjbSubstituicaoNacionalCockpitOndaResponse(
                            entry.getKey(),
                            items.size(),
                            tribunais,
                            averageGateScore(items),
                            cutoversProntos,
                            rollbacksReversiveis
                    );
                })
                .sorted(Comparator.comparing(PjbSubstituicaoNacionalCockpitOndaResponse::ondaCodigo))
                .toList();
    }

    private List<PjbSubstituicaoComunicacaoSyncCursorResponse> mapCursores(Long execucaoId) {
        Map<Long, List<PjbSubstituicaoComunicacaoSyncItemResponse>> itensPorCursor = syncItemRepository.findByCursorExecucaoIdOrderByCreatedAtAsc(execucaoId).stream()
                .collect(Collectors.groupingBy(item -> item.getCursor().getId(), LinkedHashMap::new, Collectors.mapping(this::mapItem, Collectors.toList())));
        return syncCursorRepository.findByExecucaoIdOrderByJanelaInicioAsc(execucaoId).stream()
                .map(cursor -> mapCursor(cursor, itensPorCursor.getOrDefault(cursor.getId(), List.of())))
                .toList();
    }

    private PjbSubstituicaoHomologacaoProbeResponse mapProbe(PjbSubstituicaoTribunalHomologacaoProbeEntity entity) {
        return new PjbSubstituicaoHomologacaoProbeResponse(
                entity.getId(),
                entity.getTribunalCodigo(),
                entity.getProbeCodigo(),
                entity.getConnectorCodigo(),
                entity.getAmbienteCodigo(),
                entity.getSituacao(),
                entity.getGateScore(),
                decodeMap(entity.getEvidenciasJson()),
                decodeMap(entity.getResultadoJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PjbSubstituicaoMigracaoLoteResponse mapLote(PjbSubstituicaoMigracaoLoteEntity entity) {
        return new PjbSubstituicaoMigracaoLoteResponse(
                entity.getId(),
                entity.getTribunalCodigo(),
                entity.getLoteCodigo(),
                entity.getLoteOrdem(),
                entity.getFaixaReferencia(),
                entity.getTotalItens(),
                entity.getSituacao(),
                entity.getChecksumEsperado(),
                entity.getChecksumApurado(),
                entity.getDivergencias(),
                decodeMap(entity.getSnapshotJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PjbSubstituicaoComunicacaoSyncCursorResponse mapCursor(PjbSubstituicaoComunicacaoSyncCursorEntity entity,
                                                                   List<PjbSubstituicaoComunicacaoSyncItemResponse> itens) {
        return new PjbSubstituicaoComunicacaoSyncCursorResponse(
                entity.getId(),
                entity.getTribunalCodigo(),
                entity.getCanalOrigem(),
                entity.getJanelaInicio(),
                entity.getJanelaFim(),
                entity.getCorrelationNamespace(),
                entity.getDedupeNamespace(),
                entity.getSituacao(),
                entity.getTotalRecebido(),
                entity.getTotalDeduplicado(),
                entity.getTotalCorrelacionado(),
                entity.getTotalReprocessavel(),
                decodeMap(entity.getSnapshotJson()),
                itens,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PjbSubstituicaoComunicacaoSyncItemResponse mapItem(PjbSubstituicaoComunicacaoSyncItemEntity entity) {
        LinkedHashMap<String, Object> resultado = new LinkedHashMap<>(decodeMap(entity.getResultadoJson()));
        resultado.putIfAbsent("cursorId", entity.getCursor().getId());
        return new PjbSubstituicaoComunicacaoSyncItemResponse(
                entity.getId(),
                entity.getDedupeHash(),
                entity.getExternalMessageId(),
                entity.getCorrelationKey(),
                entity.getProcessoNumero(),
                entity.getSituacao(),
                entity.isReprocessavel(),
                decodeMap(entity.getPayloadJson()),
                resultado,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PjbSubstituicaoNacionalExecucaoResponse toExecucaoResponse(com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoAggregate aggregate) {
        return new PjbSubstituicaoNacionalExecucaoResponse(
                aggregate.execucaoId(),
                aggregate.tribunalCodigo(),
                aggregate.tribunalNome(),
                aggregate.ramoJustica(),
                aggregate.acao(),
                aggregate.situacao(),
                aggregate.faseAtual(),
                aggregate.modoExecucao(),
                aggregate.dryRun(),
                aggregate.gateAprovado(),
                aggregate.rollbackReversivel(),
                aggregate.gateScore(),
                aggregate.jobId(),
                aggregate.correlationId(),
                aggregate.requestHash(),
                aggregate.requestedBy(),
                aggregate.justificativa(),
                aggregate.ondaAlvo(),
                aggregate.payload(),
                aggregate.resultado(),
                aggregate.eventos().stream().map(evento -> new com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoEventoResponse(
                        evento.eventoId(),
                        evento.codigo(),
                        evento.severidade(),
                        evento.fase(),
                        evento.descricao(),
                        evento.detalhes(),
                        evento.criadoEm())).toList(),
                aggregate.criadoEm(),
                aggregate.iniciadoEm(),
                aggregate.concluidoEm(),
                aggregate.atualizadoEm()
        );
    }

    private int countProbes(Collection<PjbSubstituicaoHomologacaoProbeResponse> probes,
                            PjbSubstituicaoHomologacaoProbeSituacao situacao) {
        return (int) probes.stream().filter(item -> item.situacao() == situacao).count();
    }

    private int countLotes(Collection<PjbSubstituicaoMigracaoLoteResponse> lotes,
                           List<PjbSubstituicaoMigracaoLoteSituacao> situacoes) {
        return (int) lotes.stream().filter(item -> situacoes.contains(item.situacao())).count();
    }

    private List<String> resolveBlockers(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes) {
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        execucoes.stream()
                .sorted(Comparator.comparing(PjbSubstituicaoNacionalExecucaoResponse::atualizadoEm).reversed())
                .limit(5)
                .forEach(execucao -> {
                    Object value = execucao.resultado().get("bloqueadores");
                    if (value instanceof List<?> list) {
                        list.stream().filter(Objects::nonNull).map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).forEach(bloqueadores::add);
                    }
                    execucao.eventos().stream()
                            .filter(evento -> "ERROR".equalsIgnoreCase(evento.severidade()) || "WARN".equalsIgnoreCase(evento.severidade()))
                            .map(evento -> evento.codigo())
                            .filter(Objects::nonNull)
                            .forEach(bloqueadores::add);
                });
        return List.copyOf(bloqueadores);
    }

    private Optional<PjbSubstituicaoNacionalExecucaoResponse> resolveStatus(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes,
                                                                            PjbSubstituicaoExecucaoAcao acao) {
        return execucoes.stream()
                .filter(item -> item.acao() == acao)
                .max(Comparator.comparing(PjbSubstituicaoNacionalExecucaoResponse::atualizadoEm));
    }

    private String renderStatus(Optional<PjbSubstituicaoNacionalExecucaoResponse> execucao) {
        return execucao.map(item -> switch (item.situacao()) {
            case CONCLUIDA -> item.gateAprovado() ? "PRONTA" : "CONCLUIDA_SEM_GATE";
            case BLOQUEADA -> "BLOQUEADA";
            case FALHA -> "FALHA";
            case EM_EXECUCAO -> "EM_EXECUCAO";
            case ENFILEIRADA, RECEBIDA -> "PENDENTE";
        }).orElse("NAO_EXECUTADA");
    }

    private int averageGateScore(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes) {
        return execucoes.isEmpty() ? 0 : (int) Math.round(execucoes.stream().mapToInt(PjbSubstituicaoNacionalExecucaoResponse::gateScore).average().orElse(0));
    }

    private LinkedHashMap<String, Object> buildResumoPayload(List<PjbSubstituicaoNacionalExecucaoResponse> execucoes,
                                                             List<PjbSubstituicaoNacionalExecucaoEventoEntity> eventos,
                                                             List<PjbSubstituicaoTribunalHomologacaoProbeEntity> probes,
                                                             List<PjbSubstituicaoMigracaoLoteEntity> lotes,
                                                             List<PjbSubstituicaoComunicacaoSyncCursorEntity> cursores,
                                                             List<PjbSubstituicaoComunicacaoSyncItemEntity> itens,
                                                             int divergencias,
                                                             int reprocessaveis,
                                                             List<String> bloqueadores) {
        LinkedHashMap<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("totalExecucoes", execucoes.size());
        resumo.put("totalEventos", eventos.size());
        resumo.put("totalProbes", probes.size());
        resumo.put("totalLotes", lotes.size());
        resumo.put("totalCursores", cursores.size());
        resumo.put("totalItensComunicacao", itens.size());
        resumo.put("divergenciasMigracao", divergencias);
        resumo.put("reprocessaveisComunicacao", reprocessaveis);
        resumo.put("bloqueadores", bloqueadores);
        resumo.put("gateMedio", averageGateScore(execucoes));
        return resumo;
    }

    private Map<String, Object> execucaoPayload(PjbSubstituicaoNacionalExecucaoResponse execucao) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("execucaoId", execucao.execucaoId());
        payload.put("acao", execucao.acao().name());
        payload.put("situacao", execucao.situacao().name());
        payload.put("faseAtual", execucao.faseAtual().name());
        payload.put("gateScore", execucao.gateScore());
        payload.put("gateAprovado", execucao.gateAprovado());
        payload.put("rollbackReversivel", execucao.rollbackReversivel());
        payload.put("ondaAlvo", execucao.ondaAlvo());
        payload.put("atualizadoEm", execucao.atualizadoEm());
        payload.put("resultado", execucao.resultado());
        return payload;
    }

    private Map<String, Object> probePayload(PjbSubstituicaoTribunalHomologacaoProbeEntity probe) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("probeCodigo", probe.getProbeCodigo());
        payload.put("connectorCodigo", probe.getConnectorCodigo());
        payload.put("ambienteCodigo", probe.getAmbienteCodigo());
        payload.put("situacao", probe.getSituacao().name());
        payload.put("gateScore", probe.getGateScore());
        payload.put("resultado", decodeMap(probe.getResultadoJson()));
        return payload;
    }

    private Map<String, Object> lotePayload(PjbSubstituicaoMigracaoLoteEntity lote) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("loteCodigo", lote.getLoteCodigo());
        payload.put("loteOrdem", lote.getLoteOrdem());
        payload.put("faixaReferencia", lote.getFaixaReferencia());
        payload.put("situacao", lote.getSituacao().name());
        payload.put("divergencias", lote.getDivergencias());
        payload.put("snapshot", decodeMap(lote.getSnapshotJson()));
        return payload;
    }

    private Map<String, Object> cursorPayload(PjbSubstituicaoComunicacaoSyncCursorEntity cursor) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("cursorId", cursor.getId());
        payload.put("canalOrigem", cursor.getCanalOrigem());
        payload.put("situacao", cursor.getSituacao().name());
        payload.put("janelaInicio", cursor.getJanelaInicio());
        payload.put("janelaFim", cursor.getJanelaFim());
        payload.put("snapshot", decodeMap(cursor.getSnapshotJson()));
        return payload;
    }

    private Map<String, Object> itemPayload(PjbSubstituicaoComunicacaoSyncItemEntity item) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("itemId", item.getId());
        payload.put("correlationKey", item.getCorrelationKey());
        payload.put("processoNumero", item.getProcessoNumero());
        payload.put("situacao", item.getSituacao().name());
        payload.put("reprocessavel", item.isReprocessavel());
        payload.put("resultado", decodeMap(item.getResultadoJson()));
        return payload;
    }

    private Map<String, Object> decodeMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of("raw", json);
        }
    }

    private String normalizeOptionalTribunal(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            return null;
        }
        return tribunalCodigo.trim().toUpperCase();
    }

    private String requireTribunal(String tribunalCodigo) {
        String normalized = normalizeOptionalTribunal(tribunalCodigo);
        if (normalized == null) {
            throw new IllegalArgumentException("tribunalCodigo é obrigatório para reconciliação.");
        }
        return normalized;
    }
}
