package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AudienciaCustodiaFlowIT extends PjbFlowItBase {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private AudienciaCustodiaService audienciaCustodiaService;
    @Autowired
    private AudienciaCustodiaRepository audRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private BnmpConsultaService bnmpConsultaService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setup() {
        Usuario operador = new Usuario();
        operador.setNome("Operador Custodia");
        operador.setEmail("operador.custodia@test.local");
        operador.setSenha("x");
        operador.setCpf("11122233344");
        operador.setTipoUsuario(TipoUsuario.ADVOGADO);
        operador.setPerfil(TipoUsuario.ADVOGADO.name());
        operador.setAtivo(true);
        Long operadorId = usuarioRepository.save(operador).getId();

        when(currentUserService.currentUserIdOrZero()).thenReturn(operadorId);
        when(bnmpConsultaService.consultarMandadoAtivo(any()))
                .thenReturn(new BnmpConsultaResult(false, null));
    }

    @Test
    void deveRegistrarPrisaoEConcluirAudiencia() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CUST-1")
                .numeroUnificado("CUST-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
        var prisao = audienciaCustodiaService.registrarPrisao(new RegistrarPrisaoCommand(processo.getId(), "Custodiado", "12345678901", Instant.now()));
        var resultado = audienciaCustodiaService.concluirAudiencia(new ConcluirAudienciaCommand(prisao.custodiaId(), "LIBERDADE_PROVISORIA", List.of("COMPARECIMENTO_PERIODICO")));
        assertThat(resultado.statusProcesso()).isEqualTo(StatusProcesso.LIBERDADE_PROVISORIA.name());
        assertThat(audRepository.findById(prisao.custodiaId())).get().extracting(v -> v.getStatus()).isEqualTo("REALIZADA");
    }
}
