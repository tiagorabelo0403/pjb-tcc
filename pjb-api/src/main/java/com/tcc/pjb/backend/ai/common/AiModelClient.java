package com.tcc.pjb.backend.ai.common;

import java.util.Map;
import java.util.Objects;

public interface AiModelClient {

    
    String generate(String prompt);

    
    default String generate(Map<String,Object> context) {
        Objects.requireNonNull(context, "Contexto não pode ser nulo");
        return generate("Contexto: " + context.toString());
    }

    
    default double[] embed(String text) {
        Objects.requireNonNull(text, "Texto não pode ser nulo");
        if (text.isBlank()) throw new IllegalArgumentException("Texto não pode ser vazio");
        
        
        
        
        final int dims = 256;
        double[] v = new double[dims];
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            int idx = (i * 31 + b) & (dims - 1);
            v[idx] += (b >= 0 ? 1.0 : -1.0);
        }
        
        double norm = 0.0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }

    
    default void streamGenerate(String prompt, ResponseHandler handler) {
        String resp = generate(prompt);
        handler.onPartial(resp);
        handler.onComplete();
    }

    
    default void audit(String action, String detail) { }

    
    default void setTimeout(long millis) { }

    
    interface ResponseHandler {
        void onPartial(String chunk);
        void onComplete();
        void onError(Exception e);
    }
}