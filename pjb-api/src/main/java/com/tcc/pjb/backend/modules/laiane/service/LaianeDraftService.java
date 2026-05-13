package com.tcc.pjb.backend.modules.laiane.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftResponse;

@Service
public class LaianeDraftService {

    private final LegalDraftingService legalDraftingService;

    public LaianeDraftService(LegalDraftingService legalDraftingService) {
        this.legalDraftingService = legalDraftingService;
    }

    public LaianeDraftResponse draft(LaianeDraftRequest req) {
        String kind = req != null && req.getKind() != null ? req.getKind().trim() : "";
        Map<String, Object> ctx = req != null ? req.getCtx() : Map.of();

        String k = kind.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

        String out;
        List<String> warnings = new ArrayList<>();

        switch (k) {
            case "SENTENCA", "SENTENÇA" -> out = legalDraftingService.draftSentenca(ctx);
            case "CONTRATO" -> out = legalDraftingService.draftContrato(ctx);
            case "RECURSO", "APELACAO", "AGRAVO", "EMBARGOS" -> out = legalDraftingService.draftRecurso(ctx);
            default -> {
                
                out = legalDraftingService.draftPeticao(ctx);
                if (!"PETICAO".equals(k) && !"PETICAO_INICIAL".equals(k) && !k.isBlank()) {
                    warnings.add("Kind '" + kind + "' não possui template específico. Gerado usando o esqueleto de Petição Inicial.");
                }
            }
        }

        
        warnings.addAll(LaianePeticaoValidatorService.detectMissingFields(out));

        return LaianeDraftResponse.builder()
                .kind(kind)
                .draftMarkdown(out)
                .warnings(warnings)
                .build();
    }
}
