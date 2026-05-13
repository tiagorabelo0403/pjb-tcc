package com.tcc.pjb.backend.core.identidade.resolucao.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.identidade.grafo.application.IdentidadeJuridicaGraphApplicationService;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaChaveTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaConsulta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSemente;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoAggregate;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoEntrada;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoItem;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoStatus;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.stereotype.Service;

@Service
public class IdentidadeJuridicaResolucaoApplicationService {

    private final IdentidadeJuridicaGraphApplicationService graphApplicationService;
    private final DecisionTraceService decisionTraceService;
    private final ObjectMapper objectMapper;

    public IdentidadeJuridicaResolucaoApplicationService(IdentidadeJuridicaGraphApplicationService graphApplicationService,
                                                         ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                         ObjectMapper objectMapper) {
        this.graphApplicationService = Objects.requireNonNull(graphApplicationService);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public IdentidadeJuridicaResolucaoAggregate resolver(String solicitante,
                                                         String origemSolicitacao,
                                                         List<IdentidadeJuridicaResolucaoEntrada> entradas,
                                                         List<String> processosRaiz) {
        List<IdentidadeJuridicaResolucaoEntrada> normalizedEntries = entradas == null ? List.of() : entradas.stream()
                .filter(Objects::nonNull)
                .toList();
        if (normalizedEntries.isEmpty() && (processosRaiz == null || processosRaiz.isEmpty())) {
            throw new IllegalArgumentException("a resolução de identidade exige entradas ou processos raiz");
        }
        String correlacaoId = DeterministicUuid.v5(
                "pjb-identidade-resolucao",
                normalizedEntries.stream().map(this::identityText).sorted().reduce("", (left, right) -> left + "|" + right)
                        + "#"
                        + Objects.toString(processosRaiz, "")
        ).toString();
        List<IdentidadeJuridicaSemente> sementes = normalizedEntries.stream()
                .flatMap(entry -> sementes(entry).stream())
                .distinct()
                .toList();
        IdentidadeJuridicaGraphAggregate grafo = graphApplicationService.analisar(new IdentidadeJuridicaConsulta(
                correlacaoId,
                solicitante,
                sementes,
                processosRaiz,
                4,
                800,
                2400,
                true,
                false,
                blankToNull(origemSolicitacao) == null ? "IDENTIDADE_RESOLUCAO" : origemSolicitacao.trim()
        ));
        List<IdentidadeJuridicaResolucaoItem> itens = normalizedEntries.stream()
                .map(entry -> resolverItem(entry, grafo))
                .sorted(Comparator.comparing(IdentidadeJuridicaResolucaoItem::confianca).reversed()
                        .thenComparing(IdentidadeJuridicaResolucaoItem::codigo))
                .toList();
        List<String> conflitos = detectarConflitos(normalizedEntries, itens);
        IdentidadeJuridicaResolucaoAggregate aggregate = new IdentidadeJuridicaResolucaoAggregate(
                correlacaoId,
                Objects.toString(solicitante, "").trim(),
                Objects.toString(origemSolicitacao, "").trim(),
                normalizedEntries,
                itens,
                conflitos,
                grafo,
                Instant.now()
        );
        registrarExplainability(aggregate);
        return aggregate;
    }

    private IdentidadeJuridicaResolucaoItem resolverItem(IdentidadeJuridicaResolucaoEntrada entry, IdentidadeJuridicaGraphAggregate grafo) {
        List<IdentidadeJuridicaSemente> sementes = sementes(entry);
        List<IdentidadeJuridicaVerticeMatch> matches = new ArrayList<>();
        for (IdentidadeJuridicaSemente semente : sementes) {
            String chaveCanonica = canonicalize(semente.tipo(), semente.valor());
            for (IdentidadeJuridicaVertice vertice : grafo.vertices()) {
                if (vertice.chaveCanonica().equals(chaveCanonica) || vertice.id().equals(vertexId(preferedVertexType(semente.tipo()), chaveCanonica))) {
                    matches.add(new IdentidadeJuridicaVerticeMatch(vertice, score(semente.tipo(), vertice.tipo())));
                }
            }
        }
        List<IdentidadeJuridicaVerticeMatch> ordered = matches.stream()
                .sorted(Comparator.comparingDouble(IdentidadeJuridicaVerticeMatch::score).reversed()
                        .thenComparing(match -> match.vertice().confianca(), Comparator.reverseOrder())
                        .thenComparing(match -> match.vertice().id()))
                .toList();
        IdentidadeJuridicaVerticeMatch primary = ordered.stream().findFirst().orElseGet(() -> fallback(entry));
        boolean ambiguo = ordered.stream().limit(3).map(match -> match.vertice().id()).distinct().count() > 1
                && ordered.stream().limit(2).allMatch(match -> match.score() >= 0.9d);
        IdentidadeJuridicaResolucaoStatus status = primary.score() < 0.65d
                ? IdentidadeJuridicaResolucaoStatus.FRACA
                : ambiguo ? IdentidadeJuridicaResolucaoStatus.AMBIGUA : IdentidadeJuridicaResolucaoStatus.RESOLVIDA;
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A resolução privilegia chaves fortes, preserva rastreabilidade de sementes e evita unir identidades por aproximação fraca.");
        if (entry.documento() != null) {
            fundamentos.add("Documento canônico presente na entrada elevou a confiança de resolução.");
        }
        if (entry.numeroOab() != null) {
            fundamentos.add("OAB canônica foi tratada como credencial representativa de advocacia.");
        }
        if (ambiguo) {
            fundamentos.add("Mais de um vértice forte surgiu para a mesma entrada e a resolução foi marcada como ambígua.");
        }
        if (status == IdentidadeJuridicaResolucaoStatus.FRACA) {
            fundamentos.add("A entrada não reuniu evidência documental suficiente e ficou em resolução fraca.");
        }
        LinkedHashMap<String, String> atributosNormalizados = new LinkedHashMap<>(entry.atributos());
        putIfNotNull(atributosNormalizados, "documentoCanonico", canonicalizeDocument(entry.documento()));
        putIfNotNull(atributosNormalizados, "emailCanonico", canonicalizeEmail(entry.email()));
        putIfNotNull(atributosNormalizados, "telefoneCanonico", canonicalizePhone(entry.telefone()));
        putIfNotNull(atributosNormalizados, "oabCanonica", canonicalizeOab(entry.numeroOab()));
        return new IdentidadeJuridicaResolucaoItem(
                DeterministicUuid.v5("pjb-identidade-resolucao-item", identityText(entry)).toString(),
                Objects.toString(entry.origem(), "ENTRADA"),
                firstNonBlank(entry.nome(), entry.documento(), entry.email(), entry.telefone(), entry.numeroOab()),
                primary.vertice().tipo(),
                primary.vertice().id(),
                primary.vertice().chaveCanonica(),
                primary.vertice().rotulo(),
                status,
                confidence(primary.score(), primary.vertice().confianca(), ambiguo),
                sementes,
                List.copyOf(fundamentos),
                Map.copyOf(atributosNormalizados)
        );
    }

    private List<String> detectarConflitos(List<IdentidadeJuridicaResolucaoEntrada> entradas,
                                           List<IdentidadeJuridicaResolucaoItem> itens) {
        LinkedHashSet<String> conflitos = new LinkedHashSet<>();
        Map<String, Set<String>> nomesPorDocumento = new LinkedHashMap<>();
        for (IdentidadeJuridicaResolucaoEntrada entrada : entradas) {
            String documento = canonicalizeDocument(entrada.documento());
            String nome = canonicalizeName(entrada.nome());
            if (documento != null && nome != null) {
                nomesPorDocumento.computeIfAbsent(documento, ignored -> new LinkedHashSet<>()).add(nome);
            }
        }
        nomesPorDocumento.forEach((documento, nomes) -> {
            if (nomes.size() > 1) {
                conflitos.add("O mesmo documento canônico apareceu associado a múltiplos nomes na entrada: " + documento + ".");
            }
        });
        long ambiguos = itens.stream().filter(item -> item.status() == IdentidadeJuridicaResolucaoStatus.AMBIGUA).count();
        if (ambiguos > 0) {
            conflitos.add("Há entradas com resolução ambígua que exigem saneamento antes de distribuir prevenção, conexão ou dependência.");
        }
        return List.copyOf(conflitos);
    }

    private void registrarExplainability(IdentidadeJuridicaResolucaoAggregate aggregate) {
        if (decisionTraceService == null) {
            return;
        }
        try {
            String reasonsJson = objectMapper.writeValueAsString(aggregate.itens().stream().map(IdentidadeJuridicaResolucaoItem::fundamentos).toList());
            String metadataJson = objectMapper.writeValueAsString(Map.of(
                    "correlacaoId", aggregate.correlacaoId(),
                    "totalEntradas", aggregate.entradas().size(),
                    "totalItens", aggregate.itens().size(),
                    "totalConflitos", aggregate.conflitos().size(),
                    "fingerprint", aggregate.grafo().resumo().fingerprint()
            ));
            decisionTraceService.record(
                    "IDENTIDADE_RESOLUCAO",
                    "IDENTIDADE_JURIDICA",
                    aggregate.correlacaoId(),
                    BigDecimal.valueOf(mediaConfianca(aggregate.itens())),
                    reasonsJson,
                    null,
                    Hashes.sha256Hex(aggregate.correlacaoId() + "#" + aggregate.entradas().size()),
                    Hashes.sha256Hex(aggregate.grafo().resumo().fingerprint() + "#" + aggregate.itens().size()),
                    "pjb-identidade-resolucao-v1",
                    metadataJson
            );
        } catch (JsonProcessingException ignored) {
        }
    }

    private double mediaConfianca(List<IdentidadeJuridicaResolucaoItem> itens) {
        return itens.isEmpty()
                ? 0d
                : itens.stream().mapToDouble(IdentidadeJuridicaResolucaoItem::confianca).average().orElse(0d);
    }

    private List<IdentidadeJuridicaSemente> sementes(IdentidadeJuridicaResolucaoEntrada entry) {
        LinkedHashSet<IdentidadeJuridicaSemente> seeds = new LinkedHashSet<>();
        addSeed(seeds, IdentidadeJuridicaChaveTipo.CPF, canonicalizeDocument(entry.documento()), entry);
        addSeed(seeds, IdentidadeJuridicaChaveTipo.CNPJ, canonicalizeCnpj(entry.documento()), entry);
        addSeed(seeds, IdentidadeJuridicaChaveTipo.OAB, canonicalizeOab(entry.numeroOab()), entry);
        addSeed(seeds, IdentidadeJuridicaChaveTipo.EMAIL, canonicalizeEmail(entry.email()), entry);
        addSeed(seeds, IdentidadeJuridicaChaveTipo.TELEFONE, canonicalizePhone(entry.telefone()), entry);
        addSeed(seeds, IdentidadeJuridicaChaveTipo.NOME, canonicalizeName(entry.nome()), entry);
        return List.copyOf(seeds);
    }

    private void addSeed(Set<IdentidadeJuridicaSemente> seeds,
                         IdentidadeJuridicaChaveTipo tipo,
                         String value,
                         IdentidadeJuridicaResolucaoEntrada entry) {
        if (value == null) {
            return;
        }
        if (tipo == IdentidadeJuridicaChaveTipo.CPF && value.length() != 11) {
            return;
        }
        if (tipo == IdentidadeJuridicaChaveTipo.CNPJ && value.length() != 14) {
            return;
        }
        seeds.add(new IdentidadeJuridicaSemente(tipo, value, firstNonBlank(entry.nome(), value), entry.polo(), entry.atributos()));
    }

    private IdentidadeJuridicaVerticeMatch fallback(IdentidadeJuridicaResolucaoEntrada entry) {
        String documento = canonicalizeDocument(entry.documento());
        String email = canonicalizeEmail(entry.email());
        String telefone = canonicalizePhone(entry.telefone());
        String oab = canonicalizeOab(entry.numeroOab());
        String nome = canonicalizeName(entry.nome());
        if (documento != null && documento.length() == 11) {
            IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.PESSOA_FISICA, documento),
                    IdentidadeJuridicaVerticeTipo.PESSOA_FISICA,
                    documento,
                    firstNonBlank(entry.nome(), documento),
                    0.74d,
                    Set.of("RESOLUCAO"),
                    Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
            );
            return new IdentidadeJuridicaVerticeMatch(vertice, 0.74d);
        }
        if (documento != null && documento.length() == 14) {
            IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA, documento),
                    IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA,
                    documento,
                    firstNonBlank(entry.nome(), documento),
                    0.74d,
                    Set.of("RESOLUCAO"),
                    Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
            );
            return new IdentidadeJuridicaVerticeMatch(vertice, 0.74d);
        }
        if (oab != null) {
            IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.ADVOGADO, oab),
                    IdentidadeJuridicaVerticeTipo.ADVOGADO,
                    oab,
                    firstNonBlank(entry.nome(), oab),
                    0.72d,
                    Set.of("RESOLUCAO"),
                    Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
            );
            return new IdentidadeJuridicaVerticeMatch(vertice, 0.72d);
        }
        if (email != null) {
            IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.EMAIL, email),
                    IdentidadeJuridicaVerticeTipo.EMAIL,
                    email,
                    email,
                    0.65d,
                    Set.of("RESOLUCAO"),
                    Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
            );
            return new IdentidadeJuridicaVerticeMatch(vertice, 0.65d);
        }
        if (telefone != null) {
            IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.TELEFONE, telefone),
                    IdentidadeJuridicaVerticeTipo.TELEFONE,
                    telefone,
                    telefone,
                    0.62d,
                    Set.of("RESOLUCAO"),
                    Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
            );
            return new IdentidadeJuridicaVerticeMatch(vertice, 0.62d);
        }
        String chave = firstNonBlank(nome, identityText(entry));
        IdentidadeJuridicaVertice vertice = new IdentidadeJuridicaVertice(
                vertexId(IdentidadeJuridicaVerticeTipo.OUTRO, chave),
                IdentidadeJuridicaVerticeTipo.OUTRO,
                chave,
                firstNonBlank(entry.nome(), chave),
                0.51d,
                Set.of("RESOLUCAO"),
                Map.of("origem", Objects.toString(entry.origem(), "ENTRADA"))
        );
        return new IdentidadeJuridicaVerticeMatch(vertice, 0.51d);
    }

    private double score(IdentidadeJuridicaChaveTipo tipo, IdentidadeJuridicaVerticeTipo resolvedType) {
        return switch (tipo) {
            case CPF -> resolvedType == IdentidadeJuridicaVerticeTipo.PESSOA_FISICA || resolvedType == IdentidadeJuridicaVerticeTipo.REPRESENTANTE ? 0.99d : 0.91d;
            case CNPJ -> resolvedType == IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA || resolvedType == IdentidadeJuridicaVerticeTipo.ORGAO_PUBLICO ? 0.99d : 0.91d;
            case OAB -> resolvedType == IdentidadeJuridicaVerticeTipo.ADVOGADO || resolvedType == IdentidadeJuridicaVerticeTipo.REPRESENTANTE ? 0.97d : 0.84d;
            case EMAIL -> resolvedType == IdentidadeJuridicaVerticeTipo.EMAIL ? 0.84d : 0.76d;
            case TELEFONE -> resolvedType == IdentidadeJuridicaVerticeTipo.TELEFONE ? 0.80d : 0.72d;
            case NOME -> resolvedType == IdentidadeJuridicaVerticeTipo.PESSOA_FISICA || resolvedType == IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA ? 0.70d : 0.64d;
            default -> 0.6d;
        };
    }

    private double confidence(double score, double vertexConfidence, boolean ambiguo) {
        double value = (score * 0.65d) + (vertexConfidence * 0.35d) - (ambiguo ? 0.12d : 0d);
        return Math.max(0d, Math.min(1d, value));
    }

    private IdentidadeJuridicaVerticeTipo preferedVertexType(IdentidadeJuridicaChaveTipo tipo) {
        return switch (tipo) {
            case CPF -> IdentidadeJuridicaVerticeTipo.PESSOA_FISICA;
            case CNPJ -> IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA;
            case OAB -> IdentidadeJuridicaVerticeTipo.ADVOGADO;
            case EMAIL -> IdentidadeJuridicaVerticeTipo.EMAIL;
            case TELEFONE -> IdentidadeJuridicaVerticeTipo.TELEFONE;
            case NOME -> IdentidadeJuridicaVerticeTipo.OUTRO;
            case PROCESSO -> IdentidadeJuridicaVerticeTipo.PROCESSO;
            case ORGAO_PUBLICO -> IdentidadeJuridicaVerticeTipo.ORGAO_PUBLICO;
            case UNIDADE -> IdentidadeJuridicaVerticeTipo.UNIDADE_JUDICIARIA;
            case DOMINIO -> IdentidadeJuridicaVerticeTipo.DOMINIO;
            case DOCUMENTO -> IdentidadeJuridicaVerticeTipo.DOCUMENTO;
            case ENDERECO -> IdentidadeJuridicaVerticeTipo.ENDERECO;
            case UUID_EXTERNO -> IdentidadeJuridicaVerticeTipo.OUTRO;
        };
    }

    private String vertexId(IdentidadeJuridicaVerticeTipo tipo, String value) {
        return tipo.name() + ":" + value;
    }

    private String identityText(IdentidadeJuridicaResolucaoEntrada entry) {
        return String.join("|",
                Objects.toString(entry.origem(), ""),
                Objects.toString(entry.nome(), ""),
                Objects.toString(entry.documento(), ""),
                Objects.toString(entry.email(), ""),
                Objects.toString(entry.telefone(), ""),
                Objects.toString(entry.numeroOab(), ""),
                Objects.toString(entry.papel(), "")
        );
    }

    private String canonicalize(IdentidadeJuridicaChaveTipo tipo, String value) {
        return switch (tipo) {
            case CPF -> Optional.ofNullable(canonicalizeDocument(value)).filter(v -> v.length() == 11).orElse("");
            case CNPJ -> Optional.ofNullable(canonicalizeDocument(value)).filter(v -> v.length() == 14).orElse("");
            case OAB -> Objects.toString(canonicalizeOab(value), "");
            case EMAIL -> Objects.toString(canonicalizeEmail(value), "");
            case TELEFONE -> Objects.toString(canonicalizePhone(value), "");
            case NOME -> Objects.toString(canonicalizeName(value), "");
            default -> Objects.toString(blankToNull(value), "").trim();
        };
    }

    private String canonicalizeDocument(String value) {
        String digits = digits(value);
        return digits.length() == 11 || digits.length() == 14 ? digits : null;
    }

    private String canonicalizeCnpj(String value) {
        String digits = digits(value);
        return digits.length() == 14 ? digits : null;
    }

    private String canonicalizeOab(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String canonicalizeEmail(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String canonicalizePhone(String value) {
        String digits = digits(value);
        return digits.length() < 8 ? null : digits;
    }

    private String canonicalizeName(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String digits(String value) {
        String normalized = Objects.toString(value, "");
        return normalized.replaceAll("\\D", "");
    }

    private void putIfNotNull(Map<String, String> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String blankToNull(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private record IdentidadeJuridicaVerticeMatch(IdentidadeJuridicaVertice vertice, double score) {
    }
}
