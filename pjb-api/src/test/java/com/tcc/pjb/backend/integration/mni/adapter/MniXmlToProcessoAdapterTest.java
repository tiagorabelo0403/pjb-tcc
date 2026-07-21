package com.tcc.pjb.backend.integration.mni.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
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

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria da Silva");
        assertThat(processo.getParteAutoraCpf()).isEqualTo("12345678900");
        assertThat(processo.getParteReuNome()).isEqualTo("Joao Souza");
        assertThat(processo.getParteReuCpf()).isEqualTo("98765432100");
        assertThat(processo.getUfAutor()).isNull();
        assertThat(processo.getUfReu()).isNull();
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

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

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

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

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

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

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

        Processo processo = adapter.fromXml(xml, "TJCE", "CARTA_PRECATORIA");

        assertThat(processo.getUfAutor()).isEqualTo("CE");
    }
}
