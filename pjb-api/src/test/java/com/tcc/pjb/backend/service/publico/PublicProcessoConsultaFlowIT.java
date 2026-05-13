package com.tcc.pjb.backend.service.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.cache.type=none")
class PublicProcessoConsultaFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @Autowired
    private PublicProcessoConsultaService publicProcessoConsultaService;

    @Test
    void deveRetornarResumoPublicoComMovimentacoesLimitadas() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CP-DET-2026-01")
                .numeroUnificado("0007101-11.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Procedimento comum")
                .assunto("Obrigação de fazer")
                .parteAutoraNome("João Pereira")
                .parteReuNome("Município de Quixadá")
                .tribunal("TJCE")
                .comarca("Quixadá")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 8, 0))
                .build());

        Instant base = Instant.parse("2026-04-16T10:00:00Z");
        for (int i = 0; i < 15; i++) {
            movimentacaoProcessualRepository.save(MovimentacaoProcessual.builder()
                    .processo(processo)
                    .faseDe(FaseProcessual.CONHECIMENTO)
                    .fasePara(FaseProcessual.INSTRUTORIA)
                    .descricao("Movimentação pública " + i)
                    .dataMovimentacao(base.minus(i, ChronoUnit.HOURS))
                    .build());
        }

        var response = publicProcessoConsultaService.consultarPorNumero("0007101-11.2026.8.06.0001");

        assertThat(response.acessoRestrito()).isFalse();
        assertThat(response.partes()).isNotNull();
        assertThat(response.partes().parteAutora()).isEqualTo("João Pereira");
        assertThat(response.partes().parteReu()).isEqualTo("Município de Quixadá");
        assertThat(response.movimentacoes()).hasSize(12);
        assertThat(response.movimentacoes().getFirst().descricao()).isEqualTo("Movimentação pública 0");
        assertThat(response.movimentacoes().getLast().descricao()).isEqualTo("Movimentação pública 11");
    }

    @Test
    void deveMascararResumoQuandoProcessoExigeCredencial() {
        processoRepository.save(Processo.builder()
                .numeroProcesso("CP-DET-2026-02")
                .numeroUnificado("0007102-22.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.FAMILIA)
                .classeProcessual("Ação de guarda")
                .assunto("Guarda unilateral")
                .parteAutoraNome("Pessoa Autora")
                .parteReuNome("Pessoa Ré")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        var response = publicProcessoConsultaService.consultarPorNumero("0007102-22.2026.8.06.0001");

        assertThat(response.acessoRestrito()).isTrue();
        assertThat(response.aviso()).contains("Acesso restrito");
        assertThat(response.orientacaoAcesso()).contains("advogados habilitados");
        assertThat(response.partes()).isNull();
        assertThat(response.movimentacoes()).isEmpty();
        assertThat(response.documentos()).isEmpty();
    }

    @Test
    void deveOcultarProcessoComRestricaoMaxima() {
        processoRepository.save(Processo.builder()
                .numeroProcesso("CP-DET-2026-03")
                .numeroUnificado("0007103-33.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.PENAL)
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.SEGREDO_ESTADO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());

        assertThatThrownBy(() -> publicProcessoConsultaService.consultarPorNumero("0007103-33.2026.8.06.0001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Processo");
    }
}
