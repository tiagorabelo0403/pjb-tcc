package com.tcc.pjb.backend.modules.laiane.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.laiane.model.LaianeRoleIdea;

@Service
public class LaianeCatalogService {

    private static final String PATH = "catalog/role_ideas_2026.json";

    private final ObjectMapper objectMapper;
    private Map<String, List<LaianeRoleIdea>> byRole = new LinkedHashMap<>();

    public LaianeCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            Map<String, List<LaianeRoleIdea>> parsed = objectMapper.readValue(in, new TypeReference<>() {
            });
            this.byRole = parsed != null ? parsed : new LinkedHashMap<>();
        } catch (Exception e) {
            
            this.byRole = new LinkedHashMap<>();
        }
    }

    public List<LaianeRoleIdea> listForRole(String roleKey) {
        if (roleKey == null) return List.of();
        return byRole.getOrDefault(roleKey.trim().toUpperCase(), List.of());
    }

    public Map<String, List<LaianeRoleIdea>> snapshot() {
        return Collections.unmodifiableMap(byRole);
    }
}
