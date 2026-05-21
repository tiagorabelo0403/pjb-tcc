package com.tcc.pjb.backend.service.pericia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.dto.pericia.PeritoDisponibilidadeRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.PeritoDisponibilidadeRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.PeritoSorteioAuditRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeritoDisponibilidadeServiceTest {

    private final PeritoDisponibilidadeRepository disponibilidadeRepository = mock(PeritoDisponibilidadeRepository.class);
    private final PeritoNomeacaoRepository nomeacaoRepository = mock(PeritoNomeacaoRepository.class);
    private final PeritoSorteioAuditRepository sorteioAuditRepository = mock(PeritoSorteioAuditRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private PeritoDisponibilidadeService service;

    @BeforeEach
    void setUp() {
        service = new PeritoDisponibilidadeService(
                disponibilidadeRepository, nomeacaoRepository,
                sorteioAuditRepository, processoRepository,
                currentUserService, new ObjectMapper());
    }

    @Test
    void deveRejeitarRegistroQuandoUsuarioNaoEPerito() {
        Usuario usuario = usuarioComTipo(TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        PeritoDisponibilidadeRequest request = new PeritoDisponibilidadeRequest(
                "ENGENHARIA", null,
                LocalDate.now(), LocalDate.now().plusDays(7),
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, null);

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void deveRejeitarRegistroComDataFimAnteriorADataInicio() {
        Usuario usuario = usuarioComTipo(TipoUsuario.PERITO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        PeritoDisponibilidadeRequest request = new PeritoDisponibilidadeRequest(
                "ENGENHARIA", null,
                LocalDate.now().plusDays(5), LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, null);

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataFim");
    }

    @Test
    void deveRejeitarListarMinhasQuandoUsuarioNaoEPerito() {
        Usuario usuario = usuarioComTipo(TipoUsuario.MAGISTRADO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        assertThatThrownBy(() -> service.listarMinhas())
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void deveRejeitarSortearQuandoTipoNaoAutorizado() {
        Usuario usuario = usuarioComTipo(TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        PeritoSorteioRequest request = new PeritoSorteioRequest(
                "ENGENHARIA", "FORTALEZA", LocalDate.now(), 1L);

        assertThatThrownBy(() -> service.sortear(request))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void deveRejeitarSortearQuandoNenhumCandidatoDisponivel() {
        Usuario usuario = usuarioComTipo(TipoUsuario.JUIZ);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(disponibilidadeRepository.findDisponiveis(any(), any(), any())).thenReturn(List.of());

        PeritoSorteioRequest request = new PeritoSorteioRequest(
                "ENGENHARIA", "FORTALEZA", LocalDate.now(), null);

        assertThatThrownBy(() -> service.sortear(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum perito disponível");
    }

    private Usuario usuarioComTipo(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
