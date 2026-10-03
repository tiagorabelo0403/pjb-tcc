package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.EstruturaCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalCaixaDestinoValidatorTest {

    private final CatalogoInstitucionalUnificadoService catalogo = mock(CatalogoInstitucionalUnificadoService.class);
    private final EstruturaCaixaInstitucionalService estrutura = mock(EstruturaCaixaInstitucionalService.class);
    private final InstitutionalCaixaDestinoValidator validator = new InstitutionalCaixaDestinoValidator(catalogo, estrutura);

    private UnidadeInstitucional unidade(String codigo) {
        UnidadeInstitucional u = mock(UnidadeInstitucional.class);
        when(u.codigo()).thenReturn(codigo);
        return u;
    }

    private CaixaInstitucional caixa(String codigo) {
        CaixaInstitucional c = mock(CaixaInstitucional.class);
        when(c.codigo()).thenReturn(codigo);
        return c;
    }

    @Test
    void validarAceitaCaixaExistenteNaUnidade() {
        UnidadeInstitucional u = unidade("U1");
        CaixaInstitucional c = caixa("C1");
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(u));
        when(estrutura.expandir(u)).thenReturn(List.of(c));

        assertThatCode(() -> validator.validar("U1", "C1")).doesNotThrowAnyException();
    }

    @Test
    void validarRejeitaUnidadeInexistente() {
        UnidadeInstitucional outra = unidade("OUTRA");
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(outra));

        assertThatThrownBy(() -> validator.validar("U1", "C1"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void validarRejeitaCaixaInexistenteNaUnidade() {
        UnidadeInstitucional u = unidade("U1");
        CaixaInstitucional c2 = caixa("C2");
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(u));
        when(estrutura.expandir(any())).thenReturn(List.of(c2));

        assertThatThrownBy(() -> validator.validar("U1", "C1"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
