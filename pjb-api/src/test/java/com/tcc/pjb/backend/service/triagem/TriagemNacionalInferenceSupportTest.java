package com.tcc.pjb.backend.service.triagem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriagemNacionalInferenceSupportTest {

    private TriagemNacionalInferenceSupport support;

    @BeforeEach
    void setUp() {
        ProceduralCanonicalResolver canonicalResolver = mock(ProceduralCanonicalResolver.class);
        CompetenceResolverService competenceResolverService = mock(CompetenceResolverService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProntuarioNacionalService prontuarioNacionalService = mock(ProntuarioNacionalService.class);
        DocumentoNacionalValidator documentoValidator = mock(DocumentoNacionalValidator.class);

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
        support = new TriagemNacionalInferenceSupport(
                canonicalResolver,
                competenceResolverService,
                processoRepository,
                prontuarioNacionalService,
                documentoValidator
        );
    }

    @Test
    void sugerirClassificacaoDevePriorizarReclamacaoTrabalhistaQuandoTextoTiverSinaisClt() {
        List<TriagemNacionalIAEngine.PendenciaTriagem> pendencias = new ArrayList<>();

        TriagemNacionalIAEngine.SugestaoClassificacao classificacao = support.sugerirClassificacao(
                new TriagemNacionalIAEngine.PedidoTriagem(
                        "NUPN-2",
                        null,
                        null,
                        "TRABALHISTA",
                        new BigDecimal("15000.00"),
                        "Empregado pede verbas rescisorias, FGTS e horas extras pela CLT",
                        "12345678909",
                        "11222333000144",
                        "12345",
                        "CE",
                        List.of("CTPS", "HOLERITE"),
                        LocalDate.now().minusDays(20),
                        false,
                        false,
                        10L
                ),
                pendencias
        );

        assertThat(classificacao.classeTpu()).contains("RECLAMACAO_TRABALHISTA");
        assertThat(classificacao.confiancaClasse()).isGreaterThanOrEqualTo(0.90d);
    }

    @Test
    void construirResumoDeveRefletirUrgenciaEPrazoQuandoPresentes() {
        String resumo = support.construirResumo(
                new TriagemNacionalIAEngine.PedidoTriagem(
                        "NUPN-3",
                        "MANDADO_DE_SEGURANCA",
                        "ATO_ADMINISTRATIVO",
                        "CONSTITUCIONAL",
                        new BigDecimal("1000.00"),
                        "pedido liminar contra ato administrativo",
                        "12345678909",
                        "11222333000144",
                        "12345",
                        "CE",
                        List.of("DECISAO"),
                        LocalDate.now().minusYears(6),
                        true,
                        false,
                        11L
                ),
                TriagemNacionalIAEngine.VereditoTriagem.REQUER_REVISAO_HUMANA,
                List.of(),
                new TriagemNacionalIAEngine.SugestaoClassificacao("MANDADO_DE_SEGURANCA", "ATO_ADMINISTRATIVO", 0.91, 0.85, List.of(), false),
                new TriagemNacionalIAEngine.CompetenciaTriagem("FAZENDA_PUBLICA", "MANDADO_DE_SEGURANCA", 0.88, false, List.of("competencia")),
                new TriagemNacionalIAEngine.AnalisePrescricao(true, false, "Lei X", 5, 2200, "Prazo excedido"),
                List.of(),
                0.73
        );

        assertThat(resumo).contains("urgencia=SIM");
        assertThat(resumo).contains("prazo=Lei X");
        assertThat(resumo).contains("confianca=73%");
    }
}
