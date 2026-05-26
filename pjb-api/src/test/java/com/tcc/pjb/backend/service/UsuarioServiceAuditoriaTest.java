package com.tcc.pjb.backend.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.core.validation.oab.OabStrictValidator;
import com.tcc.pjb.backend.mapper.UsuarioMapper;
import com.tcc.pjb.backend.model.dto.UsuarioRequest;
import com.tcc.pjb.backend.model.dto.UsuarioResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAuditoriaTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private OabStrictValidator oabStrictValidator;
    @Mock private DocumentoNacionalValidator documentoNacionalValidator;
    @Mock private IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService;
    @Mock private AuditLedgerService auditLedgerService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        // Falha em compilar até AuditLedgerService ser adicionado ao construtor de UsuarioService
        service = new UsuarioService(usuarioRepository, usuarioMapper, oabStrictValidator,
                documentoNacionalValidator, identidadeJuridicaNacionalService, auditLedgerService);
    }

    @Test
    void desativarUsuario_registraEventoAuditoria() {
        Long id = 42L;
        Usuario usuario = Usuario.builder().ativo(true).build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        service.desativarUsuario(id);

        // Falha até o audit ser adicionado a desativarUsuario()
        verify(auditLedgerService).appendSafely(eq("USUARIO_DESATIVADO"), eq("USUARIO"), eq("42"), isNull(), any(String.class));
    }

    @Test
    void criarUsuario_registraEventoAuditoria() {
        UsuarioRequest dto = mock(UsuarioRequest.class);
        when(dto.getCpf()).thenReturn("12345678901");
        when(dto.getEmail()).thenReturn("teste@pjb.local");

        when(documentoNacionalValidator.normalizarDocumento("12345678901")).thenReturn("12345678901");
        when(documentoNacionalValidator.cpfValido("12345678901")).thenReturn(true);
        when(usuarioRepository.findByEmail("teste@pjb.local")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.empty());

        Usuario entidade = mock(Usuario.class);
        when(usuarioMapper.requestParaEntidade(dto)).thenReturn(entidade);
        when(usuarioRepository.save(any())).thenReturn(entidade);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidadeJuridicaNacionalService.sincronizarUsuario(any())).thenReturn(identidade);

        UsuarioResponse response = mock(UsuarioResponse.class);
        when(usuarioMapper.entidadeParaResponse(any())).thenReturn(response);
        when(identidadeJuridicaNacionalService.resumirPorId(any())).thenReturn(Optional.empty());

        service.criarUsuario(dto);

        // Falha até o audit ser adicionado a criarUsuario()
        verify(auditLedgerService).appendSafely(eq("USUARIO_CRIADO"), eq("USUARIO"), any(String.class), isNull(), any(String.class));
    }

    @Test
    void atualizarUsuario_registraEventoAuditoria() {
        Long id = 7L;
        UsuarioRequest dto = mock(UsuarioRequest.class);
        when(dto.getCpf()).thenReturn(null);
        when(dto.getEmail()).thenReturn(null);

        Usuario entidade = mock(Usuario.class);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(entidade));
        when(usuarioRepository.save(any())).thenReturn(entidade);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidadeJuridicaNacionalService.sincronizarUsuario(any())).thenReturn(identidade);

        UsuarioResponse response = mock(UsuarioResponse.class);
        when(usuarioMapper.entidadeParaResponse(any())).thenReturn(response);
        when(identidadeJuridicaNacionalService.resumirPorId(any())).thenReturn(Optional.empty());

        service.atualizarUsuario(id, dto);

        // Falha até o audit ser adicionado a atualizarUsuario()
        verify(auditLedgerService).appendSafely(eq("USUARIO_PERFIL_ALTERADO"), eq("USUARIO"), eq("7"), isNull(), any(String.class));
    }
}
