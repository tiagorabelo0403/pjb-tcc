package com.tcc.pjb.backend.integration.mni.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;
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
}
