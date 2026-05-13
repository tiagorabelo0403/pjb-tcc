package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureSystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialCertificateRevocationMode;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecurityPackReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecurityPackService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTlsMode;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcessoFederadoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.service.cidadao.govbr.CidadaoGovBrInteroperabilidadeFederadaService;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CidadaoGovBrInteroperabilidadeFederadaServiceTest {

    @Test
    void deveExporPanoramaFederadoComConectoresSeguros() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        IdentidadeJuridicaNacionalService identidadeService = mock(IdentidadeJuridicaNacionalService.class);
        JudicialConnectorRegistry registry = mock(JudicialConnectorRegistry.class);
        JudicialConnectorRuntimePostureService runtimeService = mock(JudicialConnectorRuntimePostureService.class);
        JudicialConnectorSecurityPackService securityPackService = mock(JudicialConnectorSecurityPackService.class);
        GovBrOidcProperties props = new GovBrOidcProperties(true, false,
                "https://sso.gov.br/auth", "https://sso.gov.br/token", "https://sso.gov.br/userinfo", null,
                "cid", "secret", "https://pjb.jus.br/cb", null, null,
                "https://sso.gov.br/jwks", "https://sso.gov.br", null, null,
                Duration.ofSeconds(3), Duration.ofSeconds(4), Duration.ofMinutes(5));

        Usuario usuario = new Usuario();
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidade.getGovBrNivel()).thenReturn(IdentidadeJuridicaNacional.GovBrNivel.OURO);
        when(identidade.getUltimaSincronizacaoEm()).thenReturn(Instant.parse("2026-04-19T20:00:00Z"));
        when(identidadeService.buscarPorDocumento("12345678901")).thenReturn(Optional.of(identidade));

        JudicialProcessConnector pje = mock(JudicialProcessConnector.class);
        when(pje.capability()).thenReturn(new JudicialSubmissionCapability(
                JudicialSystem.PJE,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                List.of("application/pdf"),
                List.of(),
                List.of(),
                "https://pje.exemplo.jus.br"
        ));
        when(registry.find(JudicialSystem.PJE)).thenReturn(Optional.of(pje));
        when(registry.find(JudicialSystem.ESAJ)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.EPROC)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.CRETA)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.PROJUDI)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.PDPJ)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.MNI)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.MP)).thenReturn(Optional.empty());
        when(registry.find(JudicialSystem.OUTRO)).thenReturn(Optional.empty());

        JudicialConnectorRuntimePostureSystemReport pjeRuntime = new JudicialConnectorRuntimePostureSystemReport(
                Instant.now(), JudicialSystem.PJE, null, "HEALTHY", false, false, false, false, false, Instant.now(), 10L, List.of(), List.of(), java.util.Map.of()
        );
        when(runtimeService.nationalReport()).thenReturn(new JudicialConnectorRuntimePostureReport(
                Instant.now(),
                null,
                1,
                1,
                0,
                0,
                0,
                List.of(pjeRuntime),
                List.of(),
                java.util.Map.of()
        ));
        when(securityPackService.effectivePack(JudicialSystem.PJE, null)).thenReturn(new JudicialConnectorSecurityPackReport(
                Instant.now(), "PACK_PJE", JudicialSystem.PJE, null, "prod", true, JudicialConnectorTlsMode.MTLS,
                "pkcs11:pje", "trust:juspki", "alias", true, true,
                Duration.ofSeconds(2), Duration.ofSeconds(3), List.of("TLSv1.3"), List.of("TLS_AES_256_GCM_SHA384"), List.of("pje.exemplo.jus.br"),
                JudicialCertificateRevocationMode.HARD_FAIL, true, true, false,
                Duration.ofDays(30), Duration.ofSeconds(30), true, true, true, java.util.Map.of()
        ));
        when(securityPackService.effectivePack(JudicialSystem.ESAJ, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.ESAJ, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.EPROC, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.EPROC, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.CRETA, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.CRETA, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.PROJUDI, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.PROJUDI, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.PDPJ, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.PDPJ, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.MNI, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.MNI, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.MP, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.MP, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.OUTRO, null)).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "DEFAULT", JudicialSystem.OUTRO, null, "prod", false, JudicialConnectorTlsMode.TLS, null, null, null, false, false, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of(), List.of(), List.of(), JudicialCertificateRevocationMode.DISABLED, false, false, false, Duration.ofDays(1), Duration.ofSeconds(30), false, false, false, java.util.Map.of()));

        CidadaoGovBrInteroperabilidadeFederadaService service = new CidadaoGovBrInteroperabilidadeFederadaService(currentUserService, props, identidadeService, registry, runtimeService, securityPackService);
        var response = service.panorama();

        assertTrue(response.identidade().govBrLinked());
        assertTrue(response.identidade().acessoRestritoReady());
        assertFalse(response.conectores().isEmpty());
        assertEquals("PJE", response.conectores().get(0).sistemaOrigem());
        assertTrue(response.conectores().get(0).proxySoberanoElegivel());
    }

    @Test
    void deveExigirStepUpQuandoFonteOuSigiloPediremAcessoMaisForte() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        IdentidadeJuridicaNacionalService identidadeService = mock(IdentidadeJuridicaNacionalService.class);
        JudicialConnectorRegistry registry = mock(JudicialConnectorRegistry.class);
        JudicialConnectorRuntimePostureService runtimeService = mock(JudicialConnectorRuntimePostureService.class);
        JudicialConnectorSecurityPackService securityPackService = mock(JudicialConnectorSecurityPackService.class);
        GovBrOidcProperties props = new GovBrOidcProperties(true, false,
                "https://sso.gov.br/auth", "https://sso.gov.br/token", "https://sso.gov.br/userinfo", null,
                "cid", "secret", "https://pjb.jus.br/cb", null, null,
                "https://sso.gov.br/jwks", "https://sso.gov.br", null, null,
                Duration.ofSeconds(3), Duration.ofSeconds(4), Duration.ofMinutes(5));

        Usuario usuario = new Usuario();
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidade.getGovBrNivel()).thenReturn(IdentidadeJuridicaNacional.GovBrNivel.BRONZE);
        when(identidadeService.buscarPorDocumento("12345678901")).thenReturn(Optional.of(identidade));

        JudicialProcessConnector pje = mock(JudicialProcessConnector.class);
        when(pje.capability()).thenReturn(new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, true, false, true, List.of("application/pdf"), List.of(), List.of(), "https://pje.exemplo.jus.br"));
        when(registry.find(JudicialSystem.PJE)).thenReturn(Optional.of(pje));
        when(runtimeService.nationalReport()).thenReturn(new JudicialConnectorRuntimePostureReport(Instant.now(), null, 1, 1, 0, 0, 0, List.of(new JudicialConnectorRuntimePostureSystemReport(Instant.now(), JudicialSystem.PJE, null, "HEALTHY", false, false, false, false, false, Instant.now(), 10L, List.of(), List.of(), java.util.Map.of())), List.of(), java.util.Map.of()));
        when(securityPackService.effectivePack(JudicialSystem.PJE, "TJCE")).thenReturn(new JudicialConnectorSecurityPackReport(Instant.now(), "PACK_PJE", JudicialSystem.PJE, "TJCE", "prod", true, JudicialConnectorTlsMode.MTLS, "pkcs11:pje", "trust:juspki", "alias", true, true, Duration.ofSeconds(2), Duration.ofSeconds(3), List.of("TLSv1.3"), List.of("TLS_AES_256_GCM_SHA384"), List.of("pje.exemplo.jus.br"), JudicialCertificateRevocationMode.HARD_FAIL, true, true, false, Duration.ofDays(30), Duration.ofSeconds(30), true, true, true, java.util.Map.of()));

        CidadaoGovBrInteroperabilidadeFederadaService service = new CidadaoGovBrInteroperabilidadeFederadaService(currentUserService, props, identidadeService, registry, runtimeService, securityPackService);
        var response = service.avaliarAcesso(new CidadaoGovBrAcessoFederadoRequest("PJE", "TJCE", "0001", "SIGILO_N2", true, true, false, true, true));

        assertTrue(response.exigeStepUp());
        assertEquals("EXIGIR_STEP_UP", response.decisaoAcesso());
        assertFalse(response.documentosAllowed());
    }
}
