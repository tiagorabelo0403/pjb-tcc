package com.tcc.pjb.backend.service.consultapublica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ConsultaPublicaSearchServiceTest {

    @Test
    void resolvePublicPageReturnsPageForPublicDocument() {
        DocumentoPaginaRepository paginaRepository = mock(DocumentoPaginaRepository.class);
        ConsultaPublicaSearchService service = new ConsultaPublicaSearchService(mock(NamedParameterJdbcTemplate.class), paginaRepository);

        Processo processo = Processo.builder()
                .id(11L)
                .numeroUnificado("0011223-44.2026.8.06.0001")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Decisão interlocutória")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        DocumentoPagina pagina = DocumentoPagina.builder()
                .documento(documento)
                .pageId("PG-11")
                .pageNumber(3)
                .fingerprint("fp-11")
                .textoExtraido("Texto público da página.")
                .build();

        when(paginaRepository.findByPageId("PG-11")).thenReturn(Optional.of(pagina));

        var response = service.resolvePublicPage("PG-11");

        assertThat(response.getPageId()).isEqualTo("PG-11");
        assertThat(response.getProcessoId()).isEqualTo(11L);
        assertThat(response.getNumeroUnificado()).isEqualTo("0011223-44.2026.8.06.0001");
        assertThat(response.getPublicActKind()).isEqualTo("DECISAO_PUBLICA");
        assertThat(response.getTexto()).contains("Texto público da página");
    }

    @Test
    void resolvePublicPageRejectsRestrictedDocument() {
        DocumentoPaginaRepository paginaRepository = mock(DocumentoPaginaRepository.class);
        ConsultaPublicaSearchService service = new ConsultaPublicaSearchService(mock(NamedParameterJdbcTemplate.class), paginaRepository);

        Processo processo = Processo.builder()
                .id(12L)
                .numeroUnificado("0011223-55.2026.8.06.0001")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Laudo sigiloso")
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .build();
        DocumentoPagina pagina = DocumentoPagina.builder()
                .documento(documento)
                .pageId("PG-12")
                .pageNumber(1)
                .fingerprint("fp-12")
                .textoExtraido("Trecho protegido")
                .build();

        when(paginaRepository.findByPageId("PG-12")).thenReturn(Optional.of(pagina));

        assertThatThrownBy(() -> service.resolvePublicPage("PG-12"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PaginaPublica");
    }

    @Test
    void resolvePublicPageRejectsPublicNonActDocument() {
        DocumentoPaginaRepository paginaRepository = mock(DocumentoPaginaRepository.class);
        ConsultaPublicaSearchService service = new ConsultaPublicaSearchService(mock(NamedParameterJdbcTemplate.class), paginaRepository);

        Processo processo = Processo.builder()
                .id(13L)
                .numeroUnificado("0011223-66.2026.8.06.0001")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Petição inicial")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        DocumentoPagina pagina = DocumentoPagina.builder()
                .documento(documento)
                .pageId("PG-13")
                .pageNumber(1)
                .fingerprint("fp-13")
                .textoExtraido("Conteúdo público")
                .build();

        when(paginaRepository.findByPageId("PG-13")).thenReturn(Optional.of(pagina));

        assertThatThrownBy(() -> service.resolvePublicPage("PG-13"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PaginaPublica");
    }

}
