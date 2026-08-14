package com.tcc.pjb.backend.service.competencia;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.repository.ComarcaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ComarcaResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ComarcaResolutionService.class);

    private final ComarcaRepository comarcaRepository;

    public ComarcaResolutionService(ComarcaRepository comarcaRepository) {
        this.comarcaRepository = Objects.requireNonNull(comarcaRepository, "comarcaRepository");
    }

    public Optional<Comarca> resolver(String nome, String uf) {
        String nomeNormalizado = trimToNull(nome);
        if (nomeNormalizado == null) {
            return Optional.empty();
        }
        String ufNormalizada = trimToNull(uf);
        if (ufNormalizada != null) {
            Optional<Comarca> encontrada =
                    comarcaRepository.findByNomeIgnoreCaseAndUf(nomeNormalizado, ufNormalizada.toUpperCase(Locale.ROOT));
            if (encontrada.isEmpty()) {
                log.warn("Comarca não encontrada no catálogo para nome={} uf={}; FK não resolvida", nomeNormalizado, ufNormalizada);
            }
            return encontrada;
        }
        List<Comarca> candidatas = comarcaRepository.findAllByNomeIgnoreCase(nomeNormalizado);
        if (candidatas.size() == 1) {
            return Optional.of(candidatas.get(0));
        }
        if (candidatas.isEmpty()) {
            log.warn("Comarca não encontrada no catálogo para nome={} sem UF informada; FK não resolvida", nomeNormalizado);
        } else {
            log.warn("Comarca ambígua no catálogo para nome={} sem UF informada: {} candidatas em UFs distintas; FK não resolvida",
                    nomeNormalizado, candidatas.size());
        }
        return Optional.empty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
