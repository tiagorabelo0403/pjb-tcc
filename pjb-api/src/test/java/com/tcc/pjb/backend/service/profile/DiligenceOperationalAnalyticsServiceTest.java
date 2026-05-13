package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class DiligenceOperationalAnalyticsServiceTest {

    @Test
    void agregaIndicadoresDeProcessoOperadorEUnidade() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        DiligenciaOperadorTelemetriaRepository telemetriaRepository = Mockito.mock(DiligenciaOperadorTelemetriaRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = Mockito.mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorCertidaoRepository certidaoRepository = Mockito.mock(DiligenciaOperadorCertidaoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = Mockito.mock(DiligenciaOperadorEncerramentoRepository.class);
        DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository = Mockito.mock(DiligenciaOperadorFormalizacaoProcessualRepository.class);
        DiligenciaOperadorJuntadaProcessualRepository juntadaRepository = Mockito.mock(DiligenciaOperadorJuntadaProcessualRepository.class);
        DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository = Mockito.mock(DiligenciaOperadorAnexacaoInstitucionalRepository.class);
        DiligenceReferenceResolverService referenceResolverService = Mockito.mock(DiligenceReferenceResolverService.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        ProcessEventRepository processEventRepository = Mockito.mock(ProcessEventRepository.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        DiligenceOperationalAnalyticsService service = new DiligenceOperationalAnalyticsService(
                currentUserService,
                authorizationService,
                telemetriaRepository,
                checkpointRepository,
                certidaoRepository,
                encerramentoRepository,
                formalizacaoRepository,
                juntadaRepository,
                anexacaoRepository,
                referenceResolverService,
                processoRepository,
                documentoRepository,
                processEventRepository,
                jdbcTemplate
        );

        Usuario actor = usuario();
        Processo processo = processo();
        when(currentUserService.getRequired()).thenReturn(actor);
        when(checkpointRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(checkpoint()));
        when(certidaoRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(certidao()));
        when(encerramentoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(encerramento()));
        when(formalizacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(formalizacao()));
        when(juntadaRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(juntada()));
        when(anexacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(List.of(anexacao()));
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireReadProcesso(processo);
        when(telemetriaRepository.countByOperatorUserIdAndCanal(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA)).thenReturn(9L);
        when(checkpointRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(2L);
        when(certidaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(1L);
        when(encerramentoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(1L);
        when(formalizacaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(1L);
        when(juntadaRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(1L);
        when(anexacaoRepository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(1L);
        when(documentoRepository.countByProcesso_Id(501L)).thenReturn(5L);
        when(processEventRepository.countByProcessoId(501L)).thenReturn(12L);
        when(telemetriaRepository.countByOperatorUserIdAndCanalAndCapturadoEmAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(4L);
        when(checkpointRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(2L);
        when(certidaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(1L);
        when(encerramentoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(1L);
        when(formalizacaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(1L);
        when(juntadaRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(1L);
        when(anexacaoRepository.countByOperatorUserIdAndCanalAndCreatedAtAfter(eq(88L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(1L);
        when(telemetriaRepository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA)).thenReturn(Optional.empty());
        when(checkpointRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(Optional.of(checkpoint()));
        when(certidaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77")).thenReturn(Optional.of(certidao()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(), any(), any(), any(), any())).thenReturn(6L, 3L, 2L, 2L, 1L, 1L, 1L, 2L, 4L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(java.sql.Timestamp.class), any(), any(), any(), any())).thenReturn(java.sql.Timestamp.from(Instant.parse("2026-03-12T12:20:00Z")));

        var response = service.analytics(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "77");

        assertThat(response.processScope().processoId()).isEqualTo(501L);
        assertThat(response.processScope().currentStage()).isEqualTo("ANEXACAO_INSTITUCIONAL");
        assertThat(response.processScope().anexacoesInstitucionais()).isEqualTo(1L);
        assertThat(response.operatorScope().annexationCoverage()).isEqualTo(1.0);
        assertThat(response.unitScope().operadoresAtivos()).isEqualTo(2L);
        assertThat(response.unitScope().processosImpactados()).isEqualTo(4L);
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

    private static DiligenciaOperadorCheckpointEvento checkpoint() {
        return DiligenciaOperadorCheckpointEvento.builder().id(200L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").checkpointTipo(DiligenciaCheckpointTipo.CHEGADA).processoId(501L).processoNumero("0009999-11.2026.8.06.0001").insideGeofence(true).tentativaSequencia(2).occurredAt(Instant.parse("2026-03-12T11:26:00Z")).createdAt(Instant.parse("2026-03-12T11:26:00Z")).build();
    }

    private static DiligenciaOperadorCertidao certidao() {
        return DiligenciaOperadorCertidao.builder().id(900L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").processoId(501L).processoNumero("0009999-11.2026.8.06.0001").certidaoTipo(DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO).createdAt(Instant.parse("2026-03-12T11:30:00Z")).build();
    }

    private static DiligenciaOperadorEncerramento encerramento() {
        return DiligenciaOperadorEncerramento.builder().id(901L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").processoId(501L).processoNumero("0009999-11.2026.8.06.0001").outcome(DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO).createdAt(Instant.parse("2026-03-12T11:32:00Z")).build();
    }

    private static DiligenciaOperadorFormalizacaoProcessual formalizacao() {
        return DiligenciaOperadorFormalizacaoProcessual.builder().id(1900L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").processoId(501L).processoNumero("0009999-11.2026.8.06.0001").createdAt(Instant.parse("2026-03-12T11:40:00Z")).build();
    }

    private static DiligenciaOperadorJuntadaProcessual juntada() {
        return DiligenciaOperadorJuntadaProcessual.builder().id(3000L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").processoId(501L).processoNumero("0009999-11.2026.8.06.0001").createdAt(Instant.parse("2026-03-12T12:10:00Z")).build();
    }

    private static DiligenciaOperadorAnexacaoInstitucional anexacao() {
        return DiligenciaOperadorAnexacaoInstitucional.builder().id(4000L).operatorUserId(88L).operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA).canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA).diligenceReference("77").processoId(501L).processoNumero("0009999-11.2026.8.06.0001").createdAt(Instant.parse("2026-03-12T12:15:00Z")).build();
    }
}
