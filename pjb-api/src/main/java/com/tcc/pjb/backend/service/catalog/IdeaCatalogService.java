package com.tcc.pjb.backend.service.catalog;

import java.util.Collections;
import java.io.InputStream;
import java.util.*;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.catalog.IdeaDto;
import java.util.Locale;

@Service
public class IdeaCatalogService {

    private final ObjectMapper objectMapper;

    private Map<String, List<IdeaDto>> byRole = new LinkedHashMap<>();

    public IdeaCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            ClassPathResource res = new ClassPathResource("catalog/role_ideas_2026.json");
            try (InputStream in = res.getInputStream()) {
                Map<String, List<IdeaDto>> parsed = objectMapper.readValue(in, new TypeReference<>() {});

                
                LinkedHashMap<String, List<IdeaDto>> normalized = new LinkedHashMap<>();
                for (var e : parsed.entrySet()) {
                    String role = e.getKey().trim().toUpperCase(Locale.ROOT);
                    List<IdeaDto> list = e.getValue() == null ? List.of() : e.getValue();
                    
                    for (IdeaDto dto : list) {
                        if (dto != null && (dto.getRole() == null || dto.getRole().isBlank())) {
                            dto.setRole(role);
                        }
                    }
                    normalized.put(role, list);
                }
                this.byRole = Collections.unmodifiableMap(normalized);
            }
        } catch (Exception e) {
            
            this.byRole = Map.of();
        }
    }

    public List<String> listRoles() {
        return new ArrayList<>(byRole.keySet());
    }

    public List<IdeaDto> getByRole(String role) {
        if (role == null) return List.of();
        String key = role.trim().toUpperCase(Locale.ROOT);
        return byRole.getOrDefault(key, List.of());
    }
}
