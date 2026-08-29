package com.tcc.pjb.backend.service.semantic;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DeterministicHashEmbeddingService implements EmbeddingService {

    private static final int DIM = 2048;

    @Override
    public EmbeddingVector embed(String text) {
        if (text == null) text = "";
        String t = normalize(text);
        float[] v = new float[DIM];

        byte[] bytes = t.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 3) {
            v[0] = 1f;
            return new EmbeddingVector(v).normalized();
        }

        for (int i = 0; i < bytes.length - 2; i++) {
            int h = 1;
            h = 31 * h + bytes[i];
            h = 31 * h + bytes[i + 1];
            h = 31 * h + bytes[i + 2];
            int idx = (h & 0x7fffffff) % DIM;
            v[idx] += 1f;
        }

        return new EmbeddingVector(v).normalized();
    }

    private static String normalize(String s) {
        String t = s.toLowerCase(Locale.ROOT);
        t = t.replaceAll("\\s+", " ").trim();
        if (t.length() > 20000) {
            t = t.substring(0, 20000);
        }
        return t;
    }
}
