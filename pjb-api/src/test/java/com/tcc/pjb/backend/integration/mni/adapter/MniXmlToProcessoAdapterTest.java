package com.tcc.pjb.backend.integration.mni.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class MniXmlToProcessoAdapterTest {

    private final MniXmlToProcessoAdapter adapter = new MniXmlToProcessoAdapter();

    @Test
    void shouldPopulatePartesFromPoloAtivoEPoloPassivo() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0001-22.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="12345678900"/>
                            </parte>
                        </polo>
                        <polo polo="PA">
                            <parte>
                                <pessoa nome="Joao Souza" tipoPessoa="fisica" numeroDocumentoPrincipal="98765432100"/>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");
        Processo processo = result.processo();

        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria da Silva");
        assertThat(processo.getParteAutoraCpf()).isEqualTo("12345678900");
        assertThat(processo.getParteReuNome()).isEqualTo("Joao Souza");
        assertThat(processo.getParteReuCpf()).isEqualTo("98765432100");
        assertThat(processo.getUfAutor()).isNull();
        assertThat(processo.getUfReu()).isNull();
        assertThat(result.partes()).hasSize(2);
    }

    @Test
    void shouldPopularUfDomicilioAPartirDoEnderecoDaParte() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0002-33.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="12345678900">
                                    <endereco>
                                        <estado>CE</estado>
                                    </endereco>
                                </pessoa>
                            </parte>
                        </polo>
                        <polo polo="PA">
                            <parte>
                                <pessoa nome="Joao Souza" tipoPessoa="fisica" numeroDocumentoPrincipal="98765432100"/>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");
        Processo processo = result.processo();

        assertThat(processo.getUfAutor()).isEqualTo("CE");
        assertThat(processo.getUfReu()).isNull();
    }

    @Test
    void shouldNormalizarUfComEspacoEMinuscula() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0004-55.2026.8.06.0001</numeroUnificado>
                        <polo polo="PA">
                            <parte>
                                <pessoa nome="Joao Souza" tipoPessoa="fisica" numeroDocumentoPrincipal="98765432100">
                                    <endereco>
                                        <estado> ce </estado>
                                    </endereco>
                                </pessoa>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA").processo();

        assertThat(processo.getUfReu()).isEqualTo("CE");
    }

    @Test
    void shouldDescartarEstadoQueNaoESiglaUfValida() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0005-66.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="12345678900">
                                    <endereco>
                                        <estado>Ceara</estado>
                                    </endereco>
                                </pessoa>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA").processo();

        assertThat(processo.getUfAutor()).isNull();
    }

    @Test
    void shouldUsarPrimeiroEnderecoQuandoHaMultiplos() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0006-77.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="12345678900">
                                    <endereco>
                                        <estado>CE</estado>
                                    </endereco>
                                    <endereco>
                                        <estado>SP</estado>
                                    </endereco>
                                </pessoa>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA").processo();

        assertThat(processo.getUfAutor()).isEqualTo("CE");
    }

    @Test
    void shouldParsearTodasAsPessoasEmLitisconsorcio() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0007-88.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="11111111111">
                                    <endereco><estado>CE</estado></endereco>
                                </pessoa>
                            </parte>
                            <parte>
                                <pessoa nome="Joao Coautor" tipoPessoa="fisica" numeroDocumentoPrincipal="22222222222">
                                    <endereco><estado>SP</estado></endereco>
                                </pessoa>
                            </parte>
                        </polo>
                        <polo polo="PA">
                            <parte>
                                <pessoa nome="Empresa Alpha Ltda" tipoPessoa="juridica" numeroDocumentoPrincipal="33333333000100"/>
                            </parte>
                            <parte>
                                <pessoa nome="Empresa Beta SA" tipoPessoa="juridica" numeroDocumentoPrincipal="44444444000100"/>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");
        Processo processo = result.processo();
        List<MniParteParsed> partes = result.partes();

        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria da Silva");
        assertThat(processo.getParteAutoraCpf()).isEqualTo("11111111111");
        assertThat(processo.getUfAutor()).isEqualTo("CE");
        assertThat(processo.getParteReuNome()).isEqualTo("Empresa Alpha Ltda");
        assertThat(processo.getParteReuCpf()).isEqualTo("33333333000100");

        assertThat(partes).hasSize(4);
        assertThat(partes.get(0)).isEqualTo(new MniParteParsed("AT", "Maria da Silva", "11111111111", "CE", "fisica"));
        assertThat(partes.get(1)).isEqualTo(new MniParteParsed("AT", "Joao Coautor", "22222222222", "SP", "fisica"));
        assertThat(partes.get(2)).isEqualTo(new MniParteParsed("PA", "Empresa Alpha Ltda", "33333333000100", null, "juridica"));
        assertThat(partes.get(3)).isEqualTo(new MniParteParsed("PA", "Empresa Beta SA", "44444444000100", null, "juridica"));
    }

    @Test
    void shouldParsearTerceiroInteressadoEInteressePublico() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0008-99.2026.8.06.0001</numeroUnificado>
                        <polo polo="AT">
                            <parte>
                                <pessoa nome="Maria da Silva" tipoPessoa="fisica" numeroDocumentoPrincipal="12345678900"/>
                            </parte>
                        </polo>
                        <polo polo="PA">
                            <parte>
                                <pessoa nome="Empresa XYZ Ltda" tipoPessoa="juridica" numeroDocumentoPrincipal="12345678000100"/>
                            </parte>
                        </polo>
                        <polo polo="TC">
                            <parte>
                                <pessoa nome="Carlos Terceiro" tipoPessoa="fisica" numeroDocumentoPrincipal="99988877766"/>
                            </parte>
                            <parte>
                                <interessePublico>Fazenda Publica do Estado do Ceara</interessePublico>
                            </parte>
                        </polo>
                        <polo polo="FL">
                            <parte>
                                <interessePublico>Ministerio Publico Federal</interessePublico>
                            </parte>
                        </polo>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");
        Processo processo = result.processo();
        List<MniParteParsed> partes = result.partes();

        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria da Silva");
        assertThat(processo.getParteReuNome()).isEqualTo("Empresa XYZ Ltda");

        assertThat(partes).hasSize(5);
        assertThat(partes.get(0)).isEqualTo(new MniParteParsed("AT", "Maria da Silva", "12345678900", null, "fisica"));
        assertThat(partes.get(1)).isEqualTo(new MniParteParsed("PA", "Empresa XYZ Ltda", "12345678000100", null, "juridica"));
        assertThat(partes.get(2)).isEqualTo(new MniParteParsed("TC", "Carlos Terceiro", "99988877766", null, "fisica"));
        assertThat(partes.get(3)).isEqualTo(new MniParteParsed("TC", "Fazenda Publica do Estado do Ceara", null, null, "interesse_publico"));
        assertThat(partes.get(4)).isEqualTo(new MniParteParsed("FL", "Ministerio Publico Federal", null, null, "interesse_publico"));
    }

    @Test
    void shouldPopularMovimentosComDataHoraCompactaEDescricaoDireta() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0009-10.2026.8.06.0001</numeroUnificado>
                        <movimento dataHora="20260815093000" descricao="Distribuicao por sorteio"/>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.movimentos()).hasSize(1);
        MniMovimentoParsed movimento = result.movimentos().get(0);
        assertThat(movimento.descricao()).isEqualTo("Distribuicao por sorteio");
        assertThat(movimento.dataHora()).isEqualTo(java.time.LocalDateTime.of(2026, 8, 15, 9, 30, 0).toInstant(java.time.ZoneOffset.UTC));
    }

    @Test
    void shouldPopularDescricaoDeMovimentoNacionalQuandoNaoHaAtributoDireto() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0010-11.2026.8.06.0001</numeroUnificado>
                        <movimento dataHora="20260810080000">
                            <movimentoNacional codigoNacional="26" descricao="Juntada de peticao"/>
                        </movimento>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.movimentos()).hasSize(1);
        assertThat(result.movimentos().get(0).descricao()).isEqualTo("Juntada de peticao");
    }

    @Test
    void shouldDescartarMovimentoSemDataHoraReconhecivelSemFabricarData() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0011-12.2026.8.06.0001</numeroUnificado>
                        <movimento dataHora="data-invalida" descricao="Movimento sem data valida"/>
                        <movimento dataHora="20260901120000" descricao="Movimento valido"/>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.movimentos()).hasSize(1);
        assertThat(result.movimentos().get(0).descricao()).isEqualTo("Movimento valido");
    }

    @Test
    void shouldPopularMultiplosMovimentosNaOrdemDoDocumento() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0012-13.2026.8.06.0001</numeroUnificado>
                        <movimento dataHora="20260801100000" descricao="Recebimento"/>
                        <movimento dataHora="20260805143000" descricao="Distribuicao"/>
                        <movimento dataHora="20260810091500" descricao="Conclusao para despacho"/>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.movimentos()).hasSize(3);
        assertThat(result.movimentos()).extracting(MniMovimentoParsed::descricao)
                .containsExactly("Recebimento", "Distribuicao", "Conclusao para despacho");
    }

    @Test
    void shouldRetornarListaVaziaDeMovimentosQuandoAusentesNoXml() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0013-14.2026.8.06.0001</numeroUnificado>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.movimentos()).isEmpty();
    }

    @Test
    void shouldPopularDocumentoComConteudoBase64DecodificadoENomeMimetype() {
        String base64 = pdfBase64();
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0014-15.2026.8.06.0001</numeroUnificado>
                        <documento nome="peticao_inicial.pdf" descricao="Peticao Inicial" mimetype="application/pdf" dataHora="20260701093000">
                            <conteudo>%s</conteudo>
                        </documento>
                    </dadosBasicos>
                </processoJudicial>
                """.formatted(base64);

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.documentos()).hasSize(1);
        MniDocumentoParsed documento = result.documentos().get(0);
        assertThat(documento.nome()).isEqualTo("peticao_inicial.pdf");
        assertThat(documento.descricao()).isEqualTo("Peticao Inicial");
        assertThat(documento.mimetype()).isEqualTo("application/pdf");
        assertThat(documento.conteudo()).isEqualTo(Base64.getMimeDecoder().decode(base64));
        assertThat(documento.dataHora()).isEqualTo(java.time.LocalDateTime.of(2026, 7, 1, 9, 30, 0).toInstant(java.time.ZoneOffset.UTC));
    }

    @Test
    void shouldDescartarDocumentoSemConteudo() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0015-16.2026.8.06.0001</numeroUnificado>
                        <documento nome="peticao_inicial.pdf" descricao="Peticao Inicial"/>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.documentos()).isEmpty();
    }

    @Test
    void shouldDescartarDocumentoComConteudoBase64Invalido() {
        // Comprimento matematicamente impossível de decodificar (1 char sobrando não fecha 1 byte) —
        // Base64.getMimeDecoder() é tolerante a lixo intercalado, mas isso ainda assim não decodifica.
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0016-17.2026.8.06.0001</numeroUnificado>
                        <documento nome="corrompido.pdf">
                            <conteudo>Q</conteudo>
                        </documento>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.documentos()).isEmpty();
    }

    @Test
    void shouldRetornarListaVaziaDeDocumentosQuandoAusentesNoXml() {
        String xml = """
                <processoJudicial>
                    <dadosBasicos>
                        <numeroUnificado>0017-18.2026.8.06.0001</numeroUnificado>
                    </dadosBasicos>
                </processoJudicial>
                """;

        MniAdapterResult result = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(result.documentos()).isEmpty();
    }

    private static String pdfBase64() {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF de teste", e);
        }
    }
}
