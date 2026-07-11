package com.tcc.pjb.backend.service.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
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
class DistribuicaoProcessoProtocoladoTest extends PjbIntegrationTestBase {

    @Autowired
    private LaianePeticaoInicialDraftService peticaoInicialDraftService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoDistribuicaoCompetenciaRepository distribuicaoCompetenciaRepository;

    @Autowired
    private UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OabValidationClient oabValidationClient;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @Test
    void processoProtocoladoRecebeDistribuicaoMinimaRealPersistida() {
        Usuario advogado = salvarAdvogado();
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(any(), same(advogado))).thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draft = peticaoInicialDraftService.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual",
                "Maria Distribuicao",
                "Empresa Distribuida Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido em Fortaleza"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(12500),
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

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        Processo processoDistribuido = processoRepository.findById(result.processoId()).orElseThrow();
        ProcessoDistribuicaoCompetencia distribuicao = distribuicaoCompetenciaRepository.findTopByProcesso_IdOrderByIdDesc(processo.getId()).orElseThrow();
        UnidadeJudiciariaCompetencia unidade = unidadeJudiciariaCompetenciaRepository.findById(distribuicao.getUnidade().getId()).orElseThrow();

        assertThat(distribuicao.getProcesso().getId()).isEqualTo(processo.getId());
        assertThat(unidade.getCodigo()).isNotBlank();
        assertThat(unidade.getTribunalCodigo()).isNotBlank();
        assertThat(distribuicao.getCriadoEm()).isNotNull();
        assertThat(distribuicao.getMotivacao()).contains("score=");
        assertThat(distribuicao.getStatus()).isEqualTo(StatusDistribuicaoCompetencia.APLICADA);
        assertThat(distribuicao.isDistribuicaoAutomatica()).isTrue();
        assertThat(processoDistribuido.getUnidadeJudiciariaCodigo()).isNotBlank();
        assertThat(processoDistribuido.getVara()).isNotBlank();
        assertThat(processoDistribuido.getTribunalCodigoRoteado()).isNotBlank();
        assertThat(processoDistribuido.getDataDistribuicao()).isNotNull();
        assertThat(processoDistribuido.getPreProtocoloStatus()).contains("DISTRIBUICAO");
        assertThat(processoDistribuido.getCompetenciaTerritorialModo()).isNotBlank();
        assertThat(processoDistribuido.getPreventionMode()).isNotBlank();
    }

    private Usuario salvarAdvogado() {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        String cpfSeed = cpfValido(Math.toIntExact(Math.abs(System.nanoTime() % 800000000L) + 100000000L));
        Usuario usuario = new Usuario();
        usuario.setNome("Dra. Distribuicao Teste");
        usuario.setEmail("distribuicao." + System.nanoTime() + "@test.local");
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

    private String cpfValido(int base) {
        String raiz = String.format("%09d", base);
        int primeiro = digitoCpf(raiz);
        int segundo = digitoCpf(raiz + primeiro);
        return raiz + primeiro + segundo;
    }

    private int digitoCpf(String valor) {
        int peso = valor.length() + 1;
        int soma = 0;
        for (int i = 0; i < valor.length(); i++) {
            soma += Character.digit(valor.charAt(i), 10) * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
