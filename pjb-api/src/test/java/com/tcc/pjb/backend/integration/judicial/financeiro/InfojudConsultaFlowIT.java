package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InfojudConsultaFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private InfojudConsultaService infojudConsultaService;
    @Autowired
    private InfojudConsultaRepository infojudConsultaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private InfojudHttpClient infojudHttpClient;
    @MockitoBean
    private com.tcc.pjb.backend.core.security.CurrentUserService currentUserService;
    @MockitoBean
    private com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService authorizationService;

    @Test
    void devePersistirConsultaConfirmada() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Operador Infojud").email("operador-infojud-test").cpf("00000000011").senha("x")
                .tipoUsuario(TipoUsuario.SERVIDOR_FORUM).perfil("SERVIDOR_FORUM").ativo(true).build());
        org.mockito.Mockito.when(currentUserService.getRequired()).thenReturn(usuario);
        org.mockito.Mockito.when(infojudHttpClient.consultar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse("INFO-1", "retorno-ok"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("INFOJUD-1")
                .numeroUnificado("INFOJUD-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        var result = infojudConsultaService.consultar(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaRequest(processo.getId(), "12345678901", false), "AUTHZ-I-1");

        assertThat(result.success()).isTrue();
        assertThat(infojudConsultaRepository.findById(result.consultaId())).get()
                .extracting(v -> v.getStatus(), v -> v.getProtocoloReceita())
                .containsExactly("CONFIRMED", "INFO-1");
    }
}
