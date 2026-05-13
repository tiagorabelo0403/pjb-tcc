package com.tcc.pjb.backend.service.consultapublica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.cache.type=none")
class ConsultaPublicaSearchFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private DocumentoPaginaRepository documentoPaginaRepository;

    @Autowired
    private ConsultaPublicaSearchService consultaPublicaSearchService;

    @Test
    void deveRetornarSomenteAutosPublicosEResolverPaginaDeAtoPublico() {
        Processo publico = processoRepository.save(Processo.builder()
                .numeroProcesso("CP-SEARCH-2026-01")
                .numeroUnificado("0007001-11.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Cumprimento de sentença")
                .assunto("Cobrança contratual")
                .parteAutoraNome("Maria da Silva")
                .parteReuNome("Banco Central do Sertão")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .vara("2ª Vara Cível")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 9, 30))
                .build());

        processoRepository.save(Processo.builder()
                .numeroProcesso("CP-SEARCH-2026-02")
                .numeroUnificado("0007002-22.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Cumprimento de sentença")
                .assunto("Cobrança contratual")
                .parteAutoraNome("Maria da Silva")
                .parteReuNome("Banco Central do Sertão")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .vara("3ª Vara Cível")
                .uf("CE")
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 9, 0))
                .build());

        DocumentoProcessual acordao = documentoProcessualRepository.save(DocumentoProcessual.builder()
                .processo(publico)
                .titulo("Acórdão de apelação cível")
                .nomeOriginal("acordao-publico.pdf")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .criadoEm(LocalDateTime.of(2026, 4, 16, 10, 0))
                .build());

        documentoPaginaRepository.save(DocumentoPagina.builder()
                .documento(acordao)
                .pageNumber(2)
                .pageId("CP-PAGE-2026-0001")
                .fingerprint("sha256:cp-page-1")
                .textoExtraido("Trecho público consolidado do acórdão com decisão colegiada definitiva.")
                .criadoEm(LocalDateTime.of(2026, 4, 16, 10, 1))
                .build());

        var response = consultaPublicaSearchService.searchPublic("Maria da Silva", "ESTADUAL", "CIVIL", 0, 20);

        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getHits()).singleElement().satisfies(hit -> {
            assertThat(hit.getProcessoId()).isEqualTo(publico.getId());
            assertThat(hit.getNumeroUnificado()).isEqualTo("0007001-11.2026.8.06.0001");
            assertThat(hit.getTipoJustica()).isEqualTo("ESTADUAL");
            assertThat(hit.getRamoDireito()).isEqualTo("CIVIL");
            assertThat(hit.getSnippet()).contains("Partes públicas: Maria da Silva x Banco Central do Sertão");
        });

        var page = consultaPublicaSearchService.resolvePublicPage("CP-PAGE-2026-0001");
        assertThat(page.getPageId()).isEqualTo("CP-PAGE-2026-0001");
        assertThat(page.getProcessoId()).isEqualTo(publico.getId());
        assertThat(page.getPublicActKind()).isEqualTo("ACORDAO_PUBLICO");
        assertThat(page.getTexto()).contains("Trecho público consolidado do acórdão");
    }

    @Test
    void deveRecusarResolucaoDePaginaQuandoDocumentoNaoRepresentaAtoPublico() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CP-SEARCH-2026-03")
                .numeroUnificado("0007003-33.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        DocumentoProcessual peticao = documentoProcessualRepository.save(DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Petição inicial")
                .nomeOriginal("peticao-inicial.pdf")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .criadoEm(LocalDateTime.of(2026, 4, 16, 11, 0))
                .build());

        documentoPaginaRepository.save(DocumentoPagina.builder()
                .documento(peticao)
                .pageNumber(1)
                .pageId("CP-PAGE-2026-0002")
                .fingerprint("sha256:cp-page-2")
                .textoExtraido("Conteúdo de peça privada que não deve ser aberto na navegação pública.")
                .criadoEm(LocalDateTime.of(2026, 4, 16, 11, 1))
                .build());

        assertThatThrownBy(() -> consultaPublicaSearchService.resolvePublicPage("CP-PAGE-2026-0002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PaginaPublica");
    }
}
