package com.tcc.pjb.backend.core.identidade.grafo.infrastructure;

import com.tcc.pjb.backend.core.identidade.grafo.application.IdentidadeJuridicaFontePort;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaArestaTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaChaveTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaConsulta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSemente;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSnapshot;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessoIdentidadeJuridicaFonteAdapter implements IdentidadeJuridicaFontePort {

    private final ProcessoRepository processoRepository;

    public ProcessoIdentidadeJuridicaFonteAdapter(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Override
    public String codigoFonte() {
        return "PROCESSO_JPA";
    }

    @Override
    public int prioridade() {
        return 20;
    }

    @Override
    public boolean suporta(IdentidadeJuridicaConsulta consulta) {
        return consulta != null && (!consulta.processosRaiz().isEmpty() || consulta.sementes().stream().anyMatch(this::seedSuportada));
    }

    @Override
    @Transactional(readOnly = true)
    public IdentidadeJuridicaSnapshot resolver(IdentidadeJuridicaConsulta consulta) {
        LinkedHashSet<Processo> processos = new LinkedHashSet<>();
        consulta.processosRaiz().forEach(numero -> processoRepository.findByNumero(numero).ifPresent(processos::add));
        for (IdentidadeJuridicaSemente semente : consulta.sementes()) {
            switch (semente.tipo()) {
                case PROCESSO -> processoRepository.findByNumero(semente.valor()).ifPresent(processos::add);
                case CPF -> processos.addAll(processoRepository.findAllByPartesCpf(semente.valor()));
                default -> {
                }
            }
        }
        ArrayList<IdentidadeJuridicaVertice> vertices = new ArrayList<>();
        ArrayList<IdentidadeJuridicaAresta> arestas = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        for (Processo processo : processos) {
            materializarProcesso(processo, vertices, arestas, fundamentos);
        }
        fundamentos.add("A fonte processual materializou processos, partes, representação e atuação institucional com base no acervo transacional do PJB.");
        return new IdentidadeJuridicaSnapshot(
                codigoFonte(),
                false,
                deduplicarVertices(vertices),
                deduplicarArestas(arestas),
                List.copyOf(fundamentos),
                "PROCESSOS=" + processos.size() + "; EMITIDO_EM=" + Instant.now()
        );
    }

    private boolean seedSuportada(IdentidadeJuridicaSemente semente) {
        return semente != null && switch (semente.tipo()) {
            case CPF, PROCESSO -> true;
            default -> false;
        };
    }

    private void materializarProcesso(Processo processo,
                                      List<IdentidadeJuridicaVertice> vertices,
                                      List<IdentidadeJuridicaAresta> arestas,
                                      Collection<String> fundamentos) {
        String numeroProcesso = canonicalProcesso(processo.getNumero());
        if (numeroProcesso == null) {
            return;
        }
        IdentidadeJuridicaVertice processoVertice = vertice(
                IdentidadeJuridicaVerticeTipo.PROCESSO,
                numeroProcesso,
                processo.getNumero(),
                0.99d,
                Map.of(
                        "ramoDireito", safeName(processo.getRamoDireito()),
                        "rito", safeName(processo.getRito()),
                        "tribunal", Objects.toString(processo.getTribunal(), ""),
                        "unidade", Objects.toString(processo.getVara(), ""),
                        "classeProcessual", Objects.toString(processo.getClasseProcessual(), "")
                )
        );
        vertices.add(processoVertice);
        fundamentos.add("O processo " + processo.getNumero() + " foi incorporado ao grafo como nó estruturante de correlação.");

        IdentidadeJuridicaVertice autor = pessoaVertice(processo.getParteAutoraCpf(), processo.getParteAutoraNome(), "AUTOR", vertices);
        if (autor != null) {
            arestas.add(aresta(autor.id(), processoVertice.id(), IdentidadeJuridicaArestaTipo.PARTE_EM, false, 0.97d, Map.of("polo", "ATIVO")));
        }
        IdentidadeJuridicaVertice reu = pessoaVertice(processo.getParteReuCpf(), processo.getParteReuNome(), "REU", vertices);
        if (reu != null) {
            arestas.add(aresta(reu.id(), processoVertice.id(), IdentidadeJuridicaArestaTipo.PARTE_EM, false, 0.97d, Map.of("polo", "PASSIVO")));
        }
        if (autor != null && reu != null) {
            arestas.add(aresta(autor.id(), reu.id(), IdentidadeJuridicaArestaTipo.ADVERSARIAL, true, 0.91d, Map.of("processo", processo.getNumero())));
        }

        Usuario usuario = processo.getUsuario();
        if (usuario != null) {
            materializarUsuarioLigadoAoProcesso(usuario, processoVertice, vertices, arestas);
        }

        if (notBlank(processo.getTribunal())) {
            IdentidadeJuridicaVertice unidade = vertice(
                    IdentidadeJuridicaVerticeTipo.UNIDADE_JUDICIARIA,
                    canonicalTexto(processo.getTribunal() + " " + Objects.toString(processo.getVara(), "")),
                    firstNonBlank(processo.getVara(), processo.getTribunal()),
                    0.86d,
                    Map.of(
                            "tribunal", Objects.toString(processo.getTribunal(), ""),
                            "uf", Objects.toString(processo.getUf(), ""),
                            "comarca", Objects.toString(processo.getComarca(), "")
                    )
            );
            vertices.add(unidade);
            arestas.add(aresta(processoVertice.id(), unidade.id(), IdentidadeJuridicaArestaTipo.LOTADO_EM, false, 0.82d, Map.of("fonte", codigoFonte())));
        }
    }

    private void materializarUsuarioLigadoAoProcesso(Usuario usuario,
                                                     IdentidadeJuridicaVertice processoVertice,
                                                     List<IdentidadeJuridicaVertice> vertices,
                                                     List<IdentidadeJuridicaAresta> arestas) {
        IdentidadeJuridicaVertice pessoaFisica = null;
        if (notBlank(usuario.getCpf())) {
            pessoaFisica = vertice(
                    IdentidadeJuridicaVerticeTipo.PESSOA_FISICA,
                    canonicalCpf(usuario.getCpf()),
                    firstNonBlank(usuario.getNome(), usuario.getCpf()),
                    0.95d,
                    Map.of(
                            "tipoUsuario", safeName(usuario.getTipoUsuario()),
                            "perfil", Objects.toString(usuario.getPerfil(), "")
                    )
            );
            vertices.add(pessoaFisica);
        }

        IdentidadeJuridicaVertice ator = null;
        if (usuario.isAdvogado() && notBlank(usuario.getOabNormalizada())) {
            ator = vertice(
                    IdentidadeJuridicaVerticeTipo.ADVOGADO,
                    canonicalOab(firstNonBlank(usuario.getOabNormalizada(), usuario.getOab())),
                    firstNonBlank(usuario.getNome(), usuario.getOabNormalizada(), usuario.getOab()),
                    0.98d,
                    Map.of(
                            "oab", firstNonBlank(usuario.getOabNormalizada(), usuario.getOab()),
                            "oabUf", Objects.toString(usuario.getOabUf(), ""),
                            "email", Objects.toString(usuario.getEmail(), "")
                    )
            );
        } else if (pessoaFisica != null) {
            ator = vertice(
                    IdentidadeJuridicaVerticeTipo.REPRESENTANTE,
                    canonicalTexto(firstNonBlank(usuario.getCpf(), usuario.getEmail(), usuario.getNome())),
                    firstNonBlank(usuario.getNome(), usuario.getEmail(), usuario.getCpf()),
                    0.88d,
                    Map.of(
                            "tipoUsuario", safeName(usuario.getTipoUsuario()),
                            "email", Objects.toString(usuario.getEmail(), "")
                    )
            );
        }
        if (ator != null) {
            vertices.add(ator);
            arestas.add(aresta(ator.id(), processoVertice.id(), IdentidadeJuridicaArestaTipo.ATUA_EM, false, 0.94d, Map.of("papel", safeName(usuario.getTipoUsuario()))));
        }
        if (pessoaFisica != null && ator != null && !Objects.equals(pessoaFisica.id(), ator.id())) {
            arestas.add(aresta(pessoaFisica.id(), ator.id(), IdentidadeJuridicaArestaTipo.IDENTIFICA, false, 0.96d, Map.of("fonte", codigoFonte())));
        }
        if (ator != null && notBlank(usuario.getEmail())) {
            IdentidadeJuridicaVertice email = vertice(
                    IdentidadeJuridicaVerticeTipo.EMAIL,
                    canonicalEmail(usuario.getEmail()),
                    usuario.getEmail(),
                    0.91d,
                    Map.of("dominio", dominio(usuario.getEmail()))
            );
            vertices.add(email);
            arestas.add(aresta(ator.id(), email.id(), IdentidadeJuridicaArestaTipo.POSSUI_CONTATO, false, 0.86d, Map.of("tipo", "EMAIL")));
        }
    }

    private IdentidadeJuridicaVertice pessoaVertice(String cpf,
                                                    String nome,
                                                    String polo,
                                                    List<IdentidadeJuridicaVertice> vertices) {
        String canonicalCpf = canonicalCpf(cpf);
        String canonicalNome = canonicalTexto(nome);
        if (canonicalCpf == null && canonicalNome == null) {
            return null;
        }
        IdentidadeJuridicaVertice pessoa = vertice(
                IdentidadeJuridicaVerticeTipo.PESSOA_FISICA,
                firstNonBlank(canonicalCpf, canonicalNome),
                firstNonBlank(nome, cpf, polo),
                canonicalCpf != null ? 0.94d : 0.72d,
                Map.of(
                        "nome", Objects.toString(nome, ""),
                        "cpf", Objects.toString(cpf, ""),
                        "polo", Objects.toString(polo, "")
                )
        );
        vertices.add(pessoa);
        return pessoa;
    }

    private List<IdentidadeJuridicaVertice> deduplicarVertices(List<IdentidadeJuridicaVertice> vertices) {
        LinkedHashMap<String, IdentidadeJuridicaVertice> unicos = new LinkedHashMap<>();
        for (IdentidadeJuridicaVertice vertice : vertices) {
            unicos.putIfAbsent(vertice.id(), vertice);
        }
        return List.copyOf(unicos.values());
    }

    private List<IdentidadeJuridicaAresta> deduplicarArestas(List<IdentidadeJuridicaAresta> arestas) {
        LinkedHashMap<String, IdentidadeJuridicaAresta> unicas = new LinkedHashMap<>();
        for (IdentidadeJuridicaAresta aresta : arestas) {
            unicas.putIfAbsent(aresta.id(), aresta);
        }
        return List.copyOf(unicas.values());
    }

    private IdentidadeJuridicaVertice vertice(IdentidadeJuridicaVerticeTipo tipo,
                                              String canonical,
                                              String rotulo,
                                              double confianca,
                                              Map<String, String> atributos) {
        return new IdentidadeJuridicaVertice(
                DeterministicUuid.v5("pjb-identidade-vertice", tipo.name() + ":" + canonical).toString(),
                tipo,
                canonical,
                Objects.toString(rotulo, canonical),
                confianca,
                Set.of(codigoFonte()),
                sanitize(atributos)
        );
    }

    private IdentidadeJuridicaAresta aresta(String origemId,
                                            String destinoId,
                                            IdentidadeJuridicaArestaTipo tipo,
                                            boolean bidirecional,
                                            double confianca,
                                            Map<String, String> atributos) {
        String left = origemId;
        String right = destinoId;
        if (bidirecional && left.compareTo(right) > 0) {
            left = destinoId;
            right = origemId;
        }
        String signature = left + "|" + tipo.name() + "|" + right + "|" + sanitize(atributos);
        return new IdentidadeJuridicaAresta(
                DeterministicUuid.v5("pjb-identidade-aresta", signature).toString(),
                origemId,
                destinoId,
                tipo,
                confianca,
                bidirecional,
                Set.of(codigoFonte()),
                sanitize(atributos)
        );
    }

    private Map<String, String> sanitize(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = blankToNull(key);
            String normalizedValue = blankToNull(value);
            if (normalizedKey != null && normalizedValue != null) {
                sanitized.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(sanitized);
    }

    private String dominio(String email) {
        String normalized = canonicalEmail(email);
        if (normalized == null || !normalized.contains("@")) {
            return "";
        }
        return normalized.substring(normalized.indexOf('@') + 1);
    }

    private String canonicalProcesso(String value) {
        return digitsOnly(value);
    }

    private String canonicalCpf(String value) {
        return digitsOnly(value);
    }

    private String canonicalOab(String value) {
        return digitsOnly(value);
    }

    private String canonicalEmail(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String canonicalTexto(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String digitsOnly(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        String digits = normalized.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
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

    private String safeName(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean notBlank(String value) {
        return blankToNull(value) != null;
    }

    private String blankToNull(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
