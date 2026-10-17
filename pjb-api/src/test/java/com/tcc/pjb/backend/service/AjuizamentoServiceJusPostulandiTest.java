package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AjuizamentoServiceJusPostulandiTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProcessoRepository processoRepo;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.service.orgao.ProcessoOrgaoOficialService orgaoOficialService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.service.outbox.OutboxPublisher outbox;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine triagemNacionalIAEngine;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TetoProcessualService tetoProcessualService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.core.compiler.LegalCompilerService legalCompilerService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.service.procedural.AjuizamentoProceduralContextService ajuizamentoProceduralContextService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy poloCompositionPolicy;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService poloProcessualApplicationService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator documentoNacionalValidator;

    @InjectMocks
    private AjuizamentoService service;

    private Processo processoDoCidadaoNoJuizado() {
        Usuario cidadao = new Usuario();
        cidadao.setId(9L);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        Processo processo = new Processo();
        processo.setUsuario(cidadao);
        processo.setRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setValorCausa(new BigDecimal("3000.00"));
        return processo;
    }

    @Test
    void consolidacaoQueLevaOProcessoDoCidadaoParaORitoTrabalhistaBloqueiaAntesDeGravar() {
        Processo processo = processoDoCidadaoNoJuizado();
        doAnswer(invocacao -> {
            processo.setRito(RitoProcessual.TRABALHISTA_ORDINARIO);
            processo.setTipoJustica(TipoJustica.TRABALHO);
            return null;
        }).when(ajuizamentoProceduralContextService).consolidateAndValidate(any(), any());

        assertThatThrownBy(() -> service.ajuizar(processo)).isInstanceOf(AccessDeniedPjbException.class);
        verify(processoRepo, never()).save(any());
        verify(tetoProcessualService, never()).validarAjuizamentoOuLancar(any());
    }

    @Test
    void processoDoCidadaoQuePermaneceNoJuizadoEstadualSegueParaOTeto() {
        Processo processo = processoDoCidadaoNoJuizado();
        doThrow(new IllegalStateException("teto")).when(tetoProcessualService).validarAjuizamentoOuLancar(processo);

        assertThatThrownBy(() -> service.ajuizar(processo)).isInstanceOf(IllegalStateException.class).hasMessage("teto");
    }
}
