package com.tcc.pjb.backend.inovacao.atlas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.atlas.AtlasCelulaUpsertRequest;
import com.tcc.pjb.backend.model.entity.Municipios;
import com.tcc.pjb.backend.model.entity.atlas.AtlasAcessoMunicipio;
import com.tcc.pjb.backend.model.entity.atlas.ClassificacaoDesertoAtlas;
import com.tcc.pjb.backend.model.entity.painel.PainelTribunalMetrica;
import com.tcc.pjb.backend.model.repository.AtlasAcessoMunicipioRepository;
import com.tcc.pjb.backend.model.repository.MunicipiosRepository;
import com.tcc.pjb.backend.model.repository.PainelTribunalMetricaRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class AtlasAcessoJusticaService {

    public static final String EVT_ATLAS_CELULA_ATUALIZADA = "pjb.inovacao.atlas.celula.atualizada";
    public static final String EVT_ATLAS_RELATORIO_GERADO = "pjb.inovacao.atlas.relatorio.gerado";
    private static final Duration ATLAS_CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration PAINEL_BASE_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration ATLAS_READ_BUDGET = Duration.ofSeconds(5);
    private static final Duration ATLAS_WRITE_BUDGET = Duration.ofSeconds(4);
    private static final Duration ATLAS_BATCH_WRITE_BUDGET = Duration.ofSeconds(6);
    private static final int ATLAS_SYNC_BATCH_SIZE = 200;

    private final AtlasAcessoMunicipioRepository atlasRepository;
    private final MunicipiosRepository municipiosRepository;
    private final PainelTribunalMetricaRepository painelTribunalRepository;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;
    private final AtomicReference<CachedAtlasCells> atlasCellsCache = new AtomicReference<>();
    private final AtomicReference<CachedPainelMetricas> painelMetricasCache = new AtomicReference<>();

    public AtlasAcessoJusticaService(AtlasAcessoMunicipioRepository atlasRepository,
                                     MunicipiosRepository municipiosRepository,
                                     PainelTribunalMetricaRepository painelTribunalRepository,
                                     AuditLedgerService auditLedgerService,
                                     OutboxPublisher outboxPublisher,
                                     PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.atlasRepository = Objects.requireNonNull(atlasRepository);
        this.municipiosRepository = Objects.requireNonNull(municipiosRepository);
        this.painelTribunalRepository = Objects.requireNonNull(painelTribunalRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport);
    }

    public record CelulaAtlas(
            String codigoIbge,
            String nomeMunicipio,
            String uf,
            String regiao,
            int populacao,
            int varasInstaladas,
            int juizesEmExercicio,
            int defensoriasPorMunicipio,
            int advogadosOabAtivos,
            boolean temJuizadoEspecial,
            boolean temCejusc,
            int processosPorMilHabitantes,
            int novosProcessosMes,
            double taxaResolutividadePct,
            double tempoMedioResolucaoDias,
            double indiceCongestionamento,
            double taxaJusticaGratuitaPct,
            double taxaAutoRepresentacaoPct,
            double taxaPrescricaoAparentePct,
            IndiceAcesso indiceAcesso,
            ClassificacaoDeserto classificacao,
            Instant atualizadoEm
    ) {
        public double advogadosPorMilHabitantes() {
            return populacao > 0 ? (advogadosOabAtivos * 1000.0d) / populacao : 0.0d;
        }

        public double juizesPorCemMilHabitantes() {
            return populacao > 0 ? (juizesEmExercicio * 100000.0d) / populacao : 0.0d;
        }
    }

    public record IndiceAcesso(
            double scoreInfraestrutura,
            double scoreRepresentacao,
            double scoreCeleridade,
            double scoreEfetividade,
            double scoreTotal,
            String grau
    ) {
        public static IndiceAcesso calcular(double infraestrutura, double representacao, double celeridade, double efetividade) {
            double total = infraestrutura + representacao + celeridade + efetividade;
            String grau = total >= 80.0d ? "A"
                    : total >= 65.0d ? "B"
                    : total >= 50.0d ? "C"
                    : total >= 35.0d ? "D"
                    : total >= 20.0d ? "E"
                    : "F";
            return new IndiceAcesso(round2(infraestrutura), round2(representacao), round2(celeridade), round2(efetividade), round2(total), grau);
        }
    }

    public enum ClassificacaoDeserto {
        PLENO,
        ADEQUADO,
        PARCIAL,
        PRECARIO,
        DESERTO_PARCIAL,
        DESERTO_TOTAL
    }

    public record RelatorioPoliticaPublica(
            String titulo,
            String uf,
            int totalMunicipios,
            int municipiosDeserto,
            int populacaoDesatendida,
            List<CelulaAtlas> pioresMunicipios,
            List<CelulaAtlas> melhoresMunicipios,
            List<RecomendacaoPolitica> recomendacoes,
            Map<ClassificacaoDeserto, Long> distribuicao,
            Instant geradoEm
    ) {
    }

    public record RecomendacaoPolitica(
            TipoRecomendacao tipo,
            String municipio,
            String uf,
            String descricao,
            String fundamentacao,
            PrioridadeAcao prioridade,
            int populacaoBeneficiada
    ) {
    }

    public enum TipoRecomendacao {
        CRIAR_VARA,
        EXPANDIR_DEFENSORIA,
        INSTALAR_CEJUSC,
        PROGRAMA_ADVOCACIA_PRO_BONO,
        JUIZADO_ITINERANTE,
        MUTIRAO_CONCILIACAO,
        PROGRAMA_EDUCACAO_JURIDICA
    }

    public enum PrioridadeAcao {
        URGENTE,
        ALTA,
        MEDIA,
        BAIXA
    }

    public record PontoHeatmap(
            String codigoIbge,
            String municipio,
            String uf,
            double scoreAcesso,
            String grau,
            ClassificacaoDeserto classificacao,
            int populacao
    ) {
    }

    public record ResumoNacionalAtlas(
            long totalMunicipios,
            long desertoTotal,
            long desertoParcial,
            long precario,
            long populacaoDesatendida,
            double scoreNacionalMedio,
            Instant geradoEm
    ) {
    }

    public CelulaAtlas registrarCelula(AtlasCelulaUpsertRequest request) {
        Objects.requireNonNull(request, "request");
        CelulaAtlas celula = transactionalExecutionSupport.executeInNewTransaction(
                "atlas.celula.persist",
                ATLAS_WRITE_BUDGET,
                () -> persistCelula(request)
        );
        invalidateCaches();
        registrarAuditoriaEEvento(celula, "ATLAS_CELULA_UPSERT");
        return celula;
    }

    public int sincronizarBaseIbge() {
        List<Municipios> municipios = transactionalExecutionSupport.executeReadOnly(
                "atlas.ibge.load-municipios",
                ATLAS_READ_BUDGET,
                (java.util.function.Supplier<List<Municipios>>) municipiosRepository::findAll
        );
        java.util.Set<String> existentes = transactionalExecutionSupport.executeReadOnly(
                "atlas.ibge.load-existing-codes",
                ATLAS_READ_BUDGET,
                () -> atlasRepository.findAll().stream()
                        .map(AtlasAcessoMunicipio::getCodigoIbge)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
        );
        List<AtlasIbgeBaselineDraft> drafts = new ArrayList<>();
        for (Municipios municipio : municipios) {
            AtlasIbgeBaselineDraft draft = buildBaselineDraft(municipio, existentes);
            if (draft != null) {
                drafts.add(draft);
            }
        }
        int criadas = 0;
        for (int start = 0; start < drafts.size(); start += ATLAS_SYNC_BATCH_SIZE) {
            int end = Math.min(drafts.size(), start + ATLAS_SYNC_BATCH_SIZE);
            List<AtlasIbgeBaselineDraft> batch = drafts.subList(start, end);
            criadas += transactionalExecutionSupport.executeInNewTransaction(
                    "atlas.ibge.persist-batch",
                    ATLAS_BATCH_WRITE_BUDGET,
                    () -> persistBaselineBatch(batch)
            );
        }
        invalidateCaches();
        String criadasText = Integer.toString(criadas);
        Instant geradoEm = Instant.now();
        auditLedgerService.appendSafely("ATLAS_IBGE_SYNC", "ATLAS", criadasText, Hashes.sha256Hex(criadasText));
        outboxPublisher.enqueue(
                "atlas:sync:ibge",
                EVT_ATLAS_CELULA_ATUALIZADA,
                Map.of("criadas", criadas, "geradoEm", geradoEm.toString()),
                Map.of("source", "atlas_sync_ibge"),
                "atlasSync:" + Hashes.sha256Hex(criadasText + geradoEm),
                "AtlasSync",
                criadasText
        );
        return criadas;
    }

    private CelulaAtlas persistCelula(AtlasCelulaUpsertRequest request) {
        String codigoIbge = normalizeCodigoIbge(request.codigoIbge());
        Municipios municipio = municipiosRepository.findById(Long.parseLong(codigoIbge)).orElse(null);
        AtlasAcessoMunicipio entity = atlasRepository.findByCodigoIbge(codigoIbge).orElseGet(AtlasAcessoMunicipio::new);
        PainelBaseUf painelBase = carregarPainelBasePorUf(resolveUf(request.uf(), municipio));

        String nomeMunicipio = resolveNomeMunicipio(request.nomeMunicipio(), municipio, codigoIbge);
        String uf = resolveUf(request.uf(), municipio);
        String regiao = resolveRegiao(request.regiao(), uf);
        int populacao = positive(request.populacao());
        int varas = nonNegative(request.varasInstaladas());
        int juizes = nonNegative(request.juizesEmExercicio());
        int defensorias = nonNegative(request.defensoriasPorMunicipio());
        int advogados = nonNegative(request.advogadosOabAtivos());
        int processosPorMil = nonNegative(request.processosPorMilHabitantes());
        int novosMes = nonNegative(request.novosProcessosMes());
        double taxaResolutividade = pct(request.taxaResolutividadePct(), painelBase.taxaResolutividadePct());
        double tempoMedio = nonNegative(request.tempoMedioResolucaoDias(), painelBase.tempoMedioResolucaoDias());
        double congestionamento = ratio(request.indiceCongestionamento(), painelBase.indiceCongestionamento());
        double taxaJg = pct(request.taxaJusticaGratuitaPct(), painelBase.taxaJusticaGratuitaPct());
        double autoRepresentacao = pct(request.taxaAutoRepresentacaoPct(), painelBase.taxaAutoRepresentacaoPct());
        double taxaPrescricao = pct(request.taxaPrescricaoAparentePct(), painelBase.taxaPrescricaoAparentePct());
        boolean temJuizadoEspecial = Boolean.TRUE.equals(request.temJuizadoEspecial());
        boolean temCejusc = Boolean.TRUE.equals(request.temCejusc());

        IndiceAcesso indice = calcularIndice(populacao, varas, juizes, defensorias, advogados, temJuizadoEspecial, temCejusc,
                taxaResolutividade, tempoMedio, congestionamento, taxaJg, autoRepresentacao, taxaPrescricao);
        ClassificacaoDeserto classificacao = classificar(populacao, varas, juizes, defensorias, advogados, temJuizadoEspecial, temCejusc,
                indice.scoreTotal(), taxaPrescricao, autoRepresentacao);

        entity.setCodigoIbge(codigoIbge);
        entity.setNomeMunicipio(nomeMunicipio);
        entity.setUf(uf);
        entity.setRegiao(regiao);
        entity.setPopulacao(populacao);
        entity.setVarasInstaladas(varas);
        entity.setJuizesEmExercicio(juizes);
        entity.setDefensoriasPorMunicipio(defensorias);
        entity.setAdvogadosOabAtivos(advogados);
        entity.setTemJuizadoEspecial(temJuizadoEspecial);
        entity.setTemCejusc(temCejusc);
        entity.setProcessosPorMilHabitantes(processosPorMil);
        entity.setNovosProcessosMes(novosMes);
        entity.setTaxaResolutividadePct(decimal(taxaResolutividade, 4));
        entity.setTempoMedioResolucaoDias(decimal(tempoMedio, 2));
        entity.setIndiceCongestionamento(decimal(congestionamento, 4));
        entity.setTaxaJusticaGratuitaPct(decimal(taxaJg, 4));
        entity.setTaxaAutoRepresentacaoPct(decimal(autoRepresentacao, 4));
        entity.setTaxaPrescricaoAparentePct(decimal(taxaPrescricao, 4));
        entity.setScoreInfraestrutura(decimal(indice.scoreInfraestrutura(), 4));
        entity.setScoreRepresentacao(decimal(indice.scoreRepresentacao(), 4));
        entity.setScoreCeleridade(decimal(indice.scoreCeleridade(), 4));
        entity.setScoreEfetividade(decimal(indice.scoreEfetividade(), 4));
        entity.setScoreTotal(decimal(indice.scoreTotal(), 4));
        entity.setGrau(indice.grau());
        entity.setClassificacao(toEntity(classificacao));
        entity.setOrigemDados(normalizeText(request.origemDados(), "ATLAS_MANUAL"));
        entity.setAtualizadoEm(Instant.now());
        return map(atlasRepository.save(entity));
    }

    private AtlasIbgeBaselineDraft buildBaselineDraft(Municipios municipio, java.util.Set<String> existentes) {
        if (municipio == null) {
            return null;
        }
        String codigoIbge = String.format(Locale.ROOT, "%07d", municipio.getIbgeCode());
        if (existentes.contains(codigoIbge)) {
            return null;
        }
        PainelBaseUf painelBase = carregarPainelBasePorUf(municipio.getUf());
        IndiceAcesso indice = calcularIndice(0, 0, 0, 0, 0, false, false,
                painelBase.taxaResolutividadePct(), painelBase.tempoMedioResolucaoDias(), painelBase.indiceCongestionamento(),
                painelBase.taxaJusticaGratuitaPct(), 0.0d, painelBase.taxaPrescricaoAparentePct());
        return new AtlasIbgeBaselineDraft(
                codigoIbge,
                normalizeText(municipio.getNome(), "Municipio " + codigoIbge),
                normalizeUf(municipio.getUf()),
                resolveRegiao(null, municipio.getUf()),
                painelBase,
                indice,
                Instant.now()
        );
    }

    private int persistBaselineBatch(List<AtlasIbgeBaselineDraft> batch) {
        List<AtlasAcessoMunicipio> entities = new ArrayList<>(batch.size());
        for (AtlasIbgeBaselineDraft draft : batch) {
            if (atlasRepository.findByCodigoIbge(draft.codigoIbge()).isPresent()) {
                continue;
            }
            AtlasAcessoMunicipio entity = new AtlasAcessoMunicipio();
            entity.setCodigoIbge(draft.codigoIbge());
            entity.setNomeMunicipio(draft.nomeMunicipio());
            entity.setUf(draft.uf());
            entity.setRegiao(draft.regiao());
            entity.setPopulacao(0);
            entity.setVarasInstaladas(0);
            entity.setJuizesEmExercicio(0);
            entity.setDefensoriasPorMunicipio(0);
            entity.setAdvogadosOabAtivos(0);
            entity.setTemJuizadoEspecial(false);
            entity.setTemCejusc(false);
            entity.setProcessosPorMilHabitantes(0);
            entity.setNovosProcessosMes(0);
            entity.setTaxaResolutividadePct(decimal(draft.painelBase().taxaResolutividadePct(), 4));
            entity.setTempoMedioResolucaoDias(decimal(draft.painelBase().tempoMedioResolucaoDias(), 2));
            entity.setIndiceCongestionamento(decimal(draft.painelBase().indiceCongestionamento(), 4));
            entity.setTaxaJusticaGratuitaPct(decimal(draft.painelBase().taxaJusticaGratuitaPct(), 4));
            entity.setTaxaAutoRepresentacaoPct(decimal(0.0d, 4));
            entity.setTaxaPrescricaoAparentePct(decimal(draft.painelBase().taxaPrescricaoAparentePct(), 4));
            entity.setScoreInfraestrutura(decimal(draft.indice().scoreInfraestrutura(), 4));
            entity.setScoreRepresentacao(decimal(draft.indice().scoreRepresentacao(), 4));
            entity.setScoreCeleridade(decimal(draft.indice().scoreCeleridade(), 4));
            entity.setScoreEfetividade(decimal(draft.indice().scoreEfetividade(), 4));
            entity.setScoreTotal(decimal(draft.indice().scoreTotal(), 4));
            entity.setGrau(draft.indice().grau());
            entity.setClassificacao(toEntity(ClassificacaoDeserto.DESERTO_TOTAL));
            entity.setOrigemDados("IBGE_BASELINE");
            entity.setAtualizadoEm(draft.atualizadoEm());
            entities.add(entity);
        }
        if (entities.isEmpty()) {
            return 0;
        }
        atlasRepository.saveAll(entities);
        return entities.size();
    }

    private record AtlasIbgeBaselineDraft(
            String codigoIbge,
            String nomeMunicipio,
            String uf,
            String regiao,
            PainelBaseUf painelBase,
            IndiceAcesso indice,
            Instant atualizadoEm
    ) {
    }

    @PjbTransactionalBudget(operation = "atlas.acesso-justica.gerar-relatorio-uf", maxMillis = 5000)
    @Transactional(readOnly = true)
    public RelatorioPoliticaPublica gerarRelatorioUF(String uf) {
        String siglaUf = normalizeUf(uf);
        List<CelulaAtlas> celulasUf = atlasRepository.findByUfIgnoreCaseOrderByScoreTotalAsc(siglaUf).stream().map(this::map).toList();
        int municipiosDeserto = (int) celulasUf.stream()
                .filter(c -> c.classificacao() == ClassificacaoDeserto.DESERTO_TOTAL || c.classificacao() == ClassificacaoDeserto.DESERTO_PARCIAL)
                .count();
        int populacaoDesatendida = celulasUf.stream()
                .filter(c -> c.classificacao().ordinal() >= ClassificacaoDeserto.PRECARIO.ordinal())
                .mapToInt(CelulaAtlas::populacao)
                .sum();
        List<CelulaAtlas> piores = celulasUf.stream().limit(20).toList();
        List<CelulaAtlas> melhores = celulasUf.stream()
                .sorted(Comparator.comparingDouble((CelulaAtlas c) -> c.indiceAcesso().scoreTotal()).reversed())
                .limit(20)
                .toList();
        Map<ClassificacaoDeserto, Long> distribuicao = celulasUf.stream()
                .collect(Collectors.groupingBy(CelulaAtlas::classificacao, () -> new EnumMap<>(ClassificacaoDeserto.class), Collectors.counting()));
        List<RecomendacaoPolitica> recomendacoes = gerarRecomendacoes(piores);
        Instant geradoEm = Instant.now();
        RelatorioPoliticaPublica relatorio = new RelatorioPoliticaPublica(
                "Atlas do Acesso a Justica - " + siglaUf + " - " + geradoEm.toString().substring(0, 10),
                siglaUf,
                celulasUf.size(),
                municipiosDeserto,
                populacaoDesatendida,
                piores,
                melhores,
                recomendacoes,
                distribuicao,
                geradoEm
        );
        outboxPublisher.enqueue(
                "atlas:relatorio:uf",
                EVT_ATLAS_RELATORIO_GERADO,
                Map.of("uf", siglaUf, "totalMunicipios", celulasUf.size(), "geradoEm", relatorio.geradoEm().toString()),
                Map.of("source", "atlas_relatorio_uf"),
                "atlasRelatorioUf:" + Hashes.sha256Hex(siglaUf + relatorio.geradoEm().toString()),
                "AtlasRelatorioUf",
                siglaUf
        );
        return relatorio;
    }

    @Transactional(readOnly = true)
    public Optional<CelulaAtlas> buscarMunicipio(String codigoIbge) {
        return atlasRepository.findByCodigoIbge(normalizeCodigoIbge(codigoIbge)).map(this::map);
    }

    @Transactional(readOnly = true)
    public List<CelulaAtlas> municipiosPorClassificacao(ClassificacaoDeserto classificacao) {
        return atlasRepository.findByClassificacaoOrderByPopulacaoDesc(toEntity(classificacao)).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<CelulaAtlas> municipiosPorUf(String uf) {
        return atlasRepository.findByUfIgnoreCaseOrderByScoreTotalAsc(normalizeUf(uf)).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<PontoHeatmap> gerarHeatmapNacional() {
        return atlasCells().stream()
                .sorted(Comparator.comparingDouble((CelulaAtlas c) -> c.indiceAcesso().scoreTotal()).thenComparing(CelulaAtlas::nomeMunicipio))
                .map(c -> new PontoHeatmap(c.codigoIbge(), c.nomeMunicipio(), c.uf(), c.indiceAcesso().scoreTotal(), c.indiceAcesso().grau(), c.classificacao(), c.populacao()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoNacionalAtlas resumoNacional() {
        List<CelulaAtlas> celulas = atlasCells();
        long total = celulas.size();
        long desertoTotal = celulas.stream().filter(c -> c.classificacao() == ClassificacaoDeserto.DESERTO_TOTAL).count();
        long desertoParcial = celulas.stream().filter(c -> c.classificacao() == ClassificacaoDeserto.DESERTO_PARCIAL).count();
        long precario = celulas.stream().filter(c -> c.classificacao() == ClassificacaoDeserto.PRECARIO).count();
        long populacaoDesatendida = celulas.stream()
                .filter(c -> c.classificacao().ordinal() >= ClassificacaoDeserto.PRECARIO.ordinal())
                .mapToLong(CelulaAtlas::populacao)
                .sum();
        double scoreNacional = celulas.stream().mapToDouble(c -> c.indiceAcesso().scoreTotal()).average().orElse(0.0d);
        return new ResumoNacionalAtlas(total, desertoTotal, desertoParcial, precario, populacaoDesatendida, round2(scoreNacional), Instant.now());
    }

    private List<RecomendacaoPolitica> gerarRecomendacoes(List<CelulaAtlas> piores) {
        List<RecomendacaoPolitica> recomendacoes = new ArrayList<>();
        for (CelulaAtlas c : piores) {
            if (c.varasInstaladas() == 0) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.CRIAR_VARA,
                        c.nomeMunicipio(),
                        c.uf(),
                        "Municipio sem vara instalada e com baixa capacidade institucional local.",
                        "CF art. 5 XXXV; Res. CNJ 219/2016; equilibrio federativo de acesso.",
                        c.populacao() >= 50000 ? PrioridadeAcao.URGENTE : PrioridadeAcao.ALTA,
                        c.populacao()
                ));
            }
            if (c.defensoriasPorMunicipio() == 0 && c.taxaJusticaGratuitaPct() >= 40.0d) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.EXPANDIR_DEFENSORIA,
                        c.nomeMunicipio(),
                        c.uf(),
                        String.format(Locale.ROOT, "%.0f%% da demanda depende de justica gratuita sem cobertura local da Defensoria.", c.taxaJusticaGratuitaPct()),
                        "LC 80/1994; EC 80/2014; ampliacao de cobertura institucional.",
                        PrioridadeAcao.ALTA,
                        c.populacao()
                ));
            }
            if (!c.temCejusc()) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.INSTALAR_CEJUSC,
                        c.nomeMunicipio(),
                        c.uf(),
                        "Ausencia de CEJUSC compromete acesso consensual e prevencao de litigiosidade.",
                        "Res. CNJ 125/2010; politica publica de solucao consensual.",
                        c.populacao() >= 100000 ? PrioridadeAcao.ALTA : PrioridadeAcao.MEDIA,
                        c.populacao()
                ));
            }
            if (c.taxaAutoRepresentacaoPct() >= 25.0d && c.advogadosPorMilHabitantes() < 1.0d) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.PROGRAMA_ADVOCACIA_PRO_BONO,
                        c.nomeMunicipio(),
                        c.uf(),
                        "Baixa densidade de advocacia local com alta autorrepresentacao.",
                        "Expansao de convenios OAB, nucleos de pratica e advocacia de interesse publico.",
                        PrioridadeAcao.MEDIA,
                        c.populacao()
                ));
            }
            if (c.taxaPrescricaoAparentePct() >= 15.0d) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.PROGRAMA_EDUCACAO_JURIDICA,
                        c.nomeMunicipio(),
                        c.uf(),
                        String.format(Locale.ROOT, "%.0f%% dos casos chegam com prescricao aparente, sinalizando barreira informacional.", c.taxaPrescricaoAparentePct()),
                        "Politica de orientacao preventiva, acesso informacional e cidadania juridica.",
                        PrioridadeAcao.MEDIA,
                        c.populacao()
                ));
            }
            if (c.classificacao() == ClassificacaoDeserto.DESERTO_TOTAL && c.populacao() > 0) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.JUIZADO_ITINERANTE,
                        c.nomeMunicipio(),
                        c.uf(),
                        "Municipio sem estrutura instalada exige cobertura movel e digital imediata.",
                        "Modelo de justica itinerante para cobertura emergencial de vazios territoriais.",
                        PrioridadeAcao.URGENTE,
                        c.populacao()
                ));
            }
            if (c.indiceCongestionamento() >= 0.80d) {
                recomendacoes.add(new RecomendacaoPolitica(
                        TipoRecomendacao.MUTIRAO_CONCILIACAO,
                        c.nomeMunicipio(),
                        c.uf(),
                        "Congestionamento elevado compromete acesso tempestivo e induz demora estrutural.",
                        "Mutiroes de conciliacao e triagem para reduzir estoque e tempo de espera.",
                        PrioridadeAcao.ALTA,
                        c.populacao()
                ));
            }
        }
        return recomendacoes.stream()
                .sorted(Comparator.comparing(RecomendacaoPolitica::prioridade).thenComparingInt(r -> -r.populacaoBeneficiada()))
                .distinct()
                .toList();
    }

    private CelulaAtlas map(AtlasAcessoMunicipio entity) {
        IndiceAcesso indice = new IndiceAcesso(
                entity.getScoreInfraestrutura().doubleValue(),
                entity.getScoreRepresentacao().doubleValue(),
                entity.getScoreCeleridade().doubleValue(),
                entity.getScoreEfetividade().doubleValue(),
                entity.getScoreTotal().doubleValue(),
                entity.getGrau()
        );
        return new CelulaAtlas(
                entity.getCodigoIbge(),
                entity.getNomeMunicipio(),
                entity.getUf(),
                entity.getRegiao(),
                safeInt(entity.getPopulacao()),
                safeInt(entity.getVarasInstaladas()),
                safeInt(entity.getJuizesEmExercicio()),
                safeInt(entity.getDefensoriasPorMunicipio()),
                safeInt(entity.getAdvogadosOabAtivos()),
                Boolean.TRUE.equals(entity.getTemJuizadoEspecial()),
                Boolean.TRUE.equals(entity.getTemCejusc()),
                safeInt(entity.getProcessosPorMilHabitantes()),
                safeInt(entity.getNovosProcessosMes()),
                bd(entity.getTaxaResolutividadePct()),
                bd(entity.getTempoMedioResolucaoDias()),
                bd(entity.getIndiceCongestionamento()),
                bd(entity.getTaxaJusticaGratuitaPct()),
                bd(entity.getTaxaAutoRepresentacaoPct()),
                bd(entity.getTaxaPrescricaoAparentePct()),
                indice,
                fromEntity(entity.getClassificacao()),
                entity.getAtualizadoEm()
        );
    }

    private void registrarAuditoriaEEvento(CelulaAtlas celula, String acao) {
        String payloadHash = Hashes.sha256Hex(celula.codigoIbge() + celula.uf() + celula.indiceAcesso().scoreTotal() + celula.atualizadoEm());
        auditLedgerService.appendSafely(acao, "ATLAS", celula.codigoIbge(), payloadHash);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigoIbge", celula.codigoIbge());
        payload.put("uf", celula.uf());
        payload.put("score", celula.indiceAcesso().scoreTotal());
        payload.put("classificacao", celula.classificacao().name());
        if (celula.atualizadoEm() != null) {
            payload.put("atualizadoEm", celula.atualizadoEm().toString());
        }
        outboxPublisher.enqueue(
                "atlas:celula:" + celula.codigoIbge(),
                EVT_ATLAS_CELULA_ATUALIZADA,
                Map.copyOf(payload),
                Map.of("source", "atlas_acesso_justica"),
                "atlasCelula:" + payloadHash,
                "AtlasCelula",
                celula.codigoIbge()
        );
    }

    private IndiceAcesso calcularIndice(int populacao,
                                        int varas,
                                        int juizes,
                                        int defensorias,
                                        int advogados,
                                        boolean temJuizado,
                                        boolean temCejusc,
                                        double taxaResolutividade,
                                        double tempoMedio,
                                        double congestionamento,
                                        double taxaJg,
                                        double autoRepresentacao,
                                        double taxaPrescricao) {
        double advogadosPorMil = populacao > 0 ? (advogados * 1000.0d) / populacao : 0.0d;
        double juizesPorCemMil = populacao > 0 ? (juizes * 100000.0d) / populacao : 0.0d;
        double scoreInfraestrutura = 0.0d;
        scoreInfraestrutura += clamp((varas * 7.0d), 0.0d, 10.0d);
        scoreInfraestrutura += clamp((juizesPorCemMil / 10.0d) * 7.0d, 0.0d, 7.0d);
        scoreInfraestrutura += clamp(defensorias * 5.0d, 0.0d, 5.0d);
        scoreInfraestrutura += temJuizado ? 1.5d : 0.0d;
        scoreInfraestrutura += temCejusc ? 1.5d : 0.0d;

        double scoreRepresentacao = 0.0d;
        scoreRepresentacao += clamp((advogadosPorMil / 4.0d) * 12.0d, 0.0d, 12.0d);
        scoreRepresentacao += clamp(10.0d - (autoRepresentacao / 5.0d), 0.0d, 10.0d);
        scoreRepresentacao += clamp((100.0d - Math.abs(taxaJg - 40.0d)) / 100.0d * 3.0d, 0.0d, 3.0d);

        double scoreCeleridade = 0.0d;
        scoreCeleridade += clamp((1.0d - congestionamento) * 15.0d, 0.0d, 15.0d);
        scoreCeleridade += clamp((730.0d - Math.min(730.0d, tempoMedio)) / 730.0d * 10.0d, 0.0d, 10.0d);

        double scoreEfetividade = 0.0d;
        scoreEfetividade += clamp((taxaResolutividade / 100.0d) * 16.0d, 0.0d, 16.0d);
        scoreEfetividade += clamp((100.0d - Math.min(100.0d, taxaPrescricao)) / 100.0d * 9.0d, 0.0d, 9.0d);
        return IndiceAcesso.calcular(scoreInfraestrutura, scoreRepresentacao, scoreCeleridade, scoreEfetividade);
    }

    private ClassificacaoDeserto classificar(int populacao,
                                             int varas,
                                             int juizes,
                                             int defensorias,
                                             int advogados,
                                             boolean temJuizado,
                                             boolean temCejusc,
                                             double scoreTotal,
                                             double taxaPrescricao,
                                             double autoRepresentacao) {
        if (varas == 0 && juizes == 0 && defensorias == 0 && advogados == 0 && !temJuizado && !temCejusc) {
            return ClassificacaoDeserto.DESERTO_TOTAL;
        }
        if ((varas == 0 || juizes == 0) && (defensorias == 0 || advogados == 0)) {
            return ClassificacaoDeserto.DESERTO_PARCIAL;
        }
        if (scoreTotal < 35.0d || taxaPrescricao >= 20.0d || autoRepresentacao >= 35.0d) {
            return ClassificacaoDeserto.PRECARIO;
        }
        if (scoreTotal < 50.0d) {
            return ClassificacaoDeserto.PARCIAL;
        }
        if (scoreTotal < 75.0d) {
            return ClassificacaoDeserto.ADEQUADO;
        }
        return populacao > 0 || varas > 0 || juizes > 0 ? ClassificacaoDeserto.PLENO : ClassificacaoDeserto.ADEQUADO;
    }

    private PainelBaseUf carregarPainelBasePorUf(String uf) {
        List<PainelTribunalMetrica> todas = painelMetricas();
        List<PainelTribunalMetrica> metricas = todas.stream()
                .filter(m -> uf.equalsIgnoreCase(Objects.requireNonNullElse(m.getUf(), "")))
                .toList();
        Collection<PainelTribunalMetrica> base = metricas.isEmpty() ? todas : metricas;
        double tempo = base.stream().map(PainelTribunalMetrica::getTempoMedioResolucaoDias).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(540.0d);
        double congestionamento = base.stream().map(PainelTribunalMetrica::getIndiceCongestionamento).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0.55d);
        double taxaResolutividade = base.stream()
                .mapToDouble(m -> percentualResolutividade(m.getAjuizadosMes(), m.getSentenciadosMes() + m.getArquivadosMes()))
                .average()
                .orElse(52.0d);
        double taxaJg = 35.0d;
        double taxaAutoRepresentacao = base.stream().mapToLong(PainelTribunalMetrica::getAjuizadosMes).average().orElse(0.0d) > 2500.0d ? 18.0d : 12.0d;
        double taxaPrescricao = base.stream().mapToLong(PainelTribunalMetrica::getProcessosComPrazoExcedido).average().orElse(0.0d) > 500.0d ? 14.0d : 8.0d;
        return new PainelBaseUf(round2(taxaResolutividade), round2(tempo), round4(congestionamento), round2(taxaJg), round2(taxaAutoRepresentacao), round2(taxaPrescricao));
    }

    private record PainelBaseUf(
            double taxaResolutividadePct,
            double tempoMedioResolucaoDias,
            double indiceCongestionamento,
            double taxaJusticaGratuitaPct,
            double taxaAutoRepresentacaoPct,
            double taxaPrescricaoAparentePct
    ) {
    }


    private List<CelulaAtlas> atlasCells() {
        CachedAtlasCells cache = atlasCellsCache.get();
        if (isFresh(cache)) {
            return cache.cells();
        }
        List<CelulaAtlas> loaded = atlasRepository.findAll().stream().map(this::map).toList();
        atlasCellsCache.set(new CachedAtlasCells(loaded, Instant.now().plus(ATLAS_CACHE_TTL)));
        return loaded;
    }

    private List<PainelTribunalMetrica> painelMetricas() {
        CachedPainelMetricas cache = painelMetricasCache.get();
        if (isFresh(cache)) {
            return cache.metricas();
        }
        List<PainelTribunalMetrica> loaded = painelTribunalRepository.findAll();
        painelMetricasCache.set(new CachedPainelMetricas(loaded, Instant.now().plus(PAINEL_BASE_CACHE_TTL)));
        return loaded;
    }

    private void invalidateCaches() {
        atlasCellsCache.set(null);
        painelMetricasCache.set(null);
    }

    private boolean isFresh(CachedAtlasCells cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private boolean isFresh(CachedPainelMetricas cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private static double percentualResolutividade(long ajuizados, long baixados) {
        if (ajuizados <= 0L) {
            return 50.0d;
        }
        return clamp((baixados * 100.0d) / ajuizados, 0.0d, 100.0d);
    }

    private static ClassificacaoDesertoAtlas toEntity(ClassificacaoDeserto value) {
        return ClassificacaoDesertoAtlas.valueOf(value.name());
    }

    private static ClassificacaoDeserto fromEntity(ClassificacaoDesertoAtlas value) {
        return ClassificacaoDeserto.valueOf(value.name());
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private static double bd(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private static double pct(BigDecimal value, double fallback) {
        return clamp(value == null ? fallback : value.doubleValue(), 0.0d, 100.0d);
    }

    private static double ratio(BigDecimal value, double fallback) {
        return clamp(value == null ? fallback : value.doubleValue(), 0.0d, 1.0d);
    }

    private static double nonNegative(BigDecimal value, double fallback) {
        return value == null ? fallback : Math.max(0.0d, value.doubleValue());
    }

    private static int positive(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private static int nonNegative(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String normalizeCodigoIbge(String codigoIbge) {
        String normalized = Objects.requireNonNullElse(codigoIbge, "").replaceAll("\\D", "");
        if (normalized.length() != 7) {
            throw new IllegalArgumentException("Codigo IBGE invalido: " + codigoIbge);
        }
        return normalized;
    }

    private static String resolveNomeMunicipio(String nome, Municipios municipio, String codigoIbge) {
        if (nome != null && !nome.isBlank()) {
            return normalizeText(nome, "Municipio " + codigoIbge);
        }
        if (municipio != null && municipio.getNome() != null && !municipio.getNome().isBlank()) {
            return normalizeText(municipio.getNome(), "Municipio " + codigoIbge);
        }
        return "Municipio " + codigoIbge;
    }

    private static String resolveUf(String uf, Municipios municipio) {
        String source = uf;
        if ((source == null || source.isBlank()) && municipio != null) {
            source = municipio.getUf();
        }
        return normalizeUf(source);
    }

    private static String normalizeUf(String uf) {
        String normalized = Objects.requireNonNullElse(uf, "").strip().toUpperCase(Locale.ROOT);
        if (normalized.length() != 2) {
            throw new IllegalArgumentException("UF invalida: " + uf);
        }
        return normalized;
    }

    private static String resolveRegiao(String regiao, String uf) {
        if (regiao != null && !regiao.isBlank()) {
            return normalizeText(regiao, "NACIONAL").toUpperCase(Locale.ROOT);
        }
        return switch (normalizeUf(uf)) {
            case "AC", "AM", "AP", "PA", "RO", "RR", "TO" -> "NORTE";
            case "AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE" -> "NORDESTE";
            case "DF", "GO", "MS", "MT" -> "CENTRO-OESTE";
            case "ES", "MG", "RJ", "SP" -> "SUDESTE";
            default -> "SUL";
        };
    }

    private static String normalizeText(String value, String fallback) {
        String normalized = value == null ? null : value.trim().replaceAll("\\s+", " ");
        return normalized == null || normalized.isBlank() ? fallback : normalized;
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record CachedAtlasCells(List<CelulaAtlas> cells, Instant expiresAt) {
    }

    private record CachedPainelMetricas(List<PainelTribunalMetrica> metricas, Instant expiresAt) {
    }
}

