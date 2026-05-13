package com.tcc.pjb.backend.core.plataforma.substituicao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoComunicacaoSyncItemRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoMigracaoLoteRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoTribunalHomologacaoProbeRepository;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoRepositoryContractTest {

    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V220__pjb_substituicao_nacional_execution_hardening.sql");

    @Test
    void repositories_expose_expected_methods_and_migration_contains_expected_indexes() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertHasMethod(PjbSubstituicaoNacionalExecucaoRepository.class, "findLockedById", Long.class);
        assertHasMethod(PjbSubstituicaoTribunalHomologacaoProbeRepository.class, "findByExecucaoIdAndProbeCodigo", Long.class, String.class);
        assertHasMethod(PjbSubstituicaoMigracaoLoteRepository.class, "findByExecucaoIdAndLoteCodigo", Long.class, String.class);
        assertHasMethod(PjbSubstituicaoComunicacaoSyncItemRepository.class, "findByCursorExecucaoTribunalCodigoOrderByCreatedAtAsc", String.class);
        assertTrue(sql.contains("ix_pjb_subst_hom_probe_exec"));
        assertTrue(sql.contains("ix_pjb_subst_mig_lote_exec"));
        assertTrue(sql.contains("ix_pjb_subst_com_cursor_exec"));
        assertTrue(sql.contains("ix_pjb_subst_com_item_cursor"));
    }

    private void assertHasMethod(Class<?> type, String name, Class<?>... params) throws Exception {
        Method method = type.getMethod(name, params);
        assertTrue(method.getName().equals(name));
    }
}
