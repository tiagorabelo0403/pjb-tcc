package com.tcc.pjb.backend.core.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class DistribuicaoProcessualProcessoSupportTest {

    private final DistribuicaoProcessualProcessoSupport support = new DistribuicaoProcessualProcessoSupport();

    @Test
    void deveConstruirRequestComUrgenciaESigiloDerivadosDoProcesso() {
        Processo processo = new Processo();
        processo.setNumeroUnificado("0001234-55.2026.8.06.0001");
        processo.setUf("ce");
        processo.setComarca("Fortaleza");
        processo.setRito(RitoProcessual.CIVIL_TUTELA_URGENTE);
        processo.setParteAutoraNome("Autor");
        processo.setParteReuNome("Réu");
        processo.setClasseProcessual("Tutela de urgência");
        processo.setAssunto("Fornecimento de medicamento");
        processo.setPreProtocoloStatus("URGENTE_LIMINAR");
        processo.setLinkageMode("DEPENDENCIA:0001111");

        var request = support.buildFromProcesso(processo);

        assertThat(request.numeroProcesso()).isEqualTo("0001234-55.2026.8.06.0001");
        assertThat(request.uf()).isEqualTo("CE");
        assertThat(request.urgente()).isTrue();
        assertThat(request.temDependencia()).isTrue();
        assertThat(request.areaEspecializada()).isNotBlank();
    }

    @Test
    void deveInferirFluxoColegiadoQuandoUnidadeIndicaSegundoGrau() {
        Processo processo = new Processo();
        processo.setNumero("123");
        processo.setUnidadeJudiciariaCodigo("CAMARA_DIREITO_PUBLICO");
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setClasseProcessual("Apelação");
        processo.setAssunto("Servidor público");

        var request = support.buildFromProcesso(processo);

        assertThat(request.grauJurisdicao()).isNotNull();
        assertThat(request.grauJurisdicao().name()).isEqualTo("SEGUNDO_GRAU");
    }
}
