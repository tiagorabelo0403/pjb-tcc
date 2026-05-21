package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.core.validation.oab.OabStrictValidator;
import com.tcc.pjb.backend.mapper.UsuarioMapper;
import com.tcc.pjb.backend.model.dto.UsuarioRequest;
import com.tcc.pjb.backend.model.dto.UsuarioResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UsuarioMapper usuarioMapper = mock(UsuarioMapper.class);
    private final OabStrictValidator oabStrictValidator = mock(OabStrictValidator.class);
    private final DocumentoNacionalValidator documentoNacionalValidator = mock(DocumentoNacionalValidator.class);
    private final IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService = mock(IdentidadeJuridicaNacionalService.class);

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(
                usuarioRepository,
                usuarioMapper,
                oabStrictValidator,
                documentoNacionalValidator,
                identidadeJuridicaNacionalService
        );
    }

    @Test
    void deveLancarExcecaoQuandoCpfForInvalido() {
        when(documentoNacionalValidator.normalizarDocumento("000.000.000-00")).thenReturn("00000000000");
        when(documentoNacionalValidator.cpfValido("00000000000")).thenReturn(false);

        UsuarioRequest dto = new UsuarioRequest();
        dto.setCpf("000.000.000-00");
        dto.setEmail("teste@test.local");

        assertThatThrownBy(() -> service.criarUsuario(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF inválido");
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        when(documentoNacionalValidator.normalizarDocumento("12345678909")).thenReturn("12345678909");
        when(documentoNacionalValidator.cpfValido("12345678909")).thenReturn(true);
        when(usuarioRepository.findByEmail("duplicado@test.local")).thenReturn(Optional.of(new Usuario()));

        UsuarioRequest dto = new UsuarioRequest();
        dto.setCpf("12345678909");
        dto.setEmail("duplicado@test.local");

        assertThatThrownBy(() -> service.criarUsuario(dto))
                .isInstanceOf(RecursoJaExistenteException.class)
                .hasMessageContaining("Email já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoCpfJaCadastrado() {
        when(documentoNacionalValidator.normalizarDocumento("12345678909")).thenReturn("12345678909");
        when(documentoNacionalValidator.cpfValido("12345678909")).thenReturn(true);
        when(usuarioRepository.findByEmail("novo@test.local")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf("12345678909")).thenReturn(Optional.of(new Usuario()));

        UsuarioRequest dto = new UsuarioRequest();
        dto.setCpf("12345678909");
        dto.setEmail("novo@test.local");

        assertThatThrownBy(() -> service.criarUsuario(dto))
                .isInstanceOf(RecursoJaExistenteException.class)
                .hasMessageContaining("CPF já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoAdvogadoNaoInformarOab() {
        when(documentoNacionalValidator.normalizarDocumento("12345678909")).thenReturn("12345678909");
        when(documentoNacionalValidator.cpfValido("12345678909")).thenReturn(true);
        when(usuarioRepository.findByEmail("adv@test.local")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf("12345678909")).thenReturn(Optional.empty());

        Usuario advogado = new Usuario();
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(usuarioMapper.requestParaEntidade(any())).thenReturn(advogado);

        UsuarioRequest dto = new UsuarioRequest();
        dto.setCpf("12345678909");
        dto.setEmail("adv@test.local");

        assertThatThrownBy(() -> service.criarUsuario(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OAB");
    }

    @Test
    void deveCriarUsuarioBemSucedido() {
        when(documentoNacionalValidator.normalizarDocumento("12345678909")).thenReturn("12345678909");
        when(documentoNacionalValidator.cpfValido("12345678909")).thenReturn(true);
        when(usuarioRepository.findByEmail("servidor@test.local")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCpf("12345678909")).thenReturn(Optional.empty());

        Usuario entidade = new Usuario();
        entidade.setId(1L);
        entidade.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        when(usuarioMapper.requestParaEntidade(any())).thenReturn(entidade);
        when(usuarioRepository.save(any())).thenReturn(entidade);

        IdentidadeJuridicaNacional identidade = mock(IdentidadeJuridicaNacional.class);
        when(identidadeJuridicaNacionalService.sincronizarUsuario(any())).thenReturn(identidade);
        when(usuarioMapper.entidadeParaResponse(any())).thenReturn(new UsuarioResponse());
        when(identidadeJuridicaNacionalService.resumirPorId(any())).thenReturn(Optional.empty());

        UsuarioRequest dto = new UsuarioRequest();
        dto.setCpf("12345678909");
        dto.setEmail("servidor@test.local");

        service.criarUsuario(dto);

        verify(usuarioRepository).findByEmail("servidor@test.local");
        verify(identidadeJuridicaNacionalService).sincronizarUsuario(any());
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoDesativarIdInexistente() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desativarUsuario(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
