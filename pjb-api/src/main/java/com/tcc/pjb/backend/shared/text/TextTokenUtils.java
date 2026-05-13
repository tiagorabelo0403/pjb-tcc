package com.tcc.pjb.backend.shared.text;

import java.text.Normalizer;
import java.util.*;
import java.util.Locale;

public final class TextTokenUtils {

    private TextTokenUtils() {}

    public static Set<String> tokens(String raw) {
        if (raw == null) return Set.of();
        String s = raw.trim();
        if (s.isBlank()) return Set.of();

        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        s = s.replaceAll("[^a-z0-9 ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        if (s.isBlank()) return Set.of();

        String[] parts = s.split(" ");
        LinkedHashSet<String> out = new LinkedHashSet<>(Math.min(parts.length, 512));
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.length() < 2) continue;
            if (STOP.contains(t)) continue;
            out.add(t);
            if (out.size() >= 512) break;
        }
        return Collections.unmodifiableSet(out);
    }

    
    public static List<String> orderedTokens(String raw) {
        if (raw == null) return List.of();
        String s = raw.trim();
        if (s.isBlank()) return List.of();

        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        s = s.replaceAll("[^a-z0-9 ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        if (s.isBlank()) return List.of();

        String[] parts = s.split(" ");
        ArrayList<String> out = new ArrayList<>(Math.min(parts.length, 512));
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.length() < 2) continue;
            if (STOP.contains(t)) continue;
            out.add(t);
            if (out.size() >= 512) break;
        }
        return Collections.unmodifiableList(out);
    }

    private static final Set<String> STOP = Set.of(
            "de", "da", "do", "das", "dos", "a", "o", "as", "os",
            "e", "em", "no", "na", "nos", "nas", "para", "por",
            "com", "sem", "um", "uma", "uns", "umas", "que", "se"
    );
}
