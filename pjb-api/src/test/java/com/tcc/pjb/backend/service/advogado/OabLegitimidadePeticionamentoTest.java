package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.valueobject.NumeroProcesso;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.dto.processual.AncoraTerritorial;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoConsultaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.publico.PublicProcessoConsultaService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.scheduling.enabled=false",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false",
        "pjb.integrations.oab.allow-indeterminate-in-non-production=true",
        "pjb.integrations.oab.warn-on-indeterminate-allowed=false"
})
class OabLegitimidadePeticionamentoTest extends PjbIntegrationTestBase {

    @Autowired
    private LaianePeticaoInicialDraftService service;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LaianePeticaoInicialDraftSessionRepository draftRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private PublicProcessoConsultaService publicProcessoConsultaService;

    @Autowired
    private PoloProcessualRepository poloProcessualRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private DocumentoPaginaRepository documentoPaginaRepository;

    @MockitoBean
    private com.tcc.pjb.backend.core.security.CurrentUserService currentUserService;

    @MockitoBean
    private OabValidationClient oabValidationClient;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @Test
    void oabAptaPermitePeticionamentoEPersisteProcesso() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        LaianePeticaoInicialDraftSession draft = salvarDraft(advogado);
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        assertThat(result.processoId()).isNotNull();
        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getId()).isEqualTo(result.processoId());
        assertThat(processo.getUsuario().getId()).isEqualTo(advogado.getId());
        assertThat(processo.getNumeroProcesso()).matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
        assertThat(NumeroProcesso.validar(processo.getNumeroProcesso())).isTrue();
        assertThat(processo.getNumeroUnificado()).isEqualTo(processo.getNumeroProcesso());
        assertThat(processo.getNumeroProcesso()).contains(".8.06.0001");
        PublicProcessoConsultaResponse consulta = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());
        assertThat(consulta.processoId()).isEqualTo(processo.getId());
        assertThat(consulta.numero()).isEqualTo(processo.getNumeroProcesso());
    }

    @Test
    void oabInaptaBloqueiaPeticionamentoENaoPersisteProcesso() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        LaianePeticaoInicialDraftSession draft = salvarDraft(advogado);
        long processosAntes = processoRepository.count();
        long documentosAntes = documentoProcessualRepository.count();
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.inapto("OAB_SUSPENSA", "test"));

        assertThatThrownBy(() -> service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inapta");

        assertThat(processoRepository.count()).isEqualTo(processosAntes);
        LaianePeticaoInicialDraftSession persistido = draftRepository.findById(draft.getId()).orElseThrow();
        assertThat(persistido.getProcesso()).isNull();
        assertThat(persistido.getStatus()).isEqualTo("RASCUNHO");
        assertThat(documentoProcessualRepository.count()).isEqualTo(documentosAntes);
    }

    @Test
    void oabIndeterminadaEmAmbienteDeTestePodePermitirPeticionamento() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        LaianePeticaoInicialDraftSession draft = salvarDraft(advogado);
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.indeterminado("OAB_CNA_INDISPONIVEL", "test"));

        assertThatCode(() -> service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE))))
                .doesNotThrowAnyException();
    }

    @Test
    void defensoriaNaoExigeOabParaPeticionamento() {
        Usuario defensor = salvarUsuario(TipoUsuario.DEFENSOR_PUBLICO, null);
        LaianePeticaoInicialDraftSession draft = salvarDraft(defensor);
        when(currentUserService.getRequired()).thenReturn(defensor);

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        assertThat(result.processoId()).isNotNull();
        verifyNoInteractions(oabValidationClient);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(result.processoId(), true);
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.DEFENSORIA);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.TERCEIRO_INTERESSADO);
            assertThat(polo.getNomeCompleto()).isEqualTo(defensor.getNome());
            assertThat(polo.getUsuarioId()).isEqualTo(defensor.getId());
        });
    }

    @Test
    void peticionamentoPersistePolosAutorReuEAdvogado() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draft = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual com partes qualificadas",
                "Maria Cliente",
                "Empresa Re Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(12000),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria Cliente");
        assertThat(processo.getParteReuNome()).isEqualTo("Empresa Re Ltda");
        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processo.getId(), true);
        assertThat(polos).hasSize(2);
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.ATIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.AUTOR);
            assertThat(polo.getNomeCompleto()).isEqualTo("Maria Cliente");
            assertThat(polo.getUsuarioId()).isEqualTo(advogado.getId());
            assertThat(polo.getOabNumero()).isNotBlank();
            assertThat(polo.getOabUf()).isEqualTo("CE");
        });
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.PASSIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.REU);
            assertThat(polo.getNomeCompleto()).isEqualTo("Empresa Re Ltda");
        });
        PublicProcessoConsultaResponse consulta = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());
        assertThat(consulta.partes().parteAutora()).isEqualTo("Maria Cliente");
        assertThat(consulta.partes().parteReu()).isEqualTo("Empresa Re Ltda");
        DocumentoProcessual recibo = documentoProcessualRepository.findByProcessoId(processo.getId()).stream()
                .filter(documento -> documento.getTitulo() != null && documento.getTitulo().startsWith("Recibo de protocolo"))
                .findFirst()
                .orElseThrow();
        assertThat(recibo.getSha256()).hasSize(64);
        assertThat(recibo.getSha384()).hasSize(96);
        assertThat(recibo.getProtocoloExterno()).isEqualTo(processo.getConnectorProtocolReference());
        assertThat(recibo.getEstadoOperacional()).isEqualTo("RECIBO_PROTOCOLO_EMITIDO");
        List<DocumentoPagina> paginas = documentoPaginaRepository.findByDocumentoId(recibo.getId());
        assertThat(paginas).hasSize(1);
        assertThat(paginas.getFirst().getTextoExtraido()).contains(
                "Numero CNJ: " + processo.getNumeroProcesso(),
                "Tipo de peticao: PETICAO_INICIAL",
                "Parte autora: Maria Cliente",
                "Parte re: Empresa Re Ltda",
                "Hash do conteudo: " + draft.hashIntegridade(),
                "Status: PROTOCOLO_REALIZADO"
        );
    }

    @Test
    void peticionamentoPersisteDomicilioDePartesQuandoInformado() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draft = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual com domicilio de partes",
                "Maria Cliente",
                "Empresa Re Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(9500),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                new EnderecosProcessuaisRequest(
                        new AncoraTerritorial(null, "Fortaleza", "CE"),
                        new AncoraTerritorial(null, "Sao Paulo", "SP"),
                        null,
                        null,
                        false)
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getUfAutor()).isEqualTo("CE");
        assertThat(processo.getComarcaAutor()).isEqualTo("Fortaleza");
        assertThat(processo.getUfReu()).isEqualTo("SP");
        assertThat(processo.getComarcaReu()).isEqualTo("Sao Paulo");
    }

    @Test
    void peticionamentoIgnoraDomicilioDoReuQuandoMarcadoComoDesconhecido() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draft = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual com reu de endereco desconhecido",
                "Maria Cliente",
                "Empresa Re Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(9500),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                new EnderecosProcessuaisRequest(
                        new AncoraTerritorial(null, "Fortaleza", "CE"),
                        new AncoraTerritorial(null, "Sao Paulo", "SP"),
                        null,
                        null,
                        true)
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getUfAutor()).isEqualTo("CE");
        assertThat(processo.getComarcaAutor()).isEqualTo("Fortaleza");
        assertThat(processo.getUfReu()).isNull();
        assertThat(processo.getComarcaReu()).isNull();
    }

    @Test
    void peticionamentoSemDomicilioDePartesContinuaFuncionandoComoAntes() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draft = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual sem domicilio de partes informado",
                "Maria Cliente",
                "Empresa Re Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(9500),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getUfAutor()).isNull();
        assertThat(processo.getComarcaAutor()).isNull();
        assertThat(processo.getUfReu()).isNull();
        assertThat(processo.getComarcaReu()).isNull();
    }

    @Test
    void cidadaoPeticionaEmJuizadoEspecialCivelSemAdvogado() {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, "JUIZADO_ESPECIAL_CIVEL", "Peticao inicial de juizado especial");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        assertThat(result.processoId()).isNotNull();
        verifyNoInteractions(oabValidationClient);
        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo(cidadao.getNome());
        assertThat(processo.getParteAutoraCpf()).isEqualTo(cidadao.getCpf());
    }

    @Test
    void cidadaoEForaDoConjuntoPermitidoBloqueiaProtocolo() {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, "COMUM_ORDINARIO", "Peticao inicial de rito comum");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        assertThatThrownBy(() -> service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE))))
                .isInstanceOf(com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException.class);
    }

    @Test
    void cidadaoNaoConsegueProtocolarEmNomeDeTerceiro() {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, "JUIZADO_ESPECIAL_CIVEL",
                "Joao Terceiro, por intermedio de advogado constituido, apresenta a presente reclamacao em face de Empresa Re Ltda.");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo(cidadao.getNome());
        assertThat(processo.getParteAutoraNome()).isNotEqualTo("Joao Terceiro");
    }

    @Test
    void cidadaoNaoConsegueEstruturarRascunhoEmNomeDeTerceiro() {
        Usuario cidadao = salvarCidadao();
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService.DraftView draft = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Reclamacao trabalhista de juizado",
                "Joao Terceiro",
                "Empresa Re Ltda",
                "CIVIL",
                "JUIZADO_ESPECIAL_CIVEL",
                "RECLAMACAO",
                List.of("Fato relevante"),
                List.of("Fundamento aplicavel"),
                List.of("Pedido de condenacao"),
                List.of("Prova documental"),
                BigDecimal.valueOf(3000),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ));

        assertThat(draft.minutaInicial()).contains(cidadao.getNome());
        assertThat(draft.minutaInicial()).doesNotContain("Joao Terceiro");
    }

    @Test
    void cidadaoComProcessoIdDeTerceiroEBloqueadoAntesDeCarregarOProcesso() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 12345");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftService.DraftView draftDoAdvogado = service.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca de terceiro",
                "Cliente do Advogado",
                "Empresa Re Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(9500),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ));
        LaianePeticaoInicialDraftService.ProtocolarResult protocoloDeTerceiro = service.protocolar(draftDoAdvogado.id(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));
        Long processoIdDeTerceiro = protocoloDeTerceiro.processoId();

        Usuario cidadao = salvarCidadao();
        when(currentUserService.getRequired()).thenReturn(cidadao);

        assertThatThrownBy(() -> service.estruturar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                processoIdDeTerceiro,
                "Tentativa de referenciar processo alheio",
                null,
                null,
                "CIVIL",
                "JUIZADO_ESPECIAL_CIVEL",
                "RECLAMACAO",
                List.of("Fato relevante"),
                List.of("Fundamento aplicavel"),
                List.of("Pedido de condenacao"),
                List.of("Prova documental"),
                BigDecimal.valueOf(3000),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ))).isInstanceOf(com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException.class);
    }

    @ParameterizedTest
    @EnumSource(value = RitoProcessual.class, names = {
            "TRABALHISTA_ORDINARIO",
            "TRABALHISTA_SUMARISSIMO",
            "TRABALHISTA_SUMARIO_ALCADA",
            "TRABALHISTA_ACAO_CUMPRIMENTO",
            "TRABALHISTA_CUMPRIMENTO_SENTENCA",
            "TRABALHISTA_EXECUCAO",
            "TRABALHISTA_ACIDENTE_TRABALHO"
    })
    void cidadaoPeticionaEmRitoTrabalhistaPermitidoSemAdvogado(RitoProcessual rito) {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, rito.name(), "Peticao inicial trabalhista de jus postulandi");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE,
                        com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.CTPS)));

        assertThat(result.processoId()).isNotNull();
        verifyNoInteractions(oabValidationClient);
        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo(cidadao.getNome());
        assertThat(processo.getParteAutoraCpf()).isEqualTo(cidadao.getCpf());
    }

    @ParameterizedTest
    @EnumSource(value = RitoProcessual.class, names = {
            "JUIZADO_ESPECIAL_FEDERAL",
            "PREVIDENCIARIO_JEF"
    })
    void cidadaoPeticionaEmJuizadoEspecialFederalSemAdvogado(RitoProcessual rito) {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, rito.name(), "Peticao inicial de juizado especial federal");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("FEDERAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE)));

        assertThat(result.processoId()).isNotNull();
        verifyNoInteractions(oabValidationClient);
        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo(cidadao.getNome());
        assertThat(processo.getParteAutoraCpf()).isEqualTo(cidadao.getCpf());
    }

    @ParameterizedTest
    @EnumSource(value = RitoProcessual.class, names = {
            "TRABALHISTA_ACAO_RESCISORIA",
            "TRABALHISTA_MANDADO_SEGURANCA",
            "TRABALHISTA_TUTELA_CAUTELAR",
            "TRABALHISTA_DISSIDIO_COLETIVO",
            "TRABALHISTA_INQUERITO_FALTA_GRAVE"
    })
    void cidadaoEmRitoTrabalhistaExcluidoBloqueiaProtocolo(RitoProcessual rito) {
        Usuario cidadao = salvarCidadao();
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(cidadao, rito.name(), "Peticao inicial trabalhista fora do jus postulandi");
        when(currentUserService.getRequired()).thenReturn(cidadao);

        assertThatThrownBy(() -> service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE))))
                .isInstanceOf(com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException.class);
    }

    @Test
    void advogadoEmRitoTrabalhistaContinuaProtocolandoNormalmente() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "OAB/CE 98765");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(advogado)))
                .thenReturn(OabValidationResult.apto("test"));
        LaianePeticaoInicialDraftSession draft = salvarDraftComRito(advogado, "TRABALHISTA_ORDINARIO",
                "Joao Reclamante, por intermédio de advogado constituido, apresenta a presente reclamacao em face de Empresa Re Ltda.");

        LaianePeticaoInicialDraftService.ProtocolarResult result = service.protocolar(draft.getId(), new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL", null, java.util.Set.of(),
                java.util.List.of(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.DOCUMENTO_IDENTIDADE,
                        com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento.CTPS)));

        assertThat(result.processoId()).isNotNull();
        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getParteAutoraNome()).isEqualTo("Joao Reclamante");
        assertThat(processo.getParteAutoraCpf()).isNull();
    }

    private Usuario salvarCidadao() {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        Usuario usuario = new Usuario();
        usuario.setNome("Cidadao Teste");
        usuario.setEmail("cidadao." + System.nanoTime() + "@test.local");
        usuario.setCpf(String.valueOf(Math.abs(System.nanoTime())).replaceAll("\\D", "").substring(0, 11));
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        usuario.setPerfil(TipoUsuario.CIDADAO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuarioRepository.save(usuario);
    }

    private LaianePeticaoInicialDraftSession salvarDraftComRito(Usuario solicitante, String rito, String minuta) {
        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setSolicitante(solicitante);
        draft.setTituloCaso("Peticao de juizado");
        draft.setRamoDireito("CIVIL");
        draft.setRitoSugerido(rito);
        draft.setClasseSugerida("RECLAMACAO");
        draft.setUrgenciaScore(10);
        draft.setReadinessScore(90);
        draft.setFatosJson("[\"Fato relevante\"]");
        draft.setPedidosJson("[\"Pedido de condenacao\"]");
        draft.setFundamentosJson("[\"Fundamento aplicavel\"]");
        draft.setProvasJson("[\"Prova documental\"]");
        draft.setChecklistJson("[\"Documento de identificacao\"]");
        draft.setMinutaInicial(minuta);
        draft.setHashIntegridade("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        draft.setStatus("RASCUNHO");
        return draftRepository.save(draft);
    }

    private Usuario salvarUsuario(TipoUsuario tipo, String oab) {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        String oabEfetiva = oab == null ? null : "OAB/CE " + unique;
        Usuario usuario = new Usuario();
        usuario.setNome(tipo == TipoUsuario.ADVOGADO ? "Dra. Ana Teste" : "Defensora Teste");
        usuario.setEmail(tipo.name().toLowerCase(java.util.Locale.ROOT) + "." + System.nanoTime() + "@test.local");
        usuario.setCpf(String.valueOf(Math.abs(System.nanoTime())).replaceAll("\\D", "").substring(0, 11));
        usuario.setTipoUsuario(tipo);
        usuario.setPerfil(tipo.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setOab(oabEfetiva);
        usuario.setOabUf("CE");
        usuario.setOabNormalizada(oab == null ? null : "CE-" + unique);
        return usuarioRepository.save(usuario);
    }

    private LaianePeticaoInicialDraftSession salvarDraft(Usuario solicitante) {
        LaianePeticaoInicialDraftSession draft = new LaianePeticaoInicialDraftSession();
        draft.setSolicitante(solicitante);
        draft.setTituloCaso("Cobranca contratual");
        draft.setRamoDireito("CIVIL");
        draft.setRitoSugerido("COMUM_ORDINARIO");
        draft.setClasseSugerida("ACAO_DE_COBRANCA");
        draft.setUrgenciaScore(10);
        draft.setReadinessScore(90);
        draft.setFatosJson("[\"Contrato inadimplido\"]");
        draft.setPedidosJson("[\"Condenacao ao pagamento\"]");
        draft.setFundamentosJson("[\"Obrigacao contratual vencida\"]");
        draft.setProvasJson("[\"Contrato assinado\"]");
        draft.setChecklistJson("[\"Documento de identificacao\"]");
        draft.setMinutaInicial("Peticao inicial de cobranca contratual");
        draft.setHashIntegridade("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        draft.setStatus("RASCUNHO");
        return draftRepository.save(draft);
    }
}
