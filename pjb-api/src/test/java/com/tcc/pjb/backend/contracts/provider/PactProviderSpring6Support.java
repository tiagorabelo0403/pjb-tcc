package com.tcc.pjb.backend.contracts.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.Spring6MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

final class PactProviderSpring6Support {

    private static final ObjectMapper JSON = new ObjectMapper();

    private PactProviderSpring6Support() {
    }

    static void configure(PactVerificationContext context, Object... controllers) {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controllers).build();
        configure(context, mockMvc);
    }

    static void configure(PactVerificationContext context, MockMvc mockMvc) {
        context.setTarget(new Spring6MockMvcTestTarget(mockMvc));
    }

    static void applyJsonBody(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        String body = findRequestBody(context);
        if (body != null && !body.isBlank()) {
            request.content(body).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        }
    }

    private static String findRequestBody(PactVerificationContext context) {
        String description = currentDescription(context);
        if (description == null || description.isBlank()) {
            return null;
        }
        Path folder = Path.of("src", "test", "resources", "pacts", "provider");
        if (!Files.isDirectory(folder)) {
            return null;
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> requestBodyFrom(path, description))
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String requestBodyFrom(Path pactFile, String description) {
        try {
            JsonNode root = JSON.readTree(pactFile.toFile());
            JsonNode interactions = root.path("interactions");
            if (!interactions.isArray()) {
                return null;
            }
            for (JsonNode interaction : interactions) {
                if (description.equals(interaction.path("description").asText())) {
                    JsonNode body = interaction.path("request").path("body");
                    return body.isMissingNode() || body.isNull() ? null : JSON.writeValueAsString(body);
                }
            }
            return null;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String currentDescription(PactVerificationContext context) {
        Object interaction = invoke(context, "getInteraction");
        Object description = invoke(interaction, "getDescription");
        return description == null ? null : String.valueOf(description);
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException exception) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
        return null;
    }
}
