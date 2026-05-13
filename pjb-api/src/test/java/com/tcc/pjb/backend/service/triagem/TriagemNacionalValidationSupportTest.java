package com.tcc.pjb.backend.service.triagem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriagemNacionalValidationSupportTest {

    private DocumentoNacionalValidator documentoValidator;
    private TriagemNacionalValidationSupport support;

    @BeforeEach
    void setUp() {
        documentoValidator = mock(DocumentoNacionalValidator.class);
        RitoPackService ritoPackService = mock(RitoPackService.class);
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        ProceduralCanonicalResolver canonicalResolver = mock(ProceduralCanonicalResolver.class);
        when(documentoValidator.normalizarDocumento(any())).thenAnswer(inv -> {
            Object value = inv.getArgument(0);
            return value == null ? "" : value.toString().replaceAll("\\D+", "");
        });
        when(canonicalResolver.resolve(any(Map.class))).thenReturn(new CanonicalContext(
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        ));
        support = new TriagemNacionalValidationSupport(documentoValidator, ritoPackService, tetoProcessualService, canonicalResolver);
    }

    @Test
    void normalizarPedidoDeveLimparDocumentosEOab() {
        TriagemNacionalIAEngine.PedidoTriagem normalizado = support.normalizarPedido(new TriagemNacionalIAEngine.PedidoTriagem(
                null,
                "  procedimento_comum ",
                "  consumo ",
                " civil ",
                new BigDecimal("1000.00"),
                " fato relevante ",
                "123.456.789-09",
                "11.222.333/0001-44",
                "OAB 12345",
                "ce",
                java.util.Arrays.asList(" contrato ", "CONTRATO", null, " comprovante "),
                null,
                false,
                false,
                99L
        ));

        assertThat(normalizado.nupnProvisorio()).startsWith("TRIAGEM-");
        assertThat(normalizado.classeTpuSugerida()).isEqualTo("PROCEDIMENTO_COMUM");
        assertThat(normalizado.cpfCnpjAutor()).isEqualTo("12345678909");
        assertThat(normalizado.cpfCnpjReu()).isEqualTo("11222333000144");
        assertThat(normalizado.oabAdvogado()).isEqualTo("12345");
        assertThat(normalizado.ufAdvogado()).isEqualTo("CE");
        assertThat(normalizado.documentosAnexados()).containsExactly("CONTRATO", "COMPROVANTE");
    }

    @Test
    void verificarDocumentosDasPartesDeveGerarPendenciaCriticaQuandoDocumentoInvalido() {
        doThrow(new IllegalArgumentException("cpf invalido")).when(documentoValidator).validarDocumento("12345678909");
        List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias = new ArrayList<>();

        support.verificarDocumentosDasPartes(new TriagemNacionalIAEngine.PedidoTriagem(
                "NUPN-1",
                "PROCEDIMENTO_COMUM",
                "CONSUMO",
                "CIVEL",
                new BigDecimal("50000.00"),
                "texto",
                "12345678909",
                "11222333000144",
                null,
                null,
                List.of(),
                null,
                false,
                false,
                null
        ), pendencias);

        assertThat(pendencias)
                .extracting(TriagemNacionalIAEngine.PendenciaTriagem::tipo)
                .contains(TriagemNacionalIAEngine.TipoPendencia.DOCUMENTO_AUTOR_INVALIDO);
    }
}
