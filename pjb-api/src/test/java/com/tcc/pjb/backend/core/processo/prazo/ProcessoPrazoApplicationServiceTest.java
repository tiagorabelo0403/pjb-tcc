package com.tcc.pjb.backend.core.processo.prazo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoPrazoApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private PrazoProcessualNacionalService prazoProcessualNacionalService;

    @Test
    void deveMontarJanelaDePrazosComMarcosCriticos() {
        Processo processo = Processo.builder()
                .id(99L)
                .numeroProcesso("0001234-56.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataCriacao(LocalDateTime.now().minusDays(2))
                .dataUltimaMovimentacao(LocalDateTime.now().minusDays(1))
                .build();
        when(processoRepository.findById(99L)).thenReturn(Optional.of(processo));
        when(prazoProcessualNacionalService.calcular(any())).thenAnswer(invocation -> {
            PrazoProcessualNacionalService.CalculoPrazoCommand command = invocation.getArgument(0);
            return new PrazoProcessualNacionalService.PrazoProcessualResult(
                    command.dataInicio(),
                    command.dataInicio().plusDays(5),
                    command.dataInicio().plusDays(2),
                    5,
                    3,
                    2,
                    command.tipoPrazo(),
                    command.ramo(),
                    command.grau(),
                    command.tribunalCodigo(),
                    command.uf(),
                    command.comarca(),
                    true,
                    "DIA_UTIL",
                    List.of("janela controlada"),
                    "fundamento nacional",
                    "fundamento forense"
            );
        });

        ProcessoPrazoApplicationService service = new ProcessoPrazoApplicationService(processoRepository, prazoProcessualNacionalService);
        var aggregate = service.detalhar(99L);

        assertThat(aggregate.totalMarcos()).isGreaterThanOrEqualTo(3);
        assertThat(aggregate.ciencia().cienciaPessoalObrigatoria()).isTrue();
        assertThat(aggregate.marcos()).anyMatch(marco -> marco.tipoPrazo().equals(NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL.name()));
    }
}
