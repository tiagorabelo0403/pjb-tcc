package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatProcessContactEnvelopeResolver {

    private final UsuarioRepository usuarioRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;

    public SecretariatProcessContactEnvelopeResolver(UsuarioRepository usuarioRepository,
                                                     LaianeProcuracaoRepository procuracaoRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
    }

    public List<Map<String, Object>> participantSnapshots(Processo processo) {
        if (processo == null) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(buildParticipant("AUTOR", processo.getParteAutoraNome(), processo.getParteAutoraCpf()));
        out.add(buildParticipant("REU", processo.getParteReuNome(), processo.getParteReuCpf()));
        return out.stream().filter(item -> !item.isEmpty()).toList();
    }

    public Map<String, Object> buildEnvelope(Processo processo) {
        if (processo == null) {
            return Map.of();
        }
        List<Map<String, Object>> participants = participantSnapshots(processo);
        List<Map<String, Object>> attorneys = attorneySnapshots(processo);
        LinkedHashMap<String, Object> author = findByRole(participants, "AUTOR");
        LinkedHashMap<String, Object> defendant = findByRole(participants, "REU");
        ArrayList<Map<String, Object>> contacts = new ArrayList<>();
        if (!author.isEmpty()) {
            contacts.add(Map.copyOf(author));
        }
        if (!defendant.isEmpty()) {
            contacts.add(Map.copyOf(defendant));
        }
        contacts.addAll(attorneys);
        long readyCount = contacts.stream().filter(this::isContactReady).count();
        long missingCount = contacts.stream().filter(contact -> !isContactReady(contact)).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (!author.isEmpty()) {
            out.put("autor", Map.copyOf(author));
        }
        if (!defendant.isEmpty()) {
            out.put("reu", Map.copyOf(defendant));
        }
        out.put("advogados", List.copyOf(attorneys));
        out.put("contatosOperacionais", List.copyOf(contacts));
        out.put("contactReadyCount", readyCount);
        out.put("contactMissingCount", missingCount);
        out.put("hasMissingContacts", missingCount > 0);
        out.put("contactAxis", resolveContactAxis(processo));
        return safeMap(out);
    }

    private List<Map<String, Object>> attorneySnapshots(Processo processo) {
        LinkedHashMap<String, LinkedHashMap<String, Object>> indexed = new LinkedHashMap<>();
        Usuario primary = processo.getUsuario();
        if (primary != null && primary.isAdvogado()) {
            mergeAttorney(indexed, primary, "AUTOR", "PROCESSO_USUARIO");
        }
        if (processo.getId() != null) {
            procuracaoRepository.findDistinctAdvogadosByProcessoIdAndStatus(processo.getId(), LaianeProcuracaoStatus.ATIVA)
                .forEach(advogado -> mergeAttorney(indexed, advogado, resolveAttorneySide(processo, advogado), "PROCURACAO_ATIVA"));
        }
        return indexed.values().stream().map(Map::copyOf).toList();
    }

    private void mergeAttorney(LinkedHashMap<String, LinkedHashMap<String, Object>> indexed,
                               Usuario advogado,
                               String side,
                               String source) {
        if (advogado == null) {
            return;
        }
        String key = attorneyKey(advogado);
        LinkedHashMap<String, Object> current = indexed.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        putIfPresent(current, "role", "ADVOGADO");
        putIfPresent(current, "side", firstNonBlank(side, "INDETERMINADO"));
        putIfPresent(current, "nome", advogado.getNome());
        putIfPresent(current, "documento", advogado.getCpf());
        putIfPresent(current, "email", advogado.getEmail());
        putIfPresent(current, "numeroOab", firstNonBlank(advogado.getOabNormalizada(), advogado.getOab()));
        putIfPresent(current, "usuarioId", advogado.getId());
        putIfPresent(current, "tipoUsuario", advogado.getTipoUsuario() == null ? null : advogado.getTipoUsuario().name());
        putIfPresent(current, "source", mergeSources(stringValue(current.get("source")), source));
        current.put("contactReady", isAttorneyContactReady(advogado));
    }

    private LinkedHashMap<String, Object> findByRole(List<Map<String, Object>> participants, String role) {
        if (participants == null || role == null || role.isBlank()) {
            return new LinkedHashMap<>();
        }
        return participants.stream()
            .filter(Objects::nonNull)
            .filter(item -> role.equalsIgnoreCase(stringValue(item.get("role"))))
            .findFirst()
            .map(item -> new LinkedHashMap<>(item))
            .orElseGet(LinkedHashMap::new);
    }

    private Map<String, Object> buildParticipant(String role, String nome, String cpf) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if ((nome == null || nome.isBlank()) && (cpf == null || cpf.isBlank())) {
            return Map.of();
        }
        out.put("role", role);
        out.put("side", role);
        out.put("nome", nome);
        out.put("documento", cpf);
        out.put("cpf", cpf);
        Usuario linked = cpf == null || cpf.isBlank() ? null : usuarioRepository.findByCpf(cpf).orElse(null);
        if (linked != null) {
            out.put("usuarioId", linked.getId());
            out.put("email", linked.getEmail());
            out.put("tipoUsuario", linked.getTipoUsuario() == null ? null : linked.getTipoUsuario().name());
            out.put("source", "USUARIO_VINCULADO");
            out.put("contactReady", linked.getEmail() != null && !linked.getEmail().isBlank());
        } else {
            out.put("source", "PROCESSO");
            out.put("contactReady", false);
        }
        return safeMap(out);
    }

    private String resolveAttorneySide(Processo processo, Usuario advogado) {
        if (processo == null || advogado == null) {
            return "INDETERMINADO";
        }
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), advogado.getId())) {
            return "AUTOR";
        }
        return "INDETERMINADO";
    }

    private boolean isAttorneyContactReady(Usuario advogado) {
        if (advogado == null) {
            return false;
        }
        return advogado.getEmail() != null && !advogado.getEmail().isBlank();
    }

    private boolean isContactReady(Map<String, Object> contact) {
        Object raw = contact == null ? null : contact.get("contactReady");
        return Boolean.TRUE.equals(raw) || "true".equalsIgnoreCase(stringValue(raw));
    }

    private String attorneyKey(Usuario advogado) {
        if (advogado == null) {
            return "ATTORNEY:UNKNOWN";
        }
        if (advogado.getId() != null) {
            return "ATTORNEY:ID:" + advogado.getId();
        }
        String cpf = digits(advogado.getCpf());
        if (cpf != null) {
            return "ATTORNEY:CPF:" + cpf;
        }
        String email = advogado.getEmail() == null ? null : advogado.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email != null && !email.isBlank()) {
            return "ATTORNEY:EMAIL:" + email;
        }
        return "ATTORNEY:NOME:" + firstNonBlank(advogado.getNome(), "SEM_NOME");
    }

    private String resolveContactAxis(Processo processo) {
        String justica = processo.getTipoJustica() == null ? "SEM_JUSTICA" : processo.getTipoJustica().name();
        String rito = processo.getRito() == null ? "SEM_RITO" : processo.getRito().group();
        return justica + ':' + rito;
    }

    private static String mergeSources(String current, String next) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) {
            for (String token : current.split(",")) {
                String normalized = firstNonBlank(token);
                if (normalized != null) {
                    sources.add(normalized);
                }
            }
        }
        String normalizedNext = firstNonBlank(next);
        if (normalizedNext != null) {
            sources.add(normalizedNext);
        }
        return sources.isEmpty() ? null : String.join(",", sources);
    }

    private static String digits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String out = value.replaceAll("\\D+", "");
        return out.isBlank() ? null : out;
    }

    private static String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? null : value;
    }

    private static void putIfPresent(LinkedHashMap<String, Object> out, String key, Object value) {
        if (key != null && !key.isBlank() && value != null) {
            out.put(key, value);
        }
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

    private static Map<String, Object> safeMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    out.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(out);
    }
}
