package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceOperationalCertificateServiceTest {

    @Test
    void geraCertidaoAssinadaComTrilhaGeorreferenciada() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        KeyMaterialService keyMaterialService = new KeyMaterialService(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        DiligenceReferenceResolverService referenceResolverService = Mockito.mock(DiligenceReferenceResolverService.class);
        DiligenceInstitutionalTemplateService templateService = new DiligenceInstitutionalTemplateService();
        DiligenceCheckpointService checkpointService = Mockito.mock(DiligenceCheckpointService.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = Mockito.mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        when(qualifiedDocumentSignatureEnvelopeService.signFreeContent(any(), any(), any(), any(), any(), any(), Mockito.anyBoolean(), any())).thenReturn(new QualifiedDocumentSignatureEnvelopeService.SignedContent("CERTIDAO_ASSINADA", "aa".repeat(32), java.util.Map.of("rubrica", "PJB-RUB-TESTE"), java.util.Map.of("status", "VALIDO")));
        DiligenceOperationalCertificateService service = new DiligenceOperationalCertificateService(currentUserService, keyMaterialService, referenceResolverService, templateService, checkpointService, checkpointRepository, certidaoRepository, qualifiedDocumentSignatureEnvelopeService);
        when(currentUserService.getRequired()).thenReturn(usuario());
        DiligenciaOperadorCheckpointEvento checkpoint = DiligenciaOperadorCheckpointEvento.builder()
                .id(700L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.DELEGADO_POLICIA)
                .canal(TelemetriaOperacionalCanal.DELEGADO)
                .diligenceReference("101")
                .checkpointTipo(DiligenciaCheckpointTipo.CHEGADA)
                .targetLatitude(-4.30d)
                .targetLongitude(-38.91d)
                .observedLatitude(-4.3004d)
                .observedLongitude(-38.9103d)
                .distanceMeters(52.13d)
                .geofenceRadiusMeters(120d)
                .insideGeofence(true)
                .classification("CHEGADA_CONFIRMADA")
                .source("GPS")
                .workItemId(101L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .tentativaSequencia(2)
                .locationSignatureSha256("ab".repeat(32))
                .occurredAt(Instant.parse("2026-03-11T15:00:00Z"))
                .createdAt(Instant.parse("2026-03-11T15:00:01Z"))
                .build();
        when(checkpointService.latestEntity(TelemetriaOperacionalCanal.DELEGADO, "101")).thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(88L, TelemetriaOperacionalCanal.DELEGADO, "101"))
                .thenReturn(List.of(checkpoint));
        when(referenceResolverService.resolve(TelemetriaOperacionalCanal.DELEGADO, "101"))
                .thenReturn(Optional.empty());
        when(certidaoRepository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorCertidao entity = inv.getArgument(0);
            entity.setId(900L);
            return entity;
        });

        var response = service.generate(TelemetriaOperacionalCanal.DELEGADO, "101",
                new DiligenceAutoCertificateRequest(null, null, "CST-01", "chegada confirmada com equipe completa"));

        assertThat(response.certidaoId()).isEqualTo(900L);
        assertThat(response.certidaoTipo()).isEqualTo("CHEGADA_CONFIRMADA");
        assertThat(response.signatureHmacSha256()).hasSize(64);
        assertThat(response.attemptTrailDigestSha256()).hasSize(64);
        assertThat(response.narrativa()).isEqualTo("CERTIDAO_ASSINADA");
        assertThat(response.assinaturaQualificada()).containsKey("rubrica");
        assertThat(response.validacaoSoberana()).containsEntry("status", "VALIDO");
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setNome("Delegado Operacional");
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setPerfil(TipoUsuario.DELEGADO_POLICIA.name());
        usuario.setCpf("12345678901");
        usuario.setEmail("delegado@pjb.test");
        usuario.setSenha("x");
        return usuario;
    }
}
