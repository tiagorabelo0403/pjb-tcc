package com.tcc.pjb.backend.service.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.device.DeviceRiskEngine;
import com.tcc.pjb.backend.core.security.device.RiskEvaluation;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.intelligence.PessoaLocalizacaoConsultaGovernadaRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.service.profile.PerfilBehavioralAuditService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PessoaLocalizacaoGovernanceServiceTest {

    @Test
    void deveMarcarRevisaoQuandoConsultaSensivelSemContextoFormal() {
        PessoaLocalizacaoConsultaGovernadaRepository repository = Mockito.mock(PessoaLocalizacaoConsultaGovernadaRepository.class);
        PerfilBehavioralAuditService behavioralAuditService = Mockito.mock(PerfilBehavioralAuditService.class);
        UserSecurityProfileRepository userSecurityProfileRepository = Mockito.mock(UserSecurityProfileRepository.class);
        DeviceRiskEngine deviceRiskEngine = Mockito.mock(DeviceRiskEngine.class);
        PjbTimeService timeService = new PjbTimeService(Clock.fixed(Instant.parse("2026-03-11T23:30:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Fortaleza"));

        when(behavioralAuditService.avaliar(any())).thenReturn(new PerfilDashboardPayload.BehavioralAuditResumo("DELEGADO_POLICIA", 220, 120, "ANOMALO", true, "baseline"));
        when(userSecurityProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(deviceRiskEngine.evaluateFirstLink(any(), any(), any(), anyBoolean())).thenReturn(RiskEvaluation.challenge(75, "NET", true, "challenge"));

        PessoaLocalizacaoGovernanceService service = new PessoaLocalizacaoGovernanceService(repository, behavioralAuditService, userSecurityProfileRepository, deviceRiskEngine, timeService);
        PessoaLocalizacaoGovernanceService.PosturaConsulta postura = service.avaliar(usuario(), PessoaLocalizacaoService.CanalConsulta.DELEGADO, request(), false, false, false);

        assertEquals("CRITICO", postura.level());
        assertTrue(postura.requiresReview());
        assertTrue(postura.sinais().contains("SEM_CONTEXTO_FORMAL"));
        assertTrue(postura.sinais().contains("COMPORTAMENTO_ANOMALO"));
    }


    @Test
    void deveNormalizarCamposAoPersistirConsultaGovernada() {
        PessoaLocalizacaoConsultaGovernadaRepository repository = Mockito.mock(PessoaLocalizacaoConsultaGovernadaRepository.class);
        PerfilBehavioralAuditService behavioralAuditService = Mockito.mock(PerfilBehavioralAuditService.class);
        UserSecurityProfileRepository userSecurityProfileRepository = Mockito.mock(UserSecurityProfileRepository.class);
        DeviceRiskEngine deviceRiskEngine = Mockito.mock(DeviceRiskEngine.class);
        PjbTimeService timeService = new PjbTimeService(Clock.fixed(Instant.parse("2026-03-11T10:30:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Fortaleza"));

        PessoaLocalizacaoGovernanceService service = new PessoaLocalizacaoGovernanceService(repository, behavioralAuditService, userSecurityProfileRepository, deviceRiskEngine, timeService);

        var response = new com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse(
                "corr-123",
                Instant.parse("2026-03-11T10:30:00Z"),
                "DELEGADO_POLICIA",
                "scope",
                " ",
                "***.***.***-**",
                true,
                "ESTRITO",
                new com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse.GovernancaConsultaResumo(
                        "INVESTIGACAO_POLICIAL_FORMAL",
                        false,
                        true,
                        true,
                        "TRILHA",
                        java.util.List.of("CTRL"),
                        null,
                        true
                ),
                new com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse.SecurityPostureResumo(
                        "ALTO",
                        77,
                        true,
                        false,
                        "FORMAL_ESTRITO",
                        true,
                        false,
                        " /api/v1/auth/stepup/start ",
                        java.util.List.of("S1", "S2")
                ),
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                "recomendacao"
        );
        var postura = new PessoaLocalizacaoGovernanceService.PosturaConsulta(
                "ALTO",
                77,
                true,
                false,
                "FORMAL_ESTRITO",
                true,
                false,
                " /api/v1/auth/stepup/start ",
                java.util.List.of("S1", "S2")
        );

        service.registrar("corr-123", usuario(), "12345678909", PessoaLocalizacaoService.CanalConsulta.DELEGADO, request(), response, postura);

        ArgumentCaptor<com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada> captor = ArgumentCaptor.forClass(com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada.class);
        verify(repository).save(captor.capture());
        var persisted = captor.getValue();

        assertEquals("FINALIDADE_NAO_INFORMADA", persisted.getFinalidade());
        assertEquals("IP-2026-1", persisted.getReferenciaProcedimental());
        assertEquals("/api/v1/auth/stepup/start", persisted.getChallengeHint());
        assertEquals("ALTO", persisted.getPosturaNivel());
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Delegado");
        usuario.setEmail("delegado@pjb.test");
        usuario.setCpf("12345678909");
        usuario.setSenha("x");
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.syncPerfilETipoUsuario();
        return usuario;
    }

    private static PessoaLocalizacaoRequest request() {
        return new PessoaLocalizacaoRequest("12345678909", null, null, null, "investigacao", "justificativa robusta", "IP-2026-1", true, true, true, true);
    }
}
