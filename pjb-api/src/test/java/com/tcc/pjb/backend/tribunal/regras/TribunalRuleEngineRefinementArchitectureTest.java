package com.tcc.pjb.backend.tribunal.regras;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.tribunal.regras.snapshot.AnaliseDesvio;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RelatorioCoberturaTribunal;

class TribunalRuleEngineRefinementArchitectureTest {

    private static final Path ENGINE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/TribunalRuleEngine.java");
    private static final Path RESOLUTION_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/TribunalRuleResolutionSupport.java");
    private static final Path SYNC_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/TribunalRulePackSynchronizationSupport.java");
    private static final Path ANALYTICS_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/tribunal/regras/TribunalRuleAnalyticsSupport.java");

    @Test
    void tribunalRuleEngineAndSupportsMustStayBelowHotspotThresholds() throws IOException {
        assertThat(Files.lines(ENGINE_PATH, StandardCharsets.UTF_8).count()).isLessThan(900);
        assertThat(Files.lines(RESOLUTION_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(500);
        assertThat(Files.lines(SYNC_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(350);
        assertThat(Files.lines(ANALYTICS_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(250);
    }

    @Test
    void tribunalRuleEngineMustKeepResolutionSynchronizationAndAnalyticsSeparated() throws IOException {
        String engine = Files.readString(ENGINE_PATH, StandardCharsets.UTF_8);
        String resolutionSupport = Files.readString(RESOLUTION_SUPPORT_PATH, StandardCharsets.UTF_8);
        String syncSupport = Files.readString(SYNC_SUPPORT_PATH, StandardCharsets.UTF_8);
        String analyticsSupport = Files.readString(ANALYTICS_SUPPORT_PATH, StandardCharsets.UTF_8);

        assertThat(engine).contains("TribunalRuleResolutionSupport");
        assertThat(engine).contains("TribunalRulePackSynchronizationSupport");
        assertThat(engine).contains("TribunalRuleAnalyticsSupport");
        assertThat(engine).doesNotContain("private List<NationalRulePackEngine.Regra> adaptarRulePackTribunal(");
        assertThat(engine).doesNotContain("private List<NationalRulePackEngine.Regra> adaptarRegraResolvida(");
        assertThat(engine).doesNotContain("private List<EntradaRegra> coletarCadeiaAplicavel(");
        assertThat(engine).doesNotContain("private EstadoAplicacao aplicarEntrada(");

        assertThat(resolutionSupport).contains("Optional<RegraResolvida> resolver(");
        assertThat(resolutionSupport).contains("private List<TribunalRuleEngine.EntradaRegra> coletarCadeiaAplicavel(");
        assertThat(syncSupport).contains("void sincronizarRulePackAdaptadoTribunalInterno(");
        assertThat(syncSupport).contains("private List<NationalRulePackEngine.Regra> adaptarRulePackTribunal(");
        assertThat(analyticsSupport).contains("List<AnaliseDesvio> analisarDesviosTribunal(");
        assertThat(analyticsSupport).contains("RelatorioCoberturaTribunal relatorioCobertura(");
    }
}
