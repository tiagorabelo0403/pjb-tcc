package com.tcc.pjb.backend.service.processual.legitimidade;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.validation.oab.OabStrictValidator;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationProperties;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OabValidationServiceTest {

    private final OabStrictValidator strictValidator = new OabStrictValidator();
    private final OabValidationClient client = mock(OabValidationClient.class);

    @Test
    void advogadoAptoPodeProtocolar() {
        OabValidationService service = service(prodProperties(), "prod");
        Usuario advogado = advogado("OAB/CE 12345");
        when(client.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));

        assertThatCode(() -> service.requireAdvogadoAptoParaProtocolo(advogado)).doesNotThrowAnyException();
    }

    @Test
    void advogadoInaptoNaoPodeProtocolar() {
        OabValidationService service = service(prodProperties(), "prod");
        Usuario advogado = advogado("OAB/CE 12345");
        when(client.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.inapto("OAB_SUSPENSA", "test"));

        assertThatThrownBy(() -> service.requireAdvogadoAptoParaProtocolo(advogado))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inapta");
    }

    @Test
    void indisponibilidadeBloqueiaEmProducao() {
        OabValidationService service = service(prodProperties(), "prod");
        Usuario advogado = advogado("OAB/CE 12345");
        when(client.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.indeterminado("OAB_CNA_INDISPONIVEL", "test"));

        assertThatThrownBy(() -> service.requireAdvogadoAptoParaProtocolo(advogado))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("regularidade OAB");
    }

    @Test
    void indisponibilidadePodePermitirEmTeste() {
        OabValidationService service = service(nonProductionProperties(), "test");
        Usuario advogado = advogado("OAB/CE 12345");
        when(client.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.indeterminado("OAB_CNA_INDISPONIVEL", "test"));

        assertThatCode(() -> service.requireAdvogadoAptoParaProtocolo(advogado)).doesNotThrowAnyException();
    }

    @Test
    void defensoriaMinisterioPublicoEProcuradoriaNaoExigemOab() {
        OabValidationService service = service(prodProperties(), "prod");

        assertThatCode(() -> service.requireAdvogadoAptoParaProtocolo(usuario(TipoUsuario.DEFENSOR_PUBLICO))).doesNotThrowAnyException();
        assertThatCode(() -> service.requireAdvogadoAptoParaProtocolo(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO))).doesNotThrowAnyException();
        assertThatCode(() -> service.requireAdvogadoAptoParaProtocolo(usuario(TipoUsuario.PROCURADOR))).doesNotThrowAnyException();

        verify(client, never()).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private OabValidationService service(OabValidationProperties properties, String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new OabValidationService(strictValidator, client, properties, environment);
    }

    private static OabValidationProperties prodProperties() {
        return new OabValidationProperties(true, "https://cna.oab.example", "/advogados/{uf}/{numero}", null, null, Duration.ofSeconds(1), false, false, false);
    }

    private static OabValidationProperties nonProductionProperties() {
        return new OabValidationProperties(true, "https://cna.oab.example", "/advogados/{uf}/{numero}", null, null, Duration.ofSeconds(1), false, true, false);
    }

    private static Usuario advogado(String oab) {
        Usuario usuario = usuario(TipoUsuario.ADVOGADO);
        usuario.setOab(oab);
        return usuario;
    }

    private static Usuario usuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario Teste");
        usuario.setEmail(tipo.name().toLowerCase(java.util.Locale.ROOT) + "@test.local");
        usuario.setTipoUsuario(tipo);
        usuario.setPerfil(tipo.name());
        usuario.setAtivo(true);
        return usuario;
    }
}
