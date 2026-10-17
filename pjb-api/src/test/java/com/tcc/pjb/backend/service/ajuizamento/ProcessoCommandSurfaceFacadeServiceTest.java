package com.tcc.pjb.backend.service.ajuizamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.command.AjuizarProcessoCommand;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.mapper.ProcessoMapper;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.ProcessoResponseAssemblerService;
import com.tcc.pjb.backend.service.document.SmartFileSplitter;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.processual.representacao.PeticaoInicialSemAdvogadoCabimentoService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ProcessoCommandSurfaceFacadeServiceTest {

    private final AjuizarProcessoCommand ajuizarProcessoCommand = mock(AjuizarProcessoCommand.class);
    private final SmartFileSplitter smartFileSplitter = mock(SmartFileSplitter.class);
    private final ProcessoMapper processoMapper = mock(ProcessoMapper.class);
    private final ProcessoResponseAssemblerService processoResponseAssemblerService = mock(ProcessoResponseAssemblerService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PeticaoInicialSemAdvogadoCabimentoService cabimento = mock(PeticaoInicialSemAdvogadoCabimentoService.class);

    private ProcessoCommandSurfaceFacadeService service;

    @BeforeEach
    void setup() {
        service = new ProcessoCommandSurfaceFacadeService(
                ajuizarProcessoCommand,
                smartFileSplitter,
                processoMapper,
                processoResponseAssemblerService,
                currentUserService,
                cabimento
        );
    }

    @Test
    void devePropagarJuizo100DigitalEFiltrarArquivosVaziosNoBoundaryHttp() {
        ProcessoRequest request = ProcessoRequest.builder()
                .classe("Ação de cobrança")
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .juizo100Digital(true)
                .build();
        Usuario advogado = new Usuario();
        advogado.setId(7L);
        advogado.setEmail("advogado@test.local");
        Processo mapped = Processo.builder().id(88L).numeroProcesso("AJUI-HTTP-UNIT").build();
        Processo salvo = Processo.builder().id(88L).numeroProcesso("AJUI-HTTP-UNIT").build();
        ProcessoResponse response = ProcessoResponse.builder().id(88L).numeroProcesso("AJUI-HTTP-UNIT").build();
        MockMultipartFile anexoValido = new MockMultipartFile("anexos", "peticao.pdf", "application/pdf", "pdf".getBytes());
        MockMultipartFile anexoVazio = new MockMultipartFile("anexos", "vazio.pdf", "application/pdf", new byte[0]);
        List<Attachment> anexos = List.of(Attachment.builder().name("peticao.pdf").build());

        when(currentUserService.getRequired()).thenReturn(advogado);
        when(processoMapper.toEntity(request)).thenReturn(mapped);
        when(smartFileSplitter.processarArquivos(any(), any())).thenReturn(anexos);
        when(ajuizarProcessoCommand.execute(mapped, advogado, request, anexos, true)).thenReturn(salvo);
        when(processoResponseAssemblerService.toResponse(salvo, request)).thenReturn(response);

        ProcessoResponse actual = service.ajuizar(request, Arrays.asList(anexoValido, anexoVazio, null));

        ArgumentCaptor<List<MultipartFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(smartFileSplitter).processarArquivos(captor.capture(), any());
        verify(ajuizarProcessoCommand).execute(mapped, advogado, request, anexos, true);
        verify(processoResponseAssemblerService).toResponse(salvo, request);
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getOriginalFilename()).isEqualTo("peticao.pdf");
        assertThat(actual.getId()).isEqualTo(88L);
    }

    @Test
    void deveAceitarBoundarySemAnexos() {
        ProcessoRequest request = ProcessoRequest.builder()
                .classe("Ação declaratória")
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .build();
        Usuario advogado = new Usuario();
        Processo mapped = Processo.builder().id(91L).build();
        Processo salvo = Processo.builder().id(91L).build();
        ProcessoResponse response = ProcessoResponse.builder().id(91L).build();

        when(currentUserService.getRequired()).thenReturn(advogado);
        when(processoMapper.toEntity(request)).thenReturn(mapped);
        when(smartFileSplitter.processarArquivos(eq(List.of()), any())).thenReturn(List.of());
        when(ajuizarProcessoCommand.execute(mapped, advogado, request, List.of(), false)).thenReturn(salvo);
        when(processoResponseAssemblerService.toResponse(salvo, request)).thenReturn(response);

        ProcessoResponse actual = service.ajuizar(request, null);

        verify(smartFileSplitter).processarArquivos(eq(List.of()), any());
        verify(ajuizarProcessoCommand).execute(mapped, advogado, request, List.of(), false);
        assertThat(actual.getId()).isEqualTo(91L);
    }

    @Test
    void cidadaoAcimaDoLimiteDoJuizadoNaoChegaAoComandoDeAjuizamento() {
        ProcessoRequest request = ProcessoRequest.builder().rito("JUIZADO_ESPECIAL_CIVEL").build();
        Usuario cidadao = new Usuario();
        cidadao.setId(9L);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        Processo mapped = new Processo();
        mapped.setRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
        mapped.setValorCausa(new BigDecimal("1000000.00"));
        mapped.setTipoJustica(TipoJustica.ESTADUAL);
        when(currentUserService.getRequired()).thenReturn(cidadao);
        when(processoMapper.toEntity(request)).thenReturn(mapped);
        doThrow(new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "acima de 20 salários mínimos"))
                .when(cabimento).exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("1000000.00"), TipoJustica.ESTADUAL);

        assertThatThrownBy(() -> service.ajuizar(request, List.of())).isInstanceOf(ErroDeValidacaoException.class);
        verifyNoInteractions(ajuizarProcessoCommand);
    }

    @Test
    void advogadoAjuizaSemPassarPeloLimiteDoJusPostulandi() {
        ProcessoRequest request = ProcessoRequest.builder().rito("COMUM_ORDINARIO").build();
        Usuario advogado = new Usuario();
        advogado.setId(7L);
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        Processo mapped = new Processo();
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(processoMapper.toEntity(request)).thenReturn(mapped);

        service.ajuizar(request, List.of());

        verifyNoInteractions(cabimento);
        verify(ajuizarProcessoCommand).execute(eq(mapped), eq(advogado), eq(request), any(), eq(false));
    }
}
