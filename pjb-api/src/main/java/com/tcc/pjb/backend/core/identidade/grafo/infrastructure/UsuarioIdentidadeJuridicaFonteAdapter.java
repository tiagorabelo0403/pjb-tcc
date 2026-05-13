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
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsuarioIdentidadeJuridicaFonteAdapter implements IdentidadeJuridicaFontePort {

    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;

    public UsuarioIdentidadeJuridicaFonteAdapter(UsuarioRepository usuarioRepository,
                                                 ProcessoRepository processoRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Override
    public String codigoFonte() {
        return "USUARIO_JPA";
    }

    @Override
    public int prioridade() {
        return 10;
    }

    @Override
    public boolean suporta(IdentidadeJuridicaConsulta consulta) {
        return consulta != null && consulta.sementes().stream().anyMatch(this::seedSuportada);
    }

    @Override
    @Transactional(readOnly = true)
    public IdentidadeJuridicaSnapshot resolver(IdentidadeJuridicaConsulta consulta) {
        LinkedHashSet<Usuario> usuarios = new LinkedHashSet<>();
        for (IdentidadeJuridicaSemente semente : consulta.sementes()) {
            switch (semente.tipo()) {
                case CPF -> usuarioRepository.findByCpf(semente.valor()).ifPresent(usuarios::add);
                case EMAIL -> usuarioRepository.findByEmail(semente.valor()).ifPresent(usuarios::add);
                case OAB -> usuarioRepository.findByOabNormalizada(semente.valor()).ifPresent(usuarios::add);
                default -> {
                }
            }
        }
        ArrayList<IdentidadeJuridicaVertice> vertices = new ArrayList<>();
        ArrayList<IdentidadeJuridicaAresta> arestas = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        for (Usuario usuario : usuarios) {
            materializarUsuario(usuario, vertices, arestas, fundamentos);
        }
        fundamentos.add("A fonte de usuários reforçou advocacia, representação, contatos e vínculos operacionais com identidade jurídica já persistida no PJB.");
        return new IdentidadeJuridicaSnapshot(
                codigoFonte(),
                false,
                deduplicarVertices(vertices),
                deduplicarArestas(arestas),
                List.copyOf(fundamentos),
                "USUARIOS=" + usuarios.size()
        );
    }

    private boolean seedSuportada(IdentidadeJuridicaSemente semente) {
        return semente != null && switch (semente.tipo()) {
            case CPF, EMAIL, OAB -> true;
            default -> false;
        };
    }

    private void materializarUsuario(Usuario usuario,
                                     List<IdentidadeJuridicaVertice> vertices,
                                     List<IdentidadeJuridicaAresta> arestas,
                                     Set<String> fundamentos) {
        IdentidadeJuridicaVertice pessoaFisica = null;
        if (notBlank(usuario.getCpf())) {
            pessoaFisica = vertice(
                    IdentidadeJuridicaVerticeTipo.PESSOA_FISICA,
                    canonicalize(IdentidadeJuridicaChaveTipo.CPF, usuario.getCpf()),
                    firstNonBlank(usuario.getNome(), usuario.getCpf()),
                    0.98d,
                    Map.of(
                            "tipoUsuario", safeName(usuario.getTipoUsuario()),
                            "perfil", Objects.toString(usuario.getPerfil(), ""),
                            "uf", Objects.toString(usuario.getUf(), "")
                    )
            );
            vertices.add(pessoaFisica);
        }
        IdentidadeJuridicaVertice ator = verticeAtor(usuario);
        if (ator != null) {
            vertices.add(ator);
        }
        if (pessoaFisica != null && ator != null && !Objects.equals(pessoaFisica.id(), ator.id())) {
            arestas.add(aresta(pessoaFisica.id(), ator.id(), IdentidadeJuridicaArestaTipo.IDENTIFICA, false, 0.98d, Map.of("fonte", codigoFonte())));
        }
        if (ator != null && notBlank(usuario.getEmail())) {
            IdentidadeJuridicaVertice email = vertice(
                    IdentidadeJuridicaVerticeTipo.EMAIL,
                    canonicalize(IdentidadeJuridicaChaveTipo.EMAIL, usuario.getEmail()),
                    usuario.getEmail(),
                    0.94d,
                    Map.of("dominio", dominio(usuario.getEmail()))
            );
            vertices.add(email);
            arestas.add(aresta(ator.id(), email.id(), IdentidadeJuridicaArestaTipo.POSSUI_CONTATO, false, 0.92d, Map.of("tipo", "EMAIL")));
        }
        if (pessoaFisica != null) {
            for (Processo processo : processoRepository.findAllByPartesCpf(canonicalize(IdentidadeJuridicaChaveTipo.CPF, usuario.getCpf()))) {
                if (!notBlank(processo.getNumero())) {
                    continue;
                }
                IdentidadeJuridicaVertice processoVertice = vertice(
                        IdentidadeJuridicaVerticeTipo.PROCESSO,
                        canonicalize(IdentidadeJuridicaChaveTipo.PROCESSO, processo.getNumero()),
                        processo.getNumero(),
                        0.91d,
                        Map.of(
                                "tribunal", Objects.toString(processo.getTribunal(), ""),
                                "classeProcessual", Objects.toString(processo.getClasseProcessual(), "")
                        )
                );
                vertices.add(processoVertice);
                arestas.add(aresta(pessoaFisica.id(), processoVertice.id(), IdentidadeJuridicaArestaTipo.CORRELACIONA_COM, false, 0.81d, Map.of("origem", "CPF_COMPARTILHADO")));
            }
        }
        fundamentos.add("O usuário " + firstNonBlank(usuario.getNome(), usuario.getEmail(), usuario.getCpf()) + " expandiu o cluster de identidade com base em credenciais internas do PJB.");
    }

    private IdentidadeJuridicaVertice verticeAtor(Usuario usuario) {
        if (usuario.isAdvogado() && notBlank(firstNonBlank(usuario.getOabNormalizada(), usuario.getOab()))) {
            return vertice(
                    IdentidadeJuridicaVerticeTipo.ADVOGADO,
                    canonicalize(IdentidadeJuridicaChaveTipo.OAB, firstNonBlank(usuario.getOabNormalizada(), usuario.getOab())),
                    firstNonBlank(usuario.getNome(), usuario.getOabNormalizada(), usuario.getOab()),
                    0.99d,
                    Map.of(
                            "oab", firstNonBlank(usuario.getOabNormalizada(), usuario.getOab()),
                            "oabUf", Objects.toString(usuario.getOabUf(), ""),
                            "tipoUsuario", safeName(usuario.getTipoUsuario())
                    )
            );
        }
        if (notBlank(firstNonBlank(usuario.getCpf(), usuario.getEmail(), usuario.getNome()))) {
            return vertice(
                    IdentidadeJuridicaVerticeTipo.REPRESENTANTE,
                    canonicalize(IdentidadeJuridicaChaveTipo.UUID_EXTERNO, firstNonBlank(usuario.getCpf(), usuario.getEmail(), usuario.getNome())),
                    firstNonBlank(usuario.getNome(), usuario.getEmail(), usuario.getCpf()),
                    0.89d,
                    Map.of(
                            "tipoUsuario", safeName(usuario.getTipoUsuario()),
                            "perfil", Objects.toString(usuario.getPerfil(), "")
                    )
            );
        }
        return null;
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
        String normalized = blankToNull(email);
        if (normalized == null || !normalized.contains("@")) {
            return "";
        }
        return normalized.substring(normalized.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }

    private String canonicalize(IdentidadeJuridicaChaveTipo tipo, String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return switch (tipo) {
            case CPF, CNPJ, TELEFONE, OAB, DOCUMENTO, PROCESSO -> {
                String digits = normalized.replaceAll("\\D+", "");
                yield digits.isBlank() ? null : digits;
            }
            case EMAIL, DOMINIO -> normalized.toLowerCase(Locale.ROOT);
            case NOME, ORGAO_PUBLICO, UNIDADE, ENDERECO, UUID_EXTERNO -> normalized.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        };
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
