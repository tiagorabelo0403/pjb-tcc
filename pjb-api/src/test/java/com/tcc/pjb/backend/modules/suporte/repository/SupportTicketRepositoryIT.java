package com.tcc.pjb.backend.modules.suporte.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicket;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SupportTicketRepositoryIT extends PjbFlowItBase {

    @Autowired
    private SupportTicketRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void salvaEBuscaPorAbertoPorEPorFila() {
        Usuario usuario = Usuario.builder()
                .nome("Usuario Teste Suporte")
                .email("usuario.suporte@test.local")
                .senha("x")
                .cpf("11122233344")
                .tipoUsuario(TipoUsuario.CIDADAO)
                .perfil(TipoUsuario.CIDADAO.name())
                .ativo(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        SupportTicket ticket = repository.save(SupportTicket.builder()
                .abertoPorId(usuario.getId())
                .abertoPorNome(usuario.getNome())
                .abertoPorTipoUsuario(usuario.getTipoUsuario().name())
                .categoria(SupportTicketCategoria.TECNICO)
                .assunto("Nao consigo logar")
                .descricao("Erro ao autenticar")
                .status(SupportTicketStatus.ABERTO)
                .criadoEm(Instant.now())
                .build());

        List<SupportTicket> meus = repository.findByAbertoPorId(usuario.getId());
        assertThat(meus).extracting(SupportTicket::getId).contains(ticket.getId());

        List<SupportTicket> fila = repository.findFila(List.of(SupportTicketStatus.ABERTO));
        assertThat(fila).extracting(SupportTicket::getId).contains(ticket.getId());

        long abertos = repository.countByAbertoPorIdAndStatusIn(usuario.getId(), List.of(SupportTicketStatus.ABERTO));
        assertThat(abertos).isEqualTo(1L);
    }
}
