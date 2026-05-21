package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RenajudRestricaoFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private RenajudRestricaoService renajudRestricaoService;
    @Autowired
    private RenajudRestricaoRepository renajudRestricaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RenajudHttpClient renajudHttpClient;
    @MockitoBean
    private com.tcc.pjb.backend.core.security.CurrentUserService currentUserService;

    @Test
    void devePersistirRestricaoConfirmada() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Juiz Renajud").email("juiz-renajud-test").cpf("00000000012").senha("x")
                .tipoUsuario(TipoUsuario.JUIZ_ESTADUAL).perfil("JUIZ_ESTADUAL").ativo(true).build());
        org.mockito.Mockito.when(currentUserService.getRequired()).thenReturn(usuario);
        org.mockito.Mockito.when(renajudHttpClient.solicitarRestricao(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse("REN-1", "ok"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("RENAJUD-1")
                .numeroUnificado("RENAJUD-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        var result = renajudRestricaoService.solicitarRestricao(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoRequest(processo.getId(), "RESTRICAO", "ABC1D23", "12345678901"), "AUTHZ-R-1");

        assertThat(result.success()).isTrue();
        assertThat(renajudRestricaoRepository.findById(result.restricaoId())).get()
                .extracting(v -> v.getStatus(), v -> v.getProtocoloDenatran())
                .containsExactly("CONFIRMED", "REN-1");
    }
}
