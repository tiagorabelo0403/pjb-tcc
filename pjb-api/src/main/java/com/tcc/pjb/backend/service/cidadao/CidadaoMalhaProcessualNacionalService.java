package com.tcc.pjb.backend.service.cidadao;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.Links;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection;
import com.tcc.pjb.backend.model.entity.cidadao.ProcessoVisibilidadePessoalOverride;
import com.tcc.pjb.backend.model.entity.enums.GrauConfiancaVinculoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.identity.ProcessoVinculoNacional;
import com.tcc.pjb.backend.model.entity.identity.ProntuarioNacionalEntrada;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoVinculoNacionalRepository;
import com.tcc.pjb.backend.model.repository.ProntuarioNacionalEntradaRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.repository.cidadao.CidadaoProcessoNacionalProjectionRepository;
import com.tcc.pjb.backend.repository.cidadao.ProcessoVisibilidadePessoalOverrideRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyReport;
import com.tcc.pjb.backend.service.processual.postarchive.tombstone.ProcessoTombstonePolicyEngine;
import com.tcc.pjb.backend.service.processual.postarchive.tombstone.ProcessoTombstonePolicyReport;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatProcessoVisibilidadePessoalService;
import com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService.PersonalProcessAccessEnvelope;
import com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CidadaoMalhaProcessualNacionalService {

    private static final int MAX_REFRESH_CARDS = 400;

    private final CurrentUserService currentUserService;
    private final DocumentoNacionalValidator documentoValidator;
    private final IdentidadeJuridicaNacionalService identidadeService;
    private final ProntuarioNacionalEntradaRepository prontuarioRepository;
    private final ProcessoVinculoNacionalRepository vinculoRepository;
    private final CidadaoProcessoNacionalProjectionRepository projectionRepository;
    private final ProcessoVisibilidadePessoalOverrideRepository overrideRepository;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final com.tcc.pjb.backend.model.repository.AudienciaRepository audienciaRepository;
    private final JulgamentoColegiadoRepository julgamentoRepository;
    private final CidadaoProcessoCardMapper cardMapper;
    private final PersonalProcessAccessGuardService personalProcessAccessGuardService;
    private final ProcessoTombstonePolicyEngine processoTombstonePolicyEngine;

    public CidadaoMalhaProcessualNacionalService(CurrentUserService currentUserService,
                                                 DocumentoNacionalValidator documentoValidator,
                                                 IdentidadeJuridicaNacionalService identidadeService,
                                                 ProntuarioNacionalEntradaRepository prontuarioRepository,
                                                 ProcessoVinculoNacionalRepository vinculoRepository,
                                                 CidadaoProcessoNacionalProjectionRepository projectionRepository,
                                                 ProcessoVisibilidadePessoalOverrideRepository overrideRepository,
                                                 ProcessoRepository processoRepository,
                                                 MovimentacaoProcessualRepository movimentacaoRepository,
                                                 DocumentoProcessualRepository documentoRepository,
                                                 com.tcc.pjb.backend.model.repository.AudienciaRepository audienciaRepository,
                                                 JulgamentoColegiadoRepository julgamentoRepository,
                                                 CidadaoProcessoCardMapper cardMapper,
                                                 PersonalProcessAccessGuardService personalProcessAccessGuardService,
                                                 ProcessoTombstonePolicyEngine processoTombstonePolicyEngine) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.identidadeService = Objects.requireNonNull(identidadeService);
        this.prontuarioRepository = Objects.requireNonNull(prontuarioRepository);
        this.vinculoRepository = Objects.requireNonNull(vinculoRepository);
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.overrideRepository = Objects.requireNonNull(overrideRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.julgamentoRepository = Objects.requireNonNull(julgamentoRepository);
        this.cardMapper = Objects.requireNonNull(cardMapper);
        this.personalProcessAccessGuardService = Objects.requireNonNull(personalProcessAccessGuardService);
        this.processoTombstonePolicyEngine = Objects.requireNonNull(processoTombstonePolicyEngine);
    }

    @Transactional
    public Page<CidadaoLinkedProcessView> searchVisibleCurrentUser(Pageable pageable,
                                                                   String numero,
                                                                   String uf,
                                                                   StatusProcesso status) {
        IdentityContext context = refreshContextForCurrentUser("PAINEL_PESSOAL_NACIONAL");
        Page<CidadaoProcessoNacionalProjection> page = projectionRepository.searchVisible(
                context.identidadeId(),
                normalizeNumero(numero),
                normalizeUf(uf),
                status,
                pageable
        );
        return hydrate(page);
    }

    @Transactional
    public List<CidadaoLinkedProcessView> listVisibleCurrentUser(int limit) {
        IdentityContext context = refreshContextForCurrentUser("PAINEL_PESSOAL_NACIONAL_LISTA");
        List<CidadaoProcessoNacionalProjection> rows = projectionRepository.findTop200ByIdentidadeIdAndVisivelPainelPessoalTrueOrderBySortKeyDesc(context.identidadeId());
        int safeLimit = Math.max(1, Math.min(limit, MAX_REFRESH_CARDS));
        if (rows.size() > safeLimit) {
            rows = rows.subList(0, safeLimit);
        }
        return hydrate(rows);
    }

    @Transactional
    public List<CidadaoLinkedProcessView> listVisibleForUser(Usuario usuario, int limit) {
        IdentityContext context = refreshContextForUser(usuario);
        List<CidadaoProcessoNacionalProjection> rows = projectionRepository.findTop200ByIdentidadeIdAndVisivelPainelPessoalTrueOrderBySortKeyDesc(context.identidadeId());
        int safeLimit = Math.max(1, Math.min(limit, MAX_REFRESH_CARDS));
        if (rows.size() > safeLimit) {
            rows = rows.subList(0, safeLimit);
        }
        return hydrate(rows);
    }

    @Transactional
    public boolean isCurrentUserLinkedToProcessoLocal(Long processoId) {
        if (processoId == null) {
            return false;
        }
        IdentityContext context = refreshContextForCurrentUser("VINCULO_PROCESSO_PESSOAL");
        if (projectionRepository.existsByIdentidadeIdAndProcessoLocalIdAndVisivelPainelPessoalTrue(context.identidadeId(), processoId)) {
            return true;
        }
        Usuario usuario = context.usuario();
        Processo processo = processoRepository.findContextoCompletoById(processoId).orElse(null);
        if (processo == null) {
            return false;
        }
        String cpf = normalizeDocumento(usuario.getCpf());
        boolean directIdentityMatch = Objects.equals(cpf, normalizeDocumento(processo.getParteAutoraCpf()))
                || Objects.equals(cpf, normalizeDocumento(processo.getParteReuCpf()))
                || (processo.getUsuario() != null && Objects.equals(cpf, normalizeDocumento(processo.getUsuario().getCpf())));
        if (!directIdentityMatch) {
            return false;
        }
        return isFallbackVisibleForPersonalPanel(processo);
    }

    private IdentityContext refreshContextForCurrentUser(String capability) {
        PersonalProcessAccessEnvelope access = personalProcessAccessGuardService.requireOwnProcessAccess(capability);
        Usuario usuario = currentUserService.getRequired();
        return refreshContext(usuario, access);
    }

    private IdentityContext refreshContextForUser(Usuario usuario) {
        return refreshContext(usuario, null);
    }

    private IdentityContext refreshContext(Usuario usuario, PersonalProcessAccessEnvelope access) {
        Objects.requireNonNull(usuario, "usuario");
        IdentidadeJuridicaNacional identidade = identidadeService.sincronizarUsuario(usuario);
        String documento = normalizeDocumento(usuario.getCpf());
        if (documento.isBlank()) {
            throw new AccessDeniedPjbException("Acesso pessoal ao processo bloqueado: identidade civil sem documento canônico válido.");
        }
        IdentityContext context = new IdentityContext(
                usuario,
                identidade.getId(),
                documento,
                Hashes.sha256Hex(documento),
                access
        );
        refreshLinks(context);
        refreshProjection(context);
        return context;
    }

    private void refreshLinks(IdentityContext context) {
        List<ProntuarioNacionalEntrada> entradas = prontuarioRepository.findAllByDocumentoHashOrderByOcorridoEmDescAtualizadoEmDesc(context.documentoHash());
        Map<String, ProntuarioNacionalEntrada> latestByKey = latestEntries(entradas);
        Map<String, Processo> processosByNumero = resolveLocalProcesses(latestByKey.values());
        for (ProntuarioNacionalEntrada entrada : latestByKey.values()) {
            PapelProcessualNacional papel = mapPapel(entrada);
            Processo processoLocal = processosByNumero.get(entrada.getNupn());
            Confidence confidence = resolveConfidence(context, entrada);
            ProcessoVinculoNacional.ProcessoVinculoSnapshot snapshot = new ProcessoVinculoNacional.ProcessoVinculoSnapshot(
                    processoLocal != null ? processoLocal.getId() : entrada.getProcessoLocalId(),
                    normalizeTribunalCodigo(entrada.getTribunalCodigo(), processoLocal),
                    firstNonBlank(entrada.getTribunalOrigemUri(), processoLocal != null ? processoLocal.getConnectorProtocolReference() : null),
                    resolveSistemaOrigem(entrada, processoLocal),
                    firstNonNull(entrada.getRamoDireito(), processoLocal != null ? processoLocal.getRamoDireito() : null),
                    firstNonNull(entrada.getStatusProcesso(), processoLocal != null ? processoLocal.getStatusProcesso() : null, StatusProcesso.EM_ANDAMENTO),
                    firstNonBlank(entrada.getClasseProcessual(), processoLocal != null ? processoLocal.getClasseProcessual() : null),
                    firstNonBlank(entrada.getAssunto(), processoLocal != null ? processoLocal.getAssunto() : null),
                    firstNonNull(entrada.getNivelSigilo(), processoLocal != null ? processoLocal.getNivelSigilo() : null, NivelSigilo.PUBLICO),
                    confidence.grauConfianca(),
                    confidence.score(),
                    "PRONTUARIO_NACIONAL",
                    confidence.grauConfianca().elegivelPainelPessoal(),
                    firstNonNull(entrada.getNivelSigilo(), processoLocal != null ? processoLocal.getNivelSigilo() : null, NivelSigilo.PUBLICO).exigeCredencial(),
                    false,
                    entrada.getOcorridoEm()
            );
            ProcessoVinculoNacional vinculo = vinculoRepository.findByIdentidadeIdAndNupnAndPapelProcessual(context.identidadeId(), entrada.getNupn(), papel)
                    .orElseGet(() -> ProcessoVinculoNacional.builder()
                            .identidadeId(context.identidadeId())
                            .documentoHash(context.documentoHash())
                            .nupn(entrada.getNupn())
                            .papelProcessual(papel)
                            .poloProcessual(entrada.getPolo().name())
                            .qualificacaoOriginal(entrada.getQualificacao().name())
                            .build());
            vinculo.setDocumentoHash(context.documentoHash());
            vinculo.setPoloProcessual(entrada.getPolo().name());
            vinculo.setQualificacaoOriginal(entrada.getQualificacao().name());
            vinculo.atualizarContexto(snapshot);
            vinculoRepository.save(vinculo);
        }
    }

    private void refreshProjection(IdentityContext context) {
        List<ProcessoVinculoNacional> vinculos = vinculoRepository.findAllByIdentidadeIdAndVisivelPainelPessoalTrue(context.identidadeId());
        Map<String, ProcessoVinculoNacional> principalPorNupn = principalByNupn(vinculos);
        Map<String, ProcessoVisibilidadePessoalOverride> overrides = overridesByNupn(principalPorNupn.keySet());
        Map<Long, Processo> locaisById = resolveLocalProcessesById(principalPorNupn.values());
        Map<Long, MovimentacaoProcessual> lastMov = loadLatestMovements(locaisById.keySet());
        Instant now = Instant.now();
        for (Map.Entry<String, ProcessoVinculoNacional> entry : principalPorNupn.entrySet()) {
            String nupn = entry.getKey();
            ProcessoVinculoNacional vinculo = entry.getValue();
            Processo processoLocal = vinculo.getProcessoLocalId() == null ? null : locaisById.get(vinculo.getProcessoLocalId());
            ProcessoVisibilidadePessoalOverride override = overrides.get(nupn);
            ArchivedRule archivedRule = evaluateArchivedVisibility(processoLocal, vinculo, override, now);
            MovimentacaoProcessual ultima = processoLocal == null ? null : lastMov.get(processoLocal.getId());
            CidadaoProcessoNacionalProjection projection = projectionRepository.findByIdentidadeIdAndNupn(context.identidadeId(), nupn)
                    .orElseGet(() -> CidadaoProcessoNacionalProjection.builder()
                            .identidadeId(context.identidadeId())
                            .documentoHash(context.documentoHash())
                            .nupn(nupn)
                            .build());
            projection.setDocumentoHash(context.documentoHash());
            projection.setProcessoLocalId(vinculo.getProcessoLocalId());
            projection.setNumeroExibicao(firstNonBlank(nupn, processoLocal != null ? processoLocal.getNumeroUnificado() : null, processoLocal != null ? processoLocal.getNumeroProcesso() : null));
            projection.setTribunalCodigo(normalizeTribunalCodigo(vinculo.getTribunalCodigo(), processoLocal));
            projection.setSistemaOrigem(firstNonBlank(vinculo.getSistemaOrigem(), processoLocal != null ? processoLocal.getConnectorSystem() : null));
            projection.setUf(firstNonBlank(processoLocal != null ? processoLocal.getUf() : null, processoLocal != null && processoLocal.getJurisdicao() != null ? processoLocal.getJurisdicao().getUf() : null));
            projection.setComarca(firstNonBlank(processoLocal != null ? processoLocal.getComarca() : null, processoLocal != null && processoLocal.getJurisdicao() != null ? processoLocal.getJurisdicao().getCidade() : null));
            projection.setUnidadeJudicial(firstNonBlank(processoLocal != null ? processoLocal.getVara() : null, processoLocal != null ? processoLocal.getUnidadeJudiciariaCodigo() : null));
            projection.setPapelProcessual(vinculo.getPapelProcessual());
            projection.setGrauConfianca(vinculo.getGrauConfianca());
            projection.setScoreConfianca(vinculo.getScoreConfianca());
            projection.setOrigemVinculo(vinculo.getOrigemVinculo());
            projection.setStatusProcesso(firstNonNull(processoLocal != null ? processoLocal.getStatusProcesso() : null, vinculo.getStatusProcesso()));
            projection.setFaseAtual(processoLocal != null && processoLocal.getFaseAtual() != null ? processoLocal.getFaseAtual().name() : null);
            projection.setRamoDireito(firstNonNull(processoLocal != null ? processoLocal.getRamoDireito() : null, vinculo.getRamoDireito()));
            projection.setClasseProcessual(firstNonBlank(processoLocal != null ? processoLocal.getClasseProcessual() : null, vinculo.getClasseProcessual()));
            projection.setAssunto(firstNonBlank(processoLocal != null ? processoLocal.getAssunto() : null, vinculo.getAssunto()));
            projection.setNivelSigilo(firstNonNull(processoLocal != null ? processoLocal.getNivelSigilo() : null, vinculo.getNivelSigilo(), NivelSigilo.PUBLICO));
            projection.setDataDistribuicao(processoLocal != null ? processoLocal.getDataDistribuicao() : null);
            projection.setDataUltimaMovimentacao(resolveDataUltimaMovimentacao(processoLocal, ultima, vinculo));
            projection.setUltimaMovimentacaoResumo(resolveMovimentoResumo(ultima));
            projection.setArquivado(isArchivedStatus(processoLocal, vinculo));
            projection.setOcultoPorPoliticaArquivo(archivedRule.ocultoPorPoliticaArquivo());
            projection.setReexpostoSecretaria(archivedRule.reexpostoSecretaria());
            projection.setVisivelPainelPessoal(archivedRule.visivelPainelPessoal());
            projection.setExigeStepUp(vinculo.isExigeStepUp() || projection.getNivelSigilo().exigeCredencial());
            projection.setOrigemExternaUri(firstNonBlank(vinculo.getTribunalOrigemUri(), processoLocal != null ? processoLocal.getConnectorProtocolReference() : null));
            projection.setSortKey(resolveSortKey(projection));
            projection.setGeradoEm(now);
            projectionRepository.save(projection);
        }
        for (CidadaoProcessoNacionalProjection stale : projectionRepository.findAllByIdentidadeId(context.identidadeId())) {
            if (principalPorNupn.containsKey(stale.getNupn())) {
                continue;
            }
            stale.setVisivelPainelPessoal(false);
            stale.setOcultoPorPoliticaArquivo(false);
            stale.setReexpostoSecretaria(false);
            stale.setGeradoEm(now);
            stale.setSortKey(resolveSortKey(stale));
            projectionRepository.save(stale);
        }
    }

    private Page<CidadaoLinkedProcessView> hydrate(Page<CidadaoProcessoNacionalProjection> source) {
        List<CidadaoLinkedProcessView> content = hydrate(source.getContent());
        return new PageImpl<>(content, source.getPageable(), source.getTotalElements());
    }

    private List<CidadaoLinkedProcessView> hydrate(List<CidadaoProcessoNacionalProjection> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Processo> locaisById = resolveLocalProcessesById(rows);
        Map<Long, MovimentacaoProcessual> lastMov = loadLatestMovements(locaisById.keySet());
        Map<Long, Long> docCount = loadDocCounts(locaisById.keySet());
        Map<Long, Audiencia> nextAud = loadNextAudiencias(locaisById.keySet());
        Map<Long, JulgamentoColegiado> nextJulg = loadNextJulgamentos(locaisById.keySet());
        List<CidadaoLinkedProcessView> views = new ArrayList<>(rows.size());
        for (CidadaoProcessoNacionalProjection row : rows) {
            Processo processoLocal = row.getProcessoLocalId() == null ? null : locaisById.get(row.getProcessoLocalId());
            MovimentacaoProcessual ultima = row.getProcessoLocalId() == null ? null : lastMov.get(row.getProcessoLocalId());
            Long processoId = row.getProcessoLocalId();
            CidadaoProcessoCardDto card = processoLocal != null
                    ? cardMapper.toCard(
                            processoLocal,
                            ultima,
                            docCount.getOrDefault(processoId, 0L),
                            nextAud.get(processoId),
                            nextJulg.get(processoId)
                    )
                    : syntheticCard(row);
            views.add(new CidadaoLinkedProcessView(card, row, processoLocal));
        }
        views.sort(Comparator.comparingLong(v -> -v.projection().getSortKey()));
        return List.copyOf(views);
    }

    private CidadaoProcessoCardDto syntheticCard(CidadaoProcessoNacionalProjection row) {
        List<String> tokens = new ArrayList<>();
        tokens.add("MALHA_NACIONAL");
        if (row.getSistemaOrigem() != null && !row.getSistemaOrigem().isBlank()) {
            tokens.add("SISTEMA_" + row.getSistemaOrigem().toUpperCase(Locale.ROOT));
        }
        if (row.isReexpostoSecretaria()) {
            tokens.add("REEXPOSTO_SECRETARIA");
        }
        if (row.getPapelProcessual() != null) {
            tokens.add("PAPEL_" + row.getPapelProcessual().name());
        }
        if (row.getNivelSigilo() != null && row.getNivelSigilo().exigeCredencial()) {
            tokens.add("SIGILO_REFORCADO");
        }
        Links links = row.getOrigemExternaUri() == null || row.getOrigemExternaUri().isBlank()
                ? new Links(null, null, null, null, null, null, null)
                : new Links(row.getOrigemExternaUri(), null, null, null, null, null, null);
        return new CidadaoProcessoCardDto(
                row.getProcessoLocalId(),
                row.getNumeroExibicao(),
                row.getClasseProcessual(),
                row.getAssunto(),
                row.getRamoDireito() != null ? row.getRamoDireito().name() : "INTEGRADO_NACIONAL",
                syntheticRitoTitle(row),
                row.getRamoDireito() != null ? row.getRamoDireito().name() : null,
                row.getScoreConfianca() == null ? null : Math.min(1d, row.getScoreConfianca() / 100d),
                row.getGrauConfianca() == GrauConfiancaVinculoProcessual.PENDENTE_CONFIRMACAO,
                row.getGrauConfianca() == GrauConfiancaVinculoProcessual.PENDENTE_CONFIRMACAO ? List.of("VÍNCULO ainda depende de confirmação adicional.") : null,
                row.getStatusProcesso() != null ? row.getStatusProcesso().name() : null,
                row.getFaseAtual(),
                row.getNivelSigilo() != null ? row.getNivelSigilo().name() : null,
                row.getDataUltimaMovimentacao(),
                List.copyOf(tokens),
                row.getUltimaMovimentacaoResumo(),
                row.getDataUltimaMovimentacao(),
                null,
                null,
                null,
                null,
                firstNonBlank(row.getUnidadeJudicial(), row.getComarca(), row.getTribunalCodigo()),
                null,
                null,
                0,
                links
        );
    }

    private static String syntheticRitoTitle(CidadaoProcessoNacionalProjection row) {
        if (row.getRamoDireito() != null) {
            return "Malha nacional integrada • " + row.getRamoDireito().name();
        }
        return "Malha nacional integrada";
    }

    private Map<String, ProntuarioNacionalEntrada> latestEntries(List<ProntuarioNacionalEntrada> entradas) {
        Map<String, ProntuarioNacionalEntrada> out = new LinkedHashMap<>();
        for (ProntuarioNacionalEntrada entrada : entradas) {
            if (entrada == null || entrada.getNupn() == null || entrada.getNupn().isBlank()) {
                continue;
            }
            PapelProcessualNacional papel = mapPapel(entrada);
            String key = entrada.getNupn() + '|' + papel.name();
            out.putIfAbsent(key, entrada);
        }
        return out;
    }

    private Map<String, ProcessoVinculoNacional> principalByNupn(List<ProcessoVinculoNacional> vinculos) {
        Map<String, ProcessoVinculoNacional> out = new LinkedHashMap<>();
        List<ProcessoVinculoNacional> ordered = vinculos.stream()
                .sorted(Comparator
                        .comparingInt((ProcessoVinculoNacional v) -> v.getPapelProcessual().prioridadePainel())
                        .thenComparing((ProcessoVinculoNacional v) -> safeInt(v.getScoreConfianca()), Comparator.reverseOrder())
                        .thenComparing(ProcessoVinculoNacional::getOcorridoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        for (ProcessoVinculoNacional vinculo : ordered) {
            if (vinculo.getNupn() == null || vinculo.getNupn().isBlank()) {
                continue;
            }
            out.putIfAbsent(vinculo.getNupn(), vinculo);
        }
        return out;
    }

    private Map<String, ProcessoVisibilidadePessoalOverride> overridesByNupn(Collection<String> nupns) {
        if (nupns == null || nupns.isEmpty()) {
            return Map.of();
        }
        Instant now = Instant.now();
        Map<String, ProcessoVisibilidadePessoalOverride> out = new HashMap<>();
        for (ProcessoVisibilidadePessoalOverride override : overrideRepository.findAllByNupnInAndEscopo(nupns, SecretariatProcessoVisibilidadePessoalService.ESCOPO_CIDADAO_PAINEL)) {
            if (override != null && override.ativa(now)) {
                out.put(override.getNupn(), override);
            }
        }
        return out;
    }

    private Map<String, Processo> resolveLocalProcesses(Collection<ProntuarioNacionalEntrada> entradas) {
        LinkedHashSet<String> numeros = new LinkedHashSet<>();
        for (ProntuarioNacionalEntrada entrada : entradas) {
            if (entrada == null) {
                continue;
            }
            if (entrada.getNupn() != null && !entrada.getNupn().isBlank()) {
                numeros.add(entrada.getNupn());
            }
        }
        if (numeros.isEmpty()) {
            return Map.of();
        }
        Map<String, Processo> out = new HashMap<>();
        for (Processo processo : processoRepository.findAllByNumeroUnificadoInOrNumeroProcessoIn(numeros, numeros)) {
            if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
                out.putIfAbsent(processo.getNumeroUnificado(), processo);
            }
            if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
                out.putIfAbsent(processo.getNumeroProcesso(), processo);
            }
        }
        return out;
    }

    private Map<Long, Processo> resolveLocalProcessesById(Collection<?> source) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Object item : source) {
            if (item instanceof ProcessoVinculoNacional vinculo && vinculo.getProcessoLocalId() != null) {
                ids.add(vinculo.getProcessoLocalId());
            }
            if (item instanceof CidadaoProcessoNacionalProjection projection && projection.getProcessoLocalId() != null) {
                ids.add(projection.getProcessoLocalId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Processo> out = new HashMap<>();
        for (Processo processo : processoRepository.findAllById(ids)) {
            out.put(processo.getId(), processo);
        }
        return out;
    }

    private boolean isArchived(Processo processoLocal, ProcessoVinculoNacional vinculo) {
        return isArchivedStatus(processoLocal, vinculo);
    }

    private boolean isFallbackVisibleForPersonalPanel(Processo processoLocal) {
        if (processoLocal == null) {
            return false;
        }
        String nupn = firstNonBlank(processoLocal.getNumeroUnificado(), processoLocal.getNumeroProcesso());
        ProcessoVisibilidadePessoalOverride override = nupn == null ? null : overridesByNupn(List.of(nupn)).get(nupn);
        ProcessoVinculoNacional synthetic = ProcessoVinculoNacional.builder()
                .processoLocalId(processoLocal.getId())
                .tribunalCodigo(normalizeTribunalCodigo(null, processoLocal))
                .statusProcesso(firstNonNull(processoLocal.getStatusProcesso(), StatusProcesso.EM_ANDAMENTO))
                .ramoDireito(processoLocal.getRamoDireito())
                .nivelSigilo(firstNonNull(processoLocal.getNivelSigilo(), NivelSigilo.PUBLICO))
                .ocorridoEm(resolveFallbackInstant(processoLocal))
                .build();
        ArchivedRule archivedRule = evaluateArchivedVisibility(processoLocal, synthetic, override, Instant.now());
        return archivedRule.visivelPainelPessoal();
    }

    private Instant resolveFallbackInstant(Processo processoLocal) {
        LocalDateTime reference = processoLocal == null ? null : firstNonNull(
                processoLocal.getDataUltimaMovimentacao(),
                processoLocal.getDataAtualizacao(),
                processoLocal.getDataDistribuicao(),
                processoLocal.getDataCriacao()
        );
        return reference == null ? Instant.now() : reference.toInstant(ZoneOffset.UTC);
    }

    private Map<Long, MovimentacaoProcessual> loadLatestMovements(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, MovimentacaoProcessual> out = new HashMap<>();
        for (MovimentacaoProcessual mov : movimentacaoRepository.findLatestByProcessoIds(new ArrayList<>(ids))) {
            if (mov != null && mov.getProcesso() != null && mov.getProcesso().getId() != null) {
                out.putIfAbsent(mov.getProcesso().getId(), mov);
            }
        }
        return out;
    }

    private Map<Long, Long> loadDocCounts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> out = new HashMap<>();
        for (DocumentoProcessualRepository.ProcessoDocCount row : documentoRepository.countDocsByProcessoIds(new ArrayList<>(ids))) {
            if (row != null && row.getProcessoId() != null) {
                out.put(row.getProcessoId(), row.getCnt());
            }
        }
        return out;
    }

    private Map<Long, Audiencia> loadNextAudiencias(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        long[] vector = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, Audiencia> out = new HashMap<>();
        for (Audiencia audiencia : audienciaRepository.findNextUpcomingByProcessoIds(vector, LocalDateTime.now())) {
            if (audiencia != null && audiencia.getProcesso() != null && audiencia.getProcesso().getId() != null) {
                out.putIfAbsent(audiencia.getProcesso().getId(), audiencia);
            }
        }
        return out;
    }

    private Map<Long, JulgamentoColegiado> loadNextJulgamentos(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        long[] vector = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, JulgamentoColegiado> out = new HashMap<>();
        for (JulgamentoColegiado julgamento : julgamentoRepository.findNextPautaByProcessoIds(vector, LocalDateTime.now())) {
            if (julgamento != null && julgamento.getProcesso() != null && julgamento.getProcesso().getId() != null) {
                out.putIfAbsent(julgamento.getProcesso().getId(), julgamento);
            }
        }
        return out;
    }

    private ArchivedRule evaluateArchivedVisibility(Processo processoLocal,
                                                    ProcessoVinculoNacional vinculo,
                                                    ProcessoVisibilidadePessoalOverride override,
                                                    Instant now) {
        ProcessoTombstonePolicyReport report = processoTombstonePolicyEngine.evaluate(processoLocal, vinculo, override, now);
        return new ArchivedRule(report.visiblePanel(), report.hiddenByArchivePolicy(), report.reexposedBySecretariat());
    }

    private boolean isArchivedStatus(Processo processoLocal, ProcessoVinculoNacional vinculo) {
        StatusProcesso status = firstNonNull(processoLocal != null ? processoLocal.getStatusProcesso() : null, vinculo == null ? null : vinculo.getStatusProcesso());
        return status == StatusProcesso.ARQUIVADO;
    }

    private Confidence resolveConfidence(IdentityContext context, ProntuarioNacionalEntrada entrada) {
        if (entrada.getIdentidade() != null && Objects.equals(entrada.getIdentidade().getId(), context.identidadeId())) {
            return new Confidence(GrauConfiancaVinculoProcessual.DETERMINISTICO, 100);
        }
        if (Objects.equals(context.documentoHash(), entrada.getDocumentoHash())) {
            return new Confidence(GrauConfiancaVinculoProcessual.DETERMINISTICO, 96);
        }
        String nomeEntrada = entrada.getNomeSujeitoChave();
        String nomeUsuario = documentoValidator.gerarChavePesquisa(context.usuario().getNome());
        if (nomeEntrada != null && !nomeEntrada.isBlank() && Objects.equals(nomeEntrada, nomeUsuario)) {
            return new Confidence(GrauConfiancaVinculoProcessual.PROVAVEL, 72);
        }
        return new Confidence(GrauConfiancaVinculoProcessual.PENDENTE_CONFIRMACAO, 45);
    }

    private PapelProcessualNacional mapPapel(ProntuarioNacionalEntrada entrada) {
        return switch (entrada.getQualificacao()) {
            case AUTOR -> PapelProcessualNacional.AUTOR;
            case REU -> PapelProcessualNacional.REU;
            case ADVOGADO -> PapelProcessualNacional.ADVOGADO;
            case REPRESENTANTE_LEGAL -> PapelProcessualNacional.REPRESENTANTE_LEGAL;
            case MEMBRO_MP -> PapelProcessualNacional.MEMBRO_MINISTERIO_PUBLICO;
            case DEFENSOR_PUBLICO -> PapelProcessualNacional.DEFENSOR_PUBLICO;
            case PROCURADOR_PUBLICO -> PapelProcessualNacional.PROCURADOR_PUBLICO;
            case PERITO -> PapelProcessualNacional.PERITO;
            case TESTEMUNHA -> PapelProcessualNacional.TESTEMUNHA;
            case ASSISTENTE -> PapelProcessualNacional.ASSISTENTE;
            case IMPETRANTE -> PapelProcessualNacional.IMPETRANTE;
            case IMPETRADO -> PapelProcessualNacional.IMPETRADO;
            case EXECUTANTE -> PapelProcessualNacional.EXECUTANTE;
            case EXECUTADO -> PapelProcessualNacional.EXECUTADO;
            case INTERESSADO -> PapelProcessualNacional.INTERESSADO;
            case AUTORIDADE -> PapelProcessualNacional.AUTORIDADE;
            case VITIMA -> PapelProcessualNacional.VITIMA;
            case INVESTIGADO -> PapelProcessualNacional.INVESTIGADO;
        };
    }

    private static String resolveSistemaOrigem(ProntuarioNacionalEntrada entrada, Processo processoLocal) {
        String fromProcess = processoLocal != null ? normalizeSystem(processoLocal.getConnectorSystem()) : null;
        if (fromProcess != null) {
            return fromProcess;
        }
        String uri = entrada.getTribunalOrigemUri();
        if (uri != null) {
            String normalized = uri.toUpperCase(Locale.ROOT);
            for (JudicialSystem system : JudicialSystem.values()) {
                if (normalized.contains(system.name())) {
                    return system.name();
                }
            }
            if (normalized.contains("ESAJ") || normalized.contains("E-SAJ")) {
                return JudicialSystem.ESAJ.name();
            }
            if (normalized.contains("EPROC")) {
                return JudicialSystem.EPROC.name();
            }
            if (normalized.contains("PROJUDI")) {
                return JudicialSystem.PROJUDI.name();
            }
            if (normalized.contains("CRETA")) {
                return JudicialSystem.CRETA.name();
            }
            if (normalized.contains("PJE")) {
                return JudicialSystem.PJE.name();
            }
        }
        return JudicialSystem.OUTRO.name();
    }

    private static String normalizeSystem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (token) {
            case "E_SAJ" -> JudicialSystem.ESAJ.name();
            case "PJE_JF", "PJE_TRT", "PJE_TSE", "PJE_TRE" -> JudicialSystem.PJE.name();
            default -> token;
        };
    }

    private static String normalizeNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        return numero.trim();
    }

    private static String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        String value = uf.trim().toUpperCase(Locale.ROOT);
        return value.length() == 2 ? value : null;
    }

    private String normalizeDocumento(String raw) {
        return documentoValidator.normalizarDocumento(raw);
    }

    private static String normalizeTribunalCodigo(String raw, Processo processoLocal) {
        String value = firstNonBlank(raw, processoLocal != null ? processoLocal.getTribunalCodigoRoteado() : null, processoLocal != null ? processoLocal.getTribunal() : null);
        if (value == null) {
            return "NACIONAL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static LocalDateTime resolveDataUltimaMovimentacao(Processo processoLocal,
                                                               MovimentacaoProcessual ultima,
                                                               ProcessoVinculoNacional vinculo) {
        if (ultima != null && ultima.getDataMovimentacao() != null) {
            return LocalDateTime.ofInstant(ultima.getDataMovimentacao(), ZoneOffset.UTC);
        }
        if (processoLocal != null && processoLocal.getDataUltimaMovimentacao() != null) {
            return processoLocal.getDataUltimaMovimentacao();
        }
        Instant ocorrido = vinculo.getOcorridoEm();
        return ocorrido == null ? null : LocalDateTime.ofInstant(ocorrido, ZoneOffset.UTC);
    }

    private static String resolveMovimentoResumo(MovimentacaoProcessual mov) {
        if (mov == null || mov.getDescricao() == null || mov.getDescricao().isBlank()) {
            return null;
        }
        String text = mov.getDescricao().trim();
        return text.length() <= 240 ? text : text.substring(0, 239) + '…';
    }

    private static long resolveSortKey(CidadaoProcessoNacionalProjection projection) {
        LocalDateTime movement = projection.getDataUltimaMovimentacao();
        if (movement != null) {
            return movement.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        LocalDateTime distribution = projection.getDataDistribuicao();
        if (distribution != null) {
            return distribution.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return projection.getGeradoEm() != null ? projection.getGeradoEm().toEpochMilli() : 0L;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
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

    private record Confidence(GrauConfiancaVinculoProcessual grauConfianca, int score) {
    }

    private record ArchivedRule(boolean visivelPainelPessoal,
                                boolean ocultoPorPoliticaArquivo,
                                boolean reexpostoSecretaria) {
    }

    private record IdentityContext(Usuario usuario,
                                   UUID identidadeId,
                                   String documento,
                                   String documentoHash,
                                   PersonalProcessAccessEnvelope access) {
    }

    public record CidadaoLinkedProcessView(CidadaoProcessoCardDto card,
                                           CidadaoProcessoNacionalProjection projection,
                                           Processo processoLocal) {
    }
}
