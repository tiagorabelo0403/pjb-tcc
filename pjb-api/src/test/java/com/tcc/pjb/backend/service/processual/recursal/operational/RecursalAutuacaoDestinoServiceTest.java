package com.tcc.pjb.backend.service.processual.recursal.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalAutuacaoDestinoService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecursalAutuacaoDestinoServiceTest {

    private ProcessoRepository processoRepository;
    private PjbAuthorizationService authorizationService;
    private RecursalIntelligenceFacadeService recursalIntelligenceFacadeService;
    private RecursalFactIdempotentIngestService recursalFactIdempotentIngestService;
    private ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService;
    private DocumentoProcessualRepository documentoProcessualRepository;
    private DocumentoPaginaRepository documentoPaginaRepository;
    private RecursalAutuacaoDestinoService service;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        authorizationService = mock(PjbAuthorizationService.class);
        recursalIntelligenceFacadeService = mock(RecursalIntelligenceFacadeService.class);
        recursalFactIdempotentIngestService = mock(RecursalFactIdempotentIngestService.class);
        processualOperationalSurfaceFacadeService = mock(ProcessualOperationalSurfaceFacadeService.class);
        documentoProcessualRepository = mock(DocumentoProcessualRepository.class);
        documentoPaginaRepository = mock(DocumentoPaginaRepository.class);
        service = new RecursalAutuacaoDestinoService(
                processoRepository,
                authorizationService,
                recursalIntelligenceFacadeService,
                recursalFactIdempotentIngestService,
                processualOperationalSurfaceFacadeService,
                documentoProcessualRepository,
                documentoPaginaRepository
        );
    }

    @Test
    void deveConsolidarAutuacaoDestinoComPrevencaoEUnidadeFracionaria() {
        Processo processo = processoBase();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .processo(processo)
                .titulo("Sentença")
                .nomeOriginal("sentenca-origem.pdf")
                .contentType("application/pdf")
                .tamanhoBytes(4096L)
                .pdf(new byte[]{9,8,7})
                .criadoEm(LocalDateTime.now())
                .build();
        DocumentoPagina pagina = DocumentoPagina.builder()
                .documento(documento)
                .pageNumber(1)
                .pageId("p-1")
                .fingerprint("fp-1")
                .textoExtraido("Sentença original integral disponível ao órgão recursal.")
                .criadoEm(LocalDateTime.now())
                .build();
        when(documentoProcessualRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(10L)).thenReturn(List.of(documento));
        when(documentoPaginaRepository.findByDocumentoId(documento.getId())).thenReturn(List.of(pagina));

        RecursalGraphResponse graphBefore = new RecursalGraphResponse(
                900L,
                "ROOT-10",
                new RecursalGraphResponse.SummaryDto(2, 1, 1, 1, 0, InstanceLevel.SECOND_INSTANCE),
                List.of(
                        new RecursalGraphResponse.NodeDto("ROOT-10", false, "ACTIVE", InstanceLevel.FIRST_INSTANCE, "TJCE", processo.getNumeroUnificado(), 10L, "PUBLICO", "MANUAL", "Raiz"),
                        new RecursalGraphResponse.NodeDto("SHADOW-2G-TJCE", true, "PREDICTED", InstanceLevel.SECOND_INSTANCE, "TJCE", "", null, "PUBLICO", "MANUAL", "3_CAMARA_DIREITO_PRIVADO")
                ),
                List.of(new RecursalGraphResponse.EdgeDto("ROOT-10", "SHADOW-2G-TJCE", "APPEAL_DERIVED", "APELACAO"))
        );
        when(recursalIntelligenceFacadeService.graph(10L)).thenReturn(graphBefore);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fracionary", Map.of(
                "internalOrgan", Map.of(
                        "secretariatDesk", "SECRETARIA_3_CAMARA_PRIVADO",
                        "gabineteDesk", "GABINETE_CAMARA_3",
                        "chamber", Map.of(
                                "relatoriaDesk", "RELATORIA_CAMARA_3",
                                "sessionRoom", "SESSAO_CAMARA_3",
                                "preventionClass", "PREVENCAO_CAMARA_PRIVADO"
                        ),
                        "sessionTopology", Map.of(
                                "internalReviewDesk", "REVISAO_CAMARA_3"
                        ),
                        "panelComposition", Map.of(
                                "panelCompositionLabel", "3_DESEMBARGADORES"
                        ),
                        "deliberationCycle", Map.of(
                                "deliberationMode", "COLEGIADO_PADRAO"
                        )
                ),
                "bridge", Map.of(
                        "uniformizationHub", "NUF_CAMARAS_CIVEIS"
                )
        ));
        NationalProcessRoutingResponse routing = new NationalProcessRoutingResponse(
                RitoProcessual.COMUM_ORDINARIO,
                RamoDireito.CIVIL,
                com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.SEGUNDO_GRAU,
                com.tcc.pjb.backend.domain.enums.TipoJustica.ESTADUAL,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJB",
                "PJE",
                "SEGUNDO_GRAU",
                "3_CAMARA_DIREITO_PRIVADO",
                "UNID-3CAM",
                "DISTRIBUICAO_SEGUNDO_GRAU",
                false,
                false,
                false,
                24,
                null,
                "Fortaleza",
                "Fortaleza",
                "Fórum Clóvis Beviláqua",
                null,
                null,
                null,
                "TERRITORIAL",
                "PREVENCAO_RECURSAL",
                "DISTRIBUICAO_PREVENCAO",
                "CIVEL",
                "CAMARA_PREVENTA",
                "VINCULADA",
                "COBERTURA_CIVEL_RECURSAL",
                "CONTROLADO",
                "GABINETE_CAMARA_3",
                "MESA_CAMARA_3",
                List.of(),
                List.of(),
                List.of(),
                metadata
        );
        when(processualOperationalSurfaceFacadeService.diagnosticarRouting(any())).thenReturn(routing);

        RecursalGraphResponse graphAfter = new RecursalGraphResponse(
                900L,
                "ROOT-10",
                new RecursalGraphResponse.SummaryDto(3, 2, 1, 2, 1, InstanceLevel.SECOND_INSTANCE),
                List.of(),
                List.of()
        );
        RecursalFactIngestResponse ingestResponse = new RecursalFactIngestResponse(
                "PJB_2_0_RECURSAL",
                UUID.randomUUID(),
                "dedup-10-aut",
                10L,
                77L,
                null,
                graphAfter
        );
        when(recursalFactIdempotentIngestService.ingest(any(), any(), any(), anyString())).thenReturn(ingestResponse);

        Map<String, Object> out = service.registrar(10L, null, null, null, null, "registro efetivado no tribunal");

        assertThat(out.get("status")).isEqualTo("REGISTRO_DESTINO_RECURSAL_CONFIRMADO");
        assertThat(out.get("tribunalDestino")).isEqualTo("TJCE");
        assertThat(out.get("instanciaDestino")).isEqualTo("SECOND_INSTANCE");
        assertThat(out.get("numeroDestino")).isEqualTo("1000001-12.2026.8.06.0001");
        assertThat(out.get("modoNumeracaoDestino")).isEqualTo("MESMA_NUMERACAO_CNJ");
        assertThat(out).containsKey("prevencaoRecursal");
        assertThat(out).containsKey("unidadeFracionariaDestino");
        assertThat((Map<String, Object>) out.get("prevencaoRecursal")).containsEntry("shadowProceedingReferencia", "SHADOW-2G-TJCE");
        assertThat((Map<String, Object>) out.get("unidadeFracionariaDestino")).containsEntry("fracionarySnapshotLabel", "3_CAMARA_DIREITO_PRIVADO");
        assertThat((Map<String, Object>) out.get("inteligenciaRecursal")).containsEntry("autuacaoDestinoEndpoint", "/api/v1/intelligence/recursal/processo/10/autuacao-destino");
        Map<String, Object> cadernoDecisorioOrigem = (Map<String, Object>) out.get("cadernoDecisorioOrigem");
        assertThat(cadernoDecisorioOrigem).containsKey("documentoDecisaoOriginal");
        Map<String, Object> documentoDecisaoOriginal = (Map<String, Object>) cadernoDecisorioOrigem.get("documentoDecisaoOriginal");
        assertThat(documentoDecisaoOriginal.get("pdfEndpoint"))
                .isEqualTo("/api/v1/documentos/22222222-2222-2222-2222-222222222222/pdf");
    }

    private Processo processoBase() {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setSigla("TJCE");
        jurisdicao.setEstado("CE");
        jurisdicao.setComarca("Fortaleza");
        jurisdicao.setMunicipioSede("Fortaleza");
        jurisdicao.setForo("Fórum Clóvis Beviláqua");
        return Processo.builder()
                .id(10L)
                .numeroUnificado("1000001-12.2026.8.06.0001")
                .tribunal("TJCE")
                .tribunalCodigoRoteado("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .nivelSigilo(NivelSigilo.PUBLICO)
                .jurisdicao(jurisdicao)
                .build();
    }
}
