package com.tcc.pjb.backend.service.extrajudicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.extrajudicial.EscrituraExtrajudicialRegistro;
import com.tcc.pjb.backend.model.repository.EscrituraExtrajudicialRegistroRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EscrituraExtrajudicialServiceComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoLavrar() {
        EscrituraExtrajudicialRegistroRepository repository = mock(EscrituraExtrajudicialRegistroRepository.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        EscrituraExtrajudicialService service = new EscrituraExtrajudicialService(
                repository, processoRepository, workItemRepository, currentUserService, resolutionService);

        Usuario cartorio = Usuario.builder()
                .id(1L)
                .tipoUsuario(TipoUsuario.TABELIAO)
                .uf("CE")
                .comarca("Fortaleza")
                .build();
        Comarca comarcaEsperada = mock(Comarca.class);

        when(currentUserService.getRequired()).thenReturn(cartorio);
        when(resolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));
        when(repository.save(org.mockito.ArgumentMatchers.any(EscrituraExtrajudicialRegistro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.lavrar(new EscrituraExtrajudicialService.LavraturaRequest(
                "PROCURACAO", "resumo do ato", "resumo das partes", null, null));

        ArgumentCaptor<EscrituraExtrajudicialRegistro> captor = ArgumentCaptor.forClass(EscrituraExtrajudicialRegistro.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        EscrituraExtrajudicialRegistroRepository repository = mock(EscrituraExtrajudicialRegistroRepository.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        EscrituraExtrajudicialService service = new EscrituraExtrajudicialService(
                repository, processoRepository, workItemRepository, currentUserService, resolutionService);

        Usuario cartorio = Usuario.builder()
                .id(2L)
                .tipoUsuario(TipoUsuario.TABELIAO)
                .uf(null)
                .comarca(null)
                .build();

        when(currentUserService.getRequired()).thenReturn(cartorio);
        when(repository.save(org.mockito.ArgumentMatchers.any(EscrituraExtrajudicialRegistro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.lavrar(new EscrituraExtrajudicialService.LavraturaRequest(
                "PROCURACAO", "resumo do ato", "resumo das partes", null, null));

        ArgumentCaptor<EscrituraExtrajudicialRegistro> captor = ArgumentCaptor.forClass(EscrituraExtrajudicialRegistro.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        EscrituraExtrajudicialRegistroRepository repository = mock(EscrituraExtrajudicialRegistroRepository.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        EscrituraExtrajudicialService service = new EscrituraExtrajudicialService(
                repository, processoRepository, workItemRepository, currentUserService, resolutionService);

        Usuario cartorio = Usuario.builder()
                .id(3L)
                .tipoUsuario(TipoUsuario.TABELIAO)
                .uf("CE")
                .comarca("Comarca Inexistente")
                .build();

        when(currentUserService.getRequired()).thenReturn(cartorio);
        when(resolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(EscrituraExtrajudicialRegistro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.lavrar(new EscrituraExtrajudicialService.LavraturaRequest(
                "PROCURACAO", "resumo do ato", "resumo das partes", null, null));

        ArgumentCaptor<EscrituraExtrajudicialRegistro> captor = ArgumentCaptor.forClass(EscrituraExtrajudicialRegistro.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }
}
