package com.tcc.pjb.backend.core.processo.ciencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class CienciaProcessualRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private CienciaProcessualRepository cienciaRepository;
    @Autowired private ProcessoRepository processoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;
    private Long usuarioId;

    @BeforeEach
    void inserirDadosBase() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        usuarioId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'ADVOGADO', 'hash', ?, true, 'ADVOGADO') RETURNING id",
                Long.class,
                "Teste IT " + sufixo,
                "ciencia.it." + sufixo + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(sufixo, 16))
        );

        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'EM_ANDAMENTO') RETURNING id",
                Long.class,
                "0001000-" + sufixo + ".2025.8.06.0001"
        );
    }

    private CienciaProcessual criarEPersistir(TipoCienciaProcessual tipo,
                                               CanalCiencia canal,
                                               String polo,
                                               Long atoId,
                                               Instant diesAQuo) {
        Instant disponibilizacao = diesAQuo.minus(1, ChronoUnit.HOURS);
        return transactionTemplate.execute(status -> {
            Processo processo = processoRepository.findById(processoId).orElseThrow();
            Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
            CienciaProcessual c = new CienciaProcessual(
                    processo, usuario, tipo, canal,
                    GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                    "COMUM_ORDINARIO", null, polo,
                    null, atoId,
                    processo.getNumeroProcesso(), null,
                    "hashit" + UUID.randomUUID().toString().substring(0, 8),
                    disponibilizacao,
                    null, diesAQuo);
            return cienciaRepository.save(c);
        });
    }

    @Test
    void it01_salvarEBuscarCienciaPendentePorProcessoEStatus() {
        criarEPersistir(TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", null, Instant.now());

        List<CienciaProcessual> pendentes =
                cienciaRepository.findByProcessoIdAndStatus(processoId, StatusCiencia.PENDENTE);

        assertThat(pendentes).isNotEmpty();
        assertThat(pendentes.get(0).getStatus()).isEqualTo(StatusCiencia.PENDENTE);
    }

    @Test
    void it02_confirmarCiencia_comprovanteHashPreenchido() {
        CienciaProcessual salva = criarEPersistir(
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", null, Instant.now());

        Long cienciaId = salva.getId();

        transactionTemplate.execute(status -> {
            CienciaProcessual c = cienciaRepository.findById(cienciaId).orElseThrow();
            c.confirmar("127.0.0.1", "agentHash", "sessHash", Instant.now());
            return cienciaRepository.save(c);
        });

        CienciaProcessual confirmada = cienciaRepository.findById(cienciaId).orElseThrow();
        assertThat(confirmada.getStatus()).isEqualTo(StatusCiencia.CONFIRMADA);
        assertThat(confirmada.getComprovanteHash()).isNotBlank();
    }

    @Test
    void it03_findPendentesExpirados_retornaApenasComDataPassada() {
        Instant diasAQuo = Instant.now().minus(30, ChronoUnit.DAYS);
        criarEPersistir(TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", null, diasAQuo);

        criarEPersistir(TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", null, Instant.now().plus(30, ChronoUnit.DAYS));

        List<CienciaProcessual> expirados = cienciaRepository
                .findPendentesExpirados(Instant.now(), PageRequest.of(0, 50));

        boolean algumComProcessoTeste = expirados.stream()
                .anyMatch(c -> c.getDataExpiracao() != null
                        && c.getDataExpiracao().isBefore(Instant.now()));

        assertThat(algumComProcessoTeste).isTrue();
        expirados.forEach(c ->
                assertThat(c.getDataExpiracao()).isBefore(Instant.now()));
    }

    @Test
    void it04_unicidadeAtoUsuario_segundaInsercaoIdempotente() {
        Long atoId = 77777L + processoId;

        CienciaProcessual primeira = criarEPersistir(
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", atoId, Instant.now());

        CienciaProcessual segunda = cienciaRepository
                .findByAtoProcessualIdAndUsuarioId(atoId, usuarioId)
                .orElse(null);

        assertThat(segunda).isNotNull();
        assertThat(segunda.getId()).isEqualTo(primeira.getId());
    }

    @Test
    void it05_countByProcessoIdAndStatus_retornaContagemCorreta() {
        criarEPersistir(TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                "ATIVO", null, Instant.now());
        criarEPersistir(TipoCienciaProcessual.INTIMACAO_SENTENCA, CanalCiencia.PORTAL_PJB,
                "PASSIVO", null, Instant.now());

        long contagem = cienciaRepository.countByProcessoIdAndStatus(processoId, StatusCiencia.PENDENTE);

        assertThat(contagem).isGreaterThanOrEqualTo(2);
    }
}
