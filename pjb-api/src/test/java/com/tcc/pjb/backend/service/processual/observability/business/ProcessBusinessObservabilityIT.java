package com.tcc.pjb.backend.service.processual.observability.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProcessBusinessObservabilityIT extends PjbIntegrationTestBase {

    @Autowired private ProcessoRepository processoRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpar() {
        jdbcTemplate.update("DELETE FROM tb_processo WHERE numero_processo LIKE 'OBS-IT-%'");
    }

    @Test
    void it01_countScalarsCorretos() {
        long totalBase = processoRepository.count();
        long arquivadosBase = processoRepository.countByStatusProcesso(StatusProcesso.ARQUIVADO);
        long transitoBase = processoRepository.countByStatusProcesso(StatusProcesso.TRANSITO_EM_JULGADO);

        List<FaseProcessual> recursalPhases = Arrays.stream(FaseProcessual.values())
                .filter(FaseProcessual::isRecursal)
                .toList();
        long recursaisBase = processoRepository.countRecursais(StatusProcesso.RECURSO_INTERPOSTO, recursalPhases);

        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-1", "EM_ANDAMENTO", "CIVIL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-2", "ARQUIVADO", "PENAL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-3", "RECURSO_INTERPOSTO", "CIVIL", "RECURSAL");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-4", null, null, "CONHECIMENTO");

        assertEquals(totalBase + 4, processoRepository.count());
        assertEquals(arquivadosBase + 1, processoRepository.countByStatusProcesso(StatusProcesso.ARQUIVADO));
        assertEquals(transitoBase, processoRepository.countByStatusProcesso(StatusProcesso.TRANSITO_EM_JULGADO));
        assertEquals(recursaisBase + 1,
                processoRepository.countRecursais(StatusProcesso.RECURSO_INTERPOSTO, recursalPhases));
    }

    @Test
    void it02_countPorRamoCoalesceEOrdem() {
        Map<String, Long> ramoBase = toMap(processoRepository.countPorRamo());

        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-1", "EM_ANDAMENTO", "CIVIL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-2", "ARQUIVADO", "PENAL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-3", "RECURSO_INTERPOSTO", "CIVIL", "RECURSAL");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-4", null, null, "CONHECIMENTO");

        List<Object[]> ramoRows = processoRepository.countPorRamo();
        Map<String, Long> ramoAfter = toMap(ramoRows);

        assertEquals(ramoBase.getOrDefault("CIVIL", 0L) + 2, ramoAfter.get("CIVIL"));
        assertEquals(ramoBase.getOrDefault("PENAL", 0L) + 1, ramoAfter.get("PENAL"));
        assertEquals(ramoBase.getOrDefault("NAO_CLASSIFICADO", 0L) + 1, ramoAfter.get("NAO_CLASSIFICADO"));

        List<String> keys = ramoRows.stream().map(r -> (String) r[0]).toList();
        assertThat(keys).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void it03_countPorStatusCoalesceEOrdem() {
        Map<String, Long> statusBase = toMap(processoRepository.countPorStatus());

        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-1", "EM_ANDAMENTO", "CIVIL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-2", "ARQUIVADO", "PENAL", "CONHECIMENTO");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-3", "RECURSO_INTERPOSTO", "CIVIL", "RECURSAL");
        jdbcTemplate.update(
                "INSERT INTO tb_processo (numero_processo, status_processo, ramo_direito, fase_atual) VALUES (?,?,?,?)",
                "OBS-IT-4", null, null, "CONHECIMENTO");

        List<Object[]> statusRows = processoRepository.countPorStatus();
        Map<String, Long> statusAfter = toMap(statusRows);

        assertEquals(statusBase.getOrDefault("ARQUIVADO", 0L) + 1, statusAfter.get("ARQUIVADO"));
        assertEquals(statusBase.getOrDefault("EM_ANDAMENTO", 0L) + 1, statusAfter.get("EM_ANDAMENTO"));
        assertEquals(statusBase.getOrDefault("RECURSO_INTERPOSTO", 0L) + 1, statusAfter.get("RECURSO_INTERPOSTO"));
        assertEquals(statusBase.getOrDefault("NAO_CLASSIFICADO", 0L) + 1, statusAfter.get("NAO_CLASSIFICADO"));

        List<String> keys = statusRows.stream().map(r -> (String) r[0]).toList();
        assertThat(keys).isSortedAccordingTo(Comparator.naturalOrder());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }
}
