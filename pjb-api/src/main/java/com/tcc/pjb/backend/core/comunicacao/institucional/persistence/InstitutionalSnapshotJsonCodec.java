package com.tcc.pjb.backend.core.comunicacao.institucional.persistence;

import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InstitutionalSnapshotJsonCodec {

    private final ObjectMapper objectMapper;

    public InstitutionalSnapshotJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String write(Object value) {
        Objects.requireNonNull(value, "value");
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar snapshot institucional", ex);
        }
    }

    public <T> T read(String json, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("snapshotJson é obrigatório");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao desserializar snapshot institucional", ex);
        }
    }
}
