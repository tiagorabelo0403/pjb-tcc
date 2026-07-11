package com.tcc.pjb.backend.service.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoConsultaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.scheduling.enabled=false",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false",
        "pjb.integrations.oab.warn-on-indeterminate-allowed=false"
})
class ConsultaPublicaProcessoProtocoladoTest extends PjbIntegrationTestBase {

    @Autowired
    private LaianePeticaoInicialDraftService peticaoInicialDraftService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private PublicProcessoConsultaService publicProcessoConsultaService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OabValidationClient oabValidationClient;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @Test
    void processoProtocoladoApareceNaConsultaPublicaPorCnjFormatadoEDigitos() {
        Processo processo = protocolarProcesso("Carla Autora", "Loja Re Ltda");
        movimentacaoProcessualRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(FaseProcessual.AUTUACAO)
                .fasePara(FaseProcessual.CONHECIMENTO)
                .descricao("Peticao inicial protocolada e autuada")
                .build());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.saveAndFlush(processo);

        PublicProcessoConsultaResponse consultaFormatada = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());
        PublicProcessoConsultaResponse consultaSemMascara = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso().replaceAll("\\D", ""));

        assertThat(consultaFormatada.processoId()).isEqualTo(processo.getId());
        assertThat(consultaSemMascara.processoId()).isEqualTo(processo.getId());
        assertThat(consultaFormatada.numero()).isEqualTo(processo.getNumeroProcesso());
        assertThat(consultaFormatada.acessoRestrito()).isFalse();
        assertThat(consultaFormatada.partes().parteAutora()).isEqualTo("Carla Autora");
        assertThat(consultaFormatada.partes().parteReu()).isEqualTo("Loja Re Ltda");
        assertThat(consultaFormatada.movimentacoes()).anySatisfy(movimentacao -> {
            assertThat(movimentacao.faseDe()).isEqualTo(FaseProcessual.AUTUACAO.name());
            assertThat(movimentacao.fasePara()).isEqualTo(FaseProcessual.CONHECIMENTO.name());
            assertThat(movimentacao.descricao()).isEqualTo("Peticao inicial protocolada e autuada");
        });
        assertThat(documentoProcessualRepository.countByProcesso_Id(processo.getId())).isGreaterThan(0);
        assertThat(consultaFormatada.documentos()).isEmpty();
    }

    @Test
    void processoComSigiloNaoVazaPartesMovimentacoesOuDocumentos() {
        Processo processo = protocolarProcesso("Pessoa Autora Sigilosa", "Pessoa Re Sigilosa");
        movimentacaoProcessualRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(FaseProcessual.AUTUACAO)
                .fasePara(FaseProcessual.CONHECIMENTO)
                .descricao("Movimentacao interna sigilosa")
                .build());
        processo.setNivelSigilo(NivelSigilo.SIGILO_N2);
        processoRepository.saveAndFlush(processo);

        PublicProcessoConsultaResponse consulta = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());

        assertThat(consulta.processoId()).isEqualTo(processo.getId());
        assertThat(consulta.acessoRestrito()).isTrue();
        assertThat(consulta.partes()).isNull();
        assertThat(consulta.movimentacoes()).isEmpty();
        assertThat(consulta.documentos()).isEmpty();
        assertThat(consulta.aviso()).isNotBlank();
        assertThat(consulta.orientacaoAcesso()).isNotBlank();
    }

    private Processo protocolarProcesso(String autora, String reu) {
        Usuario advogado = salvarAdvogado();
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(any(), same(advogado))).thenReturn(OabValidationResult.apto("test"));

        LaianePeticaoInicialDraftService.DraftView draft = peticaoInicialDraftService.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual",
                autora,
                reu,
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(8700),
                false,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult result = peticaoInicialDraftService.protocolar(draft.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));
        return processoRepository.findById(result.processoId()).orElseThrow();
    }

    private Usuario salvarAdvogado() {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        String cpfSeed = (String.valueOf(Math.abs(System.nanoTime())) + "00000000000").substring(0, 11);
        Usuario usuario = new Usuario();
        usuario.setNome("Dra. Consulta Publica");
        usuario.setEmail("consulta.publica." + System.nanoTime() + "@test.local");
        usuario.setCpf(cpfSeed);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setOab("OAB/CE " + unique);
        usuario.setOabUf("CE");
        usuario.setOabNormalizada("CE-" + unique);
        return usuarioRepository.save(usuario);
    }
}
