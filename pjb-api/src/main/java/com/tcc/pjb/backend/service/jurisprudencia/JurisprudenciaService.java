package com.tcc.pjb.backend.service.jurisprudencia;

import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.*;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Service
public class JurisprudenciaService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenciaService.class);
    private static final String SEED_PATH = "jurisprudencia/seed_precedentes_2026.json";

    private final PrecedenteRepository repository;
    private final ObjectMapper objectMapper;
    private final ProceduralCatalogService proceduralCatalogService;

    public JurisprudenciaService(PrecedenteRepository repository, ObjectMapper objectMapper, ProceduralCatalogService proceduralCatalogService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.proceduralCatalogService = proceduralCatalogService;
    }

    @PostConstruct
    public void seedIfEmpty() {
        try {
            if (repository.count() > 0) return;
            ClassPathResource r = new ClassPathResource(SEED_PATH);
            if (!r.exists()) return;

            try (InputStream in = r.getInputStream()) {
                List<PrecedenteSeed> seeds = objectMapper.readValue(in, new TypeReference<>() {});
                for (PrecedenteSeed s : seeds) {
                    Precedente incoming = Precedente.builder()
                            .fonte(s.fonte())
                            .tipo(s.tipo())
                            .identificador(s.identificador())
                            .titulo(s.titulo())
                            .tese(s.tese())
                            .ementaResumo(s.ementaResumo())
                            .urlReferencia(s.urlReferencia())
                            .dataPublicacao(s.dataPublicacao())
                            .ramoSugerido(s.ramoSugerido())
                            .ritoSugerido(s.ritoSugerido())
                            .build();

                    
                    Precedente existing = null;
                    if (incoming.getFonte() != null && incoming.getTipo() != null && incoming.getIdentificador() != null && !incoming.getIdentificador().isBlank()) {
                        existing = repository.findFirstByFonteAndTipoAndIdentificador(incoming.getFonte(), incoming.getTipo(), incoming.getIdentificador());
                    }
                    if (existing != null) {
                        
                        if (existing.getTitulo() == null) existing.setTitulo(incoming.getTitulo());
                        if (existing.getTese() == null) existing.setTese(incoming.getTese());
                        if (existing.getEmentaResumo() == null) existing.setEmentaResumo(incoming.getEmentaResumo());
                        if (existing.getUrlReferencia() == null) existing.setUrlReferencia(incoming.getUrlReferencia());
                        if (existing.getDataPublicacao() == null) existing.setDataPublicacao(incoming.getDataPublicacao());
                        if (existing.getRamoSugerido() == null) existing.setRamoSugerido(incoming.getRamoSugerido());
                        if (existing.getRitoSugerido() == null) existing.setRitoSugerido(incoming.getRitoSugerido());
                        repository.save(existing);
                    } else {
                        repository.save(incoming);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao carregar seed de jurisprudência: {}", e.getMessage());
        }
    }

    public Page<Precedente> search(TribunalFonte fonte,
                                   TipoPrecedente tipo,
                                   RamoDireito ramo,
                                   String ritoName,
                                   String q,
                                   int page,
                                   int size) {
        RitoProcessual rito = ritoName == null || ritoName.isBlank() ? null : proceduralCatalogService.resolveRito(ritoName, ramo != null ? ramo.name() : null, null);
        return search(fonte, tipo, ramo, rito, q, page, size);
    }

    public Page<Precedente> search(TribunalFonte fonte,
                                   TipoPrecedente tipo,
                                   RamoDireito ramo,
                                   RitoProcessual rito,
                                   String q,
                                   int page,
                                   int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return repository.search(fonte, tipo, ramo, rito, query, pageable);
    }

    
    public record PrecedenteSeed(
            TribunalFonte fonte,
            TipoPrecedente tipo,
            String identificador,
            String titulo,
            String tese,
            String ementaResumo,
            String urlReferencia,
            java.time.LocalDate dataPublicacao,
            RamoDireito ramoSugerido,
            RitoProcessual ritoSugerido
    ) {}
}
