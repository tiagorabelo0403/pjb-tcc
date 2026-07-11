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
    }
}
