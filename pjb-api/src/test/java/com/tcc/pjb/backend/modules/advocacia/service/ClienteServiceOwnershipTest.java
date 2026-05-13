package com.tcc.pjb.backend.modules.advocacia.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.mapper.ClienteMapper;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClienteServiceOwnershipTest {

    @Test
    void advogado_cannot_archive_other_advogado_cliente_without_team_scope() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        ClienteMapper clienteMapper = mock(ClienteMapper.class);
        ClienteSpecification clienteSpecification = mock(ClienteSpecification.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        ClienteService service = new ClienteService(
                clienteRepository,
                usuarioRepository,
                membroEquipeRepository,
                clienteMapper,
                clienteSpecification,
                currentUserService,
                auditLedgerService
        );

        Usuario current = Usuario.builder().id(1L).tipoUsuario(TipoUsuario.ADVOGADO).nome("A").email("a@a.com").cpf("11111111111").senha("x").perfil("ADVOGADO").build();
        when(currentUserService.getOrNull()).thenReturn(current);

        Usuario owner = Usuario.builder().id(2L).tipoUsuario(TipoUsuario.ADVOGADO).nome("B").email("b@b.com").cpf("22222222222").senha("x").perfil("ADVOGADO").build();
        Cliente cliente = new Cliente();
        cliente.setId(99L);
        cliente.setAdvogado(owner);
        cliente.setStatus(StatusCliente.ATIVO);

        when(clienteRepository.findById(99L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.excluirCliente(99L))
                .isInstanceOf(AccessDeniedPjbException.class);
    }
}
