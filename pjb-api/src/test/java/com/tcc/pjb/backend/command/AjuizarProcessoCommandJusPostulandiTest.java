package com.tcc.pjb.backend.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.policy.SigiloPolicyFactory;
import com.tcc.pjb.backend.service.procedural.AjuizamentoCanonicalContextService;
import com.tcc.pjb.backend.service.processo.ProcessoMaterialObjetoEnrichmentService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AjuizarProcessoCommandJusPostulandiTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ProcessoRepository processoRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private LegalCompilerService legalCompiler;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private SigiloPolicyFactory sigiloPolicyFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ApplicationEventPublisher eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ProcessoMaterialObjetoEnrichmentService materialObjetoEnrichmentService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private CaseContinuityOrchestratorService caseContinuityOrchestratorService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private NationalProceduralRoutingService nationalProceduralRoutingService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private TetoProcessualService tetoProcessualService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private TerritorialProcessualService territorialProcessualService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private CompletudeDocumentalPolicyService completudeDocumentalPolicyService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PoloProcessualApplicationService poloProcessualApplicationService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PoloCompositionPolicy poloCompositionPolicy;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private DocumentoNacionalValidator documentoNacionalValidator;

    @InjectMocks
    private AjuizarProcessoCommand command;

    @Test
    void roteamentoQueLevaOProcessoDoCidadaoParaORitoTrabalhistaBloqueiaAntesDeGravar() {
        Usuario cidadao = new Usuario();
        cidadao.setId(9L);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setValorCausa(new BigDecimal("3000.00"));
        doAnswer(invocacao -> {
            processo.setRito(RitoProcessual.TRABALHISTA_ORDINARIO);
            processo.setTipoJustica(TipoJustica.TRABALHO);
            return null;
        }).when(ajuizamentoCanonicalContextService).consolidate(any(), any(), any());

        assertThatThrownBy(() -> command.execute(processo, cidadao, new ProcessoRequest(), List.of(), false))
                .isInstanceOf(AccessDeniedPjbException.class);
        verify(processoRepository, never()).save(any());
    }
}
