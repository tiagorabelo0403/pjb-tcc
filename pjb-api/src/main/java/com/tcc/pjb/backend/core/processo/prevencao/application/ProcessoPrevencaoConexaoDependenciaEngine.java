package com.tcc.pjb.backend.core.processo.prevencao.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAchadoTipo;
import com.tcc.pjb.backend.core.identidade.vinculo.application.IdentidadeJuridicaVinculoApplicationService;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaPapelProcessual;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoAggregate;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoSolicitacao;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoItem;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseItem;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculoTipo;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoPrevencaoConexaoDependenciaEngine {

    private final ProcessoRepository processoRepository;
    private final IdentidadeJuridicaVinculoApplicationService vinculoApplicationService;
    private final DecisionTraceService decisionTraceService;
    private final ObjectMapper objectMapper;

    public ProcessoPrevencaoConexaoDependenciaEngine(ProcessoRepository processoRepository,
                                                     IdentidadeJuridicaVinculoApplicationService vinculoApplicationService,
                                                     ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                     ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.vinculoApplicationService = Objects.requireNonNull(vinculoApplicationService);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public ProcessoVinculacaoAnaliseAggregate analisar(ProcessoVinculacaoAnaliseConsulta consulta) {
        Processo raiz = carregarProcesso(consulta);
        IdentidadeJuridicaVinculoAggregate vinculo = vinculoApplicationService.analisar(new IdentidadeJuridicaVinculoSolicitacao(
                raiz.getId(),
                raiz.getNumero(),
                true,
                consulta.solicitante(),
                blankToNull(consulta.origemSolicitacao()) == null ? "PROCESSO_VINCULACAO" : consulta.origemSolicitacao()
        ));
        List<Processo> correlatos = carregarCorrelatos(raiz, vinculo);
        List<ProcessoVinculacaoAnaliseItem> itens = new ArrayList<>();
        for (Processo correlato : correlatos) {
            itens.addAll(analisarCorrelato(raiz, correlato, vinculo));
        }
        List<ProcessoVinculacaoAnaliseItem> ordenados = itens.stream()
                .sorted(Comparator.comparingDouble(ProcessoVinculacaoAnaliseItem::score).reversed()
                        .thenComparing(ProcessoVinculacaoAnaliseItem::tipo)
                        .thenComparing(ProcessoVinculacaoAnaliseItem::numeroProcesso))
                .toList();
        List<String> fundamentos = consolidarFundamentos(raiz, vinculo, ordenados);
        ProcessoVinculacaoAnaliseAggregate aggregate = new ProcessoVinculacaoAnaliseAggregate(
                raiz.getId(),
                raiz.getNumero(),
                ordenados,
                fundamentos,
                vinculo,
                Instant.now()
        );
        registrarExplainability(aggregate);
        return aggregate;
    }

    public List<ProcessoPrevencaoItem> extrairPrevencao(ProcessoVinculacaoAnaliseAggregate aggregate) {
        return aggregate.itens().stream()
                .filter(item -> item.tipo() == ProcessoVinculoTipo.PREVENCAO)
                .map(item -> new ProcessoPrevencaoItem(
                        item.processoId(),
                        item.numeroProcesso(),
                        item.natureza(),
                        item.score(),
                        item.bloquearDistribuicao(),
                        item.remeterPorPrevencao(),
                        item.unidadeSugerida(),
                        item.fundamentos(),
                        item.distribuidoEm()
                ))
                .toList();
    }

    private Processo carregarProcesso(ProcessoVinculacaoAnaliseConsulta consulta) {
        if (consulta.processoId() != null) {
            return processoRepository.findById(consulta.processoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", consulta.processoId()));
        }
        return processoRepository.findByNumero(consulta.numeroProcesso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", consulta.numeroProcesso()));
    }

    private List<Processo> carregarCorrelatos(Processo raiz, IdentidadeJuridicaVinculoAggregate vinculo) {
        LinkedHashMap<Long, Processo> correlatos = new LinkedHashMap<>();
        String autorDoc = digits(raiz.getParteAutoraCpf());
        String reuDoc = digits(raiz.getParteReuCpf());
        Usuario usuario = raiz.getUsuario();
        String advogadoCpf = usuario == null ? null : digits(usuario.getCpf());
        if (autorDoc != null && autorDoc.length() == 11) {
            processoRepository.findAllByPartesCpf(autorDoc).forEach(processo -> correlatos.put(processo.getId(), processo));
        }
        if (reuDoc != null && reuDoc.length() == 11) {
            processoRepository.findAllByPartesCpf(reuDoc).forEach(processo -> correlatos.put(processo.getId(), processo));
        }
        if (advogadoCpf != null && advogadoCpf.length() == 11) {
            processoRepository.findByAdvogadoCpf(advogadoCpf, PageRequest.of(0, 250)).forEach(processo -> correlatos.put(processo.getId(), processo));
        }
        vinculo.grafo().vertices().stream()
                .filter(vertice -> vertice.tipo() == com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo.PROCESSO)
                .map(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice::chaveCanonica)
                .filter(numero -> !numero.equals(raiz.getNumero()))
                .forEach(numero -> processoRepository.findByNumero(numero).ifPresent(processo -> correlatos.put(processo.getId(), processo)));
        correlatos.remove(raiz.getId());
        return correlatos.values().stream().toList();
    }

    private List<ProcessoVinculacaoAnaliseItem> analisarCorrelato(Processo raiz,
                                                                  Processo correlato,
                                                                  IdentidadeJuridicaVinculoAggregate vinculo) {
        CandidateContext context = candidateContext(raiz, correlato, vinculo);
        ArrayList<ProcessoVinculacaoAnaliseItem> itens = new ArrayList<>();
        if (context.scoreConexao() >= 0.60d || context.grafoCorrelato()) {
            itens.add(new ProcessoVinculacaoAnaliseItem(
                    ProcessoVinculoTipo.CONEXAO,
                    correlato.getId(),
                    correlato.getNumero(),
                    context.naturezaConexao(),
                    context.scoreConexao(),
                    false,
                    false,
                    unidadeSugerida(raiz, correlato),
                    context.chavesCompartilhadas(),
                    fundamentosConexao(raiz, correlato, context),
                    toInstant(correlato.getDataDistribuicao())
            ));
        }
        if (context.haPrevencao()) {
            itens.add(new ProcessoVinculacaoAnaliseItem(
                    ProcessoVinculoTipo.PREVENCAO,
                    correlato.getId(),
                    correlato.getNumero(),
                    context.naturezaPrevencao(),
                    context.scorePrevencao(),
                    true,
                    true,
                    unidadeSugerida(raiz, correlato),
                    context.chavesCompartilhadas(),
                    fundamentosPrevencao(raiz, correlato, context),
                    toInstant(correlato.getDataDistribuicao())
            ));
        }
        if (context.naturezaDependencia() != null) {
            itens.add(new ProcessoVinculacaoAnaliseItem(
                    ProcessoVinculoTipo.DEPENDENCIA,
                    correlato.getId(),
                    correlato.getNumero(),
                    context.naturezaDependencia(),
                    context.scoreDependencia(),
                    context.scoreDependencia() >= 0.70d,
                    false,
                    unidadeSugerida(raiz, correlato),
                    context.chavesCompartilhadas(),
                    fundamentosDependencia(raiz, correlato, context),
                    toInstant(correlato.getDataDistribuicao())
            ));
        }
        return itens;
    }

    private CandidateContext candidateContext(Processo raiz,
                                              Processo correlato,
                                              IdentidadeJuridicaVinculoAggregate vinculo) {
        LinkedHashSet<String> chavesCompartilhadas = new LinkedHashSet<>();
        boolean mesmoAutor = compartilhaDocumento(raiz.getParteAutoraCpf(), correlato.getParteAutoraCpf(), correlato.getParteReuCpf());
        boolean mesmoReu = compartilhaDocumento(raiz.getParteReuCpf(), correlato.getParteAutoraCpf(), correlato.getParteReuCpf());
        boolean advogadoRecorrente = compartilhaAdvogado(raiz.getUsuario(), correlato.getUsuario());
        if (mesmoAutor) {
            chavesCompartilhadas.add("AUTOR:" + digits(raiz.getParteAutoraCpf()));
        }
        if (mesmoReu) {
            chavesCompartilhadas.add("REU:" + digits(raiz.getParteReuCpf()));
        }
        if (advogadoRecorrente && raiz.getUsuario() != null) {
            chavesCompartilhadas.add("ADVOGADO:" + digits(raiz.getUsuario().getCpf()));
        }
        boolean classeCompativel = semanticamenteProximo(raiz.getClasseProcessual(), correlato.getClasseProcessual());
        boolean assuntoCompativel = semanticamenteProximo(raiz.getAssunto(), correlato.getAssunto()) || semanticamenteProximo(raiz.getObjetoProcessual(), correlato.getObjetoProcessual());
        boolean mesmoTribunal = Objects.equals(blankToNull(raiz.getTribunal()), blankToNull(correlato.getTribunal()));
        boolean mesmaUnidade = Objects.equals(blankToNull(raiz.getUnidadeJudiciariaCodigo()), blankToNull(correlato.getUnidadeJudiciariaCodigo()))
                || Objects.equals(blankToNull(raiz.getVara()), blankToNull(correlato.getVara()));
        boolean grafoCorrelato = vinculo.grafo().vertices().stream()
                .anyMatch(vertice -> vertice.tipo() == com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo.PROCESSO
                        && Objects.equals(vertice.chaveCanonica(), correlato.getNumero()));
        boolean conexaoOculta = vinculo.grafo().achados().stream()
                .anyMatch(achado -> achado.tipo() == IdentidadeJuridicaAchadoTipo.CONEXAO_OCULTA);
        boolean litiganteRepetitivo = vinculo.grafo().achados().stream()
                .anyMatch(achado -> achado.tipo() == IdentidadeJuridicaAchadoTipo.LITIGANCIA_REPETITIVA);
        String naturezaDependencia = classificarDependencia(raiz, correlato);
        double scoreConexao = clamp(
                (mesmoAutor ? 0.33d : 0d)
                        + (mesmoReu ? 0.33d : 0d)
                        + (advogadoRecorrente ? 0.10d : 0d)
                        + (classeCompativel ? 0.09d : 0d)
                        + (assuntoCompativel ? 0.09d : 0d)
                        + (mesmoTribunal ? 0.03d : 0d)
                        + (mesmaUnidade ? 0.03d : 0d)
                        + (grafoCorrelato || conexaoOculta ? 0.15d : 0d)
        );
        boolean correlatoMaisAntigo = isBefore(correlato.getDataDistribuicao(), raiz.getDataDistribuicao())
                || isBefore(correlato.getDataCriacao(), raiz.getDataCriacao());
        double scorePrevencao = clamp(scoreConexao + (correlatoMaisAntigo ? 0.15d : 0d) + (mesmaUnidade ? 0.05d : 0d));
        boolean haPrevencao = scoreConexao >= 0.70d && correlatoMaisAntigo && mesmoTribunal && (mesmaUnidade || classeCompativel || assuntoCompativel);
        double scoreDependencia = naturezaDependencia == null
                ? 0d
                : clamp(0.64d + (grafoCorrelato ? 0.12d : 0d) + (mesmoTribunal ? 0.05d : 0d) + (litiganteRepetitivo ? 0.03d : 0d));
        String naturezaConexao = grafoCorrelato || conexaoOculta
                ? "CONEXAO_OCULTA_MATERIALIZADA"
                : litiganteRepetitivo ? "LITIGANCIA_REPETITIVA_CORRELATA" : "CONEXAO_MATERIAL_OU_PROBATORIA";
        String naturezaPrevencao = mesmaUnidade
                ? "PREVENCAO_POR_UNIDADE_E_ANTERIORIDADE"
                : "PREVENCAO_POR_ANTERIORIDADE_MATERIAL";
        return new CandidateContext(
                List.copyOf(chavesCompartilhadas),
                grafoCorrelato || conexaoOculta,
                scoreConexao,
                naturezaConexao,
                haPrevencao,
                scorePrevencao,
                naturezaPrevencao,
                naturezaDependencia,
                scoreDependencia
        );
    }

    private List<String> fundamentosConexao(Processo raiz, Processo correlato, CandidateContext context) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A conexão é reconhecida quando partes, causa de pedir, objeto, prova ou cluster de identidade convergem além do acaso operacional.");
        if (!context.chavesCompartilhadas().isEmpty()) {
            fundamentos.add("Há chaves compartilhadas entre os feitos: " + String.join(", ", context.chavesCompartilhadas()) + ".");
        }
        if (context.grafoCorrelato()) {
            fundamentos.add("O grafo nacional de identidade jurídica já aproximou os dois processos por caminho oculto ou correlação relevante.");
        }
        if (semanticamenteProximo(raiz.getClasseProcessual(), correlato.getClasseProcessual())) {
            fundamentos.add("Classe processual semanticamente próxima reforçou a conexão material.");
        }
        if (semanticamenteProximo(raiz.getAssunto(), correlato.getAssunto())) {
            fundamentos.add("Assunto processual semanticamente próximo reforçou a conexão probatória e temática.");
        }
        return List.copyOf(fundamentos);
    }

    private List<String> fundamentosPrevencao(Processo raiz, Processo correlato, CandidateContext context) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A prevenção deriva da anterioridade útil de feito conexo capaz de atrair o novo processo ou travar distribuição autônoma.");
        fundamentos.addAll(fundamentosConexao(raiz, correlato, context));
        fundamentos.add("O processo correlato é anterior em distribuição ou criação e preserva o juízo prevento.");
        if (Objects.equals(blankToNull(raiz.getUnidadeJudiciariaCodigo()), blankToNull(correlato.getUnidadeJudiciariaCodigo()))) {
            fundamentos.add("A coincidência de unidade judiciária reforça a remessa por prevenção.");
        }
        return List.copyOf(fundamentos);
    }

    private List<String> fundamentosDependencia(Processo raiz, Processo correlato, CandidateContext context) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A dependência é tratada como vínculo forte entre processos cuja utilidade, admissibilidade ou andamento pressupõe o outro feito.");
        fundamentos.add("Natureza da dependência detectada: " + context.naturezaDependencia() + ".");
        if (!context.chavesCompartilhadas().isEmpty()) {
            fundamentos.add("As mesmas partes ou representantes aparecem nos dois feitos, reduzindo a chance de dependência acidental.");
        }
        if (context.grafoCorrelato()) {
            fundamentos.add("O grafo reforçou a dependência ao projetar correlação processual entre os números analisados.");
        }
        return List.copyOf(fundamentos);
    }

    private List<String> consolidarFundamentos(Processo raiz,
                                               IdentidadeJuridicaVinculoAggregate vinculo,
                                               List<ProcessoVinculacaoAnaliseItem> itens) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("Prevenção, conexão e dependência devem nascer do mesmo plano de identidade, partes, distribuição e explicabilidade.");
        fundamentos.add("O motor nacional evita bifurcar inteligência entre módulos que observam o mesmo fato processual por lentes diferentes.");
        if (!vinculo.alertas().isEmpty()) {
            fundamentos.addAll(vinculo.alertas());
        }
        long totalPrevencao = itens.stream().filter(item -> item.tipo() == ProcessoVinculoTipo.PREVENCAO).count();
        long totalConexao = itens.stream().filter(item -> item.tipo() == ProcessoVinculoTipo.CONEXAO).count();
        long totalDependencia = itens.stream().filter(item -> item.tipo() == ProcessoVinculoTipo.DEPENDENCIA).count();
        fundamentos.add("O processo raiz " + raiz.getNumero() + " gerou " + totalPrevencao + " alertas de prevenção, " + totalConexao + " de conexão e " + totalDependencia + " de dependência.");
        return List.copyOf(fundamentos);
    }

    private void registrarExplainability(ProcessoVinculacaoAnaliseAggregate aggregate) {
        if (decisionTraceService == null) {
            return;
        }
        try {
            String reasonsJson = objectMapper.writeValueAsString(aggregate.itens().stream().map(ProcessoVinculacaoAnaliseItem::fundamentos).toList());
            String metadataJson = objectMapper.writeValueAsString(Map.of(
                    "processo", aggregate.numeroProcessoRaiz(),
                    "itens", aggregate.itens().size(),
                    "prevencao", aggregate.itens().stream().filter(item -> item.tipo() == ProcessoVinculoTipo.PREVENCAO).count(),
                    "conexao", aggregate.itens().stream().filter(item -> item.tipo() == ProcessoVinculoTipo.CONEXAO).count(),
                    "dependencia", aggregate.itens().stream().filter(item -> item.tipo() == ProcessoVinculoTipo.DEPENDENCIA).count(),
                    "fingerprint", aggregate.vinculo().grafo().resumo().fingerprint()
            ));
            decisionTraceService.record(
                    "PROCESSO_PREVENCAO_CONEXAO_DEPENDENCIA",
                    "PROCESSO",
                    aggregate.numeroProcessoRaiz(),
                    BigDecimal.valueOf(aggregate.itens().stream().mapToDouble(ProcessoVinculacaoAnaliseItem::score).average().orElse(0d)),
                    reasonsJson,
                    null,
                    Hashes.sha256Hex(aggregate.numeroProcessoRaiz() + "#" + aggregate.itens().size()),
                    Hashes.sha256Hex(aggregate.vinculo().grafo().resumo().fingerprint() + "#" + aggregate.itens().size()),
                    "pjb-processo-vinculacao-v1",
                    metadataJson
            );
        } catch (JsonProcessingException ignored) {
        }
    }

    private String classificarDependencia(Processo raiz, Processo correlato) {
        String classeRaiz = normalizeText(firstNonBlank(raiz.getClasseProcessual(), raiz.getAssunto(), raiz.getObjetoProcessual()));
        String classeCorrelata = normalizeText(firstNonBlank(correlato.getClasseProcessual(), correlato.getAssunto(), correlato.getObjetoProcessual()));
        if ((containsAny(classeRaiz, "EMBARGOS", "EXCECAO PRE EXECUTIVIDADE") && containsAny(classeCorrelata, "EXECUCAO", "CUMPRIMENTO DE SENTENCA"))
                || (containsAny(classeCorrelata, "EMBARGOS", "EXCECAO PRE EXECUTIVIDADE") && containsAny(classeRaiz, "EXECUCAO", "CUMPRIMENTO DE SENTENCA"))) {
            return "EXECUCAO_E_EMBARGOS";
        }
        if ((containsAny(classeRaiz, "CAUTELAR", "LIMINAR", "TUTELA", "ANTECIPADA") && !containsAny(classeCorrelata, "CAUTELAR", "LIMINAR", "TUTELA"))
                || (containsAny(classeCorrelata, "CAUTELAR", "LIMINAR", "TUTELA", "ANTECIPADA") && !containsAny(classeRaiz, "CAUTELAR", "LIMINAR", "TUTELA"))) {
            return "CAUTELAR_E_PRINCIPAL";
        }
        if ((containsAny(classeRaiz, "ACAO PENAL", "QUEIXA CRIME", "INQUERITO") && containsAny(classeCorrelata, "MEDIDA CAUTELAR", "PRISAO", "BUSCA", "APREENSAO"))
                || (containsAny(classeCorrelata, "ACAO PENAL", "QUEIXA CRIME", "INQUERITO") && containsAny(classeRaiz, "MEDIDA CAUTELAR", "PRISAO", "BUSCA", "APREENSAO"))) {
            return "ACAO_PENAL_E_MEDIDA_CORRELATA";
        }
        if ((containsAny(classeRaiz, "INCIDENTE", "CUMPRIMENTO DE SENTENCA", "HABILITACAO", "LIQUIDACAO") && semanticamenteProximo(raiz.getAssunto(), correlato.getAssunto()))
                || (containsAny(classeCorrelata, "INCIDENTE", "CUMPRIMENTO DE SENTENCA", "HABILITACAO", "LIQUIDACAO") && semanticamenteProximo(raiz.getAssunto(), correlato.getAssunto()))) {
            return "INCIDENTE_E_PRINCIPAL";
        }
        return null;
    }

    private boolean compartilhaDocumento(String principal, String... candidatos) {
        String canonicalPrincipal = digits(principal);
        if (canonicalPrincipal == null) {
            return false;
        }
        for (String candidato : candidatos) {
            String canonicalCandidate = digits(candidato);
            if (canonicalPrincipal.equals(canonicalCandidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean compartilhaAdvogado(Usuario esquerda, Usuario direita) {
        if (esquerda == null || direita == null) {
            return false;
        }
        String cpfEsquerda = digits(esquerda.getCpf());
        String cpfDireita = digits(direita.getCpf());
        if (cpfEsquerda != null && cpfEsquerda.equals(cpfDireita)) {
            return true;
        }
        String oabEsquerda = normalizeText(firstNonBlank(esquerda.getOabNormalizada(), esquerda.getOab()));
        String oabDireita = normalizeText(firstNonBlank(direita.getOabNormalizada(), direita.getOab()));
        return oabEsquerda != null && oabEsquerda.equals(oabDireita);
    }

    private boolean semanticamenteProximo(String left, String right) {
        String normalizedLeft = normalizeText(left);
        String normalizedRight = normalizeText(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return true;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return true;
        }
        Set<String> leftTokens = tokenizar(normalizedLeft);
        Set<String> rightTokens = tokenizar(normalizedRight);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }
        long intersecao = leftTokens.stream().filter(rightTokens::contains).count();
        double score = (double) intersecao / (double) Math.min(leftTokens.size(), rightTokens.size());
        return score >= 0.6d;
    }

    private Set<String> tokenizar(String value) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : value.split(" ")) {
            String normalized = blankToNull(token);
            if (normalized != null && normalized.length() >= 3) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }

    private boolean containsAny(String source, String... needles) {
        if (source == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String normalizedNeedle = normalizeText(needle);
            if (normalizedNeedle != null && source.contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private String unidadeSugerida(Processo raiz, Processo correlato) {
        return firstNonBlank(correlato.getUnidadeJudiciariaCodigo(), correlato.getVara(), raiz.getUnidadeJudiciariaCodigo(), raiz.getVara());
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.EPOCH : value.toInstant(ZoneOffset.UTC);
    }

    private boolean isBefore(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return left.isBefore(right);
    }

    private String normalizeText(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String digits(String value) {
        String normalized = Objects.toString(value, "").replaceAll("\\D", "");
        return normalized.isBlank() ? null : normalized;
    }

    private double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private record CandidateContext(
            List<String> chavesCompartilhadas,
            boolean grafoCorrelato,
            double scoreConexao,
            String naturezaConexao,
            boolean haPrevencao,
            double scorePrevencao,
            String naturezaPrevencao,
            String naturezaDependencia,
            double scoreDependencia
    ) {
    }
}
