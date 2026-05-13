package com.tcc.pjb.backend.ai.core;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;

@Getter
public class IAPipelineContext {

    private final IARequest requestEntrada;
    private IAResponse ultimaResposta;
    private String etapaAtual;
    private final List<String> stageHistory = new ArrayList<>();
    private final Map<String, Object> memoria = new HashMap<>();

    public IAPipelineContext(IARequest requestEntrada) {
        this.requestEntrada = Objects.requireNonNull(requestEntrada);
        this.etapaAtual = "INICIO";
        this.stageHistory.add(this.etapaAtual);
    }

    public void setUltimaResposta(IAResponse resposta) {
        this.ultimaResposta = resposta;
    }

    public void avancarEtapa(String novaEtapa) {
        if (novaEtapa == null || novaEtapa.isBlank()) {
            return;
        }
        this.etapaAtual = novaEtapa;
        this.stageHistory.add(novaEtapa);
    }

    public List<String> getStageHistory() {
        return Collections.unmodifiableList(stageHistory);
    }

    public void memorizar(String chave, Object valor) {
        memoria.put(chave, valor);
    }

    public Object lembrar(String chave) {
        return memoria.get(chave);
    }

    public boolean possui(String chave) {
        return memoria.containsKey(chave);
    }

    public <T> T lembrarComo(String chave, Class<T> tipo) {
        Object v = memoria.get(chave);
        if (v == null) {
            return null;
        }
        return tipo.isInstance(v) ? tipo.cast(v) : null;
    }


    public Map<String, Object> facts() {
        return Collections.unmodifiableMap(memoria);
    }

    public Map<String, Object> getFacts() {
        return facts();
    }

    public boolean lembrarBoolean(String chave) {
        Object v = memoria.get(chave);
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = String.valueOf(v).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "sim".equals(s) || "ok".equals(s);
    }

    public IAProfile resolveProfile() {
        IAProfile fromMemory = lembrarComo("iaProfile", IAProfile.class);
        if (fromMemory != null) {
            return fromMemory;
        }
        IAProfile resolved = firstProfile(
                requestEntrada.getSafeString("iaProfile"),
                requestEntrada.getSafeString("perfilIa"),
                requestEntrada.getSafeString("perfil"),
                requestEntrada.getSafeString("role"),
                requestEntrada.getSafeString("tipoUsuario"),
                requestEntrada.getOrigem(),
                requestEntrada.getAcao()
        );
        if (resolved != null) {
            memorizar("iaProfile", resolved);
        }
        return resolved;
    }

    public Set<IACapability> resolveCapabilities() {
        @SuppressWarnings("unchecked")
        Set<IACapability> fromMemory = lembrarComo("iaCapabilities", Set.class);
        if (fromMemory != null && !fromMemory.isEmpty()) {
            return Set.copyOf(fromMemory);
        }
        LinkedHashSet<IACapability> out = new LinkedHashSet<>();
        IAProfile profile = resolveProfile();
        if (profile != null) {
            out.addAll(profile.capabilities());
        }
        for (String key : List.of("capacidades", "capabilities", "iaCapabilities")) {
            for (String raw : requestEntrada.getSafeStringList(key)) {
                IACapability.tryParse(raw).ifPresent(out::add);
            }
        }
        IACapability.tryParse(requestEntrada.getSafeString("capability")).ifPresent(out::add);
        if (out.isEmpty()) {
            out.add(IACapability.ANALISE_JURIDICA);
            out.add(IACapability.TRIAGEM_PROCESSUAL);
        }
        Set<IACapability> resolved = Set.copyOf(out);
        memorizar("iaCapabilities", resolved);
        return resolved;
    }

    private static IAProfile firstProfile(String... raws) {
        if (raws == null) {
            return null;
        }
        for (String raw : raws) {
            IAProfile profile = IAProfile.tryParse(raw)
                    .or(() -> IAProfile.fromTipoUsuario(TipoUsuario.fromPerfil(raw == null ? null : raw.trim())))
                    .orElse(null);
            if (profile != null) {
                return profile;
            }
        }
        return null;
    }
}
