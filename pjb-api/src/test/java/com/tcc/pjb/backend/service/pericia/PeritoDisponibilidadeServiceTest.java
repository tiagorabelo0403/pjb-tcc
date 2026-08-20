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
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoDisponibilidade;
import com.tcc.pjb.backend.model.repository.PeritoDisponibilidadeRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.PeritoSorteioAuditRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

class PeritoDisponibilidadeServiceTest {

    private final PeritoDisponibilidadeRepository disponibilidadeRepository = mock(PeritoDisponibilidadeRepository.class);
    private final PeritoNomeacaoRepository nomeacaoRepository = mock(PeritoNomeacaoRepository.class);
    private final PeritoSorteioAuditRepository sorteioAuditRepository = mock(PeritoSorteioAuditRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

    private PeritoDisponibilidadeService service;

    @BeforeEach
    void setUp() {
        service = new PeritoDisponibilidadeService(
                disponibilidadeRepository, nomeacaoRepository,
                sorteioAuditRepository, processoRepository,
                currentUserService, new ObjectMapper(), comarcaResolutionService);
    }

    @Test
    void deveResolverComarcaEntidadeAoRegistrarDisponibilidade() {
        Usuario usuario = usuarioComTipo(TipoUsuario.PERITO);
        usuario.setComarca(null);
        when(currentUserService.getRequired()).thenReturn(usuario);
        Comarca fortaleza = mock(Comarca.class);
        when(comarcaResolutionService.resolver("FORTALEZA", null)).thenReturn(Optional.of(fortaleza));
        when(disponibilidadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PeritoDisponibilidadeRequest request = new PeritoDisponibilidadeRequest(
                "ENGENHARIA", "Fortaleza",
                LocalDate.now(), LocalDate.now().plusDays(7),
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, null);

        service.registrar(request);

        verify(comarcaResolutionService).resolver(eq("FORTALEZA"), isNull());
    }

    @Test
    void naoResolveComarcaEntidadeQuandoComarcaNaoInformada() {
        Usuario usuario = usuarioComTipo(TipoUsuario.PERITO);
        usuario.setComarca(null);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(disponibilidadeRepository.save(any())).thenAnswer(inv -> {
            PeritoDisponibilidade saved = inv.getArgument(0);
            assertThat(saved.getComarcaEntidade()).isNull();
            return saved;
        });

        PeritoDisponibilidadeRequest request = new PeritoDisponibilidadeRequest(
                "ENGENHARIA", null,
                LocalDate.now(), LocalDate.now().plusDays(7),
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                true, null);

        service.registrar(request);
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
