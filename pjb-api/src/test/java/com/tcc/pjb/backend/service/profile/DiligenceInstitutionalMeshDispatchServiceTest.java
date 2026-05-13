package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.idempotency.IdempotencyStatus;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceInstitutionalMeshDispatchServiceTest {

    @Test
    void enfileiraOutboxTransacionalParaMalhaInstitucional() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ActionIdempotencyService actionIdempotencyService = Mockito.mock(ActionIdempotencyService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository = Mockito.mock(DiligenciaOperadorMalhaInstitucionalDispatchRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        OutboxPublisher outboxPublisher = Mockito.mock(OutboxPublisher.class);
        DiligenceInstitutionalMeshDispatchService service = new DiligenceInstitutionalMeshDispatchService(
                currentUserService,
                authorizationService,
                actionIdempotencyService,
                keyMaterialService,
                annexationRepository,
                dispatchRepository,
                processoRepository,
                outboxPublisher,
                new ObjectMapper()
        );

        Usuario actor = usuario();
        Processo processo = processo();
        DiligenciaOperadorAnexacaoInstitucional annexation = annexation();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(annexationRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(Optional.of(annexation));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(actionIdempotencyService.begin(any(), any(), any(), any()))
                .thenReturn(new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, "scope", "idem", "hash", null, null, null));
        when(outboxPublisher.enqueueTracked(any(), eq(OutboxPublisher.EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH), any(), anyMap(), any(), eq("DILIGENCE_MESH_DISPATCH"), any()))
                .thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(dispatchRepository.save(any())).thenAnswer(invocation -> {
            DiligenciaOperadorMalhaInstitucionalDispatch entity = invocation.getArgument(0);
            entity.setId(9001L);
            return entity;
        });

        var response = service.dispatch(
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                "77",
                new DiligenceInstitutionalMeshDispatchRequest(null, "MALHA_CE", "MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA", "TJCE", "QUIXADA", null, "mesh-idem", "expedicao")
        );

        assertThat(response.dispatchId()).isEqualTo(9001L);
        assertThat(response.outboxEventId()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(response.dispatchStatus()).isEqualTo("OUTBOX_ENQUEUED");
        assertThat(response.routingKey()).contains("MESH:DILIGENCE:OFICIAL_JUSTICA");
        assertThat(response.payloadDigestSha256()).hasSize(64);
        assertThat(response.payloadSignatureHmacSha256()).hasSize(64);
    }

    @Test
    void criaReplayControladoSemDuplicacaoDeChave() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ActionIdempotencyService actionIdempotencyService = Mockito.mock(ActionIdempotencyService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository = Mockito.mock(DiligenciaOperadorMalhaInstitucionalDispatchRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        OutboxPublisher outboxPublisher = Mockito.mock(OutboxPublisher.class);
        DiligenceInstitutionalMeshDispatchService service = new DiligenceInstitutionalMeshDispatchService(
                currentUserService,
                authorizationService,
                actionIdempotencyService,
                keyMaterialService,
                annexationRepository,
                dispatchRepository,
                processoRepository,
                outboxPublisher,
                new ObjectMapper()
        );

        Usuario actor = usuario();
        Processo processo = processo();
        DiligenciaOperadorAnexacaoInstitucional annexation = annexation();
        DiligenciaOperadorMalhaInstitucionalDispatch original = DiligenciaOperadorMalhaInstitucionalDispatch.builder()
                .id(9100L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .annexationId(4000L)
                .juntadaId(3000L)
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .meshOrgKey("TJCE")
                .meshUnitKey("QUIXADA")
                .dispatchStatus("DISPATCHED")
                .replayToken("ab".repeat(32))
                .chainIdempotencyKey("cd".repeat(32))
                .payloadDigestSha256("ef".repeat(32))
                .payloadSignatureHmacSha256("12".repeat(32))
                .createdAt(Instant.parse("2026-03-12T12:15:00Z"))
                .build();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(dispatchRepository.findById(9100L)).thenReturn(Optional.of(original));
        when(annexationRepository.findById(4000L)).thenReturn(Optional.of(annexation));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(actionIdempotencyService.begin(any(), any(), any(), any()))
                .thenReturn(new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, "scope", "idem", "hash", null, null, null));
        when(outboxPublisher.enqueueTracked(any(), eq(OutboxPublisher.EVT_PROFILE_INSTITUTIONAL_MESH_DISPATCH), any(), anyMap(), any(), eq("DILIGENCE_MESH_DISPATCH"), any()))
                .thenReturn(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        when(dispatchRepository.save(any())).thenAnswer(invocation -> {
            DiligenciaOperadorMalhaInstitucionalDispatch entity = invocation.getArgument(0);
            entity.setId(9200L);
            return entity;
        });

        var response = service.replay(
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                "77",
                new DiligenceInstitutionalMeshReplayRequest(9100L, "reenvio_controlado", null, "mesh-replay", "reenvio")
        );

        assertThat(response.originalDispatch().dispatchId()).isEqualTo(9100L);
        assertThat(response.replayDispatch().dispatchId()).isEqualTo(9200L);
        assertThat(response.replayDispatch().outboxEventId()).isEqualTo(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
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
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setUnidadeJudiciariaCodigo("QUIXADA");
        return processo;
    }

    private static DiligenciaOperadorAnexacaoInstitucional annexation() {
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
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .ackProtocol("ACK-20260312")
                .ackReference("REF-20260312")
                .bundleDigestSha256("ab".repeat(32))
                .createdAt(Instant.parse("2026-03-12T12:10:00Z"))
                .build();
    }
}
