package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SecretariaInstitucionalItemRepositoryIT extends PjbIntegrationTestBase {

    @Autowired
    SecretariaInstitucionalItemRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long processoId1;
    private Long processoId2;
    private Long processoId3;

    @BeforeEach
    void inserirDadosBase() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        processoId1 = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'EM_ANDAMENTO') RETURNING id",
                Long.class,
                "0001000-" + sufixo + ".2025.8.06.0001"
        );

        processoId2 = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'EM_ANDAMENTO') RETURNING id",
                Long.class,
                "0001001-" + sufixo + ".2025.8.06.0001"
        );

        processoId3 = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'EM_ANDAMENTO') RETURNING id",
                Long.class,
                "0001002-" + sufixo + ".2025.8.06.0001"
        );
    }

    private SecretariaInstitucionalItem novoItem(Long processoId, TipoUnidadeInstitucional tipo,
                                                  StatusSecretariaInstitucionalItem status, Instant criadoEm) {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setProcessoId(processoId);
        item.setTipoInstituicaoAlvo(tipo);
        item.setMotivo(MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA);
        item.setStatus(status);
        item.setPrazoBaseDias(15);
        item.setPrazoEmDobro(true);
        item.setCriadoEm(criadoEm);
        return item;
    }

    @Test
    void existePendenteOuEmAnaliseDetectaCorretamente() {
        SecretariaInstitucionalItem item = repository.save(
                novoItem(processoId1, TipoUnidadeInstitucional.PROMOTORIA, StatusSecretariaInstitucionalItem.PENDENTE, Instant.now()));

        boolean existe = repository.existePendenteOuEmAnalise(processoId1, TipoUnidadeInstitucional.PROMOTORIA);
        boolean naoExisteOutroTipo = repository.existePendenteOuEmAnalise(processoId1, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);

        assertThat(existe).isTrue();
        assertThat(naoExisteOutroTipo).isFalse();
        assertThat(item.getId()).isNotNull();
    }

    @Test
    void buscarPendentesSemCienciaAntesDeSoTrazItemVencido() {
        repository.save(novoItem(processoId2, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                StatusSecretariaInstitucionalItem.PENDENTE, Instant.now().minus(15, ChronoUnit.DAYS)));
        repository.save(novoItem(processoId3, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                StatusSecretariaInstitucionalItem.PENDENTE, Instant.now()));

        List<SecretariaInstitucionalItem> vencidos = repository.buscarPendentesSemCienciaAntesDe(Instant.now().minus(10, ChronoUnit.DAYS));

        assertThat(vencidos).extracting(SecretariaInstitucionalItem::getProcessoId).containsExactly(processoId2);
    }
}
