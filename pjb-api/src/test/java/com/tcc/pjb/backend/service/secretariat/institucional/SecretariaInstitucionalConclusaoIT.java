package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecretariaInstitucionalConclusaoIT extends PjbIntegrationTestBase {

    @Autowired
    TomarCienciaService tomarCienciaService;

    @Autowired
    SecretariaInstitucionalEnfileiramentoService enfileiramentoService;

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    SecretariaInstitucionalItemRepository itemRepository;

    @Autowired
    LotacaoInstituicaoRepository lotacaoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    void concluirUmItemLiberaOParProcessoTipoParaUmNovoRoteamento() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String comarca = "Fortaleza-Conclusao-" + sufixo;
        Instituicao instituicao = instituicaoRepository.save(novaInstituicao(sufixo));
        UnidadeInstituicao unidade = unidadeRepository.save(novaUnidade(instituicao, "Promotoria Conclusao " + sufixo, comarca));
        Long processoId = processoRepository.save(novoProcesso("CONCLUSAO-" + sufixo, comarca)).getId();

        Usuario promotor = usuarioRepository.save(novoPromotor(sufixo));
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(promotor);
        lotacao.setUnidade(unidade);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        lotacaoRepository.save(lotacao);

        SecretariaInstitucionalItem primeiro = enfileiramentoService.enfileirar(processoId, comarca,
                TipoUnidadeInstitucional.PROMOTORIA, MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);
        assertThat(primeiro.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);

        SecretariaInstitucionalItem duplicataAntesDeConcluir = enfileiramentoService.enfileirar(processoId, comarca,
                TipoUnidadeInstitucional.PROMOTORIA, MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);
        assertThat(duplicataAntesDeConcluir)
                .as("enquanto o primeiro item nao for concluido, o indice unico parcial bloqueia um segundo roteamento")
                .isNull();

        tomarCienciaService.concluir(promotor, primeiro.getId());

        SecretariaInstitucionalItem concluido = itemRepository.findById(primeiro.getId()).orElseThrow();
        assertThat(concluido.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.CONCLUIDO);

        SecretariaInstitucionalItem segundo = enfileiramentoService.enfileirar(processoId, comarca,
                TipoUnidadeInstitucional.PROMOTORIA, MotivoEnfileiramentoInstitucional.DESPACHO_VISTA, 15);

        assertThat(segundo)
                .as("concluir o primeiro item precisa liberar o indice unico parcial para um novo roteamento real")
                .isNotNull();
        assertThat(segundo.getId()).isNotEqualTo(primeiro.getId());
        assertThat(segundo.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);

        List<SecretariaInstitucionalItem> itensDoProcesso = itemRepository.findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(unidade.getId())
                .stream().filter(i -> i.getProcessoId().equals(processoId)).toList();
        assertThat(itensDoProcesso).hasSize(2);
    }

    @Test
    void usuarioDeOutraUnidadeNaoConsegueConcluirItemAlheio() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String comarcaDona = "Fortaleza-Intruso-" + sufixo;
        String comarcaIntrusa = "Sobral-Intruso-" + sufixo;
        Instituicao instituicao = instituicaoRepository.save(novaInstituicao(sufixo));
        UnidadeInstituicao unidadeDona = unidadeRepository.save(novaUnidade(instituicao, "Promotoria Dona " + sufixo, comarcaDona));
        UnidadeInstituicao unidadeIntrusa = unidadeRepository.save(novaUnidade(instituicao, "Promotoria Intrusa " + sufixo, comarcaIntrusa));
        Long processoId = processoRepository.save(novoProcesso("CONCLUSAO-INTRUSO-" + sufixo, comarcaDona)).getId();

        Usuario intruso = usuarioRepository.save(novoPromotor("intruso-" + sufixo));
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(intruso);
        lotacao.setUnidade(unidadeIntrusa);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        lotacaoRepository.save(lotacao);

        SecretariaInstitucionalItem item = enfileiramentoService.enfileirar(processoId, comarcaDona,
                TipoUnidadeInstitucional.PROMOTORIA, MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);
        assertThat(item.getUnidadeInstitucionalId()).isEqualTo(unidadeDona.getId());

        assertThatThrownBy(() -> tomarCienciaService.concluir(intruso, item.getId()))
                .isInstanceOf(SecurityException.class);

        SecretariaInstitucionalItem inalterado = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(inalterado.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
    }

    private Instituicao novaInstituicao(String sufixo) {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicao.setNome("MP Conclusao " + sufixo);
        return instituicao;
    }

    private UnidadeInstituicao novaUnidade(Instituicao instituicao, String nome, String comarca) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome(nome);
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setComarca(comarca);
        unidade.setUf("CE");
        return unidade;
    }

    private Processo novoProcesso(String numero, String comarca) {
        return Processo.builder()
                .numeroProcesso(numero)
                .numeroUnificado(numero)
                .tribunal("TJCE")
                .uf("CE")
                .comarca(comarca)
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();
    }

    private Usuario novoPromotor(String sufixo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Promotor Conclusao Teste " + sufixo);
        usuario.setEmail("promotor.conclusao." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf(cpfUnico(sufixo));
        usuario.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        usuario.setPerfil(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }

    private String cpfUnico(String semente) {
        long valor = Math.floorMod((long) semente.hashCode(), 100_000_000_000L);
        return String.format("%011d", valor);
    }
}
