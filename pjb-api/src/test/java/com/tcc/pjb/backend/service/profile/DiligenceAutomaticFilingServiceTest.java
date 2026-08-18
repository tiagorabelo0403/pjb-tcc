package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceAutomaticFilingServiceTest {

    @Test
    void geraJuntadaAutomaticaComPacoteEMovimentacao() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository = Mockito.mock(DiligenciaOperadorFormalizacaoProcessualRepository.class);
        DiligenciaOperadorJuntadaProcessualRepository juntadaRepository = Mockito.mock(DiligenciaOperadorJuntadaProcessualRepository.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = Mockito.mock(DiligenciaOperadorEncerramentoRepository.class);
        DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository = Mockito.mock(DiligenciaOperadorCertidaoDocumentoRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        MovimentacaoProcessualRepository movimentacaoRepository = Mockito.mock(MovimentacaoProcessualRepository.class);
        ProcessEventStore processEventStore = Mockito.mock(ProcessEventStore.class);
        DiligenceAutomaticFilingPdfService filingPdfService = Mockito.mock(DiligenceAutomaticFilingPdfService.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        SovereignValidationResult sovereignResult = new SovereignValidationResult("VALIDO", null, null, false, false, false, false, false, null, null, null, null, null, null, null, List.of());
        QualifiedSignatureMetadata qsm = new QualifiedSignatureMetadata("ENV-JUNT", null, null, null, true, "PJB-RUB-JUNT", LocalDate.of(2026, 3, 11), LocalTime.of(15, 0), "QUIXADÁ", "Oficial Operacional", "OFICIAL_JUSTICA", null, null, null, null, null, null, null, null, true, null, null, null, sovereignResult);
        when(qualifiedDocumentSignatureEnvelopeService.signFreeContent(any(), any(), any(), any(), any(), any(), Mockito.anyBoolean(), any())).thenReturn(new SignedDocumentEnvelope("JUNTADA_TITULO", "JUNTADA_ASSINADA", "aa".repeat(32), true, qsm, sovereignResult));
        DiligenceAutomaticFilingService service = new DiligenceAutomaticFilingService(
                currentUserService,
                authorizationService,
                keyMaterialService,
                formalizacaoRepository,
                juntadaRepository,
                certidaoRepository,
                encerramentoRepository,
                certidaoDocumentoRepository,
                processoRepository,
                documentoRepository,
                movimentacaoRepository,
                processEventStore,
                filingPdfService,
                qualifiedDocumentSignatureEnvelopeService
        );

        Usuario actor = usuario();
        Processo processo = processo();
        DiligenciaOperadorFormalizacaoProcessual formalizacao = formalizacao();
        DiligenciaOperadorCertidao certidao = certidao();
        DiligenciaOperadorEncerramento encerramento = encerramento();
        DiligenciaOperadorCertidaoDocumento vinculo = vinculoDocumento();
        DocumentoProcessual minuta = DocumentoProcessual.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .titulo("Minuta operacional")
                .sha256("ef".repeat(32))
                .build();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(formalizacaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(Optional.of(formalizacao));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(certidaoRepository.findById(900L)).thenReturn(Optional.of(certidao));
        when(encerramentoRepository.findById(901L)).thenReturn(Optional.of(encerramento));
        when(documentoRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(Optional.of(minuta));
        when(juntadaRepository.findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(Mockito.eq(88L), Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.eq("77"), any()))
                .thenReturn(Optional.empty());
        when(certidaoDocumentoRepository.findByCertidaoIdOrderByCreatedAtDesc(900L)).thenReturn(List.of(vinculo));
        when(filingPdfService.render(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new DiligenceAutomaticFilingPdfService.RenderedAutomaticFilingPacket("Pacote Operacional", "%PDF-FAKE".getBytes(StandardCharsets.UTF_8), 14, 1));
        when(documentoRepository.findFirstByProcesso_IdAndSha256(Mockito.eq(501L), any())).thenReturn(Optional.empty());
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            DocumentoProcessual documento = inv.getArgument(0);
            documento.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return documento;
        });
        when(movimentacaoRepository.save(any())).thenAnswer(inv -> {
            MovimentacaoProcessual entity = inv.getArgument(0);
            entity.setId(700L);
            return entity;
        });
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processEventStore.append(Mockito.eq(501L), any(), any()))
                .thenReturn(ProcessEventEnvelope.builder().id(1L).processoId(501L).seq(60L).eventType("MOVEMENT_RECORDED").payload("{}").payloadHash("aa").chainHash("bb").createdAt(Instant.now()).build())
                .thenReturn(ProcessEventEnvelope.builder().id(2L).processoId(501L).seq(61L).eventType("DOCUMENTS_BULK_ADDED").payload("{}").payloadHash("cc").chainHash("dd").createdAt(Instant.now()).build());
        when(juntadaRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorJuntadaProcessual entity = inv.getArgument(0);
            entity.setId(3000L);
            entity.setCreatedAt(Instant.parse("2026-03-12T12:10:00Z"));
            return entity;
        });

        var response = service.file(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77",
                new DiligenceAutomaticFilingRequest(null, null, true, true, true, "MALHA_CE", "Pacote Operacional", "Complemento validado"));

        assertThat(response.juntadaId()).isEqualTo(3000L);
        assertThat(response.canal()).isEqualTo("OFICIAL_JUSTICA");
        assertThat(response.processoId()).isEqualTo(501L);
        assertThat(response.formalizacaoId()).isEqualTo(1900L);
        assertThat(response.movimentacaoId()).isEqualTo(700L);
        assertThat(response.movimentacaoEventSeq()).isEqualTo(60L);
        assertThat(response.pacoteEventSeq()).isEqualTo(61L);
        assertThat(response.minutaDocumentoId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.pacoteDocumentoId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(response.bundleReference()).contains("OFICIAL_JUSTICA:MALHA_CE:501:1900:900");
        assertThat(response.bundleDigestSha256()).hasSize(64);
        assertThat(response.bundleSignatureHmacSha256()).hasSize(64);
        assertThat(response.documentosReferenciados()).isEqualTo(1);
        assertThat(response.evidenceIntegrityOk()).isTrue();
        assertThat(response.assinaturaQualificada()).isNotNull();
        assertThat(response.validacaoSoberana().status()).isEqualTo("VALIDO");
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
                .checkpointEventId(333L)
                .movimentacaoId(700L)
                .movimentacaoEventSeq(44L)
                .minutaDocumentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .minutaEventSeq(45L)
                .minutaTitulo("Minuta operacional")
                .minutaSha256("de".repeat(32))
                .minutaSha384("ab".repeat(48))
                .certidaoDigestSha256("bc".repeat(32))
                .evidenceChaveCustodia("CST-XYZ")
                .evidenceIntegrityOk(true)
                .documentosReferenciados(1)
                .idempotencyKey("formalizacao-1")
                .formalizationDigestSha256("cd".repeat(32))
                .createdAt(Instant.parse("2026-03-12T11:40:00Z"))
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
                .checkpointEventId(333L)
                .certidaoTipo(DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO)
                .titulo("Certidão")
                .narrativa("Certifico o cumprimento positivo da diligência.")
                .certificateDigestSha256("bc".repeat(32))
                .signatureHmacSha256("aa".repeat(32))
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
                .checkpointEventId(333L)
                .certidaoDigestSha256("bc".repeat(32))
                .executionDigestSha256("fa".repeat(32))
                .documentosVinculados(1)
                .createdAt(Instant.parse("2026-03-12T11:31:00Z"))
                .build();
    }

    private static DiligenciaOperadorCertidaoDocumento vinculoDocumento() {
        return DiligenciaOperadorCertidaoDocumento.builder()
                .id(1L)
                .certidaoId(900L)
                .processoId(501L)
                .documentoId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .documentoTitulo("Foto do local")
                .documentoSha256("ab".repeat(32))
                .origem("REQUEST")
                .createdAt(Instant.parse("2026-03-12T11:35:00Z"))
                .build();
    }
}
