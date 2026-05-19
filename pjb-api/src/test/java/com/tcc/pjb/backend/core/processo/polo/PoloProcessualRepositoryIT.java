package com.tcc.pjb.backend.core.processo.polo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class PoloProcessualRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private PoloProcessualRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'DISTRIBUIDO') RETURNING id",
                Long.class,
                "0001000-" + s + ".2025.8.06.0001");
    }

    private PoloProcessual salvar(TipoPolo tipoPolo, TipoParte tipoParte, String nome, int ordem) {
        PoloProcessual polo = new PoloProcessual(processoId, tipoPolo, tipoParte,
                nome, null, null, null, null, null, null, null, ordem);
        return transactionTemplate.execute(st -> repository.save(polo));
    }

    @Test
    void it01_salvarPoloAtivoRetornaNoFindByProcessoAtivo() {
        salvar(TipoPolo.ATIVO, TipoParte.AUTOR, "João Silva", 0);

        List<PoloProcessual> ativos = repository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getNomeCompleto()).isEqualTo("João Silva");
        assertThat(ativos.get(0).isAtivo()).isTrue();
    }

    @Test
    void it02_findDestinatariosCienciaExcluiAmicusCuriae() {
        salvar(TipoPolo.ATIVO, TipoParte.AUTOR, "Autor Teste", 0);
        salvar(TipoPolo.PASSIVO, TipoParte.REU, "Réu Teste", 0);
        salvar(TipoPolo.AMICUS_CURIAE, TipoParte.TERCEIRO_INTERESSADO, "Amicus", 0);

        List<PoloProcessual> destinatarios = repository.findDestinatariosCiencia(processoId);
        assertThat(destinatarios).hasSize(2);
        assertThat(destinatarios).noneMatch(p -> p.getTipoPolo() == TipoPolo.AMICUS_CURIAE);
    }

    @Test
    void it03_desativarPoloNaoRetornaNoFindAtivo() {
        PoloProcessual polo = salvar(TipoPolo.ATIVO, TipoParte.AUTOR, "Parte Teste", 0);
        transactionTemplate.execute(st -> {
            PoloProcessual carregado = repository.findById(polo.getId()).orElseThrow();
            carregado.desativar();
            return repository.save(carregado);
        });

        List<PoloProcessual> ativos = repository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(ativos).isEmpty();
    }

    @Test
    void it04_litisconsorcioRetornaAmbosOsPolos() {
        salvar(TipoPolo.ATIVO, TipoParte.AUTOR, "Litisconsorte 1", 0);
        salvar(TipoPolo.ATIVO, TipoParte.AUTOR, "Litisconsorte 2", 1);

        List<PoloProcessual> ativos = repository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(ativos).hasSize(2);
        assertThat(ativos).allMatch(p -> p.getTipoPolo() == TipoPolo.ATIVO);
    }
}
