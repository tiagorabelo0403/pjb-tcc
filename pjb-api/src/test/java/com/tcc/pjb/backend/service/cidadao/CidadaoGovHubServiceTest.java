package com.tcc.pjb.backend.service.cidadao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.repository.EstadosRepository;
import com.tcc.pjb.backend.repository.gov.GovServiceRegistryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CidadaoGovHubServiceTest {

    private final GovServiceRegistryRepository repo = mock(GovServiceRegistryRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final EstadosRepository estadosRepository = mock(EstadosRepository.class);
    private final CidadaoGovHubService service = new CidadaoGovHubService(
            repo,
            new ObjectMapper(),
            currentUser,
            estadosRepository
    );

    @Test
    void deveGerarCacheTokenPorUfNoServiceSemAcessarUsuarioAtual() {
        Instant updatedAt = Instant.parse("2026-05-19T12:34:56Z");
        when(estadosRepository.existsByUfIgnoreCaseAndAtivoTrue("CE")).thenReturn(true);
        when(repo.findMaxUpdatedAtEnabledByUfs(List.of("CE", "BR"))).thenReturn(updatedAt);

        CidadaoGovHubService.CidadaoGovHubCacheToken token = service.cacheTokenForUf(" ce ");

        String expected = "W/\"govhub-CE-" + updatedAt.getEpochSecond() + "\"";
        assertEquals(expected, token.etag());
        assertTrue(token.matches(" " + expected + " "));
        assertFalse(token.matches("W/\"govhub-CE-0\""));
        verify(repo).findMaxUpdatedAtEnabledByUfs(List.of("CE", "BR"));
        verifyNoInteractions(currentUser);
    }
}
