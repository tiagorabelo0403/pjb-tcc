package com.tcc.pjb.backend.ai.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LegalReferenceValidator {

    public record ReferenceHit(String tipo, String raw) {}

    private static final Pattern LEI = Pattern.compile("(?i)lei\\s*(n[ºo]?\\s*)?\\d{1,5}([./-]\\d{2,4})?" );
    private static final Pattern ART = Pattern.compile("(?i)art\\.?\\s*\\d{1,4}[A-Za-zº°]?" );
    private static final Pattern RESP = Pattern.compile("(?i)resp\\s*\\d{1,8}" );
    private static final Pattern RE = Pattern.compile("(?i)re\\s*\\d{1,8}" );
    private static final Pattern ADI = Pattern.compile("(?i)adi\\s*\\d{1,8}" );

    public List<ReferenceHit> extrair(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        List<ReferenceHit> hits = new ArrayList<>();
        collect(hits, "LEI", LEI, texto);
        collect(hits, "ART", ART, texto);
        collect(hits, "RESP", RESP, texto);
        collect(hits, "RE", RE, texto);
        collect(hits, "ADI", ADI, texto);
        return List.copyOf(hits);
    }

    private void collect(List<ReferenceHit> hits, String tipo, Pattern p, String s) {
        var m = p.matcher(s);
        while (m.find()) {
            hits.add(new ReferenceHit(tipo, m.group()));
        }
    }
}
