package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaContextEnvelopeService {

    public Map<String, Object> oficialEnvelope(Usuario usuario, OficialJusticaOrganizationalScopeService.Scope scope) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "oficialId", usuario != null ? usuario.getId() : null);
        put(out, "oficialNome", usuario != null ? usuario.getNome() : null);
        put(out, "oficialTipo", usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null);
        put(out, "oficialTipoLabel", usuario != null ? officialTypeLabel(usuario.getTipoUsuario()) : null);
        put(out, "uf", usuario != null ? usuario.getUf() : null);
        put(out, "comarca", usuario != null ? usuario.getComarca() : null);
        put(out, "cidadeBase", usuario != null ? usuario.getComarca() : null);
        put(out, "esferaAtuacao", resolveEsfera(usuario, null, usuario != null ? usuario.getTipoUsuario() : null));
        put(out, "lotacaoPrincipal", scope != null ? scope.label() : null);
        put(out, "coberturaOrganizacional", scope != null ? scope.mode() : null);
        put(out, "tribunalPrincipal", resolveTribunalPrincipal(usuario, null));
        put(out, "regiaoJudicial", resolveRegiaoJudicial(resolveTribunalPrincipal(usuario, null), usuario != null ? usuario.getUf() : null, resolveEsfera(usuario, null, usuario != null ? usuario.getTipoUsuario() : null)));
        put(out, "varasConfiguradas", scope != null ? scope.varas() : List.of());
        put(out, "unidadesConfiguradas", scope != null ? scope.unidades() : List.of());
        put(out, "partitionKey", partitionKey(usuario, null, null, scope));
        return safeCopy(out);
    }

    public Map<String, Object> processEnvelope(Usuario usuario,
                                               Processo processo,
                                               WorkItem item,
                                               OficialJusticaOrganizationalScopeService.Scope scope,
                                               OficialJusticaOrganizationalScopeService organizationalScopeService) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String vara = organizationalScopeService != null ? organizationalScopeService.resolveVaraDisplay(processo, item) : firstNonBlank(processo != null ? processo.getVara() : null);
        String lotacao = organizationalScopeService != null ? organizationalScopeService.resolveLotacaoLabel(scope, processo, item) : null;
        String esfera = resolveEsfera(usuario, processo, item != null ? item.getAssignedRole() : usuario != null ? usuario.getTipoUsuario() : null);
        String tribunal = resolveTribunalPrincipal(usuario, processo);
        String cidade = firstNonBlank(processo != null ? processo.getComarca() : null, item != null ? item.getComarca() : null, usuario != null ? usuario.getComarca() : null);
        put(out, "oficialId", usuario != null ? usuario.getId() : null);
        put(out, "oficialNome", usuario != null ? usuario.getNome() : null);
        put(out, "oficialTipo", usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null);
        put(out, "oficialTipoLabel", usuario != null ? officialTypeLabel(usuario.getTipoUsuario()) : null);
        put(out, "processoId", processo != null ? processo.getId() : null);
        put(out, "processoNumero", processNumber(processo));
        put(out, "vara", vara);
        put(out, "lotacao", lotacao);
        put(out, "forum", resolveForum(processo, esfera, cidade));
        put(out, "cidade", cidade);
        put(out, "uf", firstNonBlank(processo != null ? processo.getUf() : null, item != null ? item.getUf() : null, usuario != null ? usuario.getUf() : null));
        put(out, "tribunal", tribunal);
        put(out, "esfera", esfera);
        put(out, "regiaoJudicial", resolveRegiaoJudicial(tribunal, firstNonBlank(processo != null ? processo.getUf() : null, usuario != null ? usuario.getUf() : null), esfera));
        put(out, "rito", processo != null && processo.getRito() != null ? processo.getRito().name() : null);
        put(out, "faseAtual", processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        put(out, "statusProcesso", processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        put(out, "queueCode", item != null ? item.getQueueCode() : null);
        put(out, "inboxKey", item != null ? item.getInboxKey() : null);
        put(out, "partitionKey", partitionKey(usuario, processo, item, scope));
        return safeCopy(out);
    }

    public String resolveForum(Processo processo, String esfera, String cidade) {
        String cidadeBase = firstNonBlank(cidade, processo != null ? processo.getComarca() : null, "CIDADE_NAO_IDENTIFICADA");
        if (esfera != null && esfera.toUpperCase(Locale.ROOT).contains("FEDERAL")) {
            return "SUBSEÇÃO JUDICIÁRIA DE " + cidadeBase.toUpperCase(Locale.ROOT);
        }
        return "FÓRUM DE " + cidadeBase.toUpperCase(Locale.ROOT);
    }

    public String resolveEsfera(Usuario usuario, Processo processo, TipoUsuario tipoUsuario) {
        String tribunal = resolveTribunalPrincipal(usuario, processo);
        String tribunalNormalized = normalizeToken(tribunal);
        if (tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA) {
            if (usuario != null && Boolean.TRUE.equals(usuario.atuaNaUniao())) {
                return "FEDERAL";
            }
        }
        if (tribunalNormalized.startsWith("TRF") || tribunalNormalized.contains("FEDERAL") || tribunalNormalized.startsWith("STJ") || tribunalNormalized.startsWith("STF") || tribunalNormalized.startsWith("TSE") || tribunalNormalized.startsWith("TST") || tribunalNormalized.startsWith("STM")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    public String resolveTribunalPrincipal(Usuario usuario, Processo processo) {
        return firstNonBlank(processo != null ? processo.getTribunal() : null, usuario != null ? usuario.getPerfil() : null, "TRIBUNAL_NAO_IDENTIFICADO");
    }

    public String resolveRegiaoJudicial(String tribunal, String uf, String esfera) {
        String normalized = normalizeToken(tribunal);
        if (normalized.startsWith("TRF 1") || normalized.startsWith("TRF1") || normalized.contains("1 REGIAO")) {
            return "1ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRF 2") || normalized.startsWith("TRF2") || normalized.contains("2 REGIAO")) {
            return "2ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRF 3") || normalized.startsWith("TRF3") || normalized.contains("3 REGIAO")) {
            return "3ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRF 4") || normalized.startsWith("TRF4") || normalized.contains("4 REGIAO")) {
            return "4ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRF 5") || normalized.startsWith("TRF5") || normalized.contains("5 REGIAO")) {
            return "5ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRF 6") || normalized.startsWith("TRF6") || normalized.contains("6 REGIAO")) {
            return "6ª REGIÃO FEDERAL";
        }
        if (normalized.startsWith("TRT")) {
            return normalized.replaceFirst("^TRT\\s*", "").trim() + "ª REGIÃO TRABALHISTA";
        }
        if (normalized.startsWith("TRE") && uf != null) {
            return "JUSTIÇA ELEITORAL / " + uf.toUpperCase(Locale.ROOT);
        }
        if (esfera != null && esfera.toUpperCase(Locale.ROOT).contains("FEDERAL")) {
            return firstNonBlank(tribunal, "JUSTIÇA FEDERAL");
        }
        return "JUDICIÁRIO ESTADUAL / " + firstNonBlank(uf, "UF");
    }

    public String partitionKey(Usuario usuario, Processo processo, WorkItem item, OficialJusticaOrganizationalScopeService.Scope scope) {
        String tribunal = normalizeToken(resolveTribunalPrincipal(usuario, processo));
        String vara = normalizeToken(firstNonBlank(processo != null ? processo.getVara() : null, item != null ? item.getQueueCode() : null, scope != null ? scope.label() : null, "VARA"));
        String userId = usuario != null && usuario.getId() != null ? String.valueOf(usuario.getId()) : "SEM_USUARIO";
        String tipo = usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "SEM_TIPO";
        return tribunal + ":" + vara + ":" + userId + ":" + tipo;
    }

    public String processNumber(Processo processo) {
        return firstNonBlank(processo != null ? processo.getNumeroProcesso() : null, processo != null ? processo.getNumero() : null, processo != null ? processo.getNumeroUnificado() : null, "PROCESSO_NAO_IDENTIFICADO");
    }

    public String officialTypeLabel(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return "OFICIAL";
        }
        return switch (tipoUsuario) {
            case OFICIAL_JUSTICA_AVALIADOR -> "Oficial de Justiça Avaliador";
            case OFICIAL_JUSTICA -> "Oficial de Justiça";
            default -> tipoUsuario.name();
        };
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private void put(Map<String, Object> out, String key, Object value) {
        if (key != null && !key.isBlank() && Objects.nonNull(value)) {
            out.put(key, value);
        }
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && Objects.nonNull(value)) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    private String firstNonBlank(String... values) {
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
}
