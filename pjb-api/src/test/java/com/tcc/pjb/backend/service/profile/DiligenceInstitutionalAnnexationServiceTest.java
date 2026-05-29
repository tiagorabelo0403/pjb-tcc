package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.idempotency.IdempotencyStatus;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalAnnexationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceInstitutionalAnnexationServiceTest {

    @Test
    void anexaJuntadaNaMalhaInstitucionalComIdempotenciaCruzada() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ActionIdempotencyService actionIdempotencyService = Mockito.mock(ActionIdempotencyService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenciaOperadorJuntadaProcessualRepository juntadaRepository = Mockito.mock(DiligenciaOperadorJuntadaProcessualRepository.class);
        DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        ProcessEventStore processEventStore = Mockito.mock(ProcessEventStore.class);
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                java.util.List.of("govbr=OURO", "mfa_ativo", "dispositivo_homologado")
        ));
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.class));
        DiligenceInstitutionalAnnexationService service = new DiligenceInstitutionalAnnexationService(
                currentUserService,
                authorizationService,
                actionIdempotencyService,
                keyMaterialService,
                juntadaRepository,
                anexacaoRepository,
                processoRepository,
                documentoRepository,
                processEventStore,
                qualifiedDocumentSignatureEnvelopeService
        );

        Usuario actor = usuario();
        Processo processo = processo();
        DocumentoProcessual pacote = DocumentoProcessual.builder().id(UUID.fromString("33333333-3333-3333-3333-333333333333")).titulo("Pacote").build();
        DiligenciaOperadorJuntadaProcessual juntada = DiligenciaOperadorJuntadaProcessual.builder()
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
                .pacoteDocumentoId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .bundleReference("OFICIAL_JUSTICA:MALHA_CE:501:1900:900")
                .bundleDigestSha256("ab".repeat(32))
                .bundleSignatureHmacSha256("cd".repeat(32))
                .exportarMalhaExterna(true)
                .externalSystemCode("MALHA_CE")
                .createdAt(Instant.parse("2026-03-12T12:10:00Z"))
                .build();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(juntadaRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77"))
                .thenReturn(Optional.of(juntada));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(documentoRepository.findById(UUID.fromString("33333333-3333-3333-3333-333333333333"))).thenReturn(Optional.of(pacote));
        when(actionIdempotencyService.begin(any(), any(), any(), any()))
                .thenReturn(new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, "scope", "idem", "hash", null, null, null));
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processEventStore.append(Mockito.eq(501L), Mockito.eq(com.tcc.pjb.backend.core.kernel.process.ProcessEventType.DOCUMENTS_BULK_ADDED), any()))
                .thenReturn(ProcessEventEnvelope.builder().id(7L).processoId(501L).seq(92L).eventType("DOCUMENTS_BULK_ADDED").payload("{}").payloadHash("ef".repeat(32)).chainHash("aa").createdAt(Instant.now()).build());
        when(anexacaoRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorAnexacaoInstitucional entity = inv.getArgument(0);
            entity.setId(4000L);
            return entity;
        });

        var response = service.annex(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77",
                new DiligenceInstitutionalAnnexationRequest(null, null, "MALHA_CE", "MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA", true, true, true, "Remessa controlada"));

        assertThat(response.annexationId()).isEqualTo(4000L);
        assertThat(response.processoId()).isEqualTo(501L);
        assertThat(response.juntadaId()).isEqualTo(3000L);
        assertThat(response.externalSystemCode()).isEqualTo("MALHA_CE");
        assertThat(response.destinationBox()).contains("MALHA_CE");
        assertThat(response.annexationStatus()).isEqualTo("ACKED");
        assertThat(response.executionDigestSha256()).hasSize(64);
        assertThat(response.processEventSeq()).isEqualTo(92L);
        assertThat(response.assinaturaQualificada()).isNotNull();
        assertThat(response.assinaturaQualificada().envelopeId()).isNotNull();
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
}
