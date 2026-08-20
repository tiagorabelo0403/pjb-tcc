package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.Links;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection;
import com.tcc.pjb.backend.service.cidadao.CidadaoMalhaProcessualNacionalService;
import com.tcc.pjb.backend.service.cidadao.govbr.CidadaoGovBrAcervoUnificadoService;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CidadaoGovBrAcervoUnificadoServiceTest {

    @Test
    void deveConsolidarAcervoPorCpfESepararPorRitoEOrigem() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        IdentidadeJuridicaNacionalService identidadeService = mock(IdentidadeJuridicaNacionalService.class);
        CidadaoMalhaProcessualNacionalService malhaService = mock(CidadaoMalhaProcessualNacionalService.class);
        GovBrOidcProperties props = new GovBrOidcProperties(true, false,
                "https://sso.gov.br/auth", "https://sso.gov.br/token", "https://sso.gov.br/userinfo", null,
                "cid", "secret", "https://pjb.jus.br/cb", null, null, null,
                "https://sso.gov.br/jwks", "https://sso.gov.br", null, null, null, null,
                Duration.ofSeconds(3), Duration.ofSeconds(4), Duration.ofMinutes(5));

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setCpf("12345678901");
        usuario.setNome("Cidadao Teste");
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidade.getGovBrNivel()).thenReturn(IdentidadeJuridicaNacional.GovBrNivel.OURO);
        when(identidade.getUltimaSincronizacaoEm()).thenReturn(Instant.parse("2026-04-19T20:00:00Z"));
        when(identidadeService.buscarPorDocumento("12345678901")).thenReturn(Optional.of(identidade));

        CidadaoProcessoNacionalProjection rowPje = mock(CidadaoProcessoNacionalProjection.class);
        when(rowPje.getPapelProcessual()).thenReturn(PapelProcessualNacional.REU);
        when(rowPje.getSistemaOrigem()).thenReturn("PJE");
        when(rowPje.getTribunalCodigo()).thenReturn("TJCE");
        when(rowPje.getUf()).thenReturn("CE");
        when(rowPje.getComarca()).thenReturn("Fortaleza");
        when(rowPje.getUnidadeJudicial()).thenReturn("2a Vara Cível");
        when(rowPje.isExigeStepUp()).thenReturn(false);
        when(rowPje.getRamoDireito()).thenReturn(com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL);
        when(rowPje.getOrigemExternaUri()).thenReturn("https://pje.tjce.jus.br/processo/1");
        when(rowPje.getDataUltimaMovimentacao()).thenReturn(LocalDateTime.now().minusDays(1));

        CidadaoProcessoCardDto cardPje = processoCard(
                "0001111-22.2026.8.06.0001",
                "Procedimento Comum Cível",
                "Indenização",
                "CIVEL_COMUM",
                "Cível comum",
                "CIVIL",
                0.98,
                "EM_ANDAMENTO",
                "INSTRUCAO",
                "PUBLICO",
                LocalDateTime.now().minusDays(1),
                List.of("MALHA_NACIONAL", "SISTEMA_PJE"),
                "Intimação para manifestação.",
                null,
                null,
                null,
                null,
                null,
                "/timeline/1"
        );

        CidadaoProcessoNacionalProjection rowEproc = mock(CidadaoProcessoNacionalProjection.class);
        when(rowEproc.getPapelProcessual()).thenReturn(PapelProcessualNacional.AUTOR);
        when(rowEproc.getSistemaOrigem()).thenReturn("EPROC");
        when(rowEproc.getTribunalCodigo()).thenReturn("TRF5");
        when(rowEproc.getUf()).thenReturn("PE");
        when(rowEproc.getComarca()).thenReturn("Recife");
        when(rowEproc.getUnidadeJudicial()).thenReturn("Turma Recursal Federal");
        when(rowEproc.isExigeStepUp()).thenReturn(true);
        when(rowEproc.getRamoDireito()).thenReturn(com.tcc.pjb.backend.model.entity.enums.RamoDireito.PREVIDENCIARIO);
        when(rowEproc.getOrigemExternaUri()).thenReturn("https://eproc.trf5.jus.br/processo/2");
        when(rowEproc.getDataUltimaMovimentacao()).thenReturn(LocalDateTime.now().minusDays(2));

        CidadaoProcessoCardDto cardEproc = processoCard(
                "0002222-33.2026.4.05.0001",
                "Juizado Especial Federal",
                "Benefício previdenciário",
                "JUIZADO_FEDERAL_PREVIDENCIARIO",
                "Juizado federal previdenciário",
                "PREVIDENCIARIO",
                0.93,
                "EM_ANDAMENTO",
                "RECURSO",
                "SIGILO_N2",
                LocalDateTime.now().minusDays(2),
                List.of("MALHA_NACIONAL", "SISTEMA_EPROC"),
                "Audiência designada.",
                LocalDateTime.now().plusDays(3),
                "INSTRUCAO",
                "VIDEO",
                "Sala virtual",
                null,
                "/timeline/2"
        );

        var viewPje = new CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView(cardPje, rowPje, null);
        var viewEproc = new CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView(cardEproc, rowEproc, null);
        when(malhaService.listVisibleCurrentUser(400)).thenReturn(List.of(viewPje, viewEproc));

        CidadaoGovBrAcervoUnificadoService service = new CidadaoGovBrAcervoUnificadoService(currentUserService, props, identidadeService, malhaService);
        var response = service.carregar(null, null, null, null, null, null);

        assertTrue(response.govBrLinked());
        assertEquals("OURO", response.govBrNivel());
        assertEquals(2, response.summary().totalProcessos());
        assertEquals(2, response.fontes().size());
        assertEquals(2, response.papeis().size());
        assertFalse(response.ritos().isEmpty());
        assertTrue(response.summary().comPendencia() >= 1);
        assertTrue(response.summary().comAudiencia() >= 1);
    }

    private CidadaoProcessoCardDto processoCard(
            String numeroUnificado,
            String classeProcessual,
            String assunto,
            String ritoCode,
            String ritoTitle,
            String ramoSugerido,
            double ritoConfidence,
            String status,
            String faseAtual,
            String nivelSigilo,
            LocalDateTime dataUltimaMovimentacao,
            List<String> uiTokens,
            String ultimaMovimentacaoResumo,
            LocalDateTime proximaAudienciaDataHora,
            String proximaAudienciaTipo,
            String proximaAudienciaModalidade,
            String proximaAudienciaLocal,
            String proximoJulgamentoResumo,
            String timelinePath
    ) {
        return new CidadaoProcessoCardDto(
                null,
                numeroUnificado,
                classeProcessual,
                assunto,
                ritoCode,
                ritoTitle,
                ramoSugerido,
                ritoConfidence,
                false,
                List.of(),
                status,
                faseAtual,
                nivelSigilo,
                dataUltimaMovimentacao,
                uiTokens,
                ultimaMovimentacaoResumo,
                dataUltimaMovimentacao,
                null,
                proximaAudienciaDataHora,
                proximaAudienciaTipo,
                proximaAudienciaModalidade,
                proximaAudienciaLocal,
                null,
                proximoJulgamentoResumo,
                0L,
                new Links(timelinePath, null, null, null, null, null, null)
        );
    }

}
