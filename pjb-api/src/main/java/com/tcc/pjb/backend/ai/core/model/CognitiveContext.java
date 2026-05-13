package com.tcc.pjb.backend.ai.core.model;

import java.util.*;
import com.tcc.pjb.backend.ai.contract.IARequest;

public class CognitiveContext {

    private final IARequest request;

    private final List<String> fundamentos = new ArrayList<>();
    private final List<String> jurisprudencias = new ArrayList<>();
    private final List<String> alertas = new ArrayList<>();
    private final Map<String, Object> memory = new LinkedHashMap<>();

    public CognitiveContext(IARequest request) {
        this.request = request;
    }

    public IARequest request() {
        return request;
    }

    
    public String papelSolicitante() {
        String raw = safePayload("papelSolicitante");
        return raw.isBlank() ? "INDEFINIDO" : raw;
    }

    
    public String dominioPrimario() {
        String raw = safePayload("dominioPrimario");
        return raw.isBlank() ? "GERAL" : raw;
    }

    
    
    

    public void addFundamento(String f) {
        if (f != null && !f.isBlank()) {
            fundamentos.add(f.trim());
        }
    }

    public void addJurisprudencia(String j) {
        if (j != null && !j.isBlank()) {
            jurisprudencias.add(j.trim());
        }
    }

    public void addAlerta(String a) {
        if (a != null && !a.isBlank()) {
            alertas.add(a.trim());
        }
    }

    public List<String> fundamentos() {
        return fundamentos;
    }

    public List<String> jurisprudencias() {
        return jurisprudencias;
    }

    public List<String> alertas() {
        return alertas;
    }

    public Map<String, Object> memory() {
        return memory;
    }

    public void putMemory(String key, Object value) {
        if (key == null || key.isBlank()) return;
        memory.put(key.trim(), value);
    }

    
    
    

    private String safePayload(String key) {
        if (request == null || key == null || key.isBlank()) return "";
        try {
            String v = request.getSafeString(key);
            return v == null ? "" : normalize(v);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalize(String s) {
        
        
        
        
        String t = s.trim().replaceAll("\\s+", " ");
        return t;
    }
    
    @Deprecated(forRemoval = true)
    public String domain() {
        return dominioPrimario();
    }

    
    @Deprecated(forRemoval = true)
    public String role() {
        return papelSolicitante();
    }
}
