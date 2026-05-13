package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.julgamento.Acordao;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import com.tcc.pjb.backend.model.repository.julgamento.AcordaoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.VotoColegiadoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;

@Service
public class NationalColegiadoEngine {

    private static final Logger log = LoggerFactory.getLogger(NationalColegiadoEngine.class);
    private static final DateTimeFormatter ACORDAO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final Set<StatusJulgamentoColegiado> STATUS_PENDENTES = EnumSet.of(
            StatusJulgamentoColegiado.AGENDADO,
            StatusJulgamentoColegiado.EM_ANDAMENTO,
            StatusJulgamentoColegiado.SUSPENSO,
            StatusJulgamentoColegiado.VISTA,
            StatusJulgamentoColegiado.ADIADO
    );
    public enum TipoSessao {
        SESSAO_ORDINARIA,
        SESSAO_EXTRAORDINARIA,
        SESSAO_PLENARIA,
        SESSAO_VIRTUAL,
        SESSAO_ADMINISTRATIVA,
        JULGAMENTO_MONOCRATICO_SUMULA
    }

    public enum TipoAcordao {
        ACORDAO_UNANIME,
        ACORDAO_MAIORIA,
        ACORDAO_PLACAR_MINIMO,
        DECISAO_MONOCRATICA,
        DESPACHO_ADMISSIBILIDADE,
        DECISAO_AFETACAO_REPETITIVO,
        DECISAO_REPERCUSSAO_GERAL
    }

    public enum StatusRepetitivo {
        PENDENTE_AFETACAO,
        AFETADO_AGUARDANDO_JULGAMENTO,
        JULGADO_TESE_FIRMADA,
        SOBRESTADO_POR_TESE_PENDENTE,
        TESE_SUSPENSA_MODULACAO
    }

    public record SessaoPauta(
            UUID sessaoId,
            String tribunalCodigo,
            String orgaoJulgador,
            TipoSessao tipoSessao,
            GrauJurisdicao grau,
            Instant dataHoraInicio,
            Instant dataHoraFim,
            List<ItemPauta> itens,
            boolean sessaoVirtual,
            String linkTransmissao,
            int quorumMinimo,
            int minutosEstimados,
            List<String> etiquetas
    ) {
        public SessaoPauta {
            itens = itens == null ? List.of() : List.copyOf(itens);
            etiquetas = etiquetas == null ? List.of() : List.copyOf(new LinkedHashSet<>(etiquetas));
            quorumMinimo = Math.max(1, quorumMinimo);
            minutosEstimados = Math.max(0, minutosEstimados);
        }

        public int totalItens() {
            return itens.size();
        }

        public long itensUrgentes() {
            return itens.stream().filter(ItemPauta::urgente).count();
        }
    }

    public record ItemPauta(
            int ordem,
            Long julgamentoId,
            Long processoId,
            String numeroUnificado,
            String classeTPU,
            String assunto,
            RamoDireito ramo,
            boolean urgente,
            boolean habeasCorpus,
            boolean tutela,
            boolean sigiloso,
            String relatorNome,
            Long relatorUsuarioId,
            boolean temSustentacaoOral,
            int minutosSustentacao,
            int prioridade,
            List<String> etiquetasPrioridade,
            StatusItemPauta status
    ) {
        public ItemPauta {
            minutosSustentacao = Math.max(0, minutosSustentacao);
            prioridade = Math.max(0, prioridade);
            etiquetasPrioridade = etiquetasPrioridade == null ? List.of() : List.copyOf(new LinkedHashSet<>(etiquetasPrioridade));
        }

        public enum StatusItemPauta {
            INCLUIDO,
            RETIRADO,
            ADIADO,
            JULGADO,
            AGUARDANDO_VOTO,
            SUSPENSO,
            VISTA
        }
    }

    public record ResultadoVotacao(
            Long julgamentoId,
            List<VotoRegistrado> votos,
            int totalFavor,
            int totalContra,
            int totalAbstencao,
            TipoVotoColegiado resultado,
            String teseFirmada,
            boolean empatadaPresidenteDesempata,
            boolean admiteEmbargosDeclaracao,
            boolean quorumAtingido,
            int quorumMinimo,
            StatusJulgamentoColegiado statusFinal,
            Instant encerradoEm
    ) {
        public ResultadoVotacao {
            votos = votos == null ? List.of() : List.copyOf(votos);
            quorumMinimo = Math.max(1, quorumMinimo);
        }

        public boolean aprovado() {
            return totalFavor > totalContra;
        }

        public int quorum() {
            return totalFavor + totalContra + totalAbstencao;
        }
    }

    public record VotoRegistrado(
            int ordem,
            UUID magistradoId,
            String nomeMagistrado,
            String cargoMagistrado,
            TipoVotoColegiado tipo,
            String papel,
            String fundamentoResumido,
            boolean votoVista,
            boolean vogal,
            String documentoRef,
            Instant registradoEm
    ) {
        public VotoRegistrado {
            nomeMagistrado = nomeMagistrado == null ? "" : nomeMagistrado.trim();
            cargoMagistrado = normalizeNullable(cargoMagistrado);
            papel = normalizeNullable(papel);
            fundamentoResumido = normalizeNullable(fundamentoResumido);
            documentoRef = normalizeNullable(documentoRef);
        }
    }

    public record RecursoRepetitivoTema(
            String numeroTema,
            String tribunalCodigo,
            String descricaoTema,
            String teseFixada,
            StatusRepetitivo status,
            List<String> processosAfetados,
            List<String> processosRepresentativos,
            GrauJurisdicao grau,
            RamoDireito ramo,
            Instant afetadoEm,
            Instant julgadoEm,
            List<String> alertas
    ) {
        public RecursoRepetitivoTema {
            numeroTema = normalizeNullable(numeroTema);
            tribunalCodigo = normalizeNullable(tribunalCodigo);
            descricaoTema = normalizeNullable(descricaoTema);
            teseFixada = normalizeNullable(teseFixada);
            processosAfetados = immutableDistinct(processosAfetados);
            processosRepresentativos = immutableDistinct(processosRepresentativos);
            alertas = immutableDistinct(alertas);
        }
    }

    public record ResultadoAfetacao(
            String numeroTema,
            int processosAfetados,
            int processosSobrestados,
            List<String> alertas,
            StatusRepetitivo status,
            Instant registradoEm
    ) {
        public ResultadoAfetacao {
            alertas = alertas == null ? List.of() : List.copyOf(new LinkedHashSet<>(alertas));
            registradoEm = registradoEm == null ? Instant.now() : registradoEm;
        }
    }

    public record JanelaSustentacaoOral(
            int ordem,
            Long julgamentoId,
            String numeroUnificado,
            Instant inicioPrevisto,
            Instant fimPrevisto,
            int duracaoMinutos,
            boolean preferencial,
            List<String> etiquetas
    ) {
        public JanelaSustentacaoOral {
            duracaoMinutos = Math.max(1, duracaoMinutos);
            etiquetas = etiquetas == null ? List.of() : List.copyOf(new LinkedHashSet<>(etiquetas));
        }
    }

    public record InsightPrecedente(
            String chaveTema,
            String classeTPU,
            String assunto,
            RamoDireito ramo,
            int ocorrencias,
            List<String> numerosProcessos,
            boolean candidatoAfetacao,
            boolean possuiUrgencia,
            List<String> alertas
    ) {
        public InsightPrecedente {
            numerosProcessos = immutableDistinct(numerosProcessos);
            alertas = immutableDistinct(alertas);
        }
    }

    public record FilaPublicacaoAcordao(
            Long julgamentoId,
            String numeroUnificado,
            String tribunalCodigo,
            String orgaoJulgador,
            Instant encerradoEm,
            LocalDate limitePublicacaoSugerido,
            boolean atrasado,
            List<String> alertas
    ) {
        public FilaPublicacaoAcordao {
            alertas = immutableDistinct(alertas);
        }
    }

    private final JulgamentoColegiadoService colegiadoService;
    private final JulgamentoColegiadoRepository colegiadoRepository;
    private final VotoColegiadoRepository votoRepository;
    private final AcordaoRepository acordaoRepository;
    private final AuditLedgerService auditLedgerService;
    private final NationalRulePackEngine rulePackEngine;
    private final NationalPrazoEngine prazoEngine;
    private final NationalColegiadoTemaSupport temaSupport;
    private final Map<String, RecursoRepetitivoTema> temasRepetitivos;
    private final Map<String, Set<String>> indiceTemaPorProcesso;
    private final NationalColegiadoSessionSupport sessionSupport;

    public NationalColegiadoEngine(JulgamentoColegiadoService colegiadoService,
                                   JulgamentoColegiadoRepository colegiadoRepository,
                                   VotoColegiadoRepository votoRepository,
                                   AcordaoRepository acordaoRepository,
                                   AuditLedgerService auditLedgerService,
                                   NationalRulePackEngine rulePackEngine,
                                   NationalPrazoEngine prazoEngine,
                                   NationalColegiadoTemaSupport temaSupport,
                                   NationalColegiadoSessionSupport sessionSupport) {
        this.colegiadoService = colegiadoService;
        this.colegiadoRepository = colegiadoRepository;
        this.votoRepository = votoRepository;
        this.acordaoRepository = acordaoRepository;
        this.auditLedgerService = auditLedgerService;
        this.rulePackEngine = rulePackEngine;
        this.prazoEngine = prazoEngine;
        this.temaSupport = temaSupport;
        this.temasRepetitivos = temaSupport.temasRepetitivosSnapshot();
        this.indiceTemaPorProcesso = temaSupport.indiceTemaPorProcessoSnapshot();
        this.sessionSupport = sessionSupport;
    }

    @Transactional(readOnly = true)
    public SessaoPauta montarPauta(String tribunalCodigo,
                                   String orgaoJulgador,
                                   TipoSessao tipo,
                                   GrauJurisdicao grau,
                                   Instant dataHoraInicio,
                                   boolean virtual) {
        String tribunal = normalizeTribunal(tribunalCodigo);
        GrauJurisdicao grauEfetivo = grau != null ? grau : GrauJurisdicao.SEGUNDO_GRAU;
        Instant inicio = dataHoraInicio != null ? dataHoraInicio : Instant.now();
        List<ItemPauta> itensOrdenados = carregarItensPendentes(tribunal, orgaoJulgador, grauEfetivo, tipo).stream()
                .sorted(Comparator.comparingInt(ItemPauta::prioridade).reversed()
                        .thenComparing((ItemPauta item) -> item.temSustentacaoOral() ? 0 : 1)
                        .thenComparing(ItemPauta::numeroUnificado, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<ItemPauta> itens = reordenarItens(itensOrdenados);
        int minutos = itens.stream().mapToInt(i -> Math.max(8, i.minutosSustentacao() + 6)).sum();
        List<String> etiquetas = sessionSupport.montarEtiquetasSessao(itens, tipo, virtual, grauEfetivo);
        SessaoPauta pauta = new SessaoPauta(
                UUID.randomUUID(),
                tribunal,
                normalizeNullable(orgaoJulgador),
                tipo != null ? tipo : virtual ? TipoSessao.SESSAO_VIRTUAL : TipoSessao.SESSAO_ORDINARIA,
                grauEfetivo,
                inicio,
                inicio.plus(Duration.ofMinutes(Math.max(45, minutos))),
                itens,
                virtual,
                virtual ? construirLinkSessaoVirtual(tribunal, orgaoJulgador, inicio) : null,
                resolverQuorumMinimo(grauEfetivo, tipo),
                minutos,
                etiquetas
        );
        auditLedgerService.appendSafely(
                "JUSOS_PAUTA_MONTADA",
                "JULGAMENTO_SESSAO",
                pauta.sessaoId().toString(),
                tribunal + ":" + grauEfetivo.name() + ":" + pauta.totalItens()
        );
        return pauta;
    }

    @Transactional
    public ResultadoVotacao registrarVotacao(Long julgamentoId,
                                             List<VotoRegistrado> votos,
                                             String teseFirmada) {
        Objects.requireNonNull(julgamentoId, "julgamentoId");
        List<VotoRegistrado> votosNormalizados = normalizarVotos(votos);
        if (votosNormalizados.isEmpty()) {
            throw new IllegalArgumentException("votos");
        }

        JulgamentoColegiado julgamento = colegiadoService.getRequired(julgamentoId);
        int ordem = 1;
        for (VotoRegistrado voto : votosNormalizados) {
            int ordemEfetiva = voto.ordem() > 0 ? voto.ordem() : ordem;
            colegiadoService.registrarVoto(
                    julgamentoId,
                    ordemEfetiva,
                    safeMagistradoNome(voto.nomeMagistrado(), ordemEfetiva),
                    voto.cargoMagistrado(),
                    normalizarPapel(voto),
                    voto.tipo() != null ? voto.tipo() : TipoVotoColegiado.OUTRO,
                    voto.fundamentoResumido(),
                    voto.documentoRef()
            );
            ordem = Math.max(ordem + 1, ordemEfetiva + 1);
        }

        List<VotoColegiado> persistidos = votoRepository.findByJulgamentoIdOrdered(julgamentoId);
        ContagemVotos contagem = consolidarVotos(persistidos);
        NationalPrazoEngine.PrazoCalculado prazoEmbargos = julgamento.getProcesso() != null
                ? prazoEngine.calcular(julgamento.getProcesso(), NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO)
                : null;
        boolean haVista = persistidos.stream().anyMatch(v -> v.getVotoTipo() == TipoVotoColegiado.PEDIR_VISTA);
        int quorumMinimo = resolverQuorumMinimo(julgamento.getGrau(), null);
        boolean quorumAtingido = persistidos.size() >= quorumMinimo;
        StatusJulgamentoColegiado statusFinal = haVista
                ? StatusJulgamentoColegiado.VISTA
                : quorumAtingido
                ? StatusJulgamentoColegiado.ENCERRADO
                : StatusJulgamentoColegiado.EM_ANDAMENTO;
        colegiadoService.atualizarStatus(julgamentoId, statusFinal);

        ResultadoVotacao resultado = new ResultadoVotacao(
                julgamentoId,
                persistidos.stream().map(this::mapearVoto).toList(),
                contagem.favor(),
                contagem.contra(),
                contagem.abstencao(),
                contagem.resultado(),
                normalizeNullable(teseFirmada),
                contagem.favor() == contagem.contra() && persistidos.stream().anyMatch(this::ehVotoPresidencial),
                prazoEmbargos != null && prazoEmbargos.vencimento() != null,
                quorumAtingido,
                quorumMinimo,
                statusFinal,
                Instant.now()
        );

        auditLedgerService.appendSafely(
                "JUSOS_VOTACAO_REGISTRADA",
                "JULGAMENTO_COLEGIADO",
                String.valueOf(julgamentoId),
                julgamentoId + ":" + statusFinal.name() + ":" + persistidos.size() + ":" + contagem.resultado().name()
        );
        return resultado;
    }

    @Transactional
    public Acordao publicarAcordao(Long julgamentoId,
                                   TipoAcordao tipoAcordao,
                                   String teseFirmada,
                                   String inteiroTeorRef) {
        Objects.requireNonNull(julgamentoId, "julgamentoId");
        JulgamentoColegiado julgamento = colegiadoService.getRequired(julgamentoId);
        Processo processo = julgamento.getProcesso();
        List<VotoColegiado> votos = votoRepository.findByJulgamentoIdOrdered(julgamentoId);
        ContagemVotos contagem = consolidarVotos(votos);
        String numero = gerarNumeroAcordao(julgamento, tipoAcordao);
        String ementa = gerarEmenta(julgamento, processo, contagem, tipoAcordao, teseFirmada);
        Acordao acordao = colegiadoService.publicarAcordao(
                julgamentoId,
                numero,
                ementa,
                normalizeNullable(inteiroTeorRef)
        );
        auditLedgerService.appendSafely(
                "JUSOS_ACORDAO_PUBLICADO",
                "ACORDAO",
                String.valueOf(acordao.getId()),
                numero + ":" + julgamentoId + ":" + contagem.resultado().name()
        );
        return acordao;
    }

    public ResultadoAfetacao afetarComoRepetitivo(String numeroTema,
                                                  List<String> numerosProcessos,
                                                  GrauJurisdicao grau,
                                                  RamoDireito ramo) {
        ResultadoAfetacao resultado = Objects.requireNonNull(temaSupport.afetarComoRepetitivo(numeroTema, numerosProcessos, grau, ramo), "resultadoAfetacao");
        auditLedgerService.appendSafely(
                "JUSOS_TEMA_AFETADO",
                "TEMA_REPETITIVO",
                resultado.numeroTema(),
                resultado.numeroTema() + ":" + resultado.status().name() + ":" + resultado.processosAfetados()
        );
        return resultado;
    }

    public RecursoRepetitivoTema registrarTeseRepetitiva(String numeroTema,
                                                         String teseFixada,
                                                         List<String> processosAfetados) {
        return temaSupport.registrarTeseRepetitiva(numeroTema, teseFixada, processosAfetados);
    }

    public RecursoRepetitivoTema consultarTema(String numeroTema) {
        return temaSupport.consultarTema(numeroTema);
    }

    public List<RecursoRepetitivoTema> listarTemas() {
        return temaSupport.listarTemas();
    }

    public List<RecursoRepetitivoTema> consultarTemasPorProcesso(String numeroUnificado) {
        return temaSupport.consultarTemasPorProcesso(numeroUnificado);
    }

    @Transactional(readOnly = true)
    public List<JanelaSustentacaoOral> gerarAgendaSustentacaoOral(SessaoPauta sessao) {
        return sessionSupport.gerarAgendaSustentacaoOral(sessao);
    }

    @Transactional(readOnly = true)
    public List<InsightPrecedente> mapearInsightsPrecedentes(SessaoPauta sessao) {
        return sessionSupport.mapearInsightsPrecedentes(sessao);
    }

    @Transactional(readOnly = true)
    public List<FilaPublicacaoAcordao> gerarFilaPublicacaoAcordao(String tribunalCodigo) {
        String tribunal = normalizeTribunal(tribunalCodigo);
        return sessionSupport.gerarFilaPublicacaoAcordao(
                tribunal,
                colegiadoRepository.findAguardandoPublicacaoAcordao(StatusJulgamentoColegiado.ENCERRADO, tribunal),
                temaSupport::consultarTemasPorProcesso,
                this::resolverMarcoEncerramento,
                this::normalizeTribunal
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarChecklistOperacionalSessao(SessaoPauta sessao) {
        return sessionSupport.gerarChecklistOperacionalSessao(sessao);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorioSessao(SessaoPauta sessao) {
        return sessionSupport.gerarRelatorioSessao(sessao);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarPainelColegiado(String tribunalCodigo) {
        String tribunal = normalizeTribunal(tribunalCodigo);
        List<JulgamentoColegiado> julgamentos = colegiadoRepository.findAllWithProcessoByTribunal(tribunal);
        List<FilaPublicacaoAcordao> filaPublicacao = gerarFilaPublicacaoAcordao(tribunal);
        long comAcordao = julgamentos.isEmpty() ? 0L : acordaoRepository.countByTribunalSigla(tribunal);
        return sessionSupport.gerarPainelColegiado(
                tribunal,
                julgamentos,
                comAcordao,
                filaPublicacao,
                temaSupport.totalTemas(),
                temaSupport.totalProcessosIndexados(),
                this::isUrgentPending,
                this::resolverMarcoEncerramento,
                STATUS_PENDENTES
        );
    }

    private boolean isUrgentPending(JulgamentoColegiado julgamento) {
        Processo processo = julgamento.getProcesso();
        return processo != null
                && (containsAny(processo.getClasseProcessual(), "HABEAS CORPUS", "HC")
                || containsAny(processo.getAssunto(), "TUTELA", "LIMINAR", "URGENTE", "URGENCIA"));
    }

    private List<ItemPauta> carregarItensPendentes(String tribunalCodigo,
                                                   String orgao,
                                                   GrauJurisdicao grau,
                                                   TipoSessao tipoSessao) {
        List<JulgamentoColegiado> julgamentos = colegiadoRepository.findByStatusInWithProcessoAndTribunal(STATUS_PENDENTES, tribunalCodigo).stream()
                .filter(j -> orgao == null || orgao.isBlank() || normalizeToken(orgao).equals(normalizeToken(j.getOrgaoJulgador())))
                .filter(j -> grau == null || j.getGrau() == grau)
                .toList();
        List<ItemPauta> itens = new ArrayList<>(julgamentos.size());
        for (JulgamentoColegiado julgamento : julgamentos) {
            try {
                itens.add(mapearItem(julgamento, tipoSessao));
            } catch (Exception ex) {
                log.warn("[JusOS-Colegiado] falha ao mapear item de pauta id={}: {}", julgamento.getId(), ex.getMessage());
            }
        }
        return itens;
    }

    private ItemPauta mapearItem(JulgamentoColegiado julgamento, TipoSessao tipoSessao) {
        Processo processo = Objects.requireNonNull(julgamento.getProcesso(), "processo");
        RamoDireito ramo = processo.getRamoDireito();
        String classeProcessual = processo.getClasseProcessual();
        String assunto = processo.getAssunto();
        String numeroUnificado = processo.getNumeroUnificado();
        NivelSigilo nivelSigilo = processo.getNivelSigilo();
        boolean habeasCorpus = containsAny(classeProcessual, "HABEAS CORPUS", "HC")
                || ramo == RamoDireito.PENAL && containsAny(assunto, "LIBERDADE", "PRISAO", "CUSTODIA");
        boolean tutela = containsAny(assunto, "TUTELA", "LIMINAR", "URGENTE", "URGENCIA")
                || containsAny(classeProcessual, "MANDADO DE SEGURANCA", "AGRAVO");
        boolean sigiloso = nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO;
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("sigilo", nivelSigilo != null ? nivelSigilo.name() : "PUBLICO");
        extras.put("temPautaMarcada", julgamento.getPautaDataHora() != null);
        extras.put("sessaoVirtual", tipoSessao == TipoSessao.SESSAO_VIRTUAL);
        if (processo.getValorCausa() != null) {
            extras.put("valorCausa", processo.getValorCausa());
        }
        NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                        processo.getClasseProcessual(),
                        processo.getAssunto(),
                        processo.getRamoDireito(),
                        julgamento.getGrau(),
                        julgamento.getTribunalSigla(),
                        extras
                ));
        int prioridade = 10;
        List<String> etiquetas = new CopyOnWriteArrayList<>();
        if (habeasCorpus) {
            prioridade += 60;
            etiquetas.add("HC");
        }
        if (tutela) {
            prioridade += 35;
            etiquetas.add("URGÊNCIA");
        }
        if (sigiloso) {
            prioridade += 8;
            etiquetas.add("SIGILO");
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.FAMILIA) {
            prioridade += 18;
            etiquetas.add("PRIORIDADE_LEGAL");
        }
        if (regras.temAlertasCriticos()) {
            prioridade += 20;
            etiquetas.add("ALERTA_CRITICO");
        }
        if (numeroUnificado != null && temaSupport.processoEmTemaRepetitivo(numeroUnificado)) {
            prioridade += 15;
            etiquetas.add("TEMA_REPETITIVO");
        }
        NationalPrazoEngine.PrazoCalculado embargosBase = prazoEngine.calcular(processo, NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO);
        if (embargosBase != null && embargosBase.diasCorridos() <= 5) {
            prioridade += 4;
            etiquetas.add("PRAZO_CURTO_POS_JULGAMENTO");
        }
        if (julgamento.getPautaDataHora() != null && processo.getDataUltimaMovimentacao() != null) {
            long diasParados = Duration.between(processo.getDataUltimaMovimentacao().toInstant(ZoneOffset.UTC), julgamento.getPautaDataHora().toInstant(ZoneOffset.UTC)).toDays();
            if (diasParados > 180) {
                prioridade += 12;
                etiquetas.add("ESTOQUE_ANTIGO");
            }
        }
        int minutosSustentacao = sugerirMinutosSustentacao(processo, julgamento.getGrau(), tipoSessao, regras);
        ItemPauta.StatusItemPauta status = mapearStatusItem(julgamento.getStatus());
        return new ItemPauta(
                0,
                julgamento.getId(),
                processo.getId(),
                processo.getNumeroUnificado(),
                classeProcessual,
                assunto,
                ramo,
                prioridade >= 45,
                habeasCorpus,
                tutela,
                sigiloso,
                julgamento.getRelatorNome(),
                processo.getUsuario() != null ? processo.getUsuario().getId() : null,
                minutosSustentacao > 0,
                minutosSustentacao,
                prioridade,
                etiquetas,
                status
        );
    }

    private ItemPauta.StatusItemPauta mapearStatusItem(StatusJulgamentoColegiado status) {
        if (status == null) {
            return ItemPauta.StatusItemPauta.INCLUIDO;
        }
        return switch (status) {
            case ADIADO -> ItemPauta.StatusItemPauta.ADIADO;
            case SUSPENSO -> ItemPauta.StatusItemPauta.SUSPENSO;
            case VISTA -> ItemPauta.StatusItemPauta.VISTA;
            case ENCERRADO -> ItemPauta.StatusItemPauta.JULGADO;
            case EM_ANDAMENTO -> ItemPauta.StatusItemPauta.AGUARDANDO_VOTO;
            default -> ItemPauta.StatusItemPauta.INCLUIDO;
        };
    }

    private String construirLinkSessaoVirtual(String tribunalCodigo,
                                              String orgaoJulgador,
                                              Instant dataHoraInicio) {
        String tribunal = tribunalCodigo == null || tribunalCodigo.isBlank() ? "nacional" : tribunalCodigo.trim().toLowerCase(Locale.ROOT);
        String orgao = orgaoJulgador == null || orgaoJulgador.isBlank() ? "colegiado" : normalizeSlug(orgaoJulgador);
        String dia = ACORDAO_DATE.format(LocalDateTime.ofInstant(dataHoraInicio, ZoneOffset.UTC));
        return "https://" + tribunal + ".jus.br/sessoes/" + orgao + "/" + dia;
    }

    private int sugerirMinutosSustentacao(Processo processo,
                                          GrauJurisdicao grau,
                                          TipoSessao tipoSessao,
                                          NationalRulePackEngine.ResultadoRegras regras) {
        if (tipoSessao == TipoSessao.SESSAO_ADMINISTRATIVA) {
            return 0;
        }
        int base = switch (grau != null ? grau : GrauJurisdicao.SEGUNDO_GRAU) {
            case CONSTITUCIONAL -> 20;
            case SUPERIOR -> 15;
            default -> 10;
        };
        if (processo.getValorCausa() != null && processo.getValorCausa().doubleValue() > 1_000_000d) {
            base += 5;
        }
        if (regras.temAlertasCriticos()) {
            base += 5;
        }
        if (processo.getRamoDireito() == RamoDireito.PENAL) {
            base += 5;
        }
        return Math.min(base, 30);
    }

    private ContagemVotos consolidarVotos(Collection<VotoColegiado> votos) {
        int favor = 0;
        int contra = 0;
        int abstencao = 0;
        int parcial = 0;
        for (VotoColegiado voto : votos) {
            TipoVotoColegiado tipo = voto != null ? voto.getVotoTipo() : null;
            if (tipo == null) {
                abstencao++;
                continue;
            }
            switch (tipo) {
                case DAR_PROVIMENTO, ACOMPANHAR_RELATOR -> favor++;
                case NEGAR_PROVIMENTO -> contra++;
                case PARCIAL_PROVIMENTO, DAR_PROVIMENTO_EM_PARTE -> parcial++;
                default -> abstencao++;
            }
        }
        TipoVotoColegiado resultado = favor > contra
                ? parcial > 0 ? TipoVotoColegiado.DAR_PROVIMENTO_EM_PARTE : TipoVotoColegiado.DAR_PROVIMENTO
                : contra > favor
                ? TipoVotoColegiado.NEGAR_PROVIMENTO
                : parcial > 0
                ? TipoVotoColegiado.PARCIAL_PROVIMENTO
                : TipoVotoColegiado.OUTRO;
        return new ContagemVotos(favor, contra, abstencao + parcial, resultado);
    }

    private boolean ehVotoPresidencial(VotoColegiado voto) {
        return voto != null && voto.getPapel() != null && "PRESIDENTE".equals(voto.getPapel().name());
    }

    private VotoRegistrado mapearVoto(VotoColegiado voto) {
        return new VotoRegistrado(
                voto.getOrdem() != null ? voto.getOrdem() : 0,
                null,
                voto.getMagistradoNome(),
                voto.getMagistradoCargo(),
                voto.getVotoTipo(),
                voto.getPapel() != null ? voto.getPapel().name() : null,
                voto.getVotoResumo(),
                voto.getVotoTipo() == TipoVotoColegiado.PEDIR_VISTA,
                voto.getPapel() != null && "VOGAL".equals(voto.getPapel().name()),
                voto.getDocumentoRef(),
                voto.getProferidoEm() != null ? voto.getProferidoEm().toInstant(ZoneOffset.UTC) : Instant.now()
        );
    }

    private List<VotoRegistrado> normalizarVotos(List<VotoRegistrado> votos) {
        if (votos == null || votos.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, VotoRegistrado> normalizados = new LinkedHashMap<>();
        int ordem = 1;
        for (VotoRegistrado voto : votos) {
            if (voto == null || voto.nomeMagistrado() == null || voto.nomeMagistrado().isBlank()) {
                continue;
            }
            int ordemEfetiva = voto.ordem() > 0 ? voto.ordem() : ordem;
            String chave = ordemEfetiva + "::" + normalizeToken(voto.nomeMagistrado());
            normalizados.put(chave, new VotoRegistrado(
                    ordemEfetiva,
                    voto.magistradoId(),
                    voto.nomeMagistrado(),
                    voto.cargoMagistrado(),
                    voto.tipo(),
                    voto.papel(),
                    voto.fundamentoResumido(),
                    voto.votoVista(),
                    voto.vogal(),
                    voto.documentoRef(),
                    voto.registradoEm() != null ? voto.registradoEm() : Instant.now()
            ));
            ordem = Math.max(ordem + 1, ordemEfetiva + 1);
        }
        return List.copyOf(normalizados.values());
    }

    private String gerarNumeroAcordao(JulgamentoColegiado julgamento, TipoAcordao tipoAcordao) {
        String tribunal = normalizeTribunal(julgamento.getTribunalSigla());
        String prefixo = tribunal != null ? tribunal : "NAC";
        String data = ACORDAO_DATE.format(LocalDateTime.now());
        String tipo = tipoAcordao != null ? tipoAcordao.name().replace("ACORDAO_", "") : "PADRAO";
        return prefixo + "-AC-" + data + "-" + julgamento.getId() + "-" + tipo;
    }

    private String gerarEmenta(JulgamentoColegiado julgamento,
                               Processo processo,
                               ContagemVotos contagem,
                               TipoAcordao tipoAcordao,
                               String teseFirmada) {
        StringBuilder sb = new StringBuilder();
        if (processo != null && processo.getRamoDireito() != null) {
            sb.append(processo.getRamoDireito().getDescricao()).append(". ");
        }
        if (processo != null && processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            sb.append(processo.getClasseProcessual().trim()).append(". ");
        }
        if (processo != null && processo.getAssunto() != null && !processo.getAssunto().isBlank()) {
            sb.append(processo.getAssunto().trim()).append(". ");
        }
        if (processo != null) {
            String numeroUnificado = processo.getNumeroUnificado();
            List<RecursoRepetitivoTema> temas = consultarTemasPorProcesso(numeroUnificado);
            if (!temas.isEmpty()) {
                sb.append("Tema correlato: ")
                        .append(temas.stream().map(RecursoRepetitivoTema::numeroTema).distinct().limit(3).toList())
                        .append(". ");
            }
        }
        sb.append("Julgamento colegiado em ")
                .append(julgamento.getGrau() != null ? julgamento.getGrau().name() : "GRAU_NAO_INFORMADO")
                .append(". ");
        sb.append("Resultado: ").append(contagem.resultado().name()).append(". ");
        if (tipoAcordao != null) {
            sb.append("Tipo do acórdão: ").append(tipoAcordao.name()).append(". ");
        }
        if (teseFirmada != null && !teseFirmada.isBlank()) {
            sb.append("Tese: ").append(teseFirmada.trim()).append(". ");
        }
        sb.append("Placar consolidado favorável ")
                .append(contagem.favor())
                .append(", contrário ")
                .append(contagem.contra())
                .append(", abstenções/outros ")
                .append(contagem.abstencao())
                .append('.');
        return sb.toString().trim();
    }

    private int resolverQuorumMinimo(GrauJurisdicao grau, TipoSessao tipoSessao) {
        if (tipoSessao == TipoSessao.JULGAMENTO_MONOCRATICO_SUMULA) {
            return 1;
        }
        if (grau == null) {
            return 3;
        }
        return switch (grau) {
            case CONSTITUCIONAL -> 6;
            case SUPERIOR -> 5;
            default -> 3;
        };
    }

    private String normalizarPapel(VotoRegistrado voto) {
        if (voto.papel() != null && !voto.papel().isBlank()) {
            return normalizeToken(voto.papel());
        }
        if (voto.vogal()) {
            return "VOGAL";
        }
        if (voto.ordem() == 1) {
            return "RELATOR";
        }
        return "VOGAL";
    }

        private Instant resolverMarcoEncerramento(JulgamentoColegiado julgamento) {
        if (julgamento == null) {
            return null;
        }
        if (julgamento.getSessaoFim() != null) {
            return julgamento.getSessaoFim().toInstant(ZoneOffset.UTC);
        }
        if (julgamento.getUpdatedAt() != null) {
            return julgamento.getUpdatedAt().toInstant(ZoneOffset.UTC);
        }
        if (julgamento.getPautaDataHora() != null) {
            return julgamento.getPautaDataHora().toInstant(ZoneOffset.UTC);
        }
        return julgamento.getCreatedAt() != null ? julgamento.getCreatedAt().toInstant(ZoneOffset.UTC) : null;
    }

    private String safeMagistradoNome(String nome, int ordem) {
        String resolved = normalizeNullable(nome);
        return resolved != null ? resolved : "MAGISTRADO_" + ordem;
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank() || needles == null || needles.length == 0) {
            return false;
        }
        String token = normalizeToken(value);
        for (String needle : needles) {
            if (token.contains(normalizeToken(needle))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTribunal(String tribunalCodigo) {
        String token = normalizeNullable(tribunalCodigo);
        return token == null ? null : token.toUpperCase(Locale.ROOT);
    }

    private String normalizeSlug(String value) {
        String token = normalizeToken(value).toLowerCase(Locale.ROOT);
        return token.replace('_', '-');
    }


    private List<ItemPauta> reordenarItens(List<ItemPauta> itens) {
        List<ItemPauta> resultado = new ArrayList<>();
        int ordem = 1;
        for (ItemPauta item : itens) {
            resultado.add(new ItemPauta(
                    ordem++,
                    item.julgamentoId(),
                    item.processoId(),
                    item.numeroUnificado(),
                    item.classeTPU(),
                    item.assunto(),
                    item.ramo(),
                    item.urgente(),
                    item.habeasCorpus(),
                    item.tutela(),
                    item.sigiloso(),
                    item.relatorNome(),
                    item.relatorUsuarioId(),
                    item.temSustentacaoOral(),
                    item.minutosSustentacao(),
                    item.prioridade(),
                    item.etiquetasPrioridade(),
                    item.status()
            ));
        }
        return List.copyOf(resultado);
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isBlank() ? null : value;
    }

    private static List<String> immutableDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalizeNullable(value);
            if (item != null) {
                sanitized.add(item);
            }
        }
        return List.copyOf(sanitized);
    }
}
