package com.tcc.pjb.backend.core.processo.plantao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.processo.plantao.application.ProcessoPlantaoSubstituicaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloJurisdicaoBridge;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusCoberturaOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCoberturaOperacionalInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoPlantaoSubstituicaoApplicationServiceTest {

    @Test
    void deveLerCoberturaDePlantaoComoDominioPesado() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        InstitutionalOperationalCoverageApplicationService coverage = mock(InstitutionalOperationalCoverageApplicationService.class);
        ProcessoSigiloInteligenteApplicationService sigilo = mock(ProcessoSigiloInteligenteApplicationService.class);
        ProcessoPreGravacaoApplicationService preGravacao = mock(ProcessoPreGravacaoApplicationService.class);
        ProcessoUnificadoApplicationService unificado = mock(ProcessoUnificadoApplicationService.class);

        ProcessoUnificadoIdentity identity = new ProcessoUnificadoIdentity(17L, "0017", "0017", "TJCE", "CE", "FORTALEZA", "1VC", "Classe", "Assunto", "Autor", "Réu", List.of());
        Processo processo = Processo.builder().id(17L).numeroProcesso("0017").unidadeJudiciariaCodigo("1VC").ramoDireito(RamoDireito.PENAL).rito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM).faseAtual(FaseProcessual.CONHECIMENTO).build();
        when(processoRepository.findById(17L)).thenReturn(Optional.of(processo));
        when(unificado.detalhar(17L)).thenReturn(new ProcessoUnificadoAggregate(identity, new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", "TJCE", "ORG", "1VC", "FILA", "MESA", "TERRITORIAL", "PREV", "DIST", "PENAL", "AUTO", "LINK", "ENVELOPE", "BAIXO", "MAGISTRADO", false, false, 24, List.of(), List.of("fund"), List.of(), new LinkedHashMap<>()), new ProcessoUnificadoDiagnostico(true, 0L, 0L, 1L, 0L, 1L, 1L, List.of(), List.of("fund"), Instant.now()), List.of(new ProcessoUnificadoAto("ASSINAR", "Assinar", "ATO", "TASK", "EIXO", "FILA", "INBOX", "fund", "A", "B", "A", "B", true, false, false, true, true, false, "m", "MAG", "TK", List.of())), List.of(), List.of(), Instant.now()));
        when(preGravacao.avaliar(17L, "MAGISTRATURA__MAGISTRADO_TITULAR", "ASSINAR")).thenReturn(new ProcessoPreGravacaoAggregate(identity, "MAGISTRATURA__MAGISTRADO_TITULAR", "ASSINAR", false, 1L, 0L, 1L, 1L, List.of("STEP_UP"), List.of(), List.of("fund"), List.of("fund"), Instant.now()));
        when(sigilo.avaliar(17L)).thenReturn(new ProcessoSigiloInteligenteAggregate(identity, NivelSigilo.PUBLICO, NivelSigilo.SIGILO_N4, "RECLASSIFICAR", true, true, true, true, "RESTRITO", new ProcessoSigiloJurisdicaoBridge("E", "G", "M", "TJCE", "1VC", "CE", "FORTALEZA", "FORO", "ANEL", true, true, true, List.of()), List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(coverage.listar("1VC")).thenReturn(List.of(new InstitutionalOperationalCoverageRule("rule-1", "1VC", "CAIXA", 1L, 2L, TipoCoberturaOperacionalInstitucional.PLANTAO, EnumSet.noneOf(com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.class), StatusCoberturaOperacionalInstitucional.ATIVA, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), "plantao", "obs", Instant.now(), Instant.now(), "hash")));

        ProcessoPlantaoSubstituicaoApplicationService service = new ProcessoPlantaoSubstituicaoApplicationService(processoRepository, coverage, sigilo, preGravacao, unificado);
        var aggregate = service.detalhar(17L);

        assertThat(aggregate.plantaoAtivo()).isTrue();
        assertThat(aggregate.titularObrigatorio()).isTrue();
        assertThat(aggregate.escalonamento()).isNotEmpty();
    }
}
