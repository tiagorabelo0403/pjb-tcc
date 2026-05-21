package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;

class SisbajudBloqueioFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private SisbajudBloqueioService sisbajudBloqueioService;
    @Autowired
    private SisbajudOperacaoRepository operacaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private SisbajudHttpClient sisbajudHttpClient;
    @MockitoBean
    private com.tcc.pjb.backend.core.security.CurrentUserService currentUserService;

    @Test
    void devePersistirOperacaoConfirmada() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Juiz Sisbajud").email("juiz-sisbajud-test").cpf("00000000005").senha("x")
                .tipoUsuario(TipoUsuario.JUIZ_ESTADUAL).perfil("JUIZ_ESTADUAL").ativo(true).build());
        org.mockito.Mockito.when(currentUserService.getRequired()).thenReturn(usuario);
        org.mockito.Mockito.when(sisbajudHttpClient.solicitarBloqueio(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse("PROTO-1", "ok"));
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("SISB-1")
                .numeroUnificado("SISB-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var result = sisbajudBloqueioService.solicitarBloqueio(new SisbajudBloqueioRequest(processo.getId(), "12345678901", BigDecimal.TEN, "OF-1", false), "AUTHZ-1");
        assertThat(result.success()).isTrue();
        assertThat(operacaoRepository.findById(result.operacaoId())).get().extracting(v -> v.getStatus()).isEqualTo("CONFIRMED");
    }
}
