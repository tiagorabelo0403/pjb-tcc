package com.tcc.pjb.backend.core.security.device.reqhash;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestBodyHashFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RequestBodyHashFilter filtroComLimite(int limite) {
        DeviceSecurityProperties properties = new DeviceSecurityProperties();
        properties.setBodyHashMaxBytes(limite);
        return new RequestBodyHashFilter(properties, new BodyHashService(objectMapper), objectMapper);
    }

    @Test
    void corpoAcimaDoLimiteSemContentLengthChegaInteiroAoControllerSemHashCalculado() throws Exception {
        String corpo = "{\"peticao\":\"" + "x".repeat(4096) + "\"}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processual/peticoes") {
            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setContent(corpo.getBytes(StandardCharsets.UTF_8));
        AtomicReference<String> recebido = new AtomicReference<>();
        AtomicReference<Object> hash = new AtomicReference<>();
        AtomicReference<Boolean> terminadoAntesDeLer = new AtomicReference<>();
        AtomicReference<Boolean> terminadoDepoisDeLer = new AtomicReference<>();

        filtroComLimite(1024).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            terminadoAntesDeLer.set(req.getInputStream().isFinished());
            recebido.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            terminadoDepoisDeLer.set(req.getInputStream().isFinished());
            hash.set(req.getAttribute("PJB_BODY_HASH"));
        });

        assertThat(recebido.get()).isEqualTo(corpo);
        assertThat(hash.get()).isNull();
        assertThat(terminadoAntesDeLer.get()).isFalse();
        assertThat(terminadoDepoisDeLer.get()).isTrue();
    }

    @Test
    void corpoDentroDoLimiteChegaInteiroComHashCanonicoCalculado() throws Exception {
        String corpo = "{\"b\":2,\"a\":1}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/processual/peticoes");
        request.setContentType("application/json");
        request.setContent(corpo.getBytes(StandardCharsets.UTF_8));
        AtomicReference<String> recebido = new AtomicReference<>();
        AtomicReference<Object> hash = new AtomicReference<>();

        filtroComLimite(1024).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            recebido.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            hash.set(req.getAttribute("PJB_BODY_HASH"));
        });

        assertThat(recebido.get()).isEqualTo(corpo);
        assertThat(hash.get()).isEqualTo(new BodyHashService(objectMapper).canonicalJsonHash(corpo.getBytes(StandardCharsets.UTF_8)));
    }
}
