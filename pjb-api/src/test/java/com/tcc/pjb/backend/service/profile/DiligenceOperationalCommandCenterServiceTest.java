package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceOperationalCommandCenterServiceTest {

    @Test
    void agregaPainelMultiunidadePorProcessoEOrgao() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository = Mockito.mock(DiligenciaOperadorMalhaInstitucionalDispatchRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DiligenceOperationalCommandCenterService service = new DiligenceOperationalCommandCenterService(
                currentUserService,
                annexationRepository,
                dispatchRepository,
                processoRepository
        );

        Usuario actor = usuario();
        Processo processo = processo();

        when(currentUserService.getRequired()).thenReturn(actor);
        when(annexationRepository.findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(Mockito.eq(88L), Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.any()))
                .thenReturn(List.of(annexation()));
        when(dispatchRepository.findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(Mockito.eq(88L), Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.any()))
                .thenReturn(List.of(dispatch()));
        when(processoRepository.findAllById(Mockito.anyIterable())).thenReturn(List.of(processo));

        var response = service.snapshot(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, 30, 10);

        assertThat(response.summary().annexations()).isEqualTo(1L);
        assertThat(response.summary().backlog()).isEqualTo(1L);
        assertThat(response.processBuckets()).hasSize(1);
        assertThat(response.unitBuckets()).hasSize(1);
        assertThat(response.organizationBuckets()).hasSize(1);
        assertThat(response.alerts()).contains("BACKLOG_MALHA=1");
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
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .createdAt(Instant.parse("2026-03-12T12:10:00Z"))
                .build();
    }

    private static DiligenciaOperadorMalhaInstitucionalDispatch dispatch() {
        return DiligenciaOperadorMalhaInstitucionalDispatch.builder()
                .id(9001L)
                .operatorUserId(88L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("77")
                .processoId(501L)
                .processoNumero("0009999-11.2026.8.06.0001")
                .annexationId(4000L)
                .juntadaId(3000L)
                .eventType("PROFILE_INSTITUTIONAL_MESH_DISPATCH")
                .routingKey("MESH:DILIGENCE:OFICIAL_JUSTICA:TJCE:QUIXADA")
                .externalSystemCode("MALHA_CE")
                .destinationBox("MALHA_CE:OFICIAL_JUSTICA:CE:QUIXADA")
                .meshOrgKey("TJCE")
                .meshUnitKey("QUIXADA")
                .dispatchStatus("DISPATCHED")
                .replayToken("ab".repeat(32))
                .chainIdempotencyKey("cd".repeat(32))
                .payloadDigestSha256("ef".repeat(32))
                .payloadSignatureHmacSha256("12".repeat(32))
                .deliveredAt(Instant.parse("2026-03-12T12:15:00Z"))
                .createdAt(Instant.parse("2026-03-12T12:12:00Z"))
                .build();
    }
}
