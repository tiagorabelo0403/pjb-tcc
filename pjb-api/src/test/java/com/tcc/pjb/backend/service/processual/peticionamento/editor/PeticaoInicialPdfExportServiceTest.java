package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticaoInicialPdfExportServiceTest {

    private final PeticaoInicialPdfExportService service = new PeticaoInicialPdfExportService();

    @Test
    void geraBytesDePdfDeVerdadeComUmaPagina() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0000001-11.2026.5.07.0001");
        Usuario usuario = new Usuario();
        usuario.setNome("Dra. Ana Teste");

        PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact artifact =
                service.export("Petição Inicial", processo, usuario, List.of("O réu descumpriu o contrato."));

        assertThat(new String(artifact.bytes(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(artifact.sha256()).hasSize(64);
        assertThat(artifact.sha384()).hasSize(96);
        assertThat(artifact.paginas()).isEqualTo(1);
    }

    @Test
    void corpoLongoQuebraEmMultiplasPaginas() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0000002-22.2026.5.07.0001");
        List<String> corpo = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            corpo.add("Linha de fato relevante numero " + i + " descrevendo o descumprimento contratual em detalhe.");
        }

        PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact artifact =
                service.export("Petição Inicial", processo, null, corpo);

        assertThat(artifact.paginas()).isGreaterThan(1);
    }

    @Test
    void corpoVazioAindaProduzPdfValidoComAPagina() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0000003-33.2026.5.07.0001");

        PeticaoInicialPdfExportService.PeticaoInicialPdfArtifact artifact =
                service.export("Petição Inicial", processo, null, List.of());

        assertThat(artifact.paginas()).isEqualTo(1);
        assertThat(new String(artifact.bytes(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
