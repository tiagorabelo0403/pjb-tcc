package com.tcc.pjb.backend.modules.laiane.service;

import java.util.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePlaybookItemDto;
import com.tcc.pjb.backend.modules.laiane.model.LaianeRoleIdea;
import java.util.Locale;

@Service
public class LaianePlaybookService {

    private final LaianeCatalogService catalog;

    public LaianePlaybookService(LaianeCatalogService catalog) {
        this.catalog = catalog;
    }

    public List<LaianePlaybookItemDto> topBenefits(TipoUsuario tipoUsuario, String ritoName, int limit) {
        String roleKey = mapRoleKey(tipoUsuario);
        List<LaianeRoleIdea> base = catalog.listForRole(roleKey);
        if (base.isEmpty()) return List.of();

        Set<String> interest = interestTagsFor(ritoName);

        List<LaianePlaybookItemDto> scored = new ArrayList<>(base.size());
        for (LaianeRoleIdea idea : base) {
            int score = 0;
            score += priorityScore(idea.getPriority());
            score += overlapScore(interest, safeTags(idea.getTags()));
            
            if (ritoName != null && !ritoName.isBlank()) {
                if (containsAny(idea.getTitle(), ritoName) || containsAny(idea.getDescription(), ritoName)) score += 2;
            }
            scored.add(LaianePlaybookItemDto.builder()
                    .id(idea.getId())
                    .role(idea.getRole())
                    .title(idea.getTitle())
                    .description(idea.getDescription())
                    .tags(idea.getTags())
                    .priority(idea.getPriority())
                    .score(score)
                    .build());
        }

        scored.sort(Comparator.comparingInt(LaianePlaybookItemDto::getScore).reversed()
                .thenComparing(LaianePlaybookItemDto::getId));

        int take = Math.max(1, Math.min(limit, 30));
        return scored.subList(0, Math.min(take, scored.size()));
    }

    private static Set<String> safeTags(List<String> tags) {
        if (tags == null) return Set.of();
        Set<String> out = new HashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String s = t.trim().toLowerCase(Locale.ROOT);
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private static int overlapScore(Set<String> interest, Set<String> tags) {
        if (interest.isEmpty() || tags.isEmpty()) return 0;
        int score = 0;
        for (String i : interest) {
            if (tags.contains(i)) score += 3;
        }
        return score;
    }

    private static int priorityScore(String priority) {
        if (priority == null) return 0;
        String p = priority.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "P0" -> 8;
            case "P1" -> 5;
            case "P2" -> 3;
            case "P3" -> 1;
            default -> 0;
        };
    }

    private static Set<String> interestTagsFor(String ritoName) {
        if (ritoName == null || ritoName.isBlank()) return Set.of("ux", "transparencia", "linha_do_tempo");

        String r = ritoName;

        
        if (r.startsWith("ELEITORAL")) {
            return Set.of("prazos", "linha_do_tempo", "provas_digitais", "auditoria", "transparencia", "checklist");
        }

        
        if (r.startsWith("PENAL")) {
            return Set.of("provas_digitais", "auditoria", "prazos", "workflow", "sigilo", "cadeia_de_custodia");
        }

        
        if (r.startsWith("MILITAR")) {
            return Set.of("sigilo", "workflow", "auditoria", "checklist", "compliance");
        }

        
        if (r.startsWith("TRIBUTARIO") || r.startsWith("FAZENDA")) {
            return Set.of("calculo", "prazos", "auditoria", "transparencia", "workflow");
        }

        
        if (r.startsWith("JUIZADO")) {
            return Set.of("ux", "linha_do_tempo", "prazos", "concilia", "workflow");
        }

        
        return Set.of("prazos", "workflow", "modelos", "ux", "transparencia");
    }

    private static String mapRoleKey(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) return "CIDADAO";

        return switch (tipoUsuario) {
            case CIDADAO -> "CIDADAO";
            case ADVOGADO, DEFENSOR_PUBLICO, OAB_PRESIDENTE_SECCIONAL -> "ADVOGADO";
            case MAGISTRADO, JUIZ, DESEMBARGADOR -> "JUIZ";
            case MINISTRO -> "MINISTRO";
            case MEMBRO_MINISTERIO_PUBLICO -> "MEMBRO_MINISTERIO_PUBLICO";
            case DELEGADO_POLICIA, AGENTE_POLICIAL -> "DELEGADO_POLICIA";
            case OFICIAL_JUSTICA -> "OFICIAL_JUSTICA";
            case SERVIDOR, SERVIDOR_FORUM, ADMINISTRADOR -> "SERVIDOR";
            default -> "CIDADAO";
        };
    }

    private static boolean containsAny(String text, String token) {
        if (text == null || token == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        String k = token.toLowerCase(Locale.ROOT);
        return t.contains(k);
    }
}
