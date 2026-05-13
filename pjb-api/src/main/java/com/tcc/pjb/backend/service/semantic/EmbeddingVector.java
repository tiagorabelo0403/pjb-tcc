package com.tcc.pjb.backend.service.semantic;

import java.util.Arrays;

public record EmbeddingVector(float[] values) {

    public EmbeddingVector {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("vetor vazio");
        }
    }

    public EmbeddingVector normalized() {
        double norm = 0.0;
        for (float v : values) norm += (double) v * v;
        norm = Math.sqrt(norm);
        if (norm == 0.0) return this;
        float[] out = Arrays.copyOf(values, values.length);
        for (int i = 0; i < out.length; i++) out[i] = (float) (out[i] / norm);
        return new EmbeddingVector(out);
    }

    public static float dot(EmbeddingVector a, EmbeddingVector b) {
        int n = Math.min(a.values.length, b.values.length);
        float s = 0f;
        for (int i = 0; i < n; i++) s += a.values[i] * b.values[i];
        return s;
    }
}
