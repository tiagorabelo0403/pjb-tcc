package com.tcc.pjb.backend.service.competencia;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.repository.ComarcaRepository;
import java.text.Normalizer;
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
        String chaveNome = chaveComparacao(nomeNormalizado);
        String ufNormalizada = trimToNull(uf);
        if (ufNormalizada != null) {
            Optional<Comarca> encontrada = comarcaRepository.findAllByUfIgnoreCase(ufNormalizada)
                    .stream()
                    .filter(c -> chaveNome.equals(chaveComparacao(c.getNome())))
                    .findFirst();
            if (encontrada.isEmpty()) {
                log.warn("Comarca não encontrada no catálogo para nome={} uf={}; FK não resolvida", nomeNormalizado, ufNormalizada);
            }
            return encontrada;
        }
        List<Comarca> candidatas = comarcaRepository.findAll()
                .stream()
                .filter(c -> chaveNome.equals(chaveComparacao(c.getNome())))
                .toList();
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

    /**
     * Equivalente Java de {@code upper(unaccent(...))}: o casamento roda aqui, nao em SQL, porque
     * {@code unaccent} e extensao do PostgreSQL e este servico e chamado tambem em contexto de boot
     * (ex.: InstitutionalCatalogPersistenceSyncService no ApplicationReadyEvent), que sobe sobre H2
     * nos testes — uma query nativa com unaccent derruba o ApplicationContext inteiro nesse cenario.
     * Mantem UMA semantica de casamento nos dois bancos, em vez de emular unaccent no H2 (o que
     * reintroduziria divergencia teste/producao). O backfill das migrations segue usando o unaccent
     * nativo do Postgres, que concorda com esta normalizacao para nomes de lugar do portugues.
     */
    private static String chaveComparacao(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
