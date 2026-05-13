package com.tcc.pjb.backend.service.material;

import java.io.InputStream;
import java.util.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.material.model.MaterialPack;
import com.tcc.pjb.backend.service.material.model.MaterialProfile;

@Service
public class MaterialPackService {

    private static final Logger log = LoggerFactory.getLogger(MaterialPackService.class);
    private static final String PACK_PATH = "material/material_pack_2026.json";

    private final ObjectMapper objectMapper;
    private volatile MaterialPack pack = MaterialPack.builder().byRamo(Map.of()).byRito(Map.of()).build();

    public MaterialPackService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(PACK_PATH).getInputStream()) {
            MaterialPack loaded = objectMapper.readValue(in, MaterialPack.class);
            if (loaded == null) {
                log.warn("Material pack carregou nulo, usando vazio.");
                return;
            }
            if (loaded.getByRamo() == null) loaded.setByRamo(Map.of());
            if (loaded.getByRito() == null) loaded.setByRito(Map.of());
            this.pack = loaded;
            log.info("MaterialPack loaded: byRamo={} byRito={}", pack.getByRamo().size(), pack.getByRito().size());
        } catch (Exception e) {
            
            log.warn("Falha ao carregar material pack: {}", e.getMessage());
            this.pack = MaterialPack.builder().byRamo(Map.of()).byRito(Map.of()).build();
        }
    }

    public MaterialProfile resolve(RamoDireito ramo, RitoProcessual rito) {
        MaterialProfile base = null;
        if (ramo != null && pack.getByRamo() != null) {
            base = pack.getByRamo().get(ramo.name());
        }
        MaterialProfile byRito = null;
        if (rito != null && pack.getByRito() != null) {
            byRito = pack.getByRito().get(rito.name());
        }

        return merge(base, byRito);
    }

    private static MaterialProfile merge(MaterialProfile a, MaterialProfile b) {
        
        List<String> docs = mergeList(a == null ? null : a.getRequiredDocuments(), b == null ? null : b.getRequiredDocuments());
        List<String> proofs = mergeList(a == null ? null : a.getProofChecklist(), b == null ? null : b.getProofChecklist());
        List<String> bases = mergeList(a == null ? null : a.getLegalBases(), b == null ? null : b.getLegalBases());
        List<String> warnings = mergeList(a == null ? null : a.getWarnings(), b == null ? null : b.getWarnings());

        return MaterialProfile.builder()
                .requiredDocuments(docs)
                .proofChecklist(proofs)
                .legalBases(bases)
                .warnings(warnings)
                .build();
    }

    private static List<String> mergeList(List<String> a, List<String> b) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (a != null) for (String s : a) if (s != null && !s.isBlank()) out.add(s.trim());
        if (b != null) for (String s : b) if (s != null && !s.isBlank()) out.add(s.trim());
        return Collections.unmodifiableList(new ArrayList<>(out));
    }
}
