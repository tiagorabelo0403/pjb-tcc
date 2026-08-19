package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SecretariaInstitucionalReprocessamentoEntidadeSujaIT extends PjbIntegrationTestBase {

    @Autowired
    UnidadeInstitucionalAdminService adminService;

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    SecretariaInstitucionalItemRepository itemRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void criarUnidadeNaoFalhaPorItemDuplicadoQueDeveriaContinuarPresoNaoFicarSujoNaTransacaoExterna() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String comarcaAlvo = "Itapipoca-" + sufixo;
        String comarcaDaNovaUnidadeIrrelevante = "Sobral-" + sufixo;

        Instituicao instituicaoNova = new Instituicao();
        instituicaoNova.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicaoNova.setNome("Ministerio Publico Reprocessamento " + sufixo);
        Instituicao instituicao = instituicaoRepository.save(instituicaoNova);

        // unidadeAlvo commitada num par criarUnidade/reprocessar proprio, ANTES do cenario de
        // corrida abaixo — este teste isola especificamente o comportamento do Important #1
        // (reverter a mutacao em memoria quando o INDICE UNICO conflita contra uma unidade ja
        // existente e commitada); o cenario em que a unidade alvo e a MESMA recem-criada na
        // mesma leva de reprocessamento é o outro teste desta classe
        // (`criarUnidadeResolveBacklogDeVerdadeQuandoAUnidadeNovaEAQueOsItensPresosEsperavam`).
        adminService.criarUnidade(instituicao.getId(), "Promotoria de Itapipoca " + sufixo,
                TipoUnidadeInstitucional.PROMOTORIA, comarcaAlvo, "CE");

        Long processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo, comarca) "
                        + "VALUES (?, 'EM_ANDAMENTO', ?) RETURNING id",
                Long.class,
                "0009000-" + sufixo + ".2025.8.06.0001",
                comarcaAlvo);

        // Simula o estado de corrida real: duas linhas SEM_UNIDADE_RESOLVIDA presas para o
        // mesmo (processo, tipo) — cenario que o Important #2 passou a impedir para novos
        // enfileiramentos, mas que ainda pode existir num volume que ja tinha itens presos
        // antes da correcao. reprocessarSemUnidade precisa resolver a primeira e devolver a
        // segunda intacta, sem contaminar a transacao de quem chamou (criarUnidade).
        Long itemResolvivelId = jdbcTemplate.queryForObject(
                "INSERT INTO secretaria_institucional_item "
                        + "(processo_id, tipo_instituicao_alvo, motivo, status, prazo_base_dias, prazo_em_dobro, criado_em) "
                        + "VALUES (?, 'PROMOTORIA', 'PARTE_AUTOMATICA', 'SEM_UNIDADE_RESOLVIDA', 15, false, now()) RETURNING id",
                Long.class,
                processoId);
        Long itemConflitanteId = jdbcTemplate.queryForObject(
                "INSERT INTO secretaria_institucional_item "
                        + "(processo_id, tipo_instituicao_alvo, motivo, status, prazo_base_dias, prazo_em_dobro, criado_em) "
                        + "VALUES (?, 'PROMOTORIA', 'PARTE_AUTOMATICA', 'SEM_UNIDADE_RESOLVIDA', 15, false, now()) RETURNING id",
                Long.class,
                processoId);

        // Este segundo cadastro (unidade nova, comarca DIFERENTE da comarcaAlvo) e o que, seguido
        // do reprocessamento explicito (mesma sequencia que o controller real executa: duas
        // chamadas de bean separadas, cada uma na sua propria transacao), dispara
        // reprocessarSemUnidade(PROMOTORIA) — mas o alvo de resolucao dos dois itens presos e a
        // unidadeAlvo, ja commitada antes, entao o REQUIRES_NEW do gravador enxerga ela normalmente.
        var unidadeSobral = adminService.criarUnidade(instituicao.getId(), "Promotoria de Sobral " + sufixo,
                TipoUnidadeInstitucional.PROMOTORIA, comarcaDaNovaUnidadeIrrelevante, "CE");
        assertThatCode(() -> adminService.reprocessarBacklogAposCriacaoDeUnidade(unidadeSobral))
                .as("reprocessar nao pode falhar por causa de uma entidade suja deixada por reprocessarSemUnidade")
                .doesNotThrowAnyException();

        SecretariaInstitucionalItem primeiro = itemRepository.findById(itemResolvivelId).orElseThrow();
        SecretariaInstitucionalItem segundo = itemRepository.findById(itemConflitanteId).orElseThrow();
        List<SecretariaInstitucionalItem> ambos = List.of(primeiro, segundo);

        long resolvidos = ambos.stream().filter(i -> i.getStatus() == StatusSecretariaInstitucionalItem.PENDENTE).count();
        long presos = ambos.stream().filter(i -> i.getStatus() == StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA).count();
        assertThat(resolvidos)
                .as("exatamente um dos dois itens duplicados deve ter sido resolvido — o indice unico so permite um PENDENTE por (processo, tipo)")
                .isEqualTo(1);
        assertThat(presos)
                .as("o item que perdeu a corrida do indice unico precisa continuar preso, nao suja/PENDENTE fantasma")
                .isEqualTo(1);
        ambos.stream().filter(i -> i.getStatus() == StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)
                .forEach(i -> assertThat(i.getUnidadeInstitucionalId()).isNull());
        ambos.stream().filter(i -> i.getStatus() == StatusSecretariaInstitucionalItem.PENDENTE)
                .forEach(i -> assertThat(i.getUnidadeInstitucionalId()).isNotNull());

        List<SecretariaInstitucionalItem> pendentesDoProcesso = itemRepository.findByStatus(StatusSecretariaInstitucionalItem.PENDENTE)
                .stream().filter(i -> i.getProcessoId().equals(processoId)).toList();
        assertThat(pendentesDoProcesso).hasSize(1);
    }

    @Test
    void criarUnidadeResolveBacklogDeVerdadeQuandoAUnidadeNovaEAQueOsItensPresosEsperavam() {
        // Cenario real da Task 10, deliberadamente evitado no teste acima: a unidade recem-criada
        // e o proprio alvo de resolucao do backlog. Antes da correcao (UnidadeInstitucionalAdminService
        // separado em criarUnidade + reprocessarBacklogAposCriacaoDeUnidade, chamados em sequencia
        // pelo controller, cada um com sua propria transacao de topo), este cenario reproduzia
        // 100% das vezes uma violacao de FK: o gravador REQUIRES_NEW nao enxergava a unidade ainda
        // nao commitada na mesma transacao que a criou. Agora criarUnidade commita sozinho antes de
        // reprocessarBacklogAposCriacaoDeUnidade comecar, entao o REQUIRES_NEW ve a unidade normalmente.
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String comarcaAlvo = "Itapipoca-real-" + sufixo;

        Instituicao instituicaoNova = new Instituicao();
        instituicaoNova.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicaoNova.setNome("Ministerio Publico Reprocessamento Real " + sufixo);
        Instituicao instituicao = instituicaoRepository.save(instituicaoNova);

        Long processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo, comarca) "
                        + "VALUES (?, 'EM_ANDAMENTO', ?) RETURNING id",
                Long.class,
                "0009100-" + sufixo + ".2025.8.06.0001",
                comarcaAlvo);

        Long itemPresoId = jdbcTemplate.queryForObject(
                "INSERT INTO secretaria_institucional_item "
                        + "(processo_id, tipo_instituicao_alvo, motivo, status, prazo_base_dias, prazo_em_dobro, criado_em) "
                        + "VALUES (?, 'PROMOTORIA', 'PARTE_AUTOMATICA', 'SEM_UNIDADE_RESOLVIDA', 15, false, now()) RETURNING id",
                Long.class,
                processoId);

        // Mesma sequencia que o controller real executa: duas chamadas de bean separadas, nao
        // uma orquestrando a outra por self-invocation.
        var unidadeCriada = adminService.criarUnidade(instituicao.getId(), "Promotoria de Itapipoca Real " + sufixo,
                TipoUnidadeInstitucional.PROMOTORIA, comarcaAlvo, "CE");
        assertThatCode(() -> adminService.reprocessarBacklogAposCriacaoDeUnidade(unidadeCriada))
                .as("reprocessar apos a criacao ja commitada nao pode falhar por FK invisivel")
                .doesNotThrowAnyException();

        SecretariaInstitucionalItem resolvido = itemRepository.findById(itemPresoId).orElseThrow();
        assertThat(resolvido.getStatus())
                .as("o item que esperava exatamente esta unidade precisa ser resolvido de verdade, nao continuar preso")
                .isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
        assertThat(resolvido.getUnidadeInstitucionalId()).isEqualTo(unidadeCriada.getId());
    }
}
