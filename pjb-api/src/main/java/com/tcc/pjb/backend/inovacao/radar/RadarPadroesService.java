package com.tcc.pjb.backend.inovacao.radar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponse;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.radar.RadarPadraoAlerta;
import com.tcc.pjb.backend.model.entity.radar.RadarPadraoAnalise;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RadarPadraoAlertaRepository;
import com.tcc.pjb.backend.model.repository.RadarPadraoAnaliseRepository;
import com.tcc.pjb.backend.service.exception.ErroDeTetoException;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;

@Service
public class RadarPadroesService {

    public static final String EVT_RADAR_ANALISADO = "pjb.inovacao.radar.analisado";
    private static final Logger log = LoggerFactory.getLogger(RadarPadroesService.class);
    private static final Set<String> JARGAO = Set.of(
            "autor", "reu", "requer", "requerente", "contestacao", "sentenca", "acordao", "liminar",
            "obrigacao", "indenizacao", "consumidor", "execucao", "condenacao", "citacao", "tutela",
            "juizado", "competencia", "sucumbencia", "prescricao", "decadencia", "peticao", "inicial"
    );
    private static final java.time.Duration RADAR_READ_BUDGET = java.time.Duration.ofSeconds(4);
    private static final java.time.Duration RADAR_WRITE_BUDGET = java.time.Duration.ofSeconds(6);

    private final ProcessoRepository processoRepository;
    private final RadarPadraoAnaliseRepository analiseRepository;
    private final RadarPadraoAlertaRepository alertaRepository;
    private final DocumentoNacionalValidator documentoValidator;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final FinanceiraAiVersionSelector financeiraAiVersionSelector;
    private final ObjectMapper objectMapper;
    private final TetoProcessualService tetoProcessualService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public RadarPadroesService(ProcessoRepository processoRepository,
                               RadarPadraoAnaliseRepository analiseRepository,
                               RadarPadraoAlertaRepository alertaRepository,
                               DocumentoNacionalValidator documentoValidator,
                               AuditLedgerService auditLedgerService,
                               OutboxPublisher outboxPublisher,
                               FinanceiraAiVersionSelector financeiraAiVersionSelector,
                               ObjectMapper objectMapper,
                               TetoProcessualService tetoProcessualService,
                               ProceduralCanonicalResolver proceduralCanonicalResolver,
                               PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.analiseRepository = Objects.requireNonNull(analiseRepository);
        this.alertaRepository = Objects.requireNonNull(alertaRepository);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.financeiraAiVersionSelector = Objects.requireNonNull(financeiraAiVersionSelector);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport);
    }

    public enum TipoPadrao {
        FABRICA_PROCESSOS,
        LITIGANCIA_PREDATORIA,
        DOCUMENTO_REUTILIZADO,
        IDENTIDADE_SUSPEITA,
        POLO_ATIVO_SERIAL,
        POLO_PASSIVO_ALVO,
        VALOR_CAUSA_MANIPULADO,
        PETICIONAMENTO_AUTOMATIZADO
    }

    public enum NivelAlerta {
        INFORMATIVO,
        ATENCAO,
        RELEVANTE,
        CRITICO
    }

    public record AlertaRadar(
            UUID alertaId,
            String nupn,
            TipoPadrao tipoPadrao,
            NivelAlerta nivel,
            double score,
            String descricaoTecnica,
            String evidenciasObjetivas,
            String orientacaoMagistrado,
            boolean processoNaoBloqueado,
            String referenciaTeto,
            String explicacaoFinanceiraIa,
            Instant detectadoEm,
            List<String> nupnsRelacionados
    ) {
        public AlertaRadar {
            if (!processoNaoBloqueado) {
                throw new IllegalStateException("Radar nao pode bloquear processo");
            }
            nupnsRelacionados = nupnsRelacionados == null ? List.of() : List.copyOf(nupnsRelacionados);
        }
    }

    public record FingerprintPeticao(
            String nupn,
            String hashEstrutura,
            String hashConteudoMinHash,
            int numeroParagrafos,
            int totalPalavras,
            double densidadeJargaoJuridico,
            double diversidadeLexica,
            String escritorioOab,
            LocalDate dataAjuizamento
    ) {
    }

    public record PerfilLitigante(
            String documento,
            int totalProcessos365d,
            int totalProcessos30d,
            double taxaAcordo,
            double taxaDesistencia,
            double tempoMedioAcordoDias,
            Map<String, Integer> distribRamosDireito,
            Map<String, Integer> distribReus,
            LocalDate primeiroProcesso,
            boolean flagLitiganteSerial
    ) {
        public boolean ehLitiganteSerial() {
            return flagLitiganteSerial || totalProcessos365d > 50 || totalProcessos30d > 12;
        }

        public boolean padraoAcordoPredatorio() {
            return taxaAcordo >= 0.65 && tempoMedioAcordoDias > 0 && tempoMedioAcordoDias < 90;
        }
    }

    public record ContextoRadar(
            Long processoId,
            String nupn,
            String documentoAutor,
            String documentoReu,
            String escritorioOab,
            String tribunalCodigo,
            String ramoDireito,
            String classeProcessual,
            String assunto,
            BigDecimal valorCausa,
            String resumoFatos,
            LocalDate dataAjuizamento,
            String statusProcesso,
            String resultadoFinal,
            FingerprintPeticao fingerprint
    ) {
    }

    public record AnaliseRadarResultado(
            String nupn,
            Long processoId,
            FingerprintPeticao fingerprint,
            PerfilLitigante perfilAutor,
            PerfilLitigante perfilReu,
            List<AlertaRadar> alertas,
            String resumoTecnico,
            double scoreGeral,
            Instant analisadoEm
    ) {
        public boolean temCritico() {
            return alertas.stream().anyMatch(a -> a.nivel() == NivelAlerta.CRITICO);
        }
    }

    public AnaliseRadarResultado analisarERegistrarProcesso(Long processoId) {
        Processo processo = transactionalExecutionSupport.executeReadOnly(
                "radar.analysis.load-process",
                RADAR_READ_BUDGET,
                () -> processoRepository.findProcessoCompletoById(processoId)
                        .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado: " + processoId))
        );
        return analisarERegistrar(processo);
    }

    public AnaliseRadarResultado analisarERegistrar(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        ContextoRadar contexto = transactionalExecutionSupport.executeReadOnly(
                "radar.analysis.build-context",
                RADAR_READ_BUDGET,
                () -> construirContexto(processo)
        );
        AnaliseRadarResultado resultado = analisar(contexto);
        Long processoId = processo.getId() != null ? processo.getId() : contexto.processoId();
        transactionalExecutionSupport.executeInNewTransaction(
                "radar.analysis.persist",
                RADAR_WRITE_BUDGET,
                () -> persistir(contexto, processoId, resultado)
        );
        publicarEventos(contexto, resultado);
        return resultado;
    }

    public AnaliseRadarResultado analisarERegistrar(ContextoRadar contexto) {
        Objects.requireNonNull(contexto, "contexto");
        AnaliseRadarResultado resultado = analisar(contexto);
        transactionalExecutionSupport.executeInNewTransaction(
                "radar.analysis.persist",
                RADAR_WRITE_BUDGET,
                () -> persistir(contexto, contexto.processoId(), resultado)
        );
        publicarEventos(contexto, resultado);
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<AnaliseRadarResultado> buscarUltimoPorProcesso(Long processoId) {
        return analiseRepository.findTopByProcessoIdOrderByGeradoEmDesc(processoId).map(this::hydrate);
    }

    @Transactional(readOnly = true)
    public List<AlertaRadar> alertasDoProcesso(Long processoId) {
        return alertaRepository.findTop100ByProcessoIdOrderByDetectadoEmDesc(processoId).stream()
                .map(this::mapAlert)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaRadar> alertasPorNupn(String nupn) {
        return alertaRepository.findTop100ByNupnOrderByDetectadoEmDesc(nupn).stream()
                .map(this::mapAlert)
                .toList();
    }

    public AnaliseRadarResultado analisar(ContextoRadar contexto) {
        Objects.requireNonNull(contexto, "contexto");
        FingerprintPeticao fingerprint = contexto.fingerprint() != null ? contexto.fingerprint() : construirFingerprint(contexto);
        PerfilLitigante perfilAutor = construirPerfil(contexto.documentoAutor(), true);
        PerfilLitigante perfilReu = construirPerfil(contexto.documentoReu(), false);
        List<AlertaRadar> alertas = new ArrayList<>();

        detectarIdentidadeSuspeita(contexto).ifPresent(alertas::add);
        detectarFabricaProcessos(contexto, fingerprint).ifPresent(alertas::add);
        detectarPeticionamentoAutomatizado(contexto, fingerprint).ifPresent(alertas::add);
        detectarDocumentoReutilizado(contexto, fingerprint).ifPresent(alertas::add);
        detectarLitiganteSerial(contexto, perfilAutor).ifPresent(alertas::add);
        detectarPoloPassivoAlvo(contexto, perfilReu).ifPresent(alertas::add);
        detectarLitiganciaPredatoria(contexto, perfilAutor).ifPresent(alertas::add);
        detectarManipulacaoValorCausa(contexto, perfilAutor).ifPresent(alertas::add);

        List<AlertaRadar> ordenados = alertas.stream()
                .sorted(Comparator.comparing((AlertaRadar a) -> a.nivel().ordinal()).reversed().thenComparing(AlertaRadar::score).reversed())
                .toList();

        double score = ordenados.stream().mapToDouble(AlertaRadar::score).max().orElse(0.0);
        String resumo = construirResumo(contexto, ordenados, score);
        return new AnaliseRadarResultado(
                contexto.nupn(),
                contexto.processoId(),
                fingerprint,
                perfilAutor,
                perfilReu,
                ordenados,
                resumo,
                score,
                Instant.now()
        );
    }

    public ContextoRadar construirContexto(Processo processo) {
        String nupn = processo.getNumeroUnificado() != null ? processo.getNumeroUnificado() : processo.getNumeroProcesso();
        String tribunalCodigo = processo.getJurisdicao() != null ? processo.getJurisdicao().getSigla() : null;
        String ramo = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null;
        String escritorio = resolverEscritorio(processo.getUsuario());
        String resumo = Optional.ofNullable(processo.getResumoIA()).orElse("");
        LocalDate dataAjuizamento = toLocalDate(processo.getDataCriacao());
        return new ContextoRadar(
                processo.getId(),
                nupn,
                normalizarDocumentoSeguro(processo.getParteAutoraCpf()),
                normalizarDocumentoSeguro(processo.getParteReuCpf()),
                escritorio,
                tribunalCodigo,
                ramo,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getValorCausa(),
                resumo,
                dataAjuizamento,
                processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                processo.getResultadoFinal(),
                null
        );
    }

    public FingerprintPeticao construirFingerprint(ContextoRadar contexto) {
        String texto = Optional.ofNullable(contexto.resumoFatos()).orElse("");
        List<String> tokens = TextTokenUtils.orderedTokens(texto);
        int palavras = tokens.size();
        int paragrafos = contarParagrafos(texto);
        double densidadeJargao = calcularDensidadeJargao(tokens);
        double diversidadeLexica = palavras == 0 ? 0.0 : round(tokens.stream().distinct().count() / (double) palavras, 6);
        String estruturaBase = String.join("|",
                safe(contexto.ramoDireito()),
                safe(contexto.classeProcessual()),
                safe(contexto.assunto()),
                String.valueOf(bucket(paragrafos, 3)),
                String.valueOf(bucket(palavras, 25)),
                String.format(Locale.ROOT, "%.2f", densidadeJargao)
        );
        String conteudoBase = String.join("|", tokens.stream().limit(64).toList());
        return new FingerprintPeticao(
                contexto.nupn(),
                Hashes.sha256Hex(estruturaBase),
                Hashes.sha256Hex(conteudoBase),
                paragrafos,
                palavras,
                densidadeJargao,
                diversidadeLexica,
                safe(contexto.escritorioOab()),
                contexto.dataAjuizamento()
        );
    }

    private PerfilLitigante construirPerfil(String documento, boolean autor) {
        String doc = normalizarDocumentoSeguro(documento);
        if (doc.isBlank()) {
            return new PerfilLitigante("", 0, 0, 0.0, 0.0, 0.0, Map.of(), Map.of(), null, false);
        }
        List<Processo> processos = processoRepository.findAllByPartesCpf(doc).stream()
                .filter(p -> autor ? doc.equals(normalizarDocumentoSeguro(p.getParteAutoraCpf())) : doc.equals(normalizarDocumentoSeguro(p.getParteReuCpf())))
                .toList();
        LocalDate hoje = LocalDate.now();
        int total365 = 0;
        int total30 = 0;
        int encerrados = 0;
        int acordos = 0;
        int desistencia = 0;
        long somaDiasAcordo = 0L;
        LocalDate primeiro = null;
        Map<String, Integer> ramos = new LinkedHashMap<>();
        Map<String, Integer> contraparte = new LinkedHashMap<>();
        for (Processo processo : processos) {
            LocalDate data = toLocalDate(processo.getDataCriacao());
            if (data == null) {
                data = hoje;
            }
            long dias = ChronoUnit.DAYS.between(data, hoje);
            if (dias <= 365) {
                total365++;
            }
            if (dias <= 30) {
                total30++;
            }
            if (primeiro == null || data.isBefore(primeiro)) {
                primeiro = data;
            }
            String ramo = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "INDETERMINADO";
            ramos.merge(ramo, 1, Integer::sum);
            String outro = autor ? normalizarDocumentoSeguro(processo.getParteReuCpf()) : normalizarDocumentoSeguro(processo.getParteAutoraCpf());
            contraparte.merge(outro.isBlank() ? "SEM_DOCUMENTO" : outro, 1, Integer::sum);
            boolean encerrado = processo.getStatusProcesso() == StatusProcesso.ARQUIVADO || isEncerradoPorTexto(processo.getResultadoFinal());
            if (encerrado) {
                encerrados++;
                if (isAcordo(processo.getResultadoFinal())) {
                    acordos++;
                    somaDiasAcordo += Math.max(0, ChronoUnit.DAYS.between(data, resolveFim(processo)));
                }
                if (isDesistencia(processo.getResultadoFinal())) {
                    desistencia++;
                }
            }
        }
        double taxaAcordo = encerrados == 0 ? 0.0 : round(acordos / (double) encerrados, 6);
        double taxaDesistencia = encerrados == 0 ? 0.0 : round(desistencia / (double) encerrados, 6);
        double tempoAcordo = acordos == 0 ? 0.0 : round(somaDiasAcordo / (double) acordos, 2);
        return new PerfilLitigante(
                doc,
                total365,
                total30,
                taxaAcordo,
                taxaDesistencia,
                tempoAcordo,
                Collections.unmodifiableMap(ramos),
                Collections.unmodifiableMap(topMap(contraparte, 12)),
                primeiro,
                total365 > 50 || total30 > 12
        );
    }

    private Optional<AlertaRadar> detectarIdentidadeSuspeita(ContextoRadar contexto) {
        List<String> evidencias = new ArrayList<>();
        String autor = normalizarDocumentoSeguro(contexto.documentoAutor());
        String reu = normalizarDocumentoSeguro(contexto.documentoReu());
        if (!autor.isBlank() && !documentoValido(autor)) {
            evidencias.add("documento_autor_invalido");
        }
        if (!reu.isBlank() && !documentoValido(reu)) {
            evidencias.add("documento_reu_invalido");
        }
        if (!autor.isBlank() && autor.equals(reu)) {
            evidencias.add("mesmo_documento_nos_dois_polos");
        }
        if (evidencias.isEmpty()) {
            return Optional.empty();
        }
        NivelAlerta nivel = evidencias.size() > 1 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.IDENTIDADE_SUSPEITA,
                nivel,
                nivel == NivelAlerta.RELEVANTE ? 0.79 : 0.61,
                "Estrutura documental do processo apresenta incongruencia objetiva nos polos processuais.",
                String.join("; ", evidencias),
                "Verificar identificacao das partes e eventual necessidade de emenda, saneamento ou certificacao cartoraria.",
                null,
                null,
                List.of()
        ));
    }

    private Optional<AlertaRadar> detectarFabricaProcessos(ContextoRadar contexto, FingerprintPeticao fingerprint) {
        String escritorioHash = hashOrBlank(contexto.escritorioOab());
        if (escritorioHash.isBlank()) {
            return Optional.empty();
        }
        LocalDate limite = LocalDate.now().minusDays(30);
        List<RadarPadraoAnalise> recentes = analiseRepository.findTop200ByEscritorioOabHashOrderByGeradoEmDesc(escritorioHash).stream()
                .filter(a -> a.getDataAjuizamento() == null || !a.getDataAjuizamento().isBefore(limite))
                .toList();
        long similares = recentes.stream()
                .filter(a -> Objects.equals(a.getFingerprintEstruturaHash(), fingerprint.hashEstrutura()))
                .count();
        if (similares < 8) {
            return Optional.empty();
        }
        NivelAlerta nivel = similares >= 30 ? NivelAlerta.CRITICO : similares >= 15 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        List<String> relacionados = recentes.stream()
                .filter(a -> Objects.equals(a.getFingerprintEstruturaHash(), fingerprint.hashEstrutura()))
                .map(RadarPadraoAnalise::getNupn)
                .filter(Objects::nonNull)
                .distinct()
                .limit(12)
                .toList();
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.FABRICA_PROCESSOS,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.96 : 0.84,
                "Padrao reiterado de ajuizamento estruturalmente homogeneo associado ao mesmo escritorio.",
                "similares_30d=" + similares + "; fingerprint=" + fingerprint.hashEstrutura(),
                "Verificar se os casos possuem individualizacao fatico-probatoria suficiente e se ha necessidade de gestao de repetitivos ou reuniao processual.",
                null,
                null,
                relacionados
        ));
    }

    private Optional<AlertaRadar> detectarDocumentoReutilizado(ContextoRadar contexto, FingerprintPeticao fingerprint) {
        List<RadarPadraoAnalise> semelhantes = analiseRepository.findTop200ByFingerprintConteudoHashOrderByGeradoEmDesc(fingerprint.hashConteudoMinHash()).stream()
                .filter(a -> !Objects.equals(a.getNupn(), contexto.nupn()))
                .toList();
        long divergentes = semelhantes.stream()
                .filter(a -> !Objects.equals(a.getDocumentoAutorHash(), hashOrBlank(contexto.documentoAutor()))
                        || !Objects.equals(a.getDocumentoReuHash(), hashOrBlank(contexto.documentoReu())))
                .count();
        if (divergentes < 4) {
            return Optional.empty();
        }
        NivelAlerta nivel = divergentes >= 20 ? NivelAlerta.CRITICO : divergentes >= 10 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.DOCUMENTO_REUTILIZADO,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.93 : 0.78,
                "Nucleo textual-fatico reaparece em varios processos com combinacoes distintas de partes.",
                "hash_conteudo=" + fingerprint.hashConteudoMinHash() + "; divergentes=" + divergentes,
                "Conferir anexos, cadeia de custodia documental e aderencia do conjunto probatorio ao caso concreto.",
                null,
                null,
                semelhantes.stream().map(RadarPadraoAnalise::getNupn).filter(Objects::nonNull).distinct().limit(10).toList()
        ));
    }

    private Optional<AlertaRadar> detectarLitiganteSerial(ContextoRadar contexto, PerfilLitigante perfil) {
        if (!perfil.ehLitiganteSerial()) {
            return Optional.empty();
        }
        NivelAlerta nivel = perfil.totalProcessos365d() > 250 ? NivelAlerta.CRITICO : perfil.totalProcessos365d() > 120 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.POLO_ATIVO_SERIAL,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.97 : 0.82,
                "Volume de ajuizamentos do polo ativo supera padrao ordinario para a janela temporal analisada.",
                "total_365d=" + perfil.totalProcessos365d() + "; total_30d=" + perfil.totalProcessos30d() + "; ramos=" + perfil.distribRamosDireito(),
                "Verificar compatibilidade do volume com atividade economica legitima, representacao massificada e eventual gestao de litigancia repetitiva.",
                null,
                null,
                List.of()
        ));
    }

    private Optional<AlertaRadar> detectarPoloPassivoAlvo(ContextoRadar contexto, PerfilLitigante perfilReu) {
        if (perfilReu == null || perfilReu.documento().isBlank()) {
            return Optional.empty();
        }
        List<RadarPadraoAnalise> recentes = analiseRepository.findTop200ByDocumentoReuHashOrderByGeradoEmDesc(hashOrBlank(contexto.documentoReu()));
        long alvo = recentes.stream().filter(a -> sameWindow(a.getDataAjuizamento(), 45)).count();
        if (alvo < 15) {
            return Optional.empty();
        }
        NivelAlerta nivel = alvo >= 80 ? NivelAlerta.CRITICO : alvo >= 35 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.POLO_PASSIVO_ALVO,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.95 : 0.76,
                "Polo passivo concentra volume anormal de demandas similares em janela curta.",
                "acoes_45d=" + alvo + "; documento_reu_hash=" + hashOrBlank(contexto.documentoReu()),
                "Verificar estrategia coordenada, eventual repeticao massiva e necessidade de tratamento concentrado ou prevencao de decisoes contraditorias.",
                null,
                null,
                recentes.stream().map(RadarPadraoAnalise::getNupn).filter(Objects::nonNull).distinct().limit(10).toList()
        ));
    }

    private Optional<AlertaRadar> detectarLitiganciaPredatoria(ContextoRadar contexto, PerfilLitigante perfil) {
        if (perfil == null || !perfil.padraoAcordoPredatorio()) {
            return Optional.empty();
        }
        NivelAlerta nivel = perfil.taxaDesistencia() > 0.30 ? NivelAlerta.CRITICO : NivelAlerta.RELEVANTE;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.LITIGANCIA_PREDATORIA,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.94 : 0.81,
                "Historico estatistico do litigante aponta combinacao intensa de acordos acelerados e elevada taxa de desistencias ou encerramentos precoces.",
                "taxa_acordo=" + pct(perfil.taxaAcordo()) + "; tempo_medio_acordo_dias=" + round(perfil.tempoMedioAcordoDias(), 2) + "; taxa_desistencia=" + pct(perfil.taxaDesistencia()),
                "Examinar se a dinamica do caso concreto exige cautela reforcada em homologacoes e controle de boa-fe processual.",
                null,
                null,
                List.of()
        ));
    }

    private Optional<AlertaRadar> detectarPeticionamentoAutomatizado(ContextoRadar contexto, FingerprintPeticao fingerprint) {
        String escritorioHash = hashOrBlank(contexto.escritorioOab());
        if (escritorioHash.isBlank()) {
            return Optional.empty();
        }
        LocalDate ref = contexto.dataAjuizamento() != null ? contexto.dataAjuizamento() : LocalDate.now();
        List<RadarPadraoAnalise> recentes = analiseRepository.findTop200ByEscritorioOabHashOrderByGeradoEmDesc(escritorioHash).stream()
                .filter(a -> Objects.equals(a.getDataAjuizamento(), ref))
                .toList();
        long similaresDia = recentes.stream()
                .filter(a -> Objects.equals(a.getFingerprintEstruturaHash(), fingerprint.hashEstrutura()))
                .count();
        boolean diversidadeBaixa = fingerprint.diversidadeLexica() > 0 && fingerprint.diversidadeLexica() < 0.42;
        if (similaresDia < 6 && !diversidadeBaixa) {
            return Optional.empty();
        }
        NivelAlerta nivel = similaresDia >= 20 || (diversidadeBaixa && similaresDia >= 10) ? NivelAlerta.CRITICO : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.PETICIONAMENTO_AUTOMATIZADO,
                nivel,
                nivel == NivelAlerta.CRITICO ? 0.91 : 0.68,
                "A combinacao de homogeneidade estrutural, baixa diversidade lexical e alto volume diario sugere geracao industrial ou altamente automatizada de peticoes.",
                "similares_mesma_data=" + similaresDia + "; diversidade_lexica=" + round(fingerprint.diversidadeLexica(), 4) + "; palavras=" + fingerprint.totalPalavras(),
                "Avaliar se a individualizacao do caso concreto esta preservada e se o impulso processual manteve aderencia ao conjunto fatico especifico.",
                null,
                null,
                recentes.stream().map(RadarPadraoAnalise::getNupn).filter(Objects::nonNull).distinct().limit(10).toList()
        ));
    }

    private Optional<AlertaRadar> detectarManipulacaoValorCausa(ContextoRadar contexto, PerfilLitigante perfilAutor) {
        BigDecimal valor = contexto.valorCausa();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        TetoDiagnostico teto = diagnosticarTeto(contexto);
        if (!teto.suspeito()) {
            return Optional.empty();
        }
        FinancialAiResponse ia = consultarIaFinanceira(contexto, teto);
        String explicacaoIa = ia != null ? ia.message() : null;
        ErroDeTetoException parecerTeto = montarParecerTeto(contexto, teto);
        NivelAlerta nivel = teto.repeticaoEscritorio() >= 5 || teto.violacaoPercentual().compareTo(new BigDecimal("0.08")) <= 0 ? NivelAlerta.RELEVANTE : NivelAlerta.ATENCAO;
        return Optional.of(novoAlerta(
                contexto.nupn(),
                TipoPadrao.VALOR_CAUSA_MANIPULADO,
                nivel,
                nivel == NivelAlerta.RELEVANTE ? 0.88 : 0.72,
                "Valor da causa gravita muito proximo do limiar de competencia ou alçada aplicavel para o rito sugerido.",
                "limite=" + teto.limite() + "; valor=" + valor + "; margem=" + teto.margemAteLimite() + "; repeticoes_escritorio=" + teto.repeticaoEscritorio() + "; parecer_teto=" + parecerTeto.getProvaIntegridade(),
                "Conferir memoria de calculo do valor da causa, documentos economicos e aderencia ao rito escolhido antes do impulso inicial relevante.",
                teto.tipoViolacao() != null ? teto.tipoViolacao().name() : null,
                (explicacaoIa == null ? "" : explicacaoIa + "\n\n") + parecerTeto.getMessage(),
                List.of()
        ));
    }

    private TetoDiagnostico diagnosticarTeto(ContextoRadar contexto) {
        BigDecimal valor = contexto.valorCausa() == null ? BigDecimal.ZERO : contexto.valorCausa();
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = tetoProcessualService.diagnosticar(
                valor,
                null,
                parseRamo(contexto.ramoDireito()),
                parseRito(contexto.classeProcessual(), contexto.assunto()),
                null,
                LocalDate.now()
        );
        if (!diagnostico.violacao() && !diagnostico.alerta()) {
            return new TetoDiagnostico(false, null, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        BigDecimal margem = diagnostico.violacao() ? diagnostico.excedente() : diagnostico.margemRestante();
        int repeticoes = contarRepeticoesProximasAoLimite(contexto, diagnostico.limiteLegal());
        return new TetoDiagnostico(true, diagnostico.tipoViolacao(), diagnostico.limiteLegal(), margem, repeticoes);
    }

    private com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual parseRito(String classe, String assunto) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("classe", safe(classe));
        payload.put("assunto", safe(assunto));
        return proceduralCanonicalResolver.resolve(payload).rito();
    }

    private com.tcc.pjb.backend.model.entity.enums.RamoDireito parseRamo(String ramo) {
        com.tcc.pjb.backend.model.entity.enums.RamoDireito explicit = com.tcc.pjb.backend.model.entity.enums.RamoDireito.fromString(ramo);
        if (explicit != null) {
            return explicit;
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("ramoDireito", safe(ramo));
        String resolved = proceduralCanonicalResolver.resolve(payload).ramoDireito();
        return resolved == null ? null : com.tcc.pjb.backend.model.entity.enums.RamoDireito.fromString(resolved);
    }

    private int contarRepeticoesProximasAoLimite(ContextoRadar contexto, BigDecimal limite) {
        String escritorioHash = hashOrBlank(contexto.escritorioOab());
        if (escritorioHash.isBlank()) {
            return 0;
        }
        return (int) analiseRepository.findTop200ByEscritorioOabHashOrderByGeradoEmDesc(escritorioHash).stream()
                .filter(a -> a.getValorCausa() != null)
                .filter(a -> withinFivePercentBelow(a.getValorCausa(), limite))
                .count();
    }


    private ErroDeTetoException montarParecerTeto(ContextoRadar contexto, TetoDiagnostico teto) {
        TipoViolacaoTeto tipo = teto.tipoViolacao() != null ? teto.tipoViolacao() : TipoViolacaoTeto.RITO_INCOMPATIVEL;
        return new ErroDeTetoException.Builder(tipo)
                .fundamento(tipo.getDescricaoPadrao())
                .calculoFinanceiro(teto.limite(), contexto.valorCausa())
                .matematica("Ramo", safe(contexto.ramoDireito()))
                .matematica("Classe", safe(contexto.classeProcessual()))
                .matematica("Tribunal", safe(contexto.tribunalCodigo()))
                .sugestao("Revisar memoria do valor da causa e justificar aderencia ao teto do rito ou da alçada.")
                .build();
    }

    private FinancialAiResponse consultarIaFinanceira(ContextoRadar contexto, TetoDiagnostico teto) {
        try {
            IARequest request = IARequest.builder()
                    .origem("RADAR_PADROES")
                    .acao("ANALISAR_VALOR_CAUSA")
                    .payload("nupn", contexto.nupn())
                    .payload("tribunalCodigo", contexto.tribunalCodigo())
                    .payload("ramoDireito", contexto.ramoDireito())
                    .payload("classeProcessual", contexto.classeProcessual())
                    .payload("assunto", contexto.assunto())
                    .payload("valorCausa", contexto.valorCausa())
                    .payload("limiteCompetencia", teto.limite())
                    .payload("margemAteLimite", teto.margemAteLimite())
                    .payload("tipoViolacaoTeto", teto.tipoViolacao() != null ? teto.tipoViolacao().name() : null)
                    .build();
            return financeiraAiVersionSelector.processUnified(request, ApiVersion.latest(), request.getAcao());
        } catch (Exception ex) {
            log.warn("Radar financeiro nao bloqueante falhou. nupn={} erro={}", contexto.nupn(), ex.getMessage());
            return null;
        }
    }

    private void persistir(ContextoRadar contexto, Long processoId, AnaliseRadarResultado resultado) {
        Processo processo = resolveProcessoParaPersistencia(processoId);
        RadarPadraoAnalise entity = new RadarPadraoAnalise();
        entity.setProcesso(processo);
        entity.setNupn(contexto.nupn());
        entity.setTribunalCodigo(contexto.tribunalCodigo());
        entity.setDocumentoAutorHash(hashOrBlank(contexto.documentoAutor()));
        entity.setDocumentoReuHash(hashOrBlank(contexto.documentoReu()));
        entity.setEscritorioOabHash(hashOrBlank(contexto.escritorioOab()));
        entity.setFingerprintEstruturaHash(resultado.fingerprint().hashEstrutura());
        entity.setFingerprintConteudoHash(resultado.fingerprint().hashConteudoMinHash());
        entity.setNumeroParagrafos(resultado.fingerprint().numeroParagrafos());
        entity.setTotalPalavras(resultado.fingerprint().totalPalavras());
        entity.setDensidadeJargao(BigDecimal.valueOf(resultado.fingerprint().densidadeJargaoJuridico()));
        entity.setDiversidadeLexica(BigDecimal.valueOf(resultado.fingerprint().diversidadeLexica()));
        entity.setValorCausa(contexto.valorCausa());
        entity.setDataAjuizamento(contexto.dataAjuizamento());
        entity.setScoreGeral(BigDecimal.valueOf(resultado.scoreGeral()));
        entity.setNivelMaisAlto(resultado.alertas().stream().map(AlertaRadar::nivel).max(Comparator.naturalOrder()).map(Enum::name).orElse("INFORMATIVO"));
        entity.setTotalAlertas(resultado.alertas().size());
        entity.setTiposDetectados(resultado.alertas().stream().map(a -> a.tipoPadrao().name()).distinct().collect(Collectors.joining(",")));
        entity.setResumoTecnico(resultado.resumoTecnico());
        entity.setRequestJson(toJson(contexto));
        entity.setResponseJson(toJson(resultado));
        entity.setGeradoEm(resultado.analisadoEm());
        RadarPadraoAnalise saved = analiseRepository.save(entity);
        for (AlertaRadar alerta : resultado.alertas()) {
            RadarPadraoAlerta a = new RadarPadraoAlerta();
            a.setAnalise(saved);
            a.setProcesso(processo);
            a.setNupn(alerta.nupn());
            a.setTipoPadrao(alerta.tipoPadrao().name());
            a.setNivel(alerta.nivel().name());
            a.setScore(BigDecimal.valueOf(alerta.score()));
            a.setDescricaoTecnica(alerta.descricaoTecnica());
            a.setEvidenciasObjetivas(alerta.evidenciasObjetivas());
            a.setOrientacaoMagistrado(alerta.orientacaoMagistrado());
            a.setProcessoNaoBloqueado(alerta.processoNaoBloqueado());
            a.setReferenciaTeto(alerta.referenciaTeto());
            a.setExplicacaoFinanceiraIa(alerta.explicacaoFinanceiraIa());
            a.setNupnsRelacionadosJson(toJson(alerta.nupnsRelacionados()));
            a.setChaveDeteccao(Hashes.sha256Hex(alerta.nupn() + "|" + alerta.tipoPadrao().name() + "|" + alerta.evidenciasObjetivas()));
            a.setDetectadoEm(alerta.detectadoEm());
            try {
                alertaRepository.save(a);
            } catch (Exception ex) {
                log.debug("Radar alerta duplicado ignorado. nupn={} tipo={}", alerta.nupn(), alerta.tipoPadrao());
            }
        }
    }

    private Processo resolveProcessoParaPersistencia(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findById(processoId).orElse(null);
    }

    private void publicarEventos(ContextoRadar contexto, AnaliseRadarResultado resultado) {
        String payloadHash = Hashes.sha256Hex(toJson(resultado));
        auditLedgerService.appendSafely("RADAR_PADROES_ANALISADO", "RadarPadroes", safe(contexto.nupn()), payloadHash);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", contexto.processoId());
        payload.put("nupn", contexto.nupn());
        payload.put("tribunalCodigo", contexto.tribunalCodigo());
        payload.put("scoreGeral", resultado.scoreGeral());
        payload.put("tipos", resultado.alertas().stream().map(a -> a.tipoPadrao().name()).distinct().toList());
        payload.put("niveis", resultado.alertas().stream().map(a -> a.nivel().name()).distinct().toList());
        payload.put("totalAlertas", resultado.alertas().size());
        payload.put("at", Instant.now().toString());
        outboxPublisher.enqueue(
                "radar:" + safe(contexto.nupn()),
                EVT_RADAR_ANALISADO,
                payload,
                Map.of("source", "radar-padroes"),
                "radarPadroes:" + safe(contexto.nupn()) + ":" + payloadHash,
                "RadarPadroes",
                safe(contexto.nupn())
        );
    }

    private AnaliseRadarResultado hydrate(RadarPadraoAnalise entity) {
        List<AlertaRadar> alertas = alertaRepository.findTop100ByNupnOrderByDetectadoEmDesc(entity.getNupn()).stream()
                .map(this::mapAlert)
                .toList();
        FingerprintPeticao fingerprint = new FingerprintPeticao(
                entity.getNupn(),
                entity.getFingerprintEstruturaHash(),
                entity.getFingerprintConteudoHash(),
                entity.getNumeroParagrafos() == null ? 0 : entity.getNumeroParagrafos(),
                entity.getTotalPalavras() == null ? 0 : entity.getTotalPalavras(),
                entity.getDensidadeJargao() != null ? entity.getDensidadeJargao().doubleValue() : 0.0,
                entity.getDiversidadeLexica() != null ? entity.getDiversidadeLexica().doubleValue() : 0.0,
                null,
                entity.getDataAjuizamento()
        );
        return new AnaliseRadarResultado(
                entity.getNupn(),
                entity.getProcesso() != null ? entity.getProcesso().getId() : null,
                fingerprint,
                new PerfilLitigante("", 0, 0, 0.0, 0.0, 0.0, Map.of(), Map.of(), null, false),
                new PerfilLitigante("", 0, 0, 0.0, 0.0, 0.0, Map.of(), Map.of(), null, false),
                alertas,
                entity.getResumoTecnico(),
                entity.getScoreGeral() != null ? entity.getScoreGeral().doubleValue() : 0.0,
                entity.getGeradoEm()
        );
    }

    private AlertaRadar mapAlert(RadarPadraoAlerta entity) {
        return new AlertaRadar(
                entity.getUuid(),
                entity.getNupn(),
                TipoPadrao.valueOf(entity.getTipoPadrao()),
                NivelAlerta.valueOf(entity.getNivel()),
                entity.getScore() != null ? entity.getScore().doubleValue() : 0.0,
                entity.getDescricaoTecnica(),
                entity.getEvidenciasObjetivas(),
                entity.getOrientacaoMagistrado(),
                entity.isProcessoNaoBloqueado(),
                entity.getReferenciaTeto(),
                entity.getExplicacaoFinanceiraIa(),
                entity.getDetectadoEm(),
                readStringList(entity.getNupnsRelacionadosJson())
        );
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String construirResumo(ContextoRadar contexto, List<AlertaRadar> alertas, double score) {
        Map<NivelAlerta, Long> contagem = alertas.stream()
                .collect(Collectors.groupingBy(AlertaRadar::nivel, () -> new EnumMap<>(NivelAlerta.class), Collectors.counting()));
        return "Radar " + safe(contexto.nupn())
                + " | score=" + round(score, 4)
                + " | alertas=" + alertas.size()
                + " | criticos=" + contagem.getOrDefault(NivelAlerta.CRITICO, 0L)
                + " | relevantes=" + contagem.getOrDefault(NivelAlerta.RELEVANTE, 0L)
                + " | tipos=" + alertas.stream().map(a -> a.tipoPadrao().name()).distinct().collect(Collectors.joining(","));
    }

    private AlertaRadar novoAlerta(String nupn,
                                   TipoPadrao tipoPadrao,
                                   NivelAlerta nivel,
                                   double score,
                                   String descricaoTecnica,
                                   String evidenciasObjetivas,
                                   String orientacaoMagistrado,
                                   String referenciaTeto,
                                   String explicacaoFinanceiraIa,
                                   List<String> nupnsRelacionados) {
        return new AlertaRadar(
                UUID.randomUUID(),
                nupn,
                tipoPadrao,
                nivel,
                score,
                descricaoTecnica,
                evidenciasObjetivas,
                orientacaoMagistrado,
                true,
                referenciaTeto,
                explicacaoFinanceiraIa,
                Instant.now(),
                nupnsRelacionados
        );
    }

    private String resolverEscritorio(Usuario usuario) {
        if (usuario == null) {
            return "SEM_ESCRITORIO";
        }
        if (usuario.getOabNormalizada() != null && !usuario.getOabNormalizada().isBlank()) {
            return usuario.getOabNormalizada().trim().toUpperCase(Locale.ROOT);
        }
        if (usuario.getOab() != null && !usuario.getOab().isBlank()) {
            return usuario.getOab().trim().toUpperCase(Locale.ROOT);
        }
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            return "CPF:" + normalizarDocumentoSeguro(usuario.getCpf());
        }
        return "SEM_ESCRITORIO";
    }

    private boolean withinFivePercentBelow(BigDecimal valor, BigDecimal limite) {
        if (valor == null || limite == null || limite.signum() <= 0) {
            return false;
        }
        BigDecimal margem = limite.subtract(valor);
        if (margem.signum() < 0) {
            return false;
        }
        return margem.divide(limite, 6, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.05")) <= 0;
    }

    private boolean documentoValido(String documento) {
        try {
            documentoValidator.validarDocumento(documento);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalizarDocumentoSeguro(String documento) {
        if (documento == null) {
            return "";
        }
        return documentoValidator.normalizarDocumento(documento);
    }

    private String hashOrBlank(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? "" : Hashes.sha256Hex(normalized);
    }

    private LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalDate resolveFim(Processo processo) {
        LocalDate fim = toLocalDate(processo.getDataUltimaMovimentacao());
        return fim != null ? fim : LocalDate.now();
    }

    private boolean isAcordo(String resultado) {
        String token = safe(resultado).toUpperCase(Locale.ROOT);
        return token.contains("ACORDO") || token.contains("CONCILIACAO") || token.contains("HOMOLOGADO");
    }

    private boolean isDesistencia(String resultado) {
        String token = safe(resultado).toUpperCase(Locale.ROOT);
        return token.contains("DESIST") || token.contains("ABANDONO") || token.contains("RENUNCIA");
    }

    private boolean isEncerradoPorTexto(String resultado) {
        String token = safe(resultado).toUpperCase(Locale.ROOT);
        return token.contains("EXTIN") || token.contains("ARQUIV") || token.contains("BAIXA") || token.contains("SENTENCA");
    }

    private boolean sameWindow(LocalDate data, int dias) {
        if (data == null) {
            return false;
        }
        return !data.isBefore(LocalDate.now().minusDays(dias));
    }

    private int contarParagrafos(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(texto.split("\\R+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .count();
    }

    private double calcularDensidadeJargao(Collection<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return 0.0;
        }
        long total = tokens.size();
        long juridicos = tokens.stream().filter(JARGAO::contains).count();
        return round(juridicos / (double) total, 6);
    }

    private Map<String, Integer> topMap(Map<String, Integer> source, int limit) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum, LinkedHashMap::new));
    }

    private int bucket(int value, int scale) {
        if (value <= 0) {
            return 0;
        }
        return (value / Math.max(scale, 1)) * Math.max(scale, 1);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private String pct(double value) {
        return BigDecimal.valueOf(value).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
