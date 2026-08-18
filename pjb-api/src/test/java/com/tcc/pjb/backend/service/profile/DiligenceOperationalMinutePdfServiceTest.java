package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceOperationalMinutePdfServiceTest {

    @Test
    void geraPdfInstitucionalValido() {
        QualifiedDocumentSignatureEnvelopeService signatureService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        SovereignValidationResult sovereignResult = new SovereignValidationResult("VALIDO", "PJB_QUALIFIED_SIGNATURE_SPINE", "DELEGADO_POLICIA_QUALIFICADA_SOBERANA", false, false, false, false, false, "DELEGADO_POLICIA", null, null, null, "SESSION", "REPLAY", "DOC-HASH", List.of());
        QualifiedSignatureMetadata qsm = new QualifiedSignatureMetadata("PJB-ENV-123", "ASSINATURA", null, null, true, "PJB-RUB-123", LocalDate.of(2026, 3, 12), LocalTime.of(12, 0), "Quixadá/CE", null, "DELEGADO_POLICIA", null, null, null, null, null, null, null, null, true, null, null, null, sovereignResult);
        when(signatureService.signFreeContent(any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new SignedDocumentEnvelope("conteudo-titulo", "conteudo", "hash-conteudo", true, qsm, sovereignResult));
        DiligenceOperationalMinutePdfService service = new DiligenceOperationalMinutePdfService(signatureService);
        var pdf = service.render(usuario(), TelemetriaOperacionalCanal.DELEGADO, "101", processo(), certidao(), encerramento(), List.of(
                DiligenciaOperadorCertidaoDocumento.builder()
                        .certidaoId(900L)
                        .processoId(501L)
                        .documentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .documentoTitulo("Foto do local")
                        .documentoSha256("ab".repeat(32))
                        .origem("REQUEST")
                        .build()
        ), "Minuta Investigativa", "Complemento institucional direto", "CST-1", true);

        assertThat(pdf.titulo()).contains("Minuta Investigativa");
        assertThat(pdf.referencedDocuments()).isEqualTo(1);
        assertThat(pdf.bodyLines()).isGreaterThan(10);
        assertThat(pdf.pdf()).isNotEmpty();
        assertThat(new String(pdf.pdf(), 0, 4)).isEqualTo("%PDF");
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setNome("Delegado Operacional");
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setPerfil(TipoUsuario.DELEGADO_POLICIA.name());
        usuario.setUf("CE");
        usuario.setComarca("Quixadá");
        return usuario;
    }

    private static Processo processo() {
        Processo processo = new Processo();
        processo.setId(501L);
        processo.setNumeroProcesso("0009999-11.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        return processo;
    }

    private static DiligenciaOperadorCertidao certidao() {
        return DiligenciaOperadorCertidao.builder()
                .id(900L)
                .workItemId(101L)
                .checkpointEventId(700L)
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .certidaoTipo(DiligenciaCertidaoTipo.CHEGADA_CONFIRMADA)
                .narrativa("Chegada confirmada ao destino com equipe completa e evidência preservada.")
                .certificateDigestSha256("ab".repeat(32))
                .attemptTrailDigestSha256("cd".repeat(32))
                .latitude(-4.30d)
                .longitude(-38.91d)
                .destinoLatitude(-4.30d)
                .destinoLongitude(-38.91d)
                .distanceMeters(11d)
                .insideGeofence(true)
                .tentativaSequencia(1)
                .createdAt(Instant.parse("2026-03-12T12:00:00Z"))
                .build();
    }

    private static DiligenciaOperadorEncerramento encerramento() {
        return DiligenciaOperadorEncerramento.builder()
                .id(901L)
                .outcome(DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO)
                .build();
    }
}
