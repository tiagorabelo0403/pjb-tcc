package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
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
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
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

class DiligenceProcessFormalizationServiceTest {

    @Test
    void formalizaEncerramentoEmMovimentacaoEDocumento() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = Mockito.mock(DiligenciaOperadorEncerramentoRepository.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository = Mockito.mock(DiligenciaOperadorCertidaoDocumentoRepository.class);
        DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository = Mockito.mock(DiligenciaOperadorFormalizacaoProcessualRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        MovimentacaoProcessualRepository movimentacaoRepository = Mockito.mock(MovimentacaoProcessualRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        ProcessEventStore processEventStore = Mockito.mock(ProcessEventStore.class);
        DigitalCustodyChainLedgerService custodyLedgerService = Mockito.mock(DigitalCustodyChainLedgerService.class);
        DiligenceOperationalMinutePdfService minutePdfService = Mockito.mock(DiligenceOperationalMinutePdfService.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        SovereignValidationResult sovereignResult = new SovereignValidationResult("VALIDO", null, null, false, false, false, false, false, null, null, null, null, null, null, null, List.of());
        QualifiedSignatureMetadata qsm = new QualifiedSignatureMetadata("ENV-FORM", null, null, null, true, "PJB-RUB-FORM", LocalDate.of(2026, 3, 11), LocalTime.of(15, 0), "QUIXADÁ", "Oficial Operacional", "OFICIAL_JUSTICA", null, null, null, null, null, null, null, null, true, null, null, null, sovereignResult);
        when(qualifiedDocumentSignatureEnvelopeService.signFreeContent(any(), any(), any(), any(), any(), any(), Mockito.anyBoolean(), any())).thenReturn(new SignedDocumentEnvelope("FORMALIZACAO_TITULO", "FORMALIZACAO_ASSINADA", "aa".repeat(32), true, qsm, sovereignResult));
        DiligenceProcessFormalizationService service = new DiligenceProcessFormalizationService(
                currentUserService,
                authorizationService,
                keyMaterialService,
                encerramentoRepository,
                certidaoRepository,
                certidaoDocumentoRepository,
                formalizacaoRepository,
                processoRepository,
                workItemRepository,
                movimentacaoRepository,
                documentoRepository,
                processEventStore,
                custodyLedgerService,
                minutePdfService,
                qualifiedDocumentSignatureEnvelopeService
        );

        Usuario actor = usuario();
        Processo processo = processo();
        DiligenciaOperadorCertidao certidao = certidao();
        DiligenciaOperadorEncerramento encerramento = encerramento();
        DiligenciaOperadorCertidaoDocumento vinculo = DiligenciaOperadorCertidaoDocumento.builder()
                .id(1L)
                .certidaoId(900L)
                .processoId(501L)
                .documentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .documentoTitulo("Foto do local")
                .documentoSha256("ab".repeat(32))
                .origem("REQUEST")
                .createdAt(Instant.parse("2026-03-12T11:40:00Z"))
                .build();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(Optional.of(encerramento));
        when(certidaoRepository.findById(900L)).thenReturn(Optional.of(certidao));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        when(workItemRepository.findById(77L)).thenReturn(Optional.of(WorkItem.builder().id(77L).processo(processo).build()));
        doNothing().when(authorizationService).requireReadProcesso(processo);
        when(formalizacaoRepository.findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(Mockito.eq(88L), Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.eq("77"), any()))
                .thenReturn(Optional.empty());
        when(certidaoDocumentoRepository.findByCertidaoIdOrderByCreatedAtDesc(900L)).thenReturn(List.of(vinculo));
        when(custodyLedgerService.findLedger("CST-XYZ")).thenReturn(Optional.of(new ChainOfCustodyLedgerResponse("L-1", "cd".repeat(32), "CST-XYZ", "OFICIAL_JUSTICA", Instant.parse("2026-03-12T10:00:00Z"), 1, true, List.of())));
        when(minutePdfService.render(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new DiligenceOperationalMinutePdfService.RenderedOperationalMinute("Minuta Operacional", "%PDF-FAKE".getBytes(StandardCharsets.UTF_8), 10, 1));
        when(documentoRepository.findFirstByProcesso_IdAndSha256(Mockito.eq(501L), any())).thenReturn(Optional.empty());
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            DocumentoProcessual documento = inv.getArgument(0);
            documento.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return documento;
        });
        when(movimentacaoRepository.save(any())).thenAnswer(inv -> {
            MovimentacaoProcessual mov = inv.getArgument(0);
            mov.setId(700L);
            return mov;
        });
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processEventStore.append(Mockito.eq(501L), any(), any()))
                .thenReturn(ProcessEventEnvelope.builder().id(1L).processoId(501L).seq(44L).eventType("MOVEMENT_RECORDED").payload("{}").payloadHash("aa").chainHash("bb").createdAt(Instant.now()).build())
                .thenReturn(ProcessEventEnvelope.builder().id(2L).processoId(501L).seq(45L).eventType("DOCUMENT_ADDED").payload("{}").payloadHash("cc").chainHash("dd").createdAt(Instant.now()).build());
        when(formalizacaoRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorFormalizacaoProcessual entity = inv.getArgument(0);
            entity.setId(1900L);
            return entity;
        });

        var response = service.formalize(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77",
                new DiligenceProcessFormalizationRequest(null, null, null, true, true, "Minuta Operacional", "Complemento objetivo", "CST-XYZ"));

        assertThat(response.formalizacaoId()).isEqualTo(1900L);
        assertThat(response.processoId()).isEqualTo(501L);
        assertThat(response.certidaoId()).isEqualTo(900L);
        assertThat(response.movimentacaoId()).isEqualTo(700L);
        assertThat(response.movimentacaoEventSeq()).isEqualTo(44L);
        assertThat(response.minutaDocumentoId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(response.minutaEventSeq()).isEqualTo(45L);
        assertThat(response.evidenceIntegrityOk()).isTrue();
        assertThat(response.documentosReferenciados()).isEqualTo(1);
        assertThat(response.formalizationDigestSha256()).hasSize(64);
        assertThat(response.assinaturaQualificada()).isNotNull();
        assertThat(response.assinaturaQualificada().rubricaEletronica()).isEqualTo("PJB-RUB-FORM");
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
                .certificateDigestSha256("ab".repeat(32))
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
                .checkpointEventId(333L)
                .certidaoDigestSha256("ab".repeat(32))
                .createdAt(Instant.parse("2026-03-12T11:31:00Z"))
                .build();
    }
}
