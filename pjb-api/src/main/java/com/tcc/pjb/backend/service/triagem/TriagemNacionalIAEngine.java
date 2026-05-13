package com.tcc.pjb.backend.service.triagem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.triagem.TriagemNacionalAnalise;
import com.tcc.pjb.backend.model.repository.TriagemNacionalAnaliseRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;

@Service
@PjbPublicApi(module = PjbModuleId.TRIAGEM_CANONIZACAO)
public class TriagemNacionalIAEngine {

    public static final String EVT_TRIAGEM_REALIZADA = "pjb.triagem.nacional.realizada";
    private final TriagemNacionalAnaliseRepository triagemRepository;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final TriagemNacionalValidationSupport validationSupport;
    private final TriagemNacionalInferenceSupport inferenceSupport;

    public TriagemNacionalIAEngine(TriagemNacionalAnaliseRepository triagemRepository,
                                   AuditLedgerService auditLedgerService,
                                   OutboxPublisher outboxPublisher,
                                   ObjectMapper objectMapper,
                                   TriagemNacionalValidationSupport validationSupport,
                                   TriagemNacionalInferenceSupport inferenceSupport) {
        this.triagemRepository = Objects.requireNonNull(triagemRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.validationSupport = Objects.requireNonNull(validationSupport);
        this.inferenceSupport = Objects.requireNonNull(inferenceSupport);
    }

    public record PedidoTriagem(
            String nupnProvisorio,
            String classeTpuSugerida,
            String assuntoTpuSugerido,
            String ramoDireito,
            BigDecimal valorCausa,
            String textoFatosResumido,
            String cpfCnpjAutor,
            String cpfCnpjReu,
            String oabAdvogado,
            String ufAdvogado,
            List<String> documentosAnexados,
            LocalDate dataFatoGerador,
            boolean requerLiminar,
            boolean atoJurisdicionalAnterior,
            Long processoId
    ) {
    }

    public record ResultadoTriagem(
            String nupnProvisorio,
            VereditoTriagem veredito,
            List<PendenciaTriagem> pendencias,
            SugestaoClassificacao classificacao,
            AnalisePrescricao prescricao,
            CompetenciaTriagem competencia,
            List<ProcessoSuspeito> processosConexos,
            String resumoDecisao,
            double confiancaGeral,
            boolean aprovacaoAutomatica,
            Instant triadoEm
    ) {
        public boolean aprovado() {
            return veredito == VereditoTriagem.APROVADO || veredito == VereditoTriagem.APROVADO_COM_RESSALVAS;
        }

        public boolean bloqueado() {
            return veredito == VereditoTriagem.BLOQUEADO;
        }
    }

    public enum VereditoTriagem {
        APROVADO,
        APROVADO_COM_RESSALVAS,
        PENDENTE_CORRECAO,
        BLOQUEADO,
        REQUER_REVISAO_HUMANA
    }

    public record PendenciaTriagem(
            TipoPendencia tipo,
            String descricao,
            boolean impeditiva,
            SeveridadePendencia severidade,
            String orientacaoCorrecao
    ) {
    }

    public enum SeveridadePendencia {
        INFORMATIVA,
        ALERTA,
        CRITICA
    }

    public enum TipoPendencia {
        DOCUMENTO_FALTANTE,
        DOCUMENTO_AUTOR_INVALIDO,
        DOCUMENTO_REU_INVALIDO,
        OAB_INVALIDA_OU_SUSPENSA,
        VALOR_CAUSA_INCONSISTENTE,
        CAUSA_PEDIR_INCOMPLETA,
        PEDIDO_INDETERMINADO,
        PRESCRICAO_APARENTE,
        DECADENCIA_APARENTE,
        LITISPENDENCIA_SUSPEITA,
        COISA_JULGADA_SUSPEITA,
        INCOMPETENCIA_MANIFESTA,
        FALTA_LEGITIMIDADE_ATIVA,
        FALTA_LEGITIMIDADE_PASSIVA,
        INTERESSE_PROCESSUAL_DUVIDOSO,
        LIMINAR_SEM_SUPORTE_MINIMO,
        CLASSIFICACAO_INCERTA,
        RISCO_DUPLICIDADE_INTERNA,
        JUIZADO_INCOMPATIVEL_COM_VALOR,
        REPRESENTACAO_INCONSISTENTE
    }

    public record SugestaoClassificacao(
            String classeTpu,
            String assuntoTpu,
            double confiancaClasse,
            double confiancaAssunto,
            List<AlternativaTpu> alternativas,
            boolean confirmacaoRecomendada
    ) {
    }

    public record AlternativaTpu(String classeTpu, String assuntoTpu, double probabilidade) {
    }

    public record AnalisePrescricao(
            boolean prescricaoAparente,
            boolean decadenciaAparente,
            String fundamentoLegal,
            int prazoLegalAnos,
            long diasDecorridosFatoGerador,
            String observacao
    ) {
        public static AnalisePrescricao semAnalise() {
            return new AnalisePrescricao(false, false, "SEM_ANALISE", 0, 0, "Data do fato nao informada");
        }
    }

    public record ProcessoSuspeito(
            String nupn,
            String tribunalCodigo,
            double similaridade,
            TipoConexao tipoConexao,
            String descricaoSimilaridade
    ) {
    }

    public enum TipoConexao {
        IDENTICO,
        LITISPENDENTE,
        COISA_JULGADA,
        CONEXO,
        CONTINENTE
    }

    public record CompetenciaTriagem(
            String tipoJusticaSugerido,
            String ritoSugerido,
            double confianca,
            boolean incompatibilidadeManifesta,
            List<String> fundamentos
    ) {
    }

    public record AnaliseRegistradaView(
            UUID id,
            String nupnProvisorio,
            Long processoId,
            String veredito,
            double confiancaGeral,
            boolean aprovacaoAutomatica,
            String classeTpu,
            String assuntoTpu,
            String competenciaSugerida,
            String ritoSugerido,
            String resumoDecisao,
            Instant triadoEm,
            Instant atualizadoEm
    ) {
        public static AnaliseRegistradaView of(TriagemNacionalAnalise entity) {
            return new AnaliseRegistradaView(
                    entity.getId(),
                    entity.getNupnProvisorio(),
                    entity.getProcessoId(),
                    entity.getVeredito(),
                    entity.getConfiancaGeral() != null ? entity.getConfiancaGeral().doubleValue() : 0.0,
                    entity.isAprovacaoAutomatica(),
                    entity.getClasseTpu(),
                    entity.getAssuntoTpu(),
                    entity.getCompetenciaSugerida(),
                    entity.getRitoSugerido(),
                    entity.getResumoDecisao(),
                    entity.getTriadoEm(),
                    entity.getAtualizadoEm()
            );
        }
    }

    @Transactional
    public ResultadoTriagem triarERegistrar(PedidoTriagem pedido) {
        ResultadoTriagem resultado = triar(pedido);
        persistirAnalise(pedido, resultado);
        publicarEventos(pedido, resultado);
        return resultado;
    }

    @Transactional
    public ResultadoTriagem preTriarProcesso(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        PedidoTriagem pedido = mapearProcesso(processo);
        ResultadoTriagem resultado = triarERegistrar(pedido);
        if (resultado.veredito() == VereditoTriagem.BLOQUEADO || resultado.veredito() == VereditoTriagem.PENDENTE_CORRECAO || resultado.veredito() == VereditoTriagem.REQUER_REVISAO_HUMANA) {
            throw new IllegalStateException(resultado.resumoDecisao());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<AnaliseRegistradaView> buscarUltimaPorNupn(String nupnProvisorio) {
        if (nupnProvisorio == null || nupnProvisorio.isBlank()) {
            return Optional.empty();
        }
        return triagemRepository.findTopByNupnProvisorioOrderByTriadoEmDesc(nupnProvisorio.trim()).map(AnaliseRegistradaView::of);
    }

    @Transactional(readOnly = true)
    public Optional<AnaliseRegistradaView> buscarUltimaPorProcessoId(Long processoId) {
        if (processoId == null) {
            return Optional.empty();
        }
        return triagemRepository.findTopByProcessoIdOrderByTriadoEmDesc(processoId).map(AnaliseRegistradaView::of);
    }

    public ResultadoTriagem triar(PedidoTriagem pedido) {
        Objects.requireNonNull(pedido, "pedido");
        PedidoTriagem normalizado = validationSupport.normalizarPedido(pedido);
        List<PendenciaTriagem> pendencias = new ArrayList<>();

        validationSupport.verificarDocumentosDasPartes(normalizado, pendencias);
        validationSupport.verificarRepresentacao(normalizado, pendencias);
        validationSupport.verificarDocumentosObrigatorios(normalizado, pendencias);
        validationSupport.verificarCompletude(normalizado, pendencias);
        validationSupport.verificarValorEJuizado(normalizado, pendencias);
        validationSupport.verificarTetoProcessual(normalizado, pendencias);
        validationSupport.verificarCoerenciaPartes(normalizado, pendencias);

        SugestaoClassificacao classificacao = inferenceSupport.sugerirClassificacao(normalizado, pendencias);
        AnalisePrescricao prescricao = inferenceSupport.analisarPrescricao(normalizado);
        if (prescricao.prescricaoAparente()) {
            pendencias.add(new PendenciaTriagem(
                    TipoPendencia.PRESCRICAO_APARENTE,
                    prescricao.observacao() + " | " + prescricao.fundamentoLegal(),
                    true,
                    SeveridadePendencia.CRITICA,
                    "Verifique causas suspensivas, interruptivas ou marcos de renovacao do prazo"
            ));
        }
        if (prescricao.decadenciaAparente()) {
            pendencias.add(new PendenciaTriagem(
                    TipoPendencia.DECADENCIA_APARENTE,
                    prescricao.observacao() + " | " + prescricao.fundamentoLegal(),
                    true,
                    SeveridadePendencia.CRITICA,
                    "Demonstre previsao legal de superacao do prazo decadencial"
            ));
        }

        CompetenciaTriagem competencia = inferenceSupport.analisarCompetencia(normalizado, classificacao, pendencias);
        List<ProcessoSuspeito> conexos = inferenceSupport.detectarConexos(normalizado, classificacao);
        inferenceSupport.adicionarPendenciasPorConexao(conexos, pendencias);

        VereditoTriagem veredito = inferirVeredito(pendencias, classificacao, competencia, conexos);
        double confianca = inferenceSupport.calcularConfiancaGeral(pendencias, classificacao, competencia, conexos);
        boolean aprovacaoAutomatica = veredito == VereditoTriagem.APROVADO && confianca >= 0.92 && conexos.isEmpty();
        String resumo = inferenceSupport.construirResumo(normalizado, veredito, pendencias, classificacao, competencia, prescricao, conexos, confianca);

        return new ResultadoTriagem(
                normalizado.nupnProvisorio(),
                veredito,
                Collections.unmodifiableList(new ArrayList<>(deduplicarPendencias(pendencias))),
                classificacao,
                prescricao,
                competencia,
                Collections.unmodifiableList(new ArrayList<>(conexos)),
                resumo,
                round4(confianca),
                aprovacaoAutomatica,
                Instant.now()
        );
    }

    private PedidoTriagem mapearProcesso(Processo processo) {
        String nupn = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), "PROC-" + UUID.randomUUID());
        String texto = firstNonBlank(processo.getResumoIA(), processo.getAssunto(), processo.getClasseProcessual());
        String oabNumero = processo.getUsuario() != null ? processo.getUsuario().getOabNumero() : null;
        String oabUf = processo.getUsuario() != null ? processo.getUsuario().getOabUf() : null;
        boolean liminar = containsAny(texto, Set.of("liminar", "tutela", "urgencia", "plantao", "inaudita"));
        return validationSupport.normalizarPedido(new PedidoTriagem(
                nupn,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getValorCausa(),
                texto,
                processo.getParteAutoraCpf(),
                processo.getParteReuCpf(),
                oabNumero,
                oabUf,
                List.of(),
                null,
                liminar,
                false,
                processo.getId()
        ));
    }

    private void persistirAnalise(PedidoTriagem pedido, ResultadoTriagem resultado) {
        TriagemNacionalAnalise entity = triagemRepository.findTopByNupnProvisorioOrderByTriadoEmDesc(resultado.nupnProvisorio())
                .orElseGet(TriagemNacionalAnalise::new);
        entity.setNupnProvisorio(resultado.nupnProvisorio());
        entity.setProcessoId(pedido.processoId());
        entity.setDocumentoAutor(pedido.cpfCnpjAutor());
        entity.setDocumentoReu(pedido.cpfCnpjReu());
        entity.setVeredito(resultado.veredito().name());
        entity.setConfiancaGeral(BigDecimal.valueOf(resultado.confiancaGeral()).setScale(4, RoundingMode.HALF_UP));
        entity.setAprovacaoAutomatica(resultado.aprovacaoAutomatica());
        entity.setClasseTpu(resultado.classificacao().classeTpu());
        entity.setAssuntoTpu(resultado.classificacao().assuntoTpu());
        entity.setCompetenciaSugerida(resultado.competencia().tipoJusticaSugerido());
        entity.setRitoSugerido(resultado.competencia().ritoSugerido());
        entity.setPrescricaoAparente(resultado.prescricao().prescricaoAparente());
        entity.setDecadenciaAparente(resultado.prescricao().decadenciaAparente());
        entity.setPendenciasJson(writeJson(resultado.pendencias()));
        entity.setConexosJson(writeJson(resultado.processosConexos()));
        entity.setResumoDecisao(resultado.resumoDecisao());
        entity.setRequestHash(Hashes.sha256Hex(writeJson(pedido)));
        entity.setTriadoEm(resultado.triadoEm());
        triagemRepository.save(entity);
        auditLedgerService.appendSafely("TRIAGEM_NACIONAL", "TRIAGEM", resultado.nupnProvisorio(), entity.getRequestHash(), resultado.resumoDecisao());
    }

    private void publicarEventos(PedidoTriagem pedido, ResultadoTriagem resultado) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nupnProvisorio", resultado.nupnProvisorio());
        payload.put("processoId", pedido.processoId());
        payload.put("veredito", resultado.veredito().name());
        payload.put("confiancaGeral", resultado.confiancaGeral());
        payload.put("aprovacaoAutomatica", resultado.aprovacaoAutomatica());
        payload.put("classeTpu", resultado.classificacao().classeTpu());
        payload.put("assuntoTpu", resultado.classificacao().assuntoTpu());
        payload.put("competencia", resultado.competencia().tipoJusticaSugerido());
        payload.put("rito", resultado.competencia().ritoSugerido());
        payload.put("pendencias", resultado.pendencias().size());
        payload.put("conexos", resultado.processosConexos().size());
        payload.put("triadoEm", resultado.triadoEm().toString());
        outboxPublisher.enqueue(
                "triagem:" + resultado.nupnProvisorio(),
                EVT_TRIAGEM_REALIZADA,
                payload,
                Map.of("source", "triagem_nacional"),
                "triagemNacional:" + resultado.nupnProvisorio() + ":" + resultado.veredito().name() + ":" + Hashes.sha256Hex(writeJson(payload)),
                "TriagemNacional",
                resultado.nupnProvisorio()
        );
    }

    private static List<PendenciaTriagem> deduplicarPendencias(Collection<PendenciaTriagem> pendencias) {
        Map<String, PendenciaTriagem> mapa = new LinkedHashMap<>();
        for (PendenciaTriagem pendencia : pendencias) {
            String chave = pendencia.tipo().name() + "|" + pendencia.descricao();
            mapa.putIfAbsent(chave, pendencia);
        }
        return new ArrayList<>(mapa.values());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("json_triagem", ex);
        }
    }

    private static boolean containsAny(String text, Set<String> needles) {
        String base = normalizeOptional(text);
        for (String needle : needles) {
            if (base.contains(normalizeOptional(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.strip().toUpperCase();
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

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private VereditoTriagem inferirVeredito(List<PendenciaTriagem> pendencias,
                                           SugestaoClassificacao classificacao,
                                           CompetenciaTriagem competencia,
                                           List<ProcessoSuspeito> conexos) {
        long bloqueantes = pendencias.stream().filter(PendenciaTriagem::impeditiva).count();
        boolean competenciaBloqueada = competencia != null && competencia.incompatibilidadeManifesta();
        if (bloqueantes > 0 || competenciaBloqueada) {
            return VereditoTriagem.BLOQUEADO;
        }
        if ((pendencias != null && !pendencias.isEmpty()) || (conexos != null && !conexos.isEmpty())) {
            return VereditoTriagem.REQUER_REVISAO_HUMANA;
        }
        return classificacao == null ? VereditoTriagem.PENDENTE_CORRECAO : VereditoTriagem.APROVADO;
    }
}
