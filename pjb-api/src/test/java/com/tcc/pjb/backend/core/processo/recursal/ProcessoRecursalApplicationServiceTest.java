package com.tcc.pjb.backend.core.processo.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoRecursalApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private DocumentoProcessualRepository documentoProcessualRepository;
    @Mock
    private DocumentoPaginaRepository documentoPaginaRepository;

    @Test
    void deveMontarJanelaRecursalComEmbargosERecursosCabiveis() {
        Processo processo = Processo.builder()
                .id(77L)
                .numeroProcesso("0000101-10.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .classeProcessual("Ação de Obrigação de Fazer")
                .assunto("Tutela recursal")
                .resultadoFinal("Sentença de improcedência com fundamento na ausência de prova mínima.")
                .pedidoPrincipal("Obrigação de fazer com tutela de urgência.")
                .materialProbatorioResumo("Documentos contratuais e mensagens eletrônicas juntadas.")
                .build();
        when(processoRepository.findById(77L)).thenReturn(Optional.of(processo));

        ProcessoRecursalApplicationService service = new ProcessoRecursalApplicationService(processoRepository, documentoProcessualRepository, documentoPaginaRepository);
        var aggregate = service.detalhar(77L);

        assertThat(aggregate.totalCabiveis()).isGreaterThan(0);
        assertThat(aggregate.janelas()).anyMatch(item -> item.codigo().equals("EDCL"));
        assertThat(aggregate.janelas()).anyMatch(item -> item.codigo().equals("APCIV") || item.codigo().equals("AGINST"));
        assertThat(aggregate.cadernoDecisorioOrigem()).isNotNull();
        assertThat(aggregate.cadernoDecisorioOrigem().available()).isTrue();
        assertThat(aggregate.cadernoDecisorioOrigem().headline()).contains("Sentença");
    }

    @Test
    void deveLevarCadeiaDecisoriaCompletaParaGrausSuperiores() {
        Processo processo = Processo.builder()
                .id(99L)
                .numeroProcesso("0000303-30.2026.8.06.0001")
                .numeroUnificado("0000303-30.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .resultadoFinal("Acórdão parcialmente reformador.")
                .build();
        DocumentoProcessual sentenca = DocumentoProcessual.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .processo(processo)
                .titulo("Sentença")
                .nomeOriginal("sentenca.pdf")
                .contentType("application/pdf")
                .pdf(new byte[]{1,2,3})
                .criadoEm(LocalDateTime.now().minusDays(5))
                .build();
        DocumentoProcessual acordao = DocumentoProcessual.builder()
                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .processo(processo)
                .titulo("Acórdão")
                .nomeOriginal("acordao.pdf")
                .contentType("application/pdf")
                .pdf(new byte[]{4,5,6})
                .criadoEm(LocalDateTime.now().minusDays(1))
                .build();
        DocumentoPagina paginaSentenca = DocumentoPagina.builder()
                .documento(sentenca)
                .pageNumber(1)
                .pageId("s-1")
                .fingerprint("fs-1")
                .textoExtraido("Íntegra da sentença de primeiro grau.")
                .criadoEm(LocalDateTime.now().minusDays(5))
                .build();
        DocumentoPagina paginaAcordao = DocumentoPagina.builder()
                .documento(acordao)
                .pageNumber(1)
                .pageId("a-1")
                .fingerprint("fa-1")
                .textoExtraido("Íntegra do acórdão recorrido.")
                .criadoEm(LocalDateTime.now().minusDays(1))
                .build();
        when(processoRepository.findById(99L)).thenReturn(Optional.of(processo));
        when(documentoProcessualRepository.findByProcessoId(99L)).thenReturn(List.of(acordao, sentenca));
        when(documentoPaginaRepository.findByDocumentoId(sentenca.getId())).thenReturn(List.of(paginaSentenca));
        when(documentoPaginaRepository.findByDocumentoId(acordao.getId())).thenReturn(List.of(paginaAcordao));

        ProcessoRecursalApplicationService service = new ProcessoRecursalApplicationService(processoRepository, documentoProcessualRepository, documentoPaginaRepository);
        var aggregate = service.detalhar(99L);

        assertThat(aggregate.cadernoDecisorioOrigem().trilhaDecisoriaIntegral()).hasSize(2);
        assertThat(aggregate.cadernoDecisorioOrigem().trilhaDecisoriaIntegral().get(0).stageLabel()).isEqualTo("PRIMEIRO_GRAU");
        assertThat(aggregate.cadernoDecisorioOrigem().trilhaDecisoriaIntegral().get(1).stageLabel()).isEqualTo("SEGUNDO_GRAU");
        assertThat(aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().displayTitle()).contains("Acórdão");
        assertThat(aggregate.cadernoDecisorioOrigem().carryOverSignals()).contains("CADEIA_DECISORIA_CUMULATIVA_ACOPLADA");
    }

    @Test
    void deveLevarDocumentoOriginalDaSentencaParaOCadernoRecursal() {
        Processo processo = Processo.builder()
                .id(88L)
                .numeroProcesso("0000202-20.2026.8.06.0001")
                .numeroUnificado("0000202-20.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .resultadoFinal("Sentença de procedência.")
                .build();
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .processo(processo)
                .titulo("Sentença")
                .nomeOriginal("sentenca.pdf")
                .contentType("application/pdf")
                .tamanhoBytes(2048L)
                .sha256("abc")
                .pdf(new byte[]{1,2,3})
                .criadoEm(LocalDateTime.now())
                .build();
        DocumentoPagina pagina = DocumentoPagina.builder()
                .documento(documento)
                .pageNumber(1)
                .pageId("p-1")
                .fingerprint("fp-1")
                .textoExtraido("Sentença integral com fundamentação e dispositivo.")
                .criadoEm(LocalDateTime.now())
                .build();
        when(processoRepository.findById(88L)).thenReturn(Optional.of(processo));
        when(documentoProcessualRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(88L)).thenReturn(List.of(documento));
        when(documentoPaginaRepository.findByDocumentoId(documento.getId())).thenReturn(List.of(pagina));

        ProcessoRecursalApplicationService service = new ProcessoRecursalApplicationService(processoRepository, documentoProcessualRepository, documentoPaginaRepository);
        var aggregate = service.detalhar(88L);

        assertThat(aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao()).isNotNull();
        assertThat(aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().displayTitle()).contains("Sentença");
        assertThat(aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().pdfEndpoint()).isEqualTo("/api/v1/documentos/11111111-1111-1111-1111-111111111111/pdf");
        assertThat(aggregate.cadernoDecisorioOrigem().documentoOriginalDecisao().textoIntegralExtraido()).contains("fundamentação");
    }

}
