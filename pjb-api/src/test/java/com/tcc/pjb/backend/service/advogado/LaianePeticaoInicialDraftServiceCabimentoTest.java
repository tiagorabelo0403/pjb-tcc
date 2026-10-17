package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.processual.representacao.PeticaoInicialSemAdvogadoCabimentoService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class LaianePeticaoInicialDraftServiceCabimentoTest {

    private final LaianePeticaoInicialDraftSessionRepository repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final LaianePeticaoInicialProtocolarService protocolarService = mock(LaianePeticaoInicialProtocolarService.class);
    private final PeticaoInicialSemAdvogadoCabimentoService cabimento = mock(PeticaoInicialSemAdvogadoCabimentoService.class);

    @SuppressWarnings("unchecked")
    private LaianePeticaoInicialDraftService servico() {
        ObjectProvider<OfficeProcessWorkspaceScopeService> escopo = mock(ObjectProvider.class);
        return new LaianePeticaoInicialDraftService(repository, mock(ProcessoRepository.class), currentUserService,
                new ObjectMapper(), mock(RepresentacaoProcessualPolicyService.class), escopo, protocolarService, cabimento);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setTipoUsuario(tipo);
        when(currentUserService.getRequired()).thenReturn(usuario);
        return usuario;
    }

    private void rascunho(BigDecimal valorCausa) {
        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setRitoSugerido(RitoProcessual.JUIZADO_ESPECIAL_CIVEL.name());
        draft.setValorCausa(valorCausa);
        when(repository.findByIdAndSolicitante_Id(55L, 7L)).thenReturn(Optional.of(draft));
    }

    @Test
    void rascunhoDoCidadaoAcimaDoLimiteNaoChegaAoProtocolo() {
        usuario(TipoUsuario.CIDADAO);
        rascunho(new BigDecimal("1000000.00"));
        doThrow(new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "acima de 20 salários mínimos"))
                .when(cabimento).exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("1000000.00"), null);

        assertThatThrownBy(() -> servico().protocolar(55L, null)).isInstanceOf(ErroDeValidacaoException.class);
        verifyNoInteractions(protocolarService);
    }

    @Test
    void rascunhoDoCidadaoDentroDoLimiteSegueParaOProtocolo() {
        usuario(TipoUsuario.CIDADAO);
        rascunho(new BigDecimal("3000.00"));

        servico().protocolar(55L, null);

        verify(cabimento).exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("3000.00"), null);
        verify(protocolarService).protocolar(55L, null);
    }

    @Test
    void advogadoNaoPassaPeloLimiteDoJusPostulandi() {
        usuario(TipoUsuario.ADVOGADO);

        servico().protocolar(55L, null);

        verifyNoInteractions(cabimento);
        verify(protocolarService).protocolar(any(), any());
    }
}
