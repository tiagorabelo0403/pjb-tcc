package com.tcc.pjb.backend.ai.juridica.knowledge.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class LegalKnowledgeJsonResourceLoader {

    private final ObjectMapper objectMapper;

    public LegalKnowledgeJsonResourceLoader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public boolean exists(String path) {
        return new ClassPathResource(path).exists();
    }

    public JsonNode readTree(String path) {
        try (InputStream in = open(path)) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw failure(path, e);
        }
    }

    public <T> List<T> readList(String path, TypeReference<List<T>> typeReference) {
        try (InputStream in = open(path)) {
            return List.copyOf(objectMapper.readValue(in, typeReference));
        } catch (IOException e) {
            throw failure(path, e);
        }
    }

    public Map<String, Object> readMap(String path) {
        try (InputStream in = open(path)) {
            return Map.copyOf(objectMapper.readValue(in, new TypeReference<Map<String, Object>>() {}));
        } catch (IOException e) {
            throw failure(path, e);
        }
    }

    public String readUtf8(String path) {
        try (InputStream in = open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw failure(path, e);
        }
    }

    private InputStream open(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IOException("Missing classpath resource: " + path);
        }
        return resource.getInputStream();
    }

    private IllegalStateException failure(String path, IOException cause) {
        return new IllegalStateException("Invalid legal knowledge resource: " + path, cause);
    }
}
