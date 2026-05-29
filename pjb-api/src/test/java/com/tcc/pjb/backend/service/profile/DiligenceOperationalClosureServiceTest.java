package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceOperationalClosureServiceTest {

    @Test
    void encerraCumprimentoComIdempotenciaEDocumentos() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenceOperationalCertificateService certificateService = Mockito.mock(DiligenceOperationalCertificateService.class);
        DiligenceCertificateEvidenceService evidenceService = Mockito.mock(DiligenceCertificateEvidenceService.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = Mockito.mock(DiligenciaOperadorEncerramentoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        InstitutionalActorRoutingService institutionalActorRoutingService = Mockito.mock(InstitutionalActorRoutingService.class);
        InstitutionalSessionSecuritySignalService securitySignalService = Mockito.mock(InstitutionalSessionSecuritySignalService.class);
        when(securitySignalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of("govbr=OURO", "mfa_ativo", "dispositivo_homologado")
        ));
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = new QualifiedDocumentSignatureEnvelopeService(securitySignalService, new QualifiedSignatureIdentityContextService(), new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.class));
        DiligenceOperationalClosureService service = new DiligenceOperationalClosureService(currentUserService, keyMaterialService, certificateService, evidenceService, certidaoRepository, encerramentoRepository, workItemRepository, institutionalActorRoutingService, qualifiedDocumentSignatureEnvelopeService);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(certificateService.generate(Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.eq("99"), any(DiligenceAutoCertificateRequest.class)))
                .thenReturn(new DiligenceCertificateResponse(900L, "OFICIAL_JUSTICA", "OFICIAL_JUSTICA", "99", 99L, 501L, "0009999-11.2026.8.06.0001", 700L, DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO.name(), "t", "n", "ab".repeat(32), "cd".repeat(32), -4.3d, -38.9d, -4.3d, -38.9d, 10d, true, 1, "CST-1", "ef".repeat(32), Instant.parse("2026-03-11T18:00:00Z"), null, null));
        DiligenciaOperadorCertidao certidao = DiligenciaOperadorCertidao.builder()
                .id(900L)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("99")
                .workItemId(99L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .checkpointEventId(700L)
                .certificateDigestSha256("ab".repeat(32))
                .createdAt(Instant.parse("2026-03-11T18:00:00Z"))
                .build();
        when(certidaoRepository.findById(900L)).thenReturn(Optional.of(certidao));
        when(encerramentoRepository.findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(Mockito.eq(88L), Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.eq("99"), any()))
                .thenReturn(Optional.empty());
        Processo processo = new Processo();
        processo.setId(501L);
        processo.setNumeroProcesso("0009999-11.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA);
        WorkItem workItem = WorkItem.builder()
                .id(99L)
                .processo(processo)
                .type(WorkItemType.EXPEDICAO)
                .titulo("Mandado de citação")
                .status(WorkItemStatus.PENDENTE)
                .templateCode("MANDADO:99")
                .build();
        when(workItemRepository.findById(99L)).thenReturn(Optional.of(workItem));
        when(workItemRepository.save(any())).thenAnswer(inv -> {
            WorkItem entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1200L);
            }
            return entity;
        });
        when(evidenceService.bind(Mockito.eq(900L), any())).thenReturn(List.of());
        when(evidenceService.count(900L)).thenReturn(2);
        when(encerramentoRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorEncerramento entity = inv.getArgument(0);
            entity.setId(1500L);
            return entity;
        });

        var response = service.close(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "99",
                new DiligenceOperationalClosureRequest(null, 700L, DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO, null, "CST-1", "cumprido com êxito", List.of(UUID.randomUUID(), UUID.randomUUID())));

        assertThat(response.encerramentoId()).isEqualTo(1500L);
        assertThat(response.outcome()).isEqualTo("CUMPRIMENTO_POSITIVO");
        assertThat(response.workItemStatusFinal()).isEqualTo("CONCLUIDO");
        assertThat(response.followupWorkItemId()).isEqualTo(1200L);
        assertThat(response.documentosVinculados()).isEqualTo(2);
        assertThat(response.executionDigestSha256()).hasSize(64);
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
}
