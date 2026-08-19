package com.tcc.pjb.backend.service.institutional.movimentacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MovimentacaoProcessualRegistrarTest {

    private final MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final MovimentacaoProcessualRegistrar registrar = new MovimentacaoProcessualRegistrar(movimentacaoRepository, processoRepository);

    @Test
    void registraMovimentacaoComAtorEDescricaoEAtualizaDataUltimaMovimentacao() {
        Processo processo = new Processo();
        processo.setId(80L);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        Usuario ator = Usuario.builder().id(30L).nome("Promotor").build();
        when(movimentacaoRepository.save(org.mockito.ArgumentMatchers.any(MovimentacaoProcessual.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimentacaoProcessual resultado = registrar.registrar(processo, ator, FaseProcessual.CONHECIMENTO, "Manifestação do Ministério Público registrada.");

        assertThat(resultado.getAtor()).isEqualTo(ator);
        assertThat(resultado.getDescricao()).isEqualTo("Manifestação do Ministério Público registrada.");
        assertThat(resultado.getProcesso()).isEqualTo(processo);
        assertThat(processo.getDataUltimaMovimentacao()).isNotNull();
        verify(processoRepository).save(processo);
    }

    @Test
    void persisteFaseDeEFaseParaCorretamente() {
        Processo processo = new Processo();
        processo.setId(80L);
        processo.setFaseAtual(FaseProcessual.SANEAMENTO);
        Usuario ator = Usuario.builder().id(30L).nome("Defensor").build();
        ArgumentCaptor<MovimentacaoProcessual> captor = ArgumentCaptor.forClass(MovimentacaoProcessual.class);
        when(movimentacaoRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        registrar.registrar(processo, ator, FaseProcessual.CONHECIMENTO, "Petição da Defensoria Pública registrada.");

        assertThat(captor.getValue().getFaseDe()).isEqualTo(FaseProcessual.CONHECIMENTO);
        assertThat(captor.getValue().getFasePara()).isEqualTo(FaseProcessual.SANEAMENTO);
    }
}
