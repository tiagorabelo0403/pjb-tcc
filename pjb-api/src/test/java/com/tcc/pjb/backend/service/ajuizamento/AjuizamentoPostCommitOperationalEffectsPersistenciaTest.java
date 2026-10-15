package com.tcc.pjb.backend.service.ajuizamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.identity.ProntuarioNacionalEntrada;
import com.tcc.pjb.backend.model.entity.painel.PainelTribunalMetrica;
import com.tcc.pjb.backend.model.repository.IdentidadeJuridicaNacionalRepository;
import com.tcc.pjb.backend.model.repository.PainelMateriaMetricaRepository;
import com.tcc.pjb.backend.model.repository.PainelSerieTemporalDiariaRepository;
import com.tcc.pjb.backend.model.repository.PainelTribunalMetricaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.ProntuarioNacionalEntradaRepository;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        AjuizamentoPostCommitOperationalEffectsService.class,
        PainelNacionalJusticaService.class,
        ProntuarioNacionalService.class,
        DocumentoNacionalValidator.class,
        com.tcc.pjb.backend.core.infra.spring.SpringContext.class,
        com.tcc.pjb.backend.core.security.crypto.CryptoVaultService.class,
        com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AjuizamentoPostCommitOperationalEffectsPersistenciaTest {

    private static final String NUMERO = "0009876-54.2026.8.06.0001";
    private static final String CPF_AUTOR = cpfComDigitosVerificadores("123456789");

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private PainelTribunalMetricaRepository painelTribunalRepository;

    @Autowired
    private PainelMateriaMetricaRepository painelMateriaRepository;

    @Autowired
    private PainelSerieTemporalDiariaRepository painelSerieRepository;

    @MockitoBean
    private AjuizamentoService ajuizamentoService;

    @Autowired
    private ProntuarioNacionalEntradaRepository prontuarioRepository;

    @Autowired
    private IdentidadeJuridicaNacionalRepository identidadeRepository;

    @MockitoBean
    private IdentidadeJuridicaNacionalService identidadeService;

    @MockitoBean
    private FederalismoJudicialEngine federalismoJudicialEngine;

    @MockitoBean
    private RadarPadroesService radarPadroesService;

    @MockitoBean
    private AuditLedgerService auditLedgerService;

    @MockitoBean
    private OutboxPublisher outboxPublisher;

    @TestConfiguration
    static class Json {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @AfterEach
    void limpar() {
        prontuarioRepository.deleteAll();
        identidadeRepository.deleteAll();
        painelSerieRepository.deleteAll();
        painelMateriaRepository.deleteAll();
        painelTribunalRepository.deleteAll();
        processoRepository.findAll().stream()
                .filter(processo -> NUMERO.equals(processo.getNumeroProcesso()))
                .forEach(processoRepository::delete);
    }

    @Test
    void painelNacionalContaOAjuizamentoDepoisDoCommit() {
        ajuizarComCommit();

        assertThat(painelTribunalRepository.findByCodigoTribunal("ESTADUAL")).get()
                .extracting(PainelTribunalMetrica::getAjuizadosHoje)
                .isEqualTo(1L);
    }

    @Test
    void prontuarioNacionalRegistraOAutorDoProcessoAjuizadoDepoisDoCommit() {
        ajuizarComCommit();

        assertThat(prontuarioRepository.findAllByNupn(NUMERO))
                .singleElement()
                .satisfies(entrada -> {
                    assertThat(entrada.getPolo()).isEqualTo(ProntuarioNacionalEntrada.PoloProcessual.ATIVO);
                    assertThat(entrada.getQualificacao()).isEqualTo(ProntuarioNacionalEntrada.QualificacaoProcessual.AUTOR);
                    assertThat(entrada.getDocumento()).isEqualTo(CPF_AUTOR);
                });
    }

    private void ajuizarComCommit() {
        IdentidadeJuridicaNacional identidade = identidadeRepository.saveAndFlush(new IdentidadeJuridicaNacional(
                UUID.randomUUID(), DocumentoNacionalValidator.TipoDocumento.CPF, CPF_AUTOR, "hash-" + CPF_AUTOR,
                CPF_AUTOR, "AUTOR AJUIZAMENTO", "autor ajuizamento", "pjb://prontuario/" + CPF_AUTOR,
                IdentidadeJuridicaNacional.OrigemCadastro.TRIBUNAL));
        when(identidadeService.resolverOuCriarPorDocumento(any(), any(), any(), any())).thenReturn(identidade);
        when(ajuizamentoService.carregarProcesso(anyLong()))
                .thenAnswer(chamada -> processoRepository.findById(chamada.getArgument(0)).orElseThrow());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Processo processo = processoRepository.saveAndFlush(processoAjuizado());
            publisher.publishEvent(ProcessoAjuizadoEvent.from(processo, List.of()));
        });
    }

    private static Processo processoAjuizado() {
        Processo processo = new Processo();
        processo.setNumeroProcesso(NUMERO);
        processo.setNumeroUnificado(NUMERO);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setClasseProcessual("Procedimento Comum Civel");
        processo.setAssunto("Indenizacao por dano moral");
        processo.setParteAutoraNome("Autor Ajuizamento");
        processo.setParteAutoraCpf(CPF_AUTOR);
        processo.setParteReuNome("Reu Ajuizamento");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setDataCriacao(LocalDateTime.now());
        return processo;
    }

    private static String cpfComDigitosVerificadores(String base) {
        String comPrimeiro = base + digitoVerificador(base);
        return comPrimeiro + digitoVerificador(comPrimeiro);
    }

    private static int digitoVerificador(String digitos) {
        int soma = 0;
        for (int i = 0; i < digitos.length(); i++) {
            soma += (digitos.charAt(i) - '0') * (digitos.length() + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
