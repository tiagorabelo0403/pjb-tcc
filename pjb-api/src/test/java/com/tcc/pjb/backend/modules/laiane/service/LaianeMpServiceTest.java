package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import com.tcc.pjb.backend.modules.laiane.event.LaianeOficioAuditEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpOficioCreateRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpOficioResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpOficioStatusUpdateRequest;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeOficioRepository;
import com.tcc.pjb.backend.modules.laiane.security.LaianeOficioAccessGuard;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.ValidationRule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LaianeMpServiceTest {

    @Test
    void createOficioRetornaEnvelopeQualificado() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        AuditoriaRepository auditoriaRepository = Mockito.mock(AuditoriaRepository.class);
        PjbTimeService timeService = Mockito.mock(PjbTimeService.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        Usuario mp = new Usuario();
        mp.setId(88L);
        mp.setNome("Promotor de Justiça");
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        mp.setComarca("Quixadá");
        mp.setUf("CE");

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-04-07T18:00:00Z"));
        when(timeService.legalZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        when(oficioRepository.findFirstByBodyHashAndCreatedAtAfter(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());
        SovereignValidationResult validacao = new SovereignValidationResult(
                "VALIDO",
                "PJB_QUALIFIED_SIGNATURE_SPINE",
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA",
                true,
                true,
                true,
                true,
                false,
                "MINISTERIO_PUBLICO",
                "ESTADUAL",
                "PRIMEIRO_GRAU",
                "MP/CE",
                "session",
                "replay",
                "cd".repeat(32),
                List.of(new ValidationRule("ASSINATURA_QUALIFICADA_COMPLETA", null, null))
        );
        QualifiedSignatureMetadata assinatura = new QualifiedSignatureMetadata(
                "PJB-ENV-TESTE",
                "ef".repeat(32),
                "ab".repeat(32),
                "cd".repeat(32),
                true,
                "PJB-RUB-TESTE",
                LocalDate.of(2026, 4, 7),
                LocalTime.of(15, 0),
                "Quixadá/CE",
                "Promotor de Justiça",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO",
                "MINISTERIO_PUBLICO",
                "ESTADUAL",
                "ESTADUAL",
                "PRIMEIRO_GRAU",
                "MP/CE",
                "MP/CE",
                "REGISTRO_NAO_INFORMADO",
                true,
                "USUARIO_CERTIFICADO_MEDIA",
                "session",
                "replay",
                validacao
        );
        when(qualifiedDocumentSignatureEnvelopeService.signFreeContent(Mockito.isNull(), Mockito.eq(mp), anyString(), anyString(), anyString(), anyString(), Mockito.eq(true), Mockito.eq(List.of("MINISTERIO_PUBLICO", "OFICIO_INSTITUCIONAL", "LAIANE_MP"))))
                .thenReturn(new SignedDocumentEnvelope(
                        "Requisição de informações",
                        "CONTEUDO_ASSINADO",
                        "ab".repeat(32),
                        true,
                        assinatura,
                        validacao
                ));
        when(oficioRepository.save(Mockito.any(LaianeOficio.class))).thenAnswer(invocation -> {
            LaianeOficio oficio = invocation.getArgument(0);
            oficio.setId(12L);
            oficio.setStatus(LaianeOficioStatus.CRIADO);
            oficio.setCreatedAt(LocalDateTime.parse("2026-04-07T15:00:00"));
            oficio.setUpdatedAt(LocalDateTime.parse("2026-04-07T15:00:00"));
            return oficio;
        });

        ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        LaianeMpService service = new LaianeMpService(
                guard,
                accessGuard,
                workItemRepository,
                oficioRepository,
                usuarioRepository,
                auditoriaRepository,
                timeService,
                qualifiedDocumentSignatureEnvelopeService,
                testObjectMapper,
                eventPublisher
        );

        LaianeMpOficioCreateRequest request = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO")
                .assunto("Requisição de informações")
                .conteudo("Encaminhar documentos em 48h.")
                .justificativa("Fluxo institucional")
                .build();

        var response = service.createOficio(request);

        assertThat(response.getConteudo()).isEqualTo("Encaminhar documentos em 48h.");
        assertThat(response.getDocumentoFormalAssinado().hashSha256()).isEqualTo("ab".repeat(32));
        assertThat(response.getAssinaturaQualificada().envelopeId()).isEqualTo("PJB-ENV-TESTE");
        assertThat(response.getAssinaturaQualificada().rubrica()).isTrue();
        assertThat(response.getValidacaoSoberana().status()).isEqualTo("VALIDO");
        UUID trackingCode = response.getTrackingCode();
        assertThat(trackingCode).isNotNull();
        assertThat((trackingCode.getMostSignificantBits() >> 12) & 0xF).isEqualTo(7L);
        verify(eventPublisher).publishEvent(Mockito.any(LaianeOficioAuditEvent.class));
    }

    @Test
    void oficioResponseNaoExpoeIdsInternosNemMapEmCamposSensiveis() {
        ObjectMapper mapper = new ObjectMapper();
        LaianeMpOficioResponse response = LaianeMpOficioResponse.builder()
                .trackingCode(PjbUuidV7Generator.generate())
                .build();

        JsonNode json = mapper.valueToTree(response);

        assertThat(json.has("id")).isFalse();
        assertThat(json.has("origemId")).isFalse();
        assertThat(json.has("destinoId")).isFalse();
        assertThat(Map.class.isAssignableFrom(fieldType("documentoFormalAssinado"))).isFalse();
        assertThat(Map.class.isAssignableFrom(fieldType("assinaturaQualificada"))).isFalse();
        assertThat(Map.class.isAssignableFrom(fieldType("validacaoSoberana"))).isFalse();
    }

    private Class<?> fieldType(String fieldName) {
        try {
            return LaianeMpOficioResponse.class.getDeclaredField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void updateOficioStatus_transicaoValida_criado_para_enviado() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        AuditoriaRepository auditoriaRepository = Mockito.mock(AuditoriaRepository.class);
        PjbTimeService timeService = Mockito.mock(PjbTimeService.class);
        QualifiedDocumentSignatureEnvelopeService qsvc = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);

        UUID tc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(1L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        LaianeOficio oficio = LaianeOficio.builder()
                .id(10L).trackingCode(tc).status(LaianeOficioStatus.CRIADO).origem(mp).build();

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(oficioRepository.findByTrackingCodeAndUsuarioEnvolvido(tc, 1L))
                .thenReturn(Optional.of(oficio));
        when(oficioRepository.save(any(LaianeOficio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-05-01T12:00:00Z"));
        when(timeService.legalZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));

        LaianeMpService service = new LaianeMpService(guard, accessGuard, workItemRepository,
                oficioRepository, usuarioRepository, auditoriaRepository, timeService, qsvc,
                Mockito.mock(ObjectMapper.class), Mockito.mock(ApplicationEventPublisher.class));

        var req = LaianeMpOficioStatusUpdateRequest.builder()
                .status("ENVIADO").justificativa("Envio confirmado").build();

        var response = service.updateOficioStatus(tc, req);

        assertThat(response.getStatus()).isEqualTo("ENVIADO");
        assertThat(response.getEnviadoEm()).isNotNull();
    }

    @Test
    void updateOficioStatus_transicaoInvalida_entregue_para_criado_retorna400() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);

        UUID tc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(2L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        LaianeOficio oficio = LaianeOficio.builder()
                .id(11L).trackingCode(tc).status(LaianeOficioStatus.ENTREGUE).origem(mp).build();

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(oficioRepository.findByTrackingCodeAndUsuarioEnvolvido(tc, 2L))
                .thenReturn(Optional.of(oficio));

        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), oficioRepository,
                Mockito.mock(UsuarioRepository.class), Mockito.mock(AuditoriaRepository.class),
                Mockito.mock(PjbTimeService.class), Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class),
                Mockito.mock(ObjectMapper.class), Mockito.mock(ApplicationEventPublisher.class));

        var req = LaianeMpOficioStatusUpdateRequest.builder()
                .status("CRIADO").justificativa("teste").build();

        assertThatThrownBy(() -> service.updateOficioStatus(tc, req))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
    }

    @Test
    void updateOficioStatus_statusInexistente_retorna400() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);

        UUID tc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(3L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        LaianeOficio oficio = LaianeOficio.builder()
                .id(12L).trackingCode(tc).status(LaianeOficioStatus.CRIADO).origem(mp).build();

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(oficioRepository.findByTrackingCodeAndUsuarioEnvolvido(tc, 3L))
                .thenReturn(Optional.of(oficio));

        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), oficioRepository,
                Mockito.mock(UsuarioRepository.class), Mockito.mock(AuditoriaRepository.class),
                Mockito.mock(PjbTimeService.class), Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class),
                Mockito.mock(ObjectMapper.class), Mockito.mock(ApplicationEventPublisher.class));

        var req = LaianeMpOficioStatusUpdateRequest.builder()
                .status("STATUS_INVALIDO").justificativa("teste").build();

        assertThatThrownBy(() -> service.updateOficioStatus(tc, req))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
    }

    @Test
    void createOficio_destinoInexistente_retorna400() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);

        Usuario mp = new Usuario();
        mp.setId(4L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), Mockito.mock(LaianeOficioRepository.class),
                usuarioRepository, Mockito.mock(AuditoriaRepository.class),
                Mockito.mock(PjbTimeService.class), Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class),
                Mockito.mock(ObjectMapper.class), Mockito.mock(ApplicationEventPublisher.class));

        var req = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO").conteudo("conteudo").destinoId(999L)
                .justificativa("teste").build();

        assertThatThrownBy(() -> service.createOficio(req))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
    }

    @Test
    void getOficio_comSignedEnvelopeJsonPersistido_naoChama_signFreeContent() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        QualifiedDocumentSignatureEnvelopeService qsvc = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);

        UUID tc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(5L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        String preSerializedJson = "{\"tituloDocumento\":\"Teste\",\"renderedContent\":\"X\","
                + "\"contentHash\":\"" + "ab".repeat(32) + "\",\"selado\":true,"
                + "\"assinaturaQualificada\":null,\"validacaoSoberana\":null}";

        LaianeOficio oficio = LaianeOficio.builder()
                .id(20L).trackingCode(tc).status(LaianeOficioStatus.ENVIADO).origem(mp)
                .signedEnvelopeJson(preSerializedJson).build();

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(oficioRepository.findByTrackingCodeAndUsuarioEnvolvido(tc, 5L))
                .thenReturn(Optional.of(oficio));

        ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), oficioRepository,
                Mockito.mock(UsuarioRepository.class), Mockito.mock(AuditoriaRepository.class),
                Mockito.mock(PjbTimeService.class), qsvc,
                testObjectMapper, Mockito.mock(ApplicationEventPublisher.class));

        service.getOficio(tc);

        verify(qsvc, never()).signFreeContent(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    void audit_sempreFiltraPorMpAutenticado() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        AuditoriaRepository auditoriaRepository = Mockito.mock(AuditoriaRepository.class);

        Usuario mp = new Usuario();
        mp.setId(77L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        when(guard.requireMinisterioPublico()).thenReturn(mp);

        org.springframework.data.domain.Page<com.tcc.pjb.backend.modules.auditoria.AuditoriaEventoComportamental> emptyPage =
                org.springframework.data.domain.Page.empty();
        when(auditoriaRepository.search(Mockito.isNull(), Mockito.isNull(), Mockito.eq(77L), Mockito.any()))
                .thenReturn(emptyPage);

        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), Mockito.mock(LaianeOficioRepository.class),
                Mockito.mock(UsuarioRepository.class), auditoriaRepository,
                Mockito.mock(PjbTimeService.class), Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class),
                Mockito.mock(ObjectMapper.class), Mockito.mock(ApplicationEventPublisher.class));

        var response = service.audit(null, null, 0, 10);

        assertThat(response.getItems()).isEmpty();
        verify(auditoriaRepository).search(Mockito.isNull(), Mockito.isNull(), Mockito.eq(77L), Mockito.any());
    }

    @Test
    void createOficio_duplicado_retornaExistenteComMesmoTrackingCode() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        PjbTimeService timeService = Mockito.mock(PjbTimeService.class);
        QualifiedDocumentSignatureEnvelopeService qsvc = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);

        UUID existingTc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(6L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        LaianeOficio existing = LaianeOficio.builder()
                .id(50L).trackingCode(existingTc).status(LaianeOficioStatus.CRIADO).origem(mp).build();

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-05-01T12:00:00Z"));
        when(timeService.legalZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        when(oficioRepository.findFirstByBodyHashAndCreatedAtAfter(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(existing));

        ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), oficioRepository,
                Mockito.mock(UsuarioRepository.class), Mockito.mock(AuditoriaRepository.class),
                timeService, qsvc, testObjectMapper, Mockito.mock(ApplicationEventPublisher.class));

        var req = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO").conteudo("conteudo idempotente")
                .justificativa("reenvio").build();

        var response = service.createOficio(req);

        assertThat(response.getTrackingCode()).isEqualTo(existingTc);
        verify(oficioRepository, never()).save(Mockito.any());
    }

    @Test
    void createOficio_diferente_criaNovo() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        LaianeOficioAccessGuard accessGuard = Mockito.mock(LaianeOficioAccessGuard.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        PjbTimeService timeService = Mockito.mock(PjbTimeService.class);
        QualifiedDocumentSignatureEnvelopeService qsvc = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);

        UUID newTc = PjbUuidV7Generator.generate();
        Usuario mp = new Usuario();
        mp.setId(7L);
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-05-01T12:00:00Z"));
        when(timeService.legalZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        when(oficioRepository.findFirstByBodyHashAndCreatedAtAfter(Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());
        when(oficioRepository.save(Mockito.any(LaianeOficio.class))).thenAnswer(inv -> {
            LaianeOficio o = inv.getArgument(0);
            if (o.getTrackingCode() == null) o.setTrackingCode(newTc);
            o.setId(51L);
            return o;
        });

        ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        LaianeMpService service = new LaianeMpService(guard, accessGuard,
                Mockito.mock(WorkItemRepository.class), oficioRepository,
                Mockito.mock(UsuarioRepository.class), Mockito.mock(AuditoriaRepository.class),
                timeService, qsvc, testObjectMapper, Mockito.mock(ApplicationEventPublisher.class));

        var req1 = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO").conteudo("conteudo A").justificativa("novo").build();
        var req2 = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO").conteudo("conteudo B").justificativa("novo").build();

        var resp1 = service.createOficio(req1);
        var resp2 = service.createOficio(req2);

        assertThat(resp1.getTrackingCode()).isNotNull();
        assertThat(resp2.getTrackingCode()).isNotNull();
    }
}
