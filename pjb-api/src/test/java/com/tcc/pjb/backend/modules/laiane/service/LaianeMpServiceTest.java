package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpOficioCreateRequest;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeOficioRepository;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LaianeMpServiceTest {

    @Test
    void createOficioRetornaEnvelopeQualificado() {
        LaianeRoleGuard guard = Mockito.mock(LaianeRoleGuard.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        LaianeOficioRepository oficioRepository = Mockito.mock(LaianeOficioRepository.class);
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        AuditoriaInteligenteService auditoria = Mockito.mock(AuditoriaInteligenteService.class);
        AuditoriaRepository auditoriaRepository = Mockito.mock(AuditoriaRepository.class);
        PjbTimeService timeService = Mockito.mock(PjbTimeService.class);
        QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);

        Usuario mp = new Usuario();
        mp.setId(88L);
        mp.setNome("Promotor de Justiça");
        mp.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        mp.setComarca("Quixadá");
        mp.setUf("CE");

        when(guard.requireMinisterioPublico()).thenReturn(mp);
        when(timeService.nowUtc()).thenReturn(Instant.parse("2026-04-07T18:00:00Z"));
        when(qualifiedDocumentSignatureEnvelopeService.signFreeContent(Mockito.isNull(), Mockito.eq(mp), anyString(), anyString(), anyString(), anyString(), Mockito.eq(true), Mockito.eq(List.of("MINISTERIO_PUBLICO", "OFICIO_INSTITUCIONAL", "LAIANE_MP"))))
                .thenReturn(new QualifiedDocumentSignatureEnvelopeService.SignedContent(
                        "CONTEUDO_ASSINADO",
                        "ab".repeat(32),
                        Map.of("rubrica", "PJB-RUB-TESTE", "envelopeId", "PJB-ENV-TESTE"),
                        Map.of("status", "VALIDO")
                ));
        when(oficioRepository.save(Mockito.any(LaianeOficio.class))).thenAnswer(invocation -> {
            LaianeOficio oficio = invocation.getArgument(0);
            oficio.setId(12L);
            oficio.setTrackingCode(UUID.fromString("12121212-1212-1212-1212-121212121212"));
            oficio.setStatus(LaianeOficioStatus.CRIADO);
            oficio.setCreatedAt(LocalDateTime.parse("2026-04-07T15:00:00"));
            oficio.setUpdatedAt(LocalDateTime.parse("2026-04-07T15:00:00"));
            return oficio;
        });

        LaianeMpService service = new LaianeMpService(
                guard,
                workItemRepository,
                oficioRepository,
                usuarioRepository,
                auditoria,
                auditoriaRepository,
                timeService,
                qualifiedDocumentSignatureEnvelopeService
        );

        LaianeMpOficioCreateRequest request = LaianeMpOficioCreateRequest.builder()
                .tipo("OFICIO_REQUISITORIO")
                .assunto("Requisição de informações")
                .conteudo("Encaminhar documentos em 48h.")
                .justificativa("Fluxo institucional")
                .build();

        var response = service.createOficio(request);

        assertThat(response.getConteudo()).isEqualTo("Encaminhar documentos em 48h.");
        assertThat(response.getDocumentoFormalAssinado()).containsEntry("hashSha256", "ab".repeat(32));
        assertThat(response.getAssinaturaQualificada()).containsEntry("rubrica", "PJB-RUB-TESTE");
        assertThat(response.getValidacaoSoberana()).containsEntry("status", "VALIDO");
    }
}
