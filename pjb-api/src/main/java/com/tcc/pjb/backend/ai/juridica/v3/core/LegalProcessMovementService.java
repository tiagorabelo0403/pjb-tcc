package com.tcc.pjb.backend.ai.juridica.v3.core;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LegalProcessMovementService {

    private static final Pattern CNJ_PROCESSO_PATTERN =
            Pattern.compile("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");

    public List<Map<String, Object>> fetchMovements(String numeroProcesso) {
        String normalizedNumeroProcesso = normalizeNumeroProcesso(numeroProcesso);
        if (!isSupportedNumeroProcesso(normalizedNumeroProcesso)) {
            return List.of();
        }
        return List.of();
    }

    public boolean isSupportedNumeroProcesso(String numeroProcesso) {
        return numeroProcesso != null && CNJ_PROCESSO_PATTERN.matcher(numeroProcesso).matches();
    }

    public String normalizeNumeroProcesso(String numeroProcesso) {
        return numeroProcesso == null ? "" : numeroProcesso.trim();
    }
}
