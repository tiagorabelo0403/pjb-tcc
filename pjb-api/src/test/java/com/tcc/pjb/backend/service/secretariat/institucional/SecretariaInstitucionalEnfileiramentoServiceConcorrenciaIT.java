package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecretariaInstitucionalEnfileiramentoServiceConcorrenciaIT extends PjbIntegrationTestBase {

    @Autowired
    SecretariaInstitucionalEnfileiramentoService service;

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    SecretariaInstitucionalItemRepository itemRepository;

    @Test
    void duasChamadasConcorrentesParaOMesmoProcessoETipoNuncaCriamDoisItensAtivos() throws InterruptedException {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.DEFENSORIA_PUBLICA);
        instituicao.setNome("Defensoria Publica Concorrencia");
        instituicao = instituicaoRepository.save(instituicao);

        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome("Nucleo Concorrencia");
        unidade.setTipo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        unidade.setComarca("Fortaleza");
        unidade.setUf("CE");
        unidade = unidadeRepository.save(unidade);

        Long processoId = processoRepository.save(Processo.builder()
                .numeroProcesso("CONCORRENCIA-SECRETARIA-1")
                .numeroUnificado("CONCORRENCIA-SECRETARIA-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build()).getId();
        Runnable tentativa = () -> service.enfileirar(processoId, "Fortaleza", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        Thread t1 = new Thread(tentativa);
        Thread t2 = new Thread(tentativa);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        List<SecretariaInstitucionalItem> itensAtivos = itemRepository.findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(unidade.getId());
        assertThat(itensAtivos).hasSize(1);
    }
}
