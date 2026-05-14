package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.service.institutional.InstituicaoJudicial;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TribunalConfigurationRegistry {

    private final Map<UUID, InstituicaoJudicial> porId = new ConcurrentHashMap<>();
    private final Map<String, UUID> porSigla = new ConcurrentHashMap<>();

    public void registrar(InstituicaoJudicial instituicao) {
        porId.put(instituicao.id(), instituicao);
        porSigla.put(instituicao.sigla().toUpperCase(), instituicao.id());
    }

    public Optional<InstituicaoJudicial> buscarPorId(UUID id) {
        return Optional.ofNullable(porId.get(id));
    }

    public Optional<InstituicaoJudicial> buscarPorSigla(String sigla) {
        if (sigla == null) return Optional.empty();
        UUID id = porSigla.get(sigla.toUpperCase());
        return id == null ? Optional.empty() : Optional.ofNullable(porId.get(id));
    }

    public Collection<InstituicaoJudicial> listarPorUf(String uf) {
        if (uf == null) return Collections.emptyList();
        String ufUpper = uf.toUpperCase();
        return porId.values().stream()
                .filter(i -> ufUpper.equals(i.uf()))
                .toList();
    }

    public Collection<InstituicaoJudicial> listarComarcasDoInterior(String uf) {
        return listarPorUf(uf).stream()
                .filter(InstituicaoJudicial::eComarcaDoInterior)
                .toList();
    }

    public boolean estaRegistrado(UUID id) {
        return porId.containsKey(id);
    }

    public int totalRegistrado() {
        return porId.size();
    }
}
