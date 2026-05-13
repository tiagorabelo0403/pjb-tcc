package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

class DiligenceOperationalTimelineServiceTest {

    @Test
    void agregaTimelineOperacionalEEventosProcessuais() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        DiligenciaOperadorTelemetriaRepository telemetriaRepository = Mockito.mock(DiligenciaOperadorTelemetriaRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = Mockito.mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = Mockito.mock(DiligenciaOperadorEncerramentoRepository.class);
        DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository = Mockito.mock(DiligenciaOperadorFormalizacaoProcessualRepository.class);
        DiligenciaOperadorJuntadaProcessualRepository juntadaRepository = Mockito.mock(DiligenciaOperadorJuntadaProcessualRepository.class);
        DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        DiligenciaOperadorMalhaInstitucionalDispatchRepository meshDispatchRepository = Mockito.mock(DiligenciaOperadorMalhaInstitucionalDispatchRepository.class);
        DiligenceReferenceResolverService referenceResolverService = Mockito.mock(DiligenceReferenceResolverService.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        ProcessEventRepository processEventRepository = Mockito.mock(ProcessEventRepository.class);
        DiligenceOperationalTimelineService service = new DiligenceOperationalTimelineService(
                currentUserService,
                authorizationService,
                telemetriaRepository,
                checkpointRepository,
                certidaoRepository,
                encerramentoRepository,
                formalizacaoRepository,
                juntadaRepository,
                anexacaoRepository,
                meshDispatchRepository,
                referenceResolverService,
                processoRepository,
                processEventRepository
        );

        Usuario actor = usuario();
        Processo processo = processo();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(checkpointRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(checkpoint()));
        when(certidaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(certidao()));
        when(encerramentoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(encerramento()));
        when(formalizacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(formalizacao()));
        when(juntadaRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(juntada()));
        when(anexacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(anexacao()));
        when(meshDispatchRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(List.of(meshDispatch()));
        when(telemetriaRepository.findByOperatorUserIdAndCanalOrderByCapturadoEmDesc(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any(PageRequest.class)))
                .thenReturn(List.of(telemetria()));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireReadProcesso(processo);
        when(processEventRepository.findRecentByProcessoIdAndTypes(eq(501L), any(), any(PageRequest.class)))
                .thenReturn(List.of(ProcessEventEnvelope.builder()
                        .id(9000L)
                        .processoId(501L)
                        .seq(91L)
                        .eventType("DOCUMENTS_BULK_ADDED")
                        .payload("{\"pacote\":\"ok\"}")
                        .payloadHash("de".repeat(32))
                        .chainHash("ab")
                        .createdAt(Instant.parse("2026-03-12T12:12:00Z"))
                        .build()));

        var response = service.timeline(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77", 20);

        assertThat(response).isNotEmpty();
        assertThat(response).extracting(item -> item.sourceType())
                .contains("TELEMETRIA", "CHECKPOINT", "CERTIDAO", "ENCERRAMENTO", "FORMALIZACAO", "JUNTADA_AUTOMATICA", "ANEXACAO_INSTITUCIONAL", "MALHA_DISPATCH", "PROCESS_EVENT");
        assertThat(response).anySatisfy(item -> {
            if ("JUNTADA_AUTOMATICA".equals(item.sourceType())) {
                assertThat(item.bundleReference()).isEqualTo("OFICIAL_JUSTICA:MALHA_CE:501:1900:900");
                assertThat(item.digestSha256()).hasSize(64);
            }
        });
        assertThat(response.get(0).occurredAt()).isNotNull();
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setNome("Oficial Operacional");
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.setPerfil(TipoUsuario.OFICIAL_JUSTICA.name());
        usuario.setCpf("12345678901");
        usuario.setEmail("oficial@pjb.test");
        usuario.setSenha("x");
        usuario.setUf("CE");
        usuario.setComarca("Quixadá");
        return usuario;
    }

    private static Processo processo() {
        Processo processo = new Processo();
        processo.setId(501L);
        processo.setNumeroProcesso("0009999-11.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        return processo;
    }

    private static DiligenciaOperadorTelemetria telemetria() {
        return DiligenciaOperadorTelemetria.builder()
                .id(100L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .latitude(-4.3d)
                .longitude(-38.9d)
                .precisaoMetros(8d)
                .fonte("GPS")
                .foreground(true)
                .capturadoEm(Instant.parse("2026-03-12T11:25:00Z"))
                .createdAt(Instant.parse("2026-03-12T11:25:00Z"))
                .build();
    }

    private static DiligenciaOperadorCheckpointEvento checkpoint() {
        return DiligenciaOperadorCheckpointEvento.builder()
                .id(200L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .checkpointTipo(DiligenciaCheckpointTipo.CHEGADA)
                .targetLatitude(-4.3d)
                .targetLongitude(-38.9d)
                .observedLatitude(-4.3001d)
                .observedLongitude(-38.9001d)
                .distanceMeters(12d)
                .geofenceRadiusMeters(100d)
                .insideGeofence(true)
                .classification("INSIDE_GEOFENCE")
                .source("GPS")
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .tentativaSequencia(1)
                .locationSignatureSha256("aa".repeat(32))
                .occurredAt(Instant.parse("2026-03-12T11:26:00Z"))
                .createdAt(Instant.parse("2026-03-12T11:26:00Z"))
                .build();
    }

    private static DiligenciaOperadorCertidao certidao() {
        return DiligenciaOperadorCertidao.builder()
                .id(900L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .checkpointEventId(200L)
                .certidaoTipo(DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO)
                .titulo("Certidão de cumprimento")
                .narrativa("Cumprimento positivo validado no local")
                .certificateDigestSha256("bc".repeat(32))
                .signatureHmacSha256("cd".repeat(32))
                .latitude(-4.3d)
                .longitude(-38.9d)
                .destinoLatitude(-4.3d)
                .destinoLongitude(-38.9d)
                .distanceMeters(12d)
                .insideGeofence(true)
                .tentativaSequencia(1)
                .evidenceChaveCustodia("CST-XYZ")
                .attemptTrailDigestSha256("ef".repeat(32))
                .createdAt(Instant.parse("2026-03-12T11:30:00Z"))
                .build();
    }

    private static DiligenciaOperadorEncerramento encerramento() {
        return DiligenciaOperadorEncerramento.builder()
                .id(901L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .outcome(DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO)
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .certidaoId(900L)
                .checkpointEventId(200L)
                .workItemStatusFinal("DONE")
                .followupWorkItemId(78L)
                .executionDigestSha256("fa".repeat(32))
                .createdAt(Instant.parse("2026-03-12T11:32:00Z"))
                .build();
    }

    private static DiligenciaOperadorFormalizacaoProcessual formalizacao() {
        return DiligenciaOperadorFormalizacaoProcessual.builder()
                .id(1900L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .encerramentoId(901L)
                .certidaoId(900L)
                .movimentacaoId(700L)
                .movimentacaoEventSeq(44L)
                .minutaDocumentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .documentosReferenciados(1)
                .formalizationDigestSha256("cd".repeat(32))
                .createdAt(Instant.parse("2026-03-12T11:35:00Z"))
                .build();
    }

    private static DiligenciaOperadorJuntadaProcessual juntada() {
        return DiligenciaOperadorJuntadaProcessual.builder()
                .id(3000L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .formalizacaoId(1900L)
                .encerramentoId(901L)
                .certidaoId(900L)
                .movimentacaoId(700L)
                .movimentacaoEventSeq(60L)
                .pacoteDocumentoId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .pacoteEventSeq(61L)
                .minutaDocumentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .pacoteTitulo("Pacote Operacional")
                .pacoteSha256("ab".repeat(32))
                .certidaoDigestSha256("bc".repeat(32))
                .formalizationDigestSha256("cd".repeat(32))
                .documentosReferenciados(1)
                .exportarMalhaExterna(true)
                .externalSystemCode("MALHA_CE")
                .bundleReference("OFICIAL_JUSTICA:MALHA_CE:501:1900:900")
                .bundleDigestSha256("de".repeat(32))
                .bundleSignatureHmacSha256("ef".repeat(32))
                .idempotencyKey("juntada-1")
                .createdAt(Instant.parse("2026-03-12T12:10:00Z"))
                .build();
    }


    private static DiligenciaOperadorAnexacaoInstitucional anexacao() {
        return DiligenciaOperadorAnexacaoInstitucional.builder()
                .id(4000L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .workItemId(77L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .juntadaId(3000L)
                .formalizacaoId(1900L)
                .encerramentoId(901L)
                .certidaoId(900L)
                .pacoteDocumentoId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .bundleReference("OFICIAL_JUSTICA:MALHA_CE:501:1900:900")
                .bundleDigestSha256("de".repeat(32))
                .bundleSignatureHmacSha256("ef".repeat(32))
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .ackProtocol("ACK-4000")
                .ackReference("ACKREF-4000")
                .annexationStatus("ACKNOWLEDGED")
                .externalReceiptDigestSha256("ab".repeat(32))
                .chainIdempotencyKey("98".repeat(32))
                .processEventSeq(62L)
                .requestHashSha256("aa".repeat(32))
                .executionDigestSha256("bb".repeat(32))
                .createdAt(Instant.parse("2026-03-12T12:12:30Z"))
                .build();
    }

    private static DiligenciaOperadorMalhaInstitucionalDispatch meshDispatch() {
        return DiligenciaOperadorMalhaInstitucionalDispatch.builder()
                .id(4001L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .workItemId(77L)
                .annexationId(4000L)
                .juntadaId(3000L)
                .eventType("PROFILE_INSTITUTIONAL_MESH_DISPATCH")
                .routingKey("MESH:DILIGENCE:OFICIAL_JUSTICA:TJCE:QUIXADA")
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .meshOrgKey("TJCE")
                .meshUnitKey("QUIXADA")
                .dispatchStatus("DISPATCHED")
                .replayToken("fe".repeat(32))
                .chainIdempotencyKey("dc".repeat(32))
                .payloadDigestSha256("ba".repeat(32))
                .payloadSignatureHmacSha256("98".repeat(32))
                .deliveredAt(Instant.parse("2026-03-12T12:13:00Z"))
                .createdAt(Instant.parse("2026-03-12T12:13:00Z"))
                .build();
    }

}
