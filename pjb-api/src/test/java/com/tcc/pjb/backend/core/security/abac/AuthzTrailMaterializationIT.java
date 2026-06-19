package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class AuthzTrailMaterializationIT extends PjbIntegrationTestBase {

    @Autowired
    private PjbAuthorizationTrailReadModelService trailReadModelService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparTabelas() {
        jdbc.execute("DELETE FROM tb_authz_trail_dedup");
        jdbc.execute("DELETE FROM tb_authz_trail_read_model");
    }

    @Test
    void deveMaterializarEPersistirNaParticaoCorreta() {
        PjbAuthorizationDecisionTrail trail = trailUnico("particao-01");

        PjbAuthorizationTrailSnapshot resultado = trailReadModelService.materializeReturningSnapshot(trail);

        assertThat(resultado).isNotNull();

        Instant agora = Instant.now();
        ZonedDateTime zdt = agora.atZone(ZoneOffset.UTC);
        int mesEsperado = zdt.getYear() * 100 + zdt.getMonthValue();
        String particao = String.format("tb_authz_trail_y%04dm%02d", zdt.getYear(), zdt.getMonthValue());

        Integer countTrail = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_read_model", Integer.class);
        assertThat(countTrail).isEqualTo(1);

        Integer countDedup = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_dedup", Integer.class);
        assertThat(countDedup).isEqualTo(1);

        Integer ocurredMonthNoDb = jdbc.queryForObject(
                "SELECT occurred_month FROM tb_authz_trail_read_model LIMIT 1", Integer.class);
        assertThat(ocurredMonthNoDb).isEqualTo(mesEsperado);

        Integer countNaParticao = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + particao, Integer.class);
        assertThat(countNaParticao).isEqualTo(1);
    }

    @Test
    void deveRejeitarDuplicataNoMesmoMes() {
        PjbAuthorizationDecisionTrail trail = trailUnico("dedup-mesmo-mes");

        PjbAuthorizationTrailSnapshot primeiro = trailReadModelService.materializeReturningSnapshot(trail);
        PjbAuthorizationTrailSnapshot segundo = trailReadModelService.materializeReturningSnapshot(trail);

        assertThat(primeiro).isNotNull();
        assertThat(segundo).isNull();

        Integer countTrail = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_read_model", Integer.class);
        assertThat(countTrail).isEqualTo(1);

        Integer countDedup = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_dedup", Integer.class);
        assertThat(countDedup).isEqualTo(1);
    }

    @Test
    void deveRejeitarDuplicataCrossDateViaDedup() {
        PjbAuthorizationDecisionTrail trail = trailUnico("cross-date");
        String hashDoTrail = trail.payloadHash();

        jdbc.update(
                "INSERT INTO tb_authz_trail_dedup (payload_hash, trail_id, occurred_month) VALUES (?, ?, ?)",
                hashDoTrail, 9999L, 202412);

        PjbAuthorizationTrailSnapshot resultado = trailReadModelService.materializeReturningSnapshot(trail);

        assertThat(resultado).isNull();

        Integer countTrail = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_read_model", Integer.class);
        assertThat(countTrail).isEqualTo(0);

        Integer countDedup = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_dedup", Integer.class);
        assertThat(countDedup).isEqualTo(1);
    }

    @Test
    void deveSuportarConcorrencia20ThreadsSemTOCTOU() throws InterruptedException {
        PjbAuthorizationDecisionTrail trail = trailUnico("concorrencia-20");

        CountDownLatch prontos = new CountDownLatch(20);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(() -> {
                prontos.countDown();
                try {
                    largada.await();
                    PjbAuthorizationTrailSnapshot r = trailReadModelService.materializeReturningSnapshot(trail);
                    if (r != null) {
                        sucessos.incrementAndGet();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            t.start();
            threads.add(t);
        }

        prontos.await();
        largada.countDown();

        for (Thread t : threads) {
            t.join(15_000);
        }

        assertThat(sucessos.get()).isEqualTo(1);

        Integer countTrail = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_read_model", Integer.class);
        assertThat(countTrail).isEqualTo(1);

        Integer countDedup = jdbc.queryForObject("SELECT COUNT(*) FROM tb_authz_trail_dedup", Integer.class);
        assertThat(countDedup).isEqualTo(1);
    }

    @Test
    void deveFiltrarPorOccurredMonthNoSearchWindow() {
        PjbAuthorizationDecisionTrail trail = trailUnico("window-filter");
        trailReadModelService.materializeReturningSnapshot(trail);

        Instant agora = Instant.now();
        Instant inicio = agora.minusSeconds(60);
        Instant fim = agora.plusSeconds(60);

        List<PjbAuthorizationTrailSnapshot> resultados = trailReadModelService.searchWindow(inicio, fim);
        assertThat(resultados).hasSize(1);

        Instant inicioVelho = agora.minusSeconds(86400 * 365 * 2);
        Instant fimVelho = agora.minusSeconds(86400 * 365);
        List<PjbAuthorizationTrailSnapshot> semResultado = trailReadModelService.searchWindow(inicioVelho, fimVelho);
        assertThat(semResultado).isEmpty();
    }

    private static PjbAuthorizationDecisionTrail trailUnico(String seed) {
        return new PjbAuthorizationDecisionTrail(
                "READ_PROCESSO_" + seed,
                "PROCESSO",
                "0001234-" + seed + ".2025.8.06.0001",
                true,
                "POLICY_GRANTED",
                "abac-v1",
                "sha256:" + seed,
                42L,
                "JUIZ",
                "req-" + seed,
                "",
                null,
                PjbAuthorizationRiskLevel.BAIXO,
                0,
                null,
                null
        );
    }
}
