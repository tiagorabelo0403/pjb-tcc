package com.tcc.pjb.backend.core.processo.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.dsl.application.ProcessoDslApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalIdentity;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalJanela;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoDslApplicationServiceTest {

    @Mock
    private ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    @Mock
    private ProcessoRecursalApplicationService processoRecursalApplicationService;

    @Mock
    private ProcessoExecucaoApplicationService processoExecucaoApplicationService;

    @Test
    void deveMontarDslVersionadaComBlocosCentrais() {
        when(processoUnificadoApplicationService.detalhar(77L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(77L, "0001", "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "AÇÃO PENAL", "Crime", "Ministério Público", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "Tribunal", "Órgão", "UNIDADE", "FILA", "MESA", "LOCAL", "PREVENCAO", "SORTEIO", "PENAL", "PADRAO", "AUTO", "ENVELOPE", "CONTROLADO", "GABINETE", false, false, 24, List.of("CHECK"), List.of("fundamento"), List.of("revisar"), new java.util.LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 1, 0, 0, 0, List.<ProcessoUnificadoFinding>of(), List.of("fundamento"), Instant.now()),
                List.of(new ProcessoUnificadoAto("ATO_BASE", "Ato base", "BASE", "TRIAGEM", "MERITO", "FILA_PROMOTORIA", "INBOX_PROMOTORIA", "fundamento", "CONHECIMENTO", "INSTRUCAO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, false, false, false, "ok", "PROMOTOR", "TRANSICAO", List.of("GUARDA"))),
                List.of(),
                List.of("ATO_BASE"),
                Instant.now()
        ));
        when(processoRecursalApplicationService.detalhar(77L)).thenReturn(new ProcessoRecursalAggregate(
                new ProcessoRecursalIdentity(77L, "0001", "TJCE", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", List.of("PENAL")),
                "PENAL",
                "FIRST_INSTANCE",
                1,
                1,
                0,
                0,
                List.of(new ProcessoRecursalJanela("APELACAO", "Apelação", "RECURSAL", true, false, true, false, true, "MESMOS_AUTOS", "TJCE", "JUÍZO_A_QUO", "CÂMARA", List.of("INTIMAR"), List.of("TEMPESTIVIDADE"), List.of("CPP"))),
                List.of("ABRIR_JANELA"),
                List.of(),
                List.of(),
                Instant.now()
        ));
        when(processoExecucaoApplicationService.detalhar(77L)).thenReturn(new ProcessoExecucaoAggregate(
                new ProcessoExecucaoIdentity(77L, "0001", "TJCE", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", List.of("PENAL")),
                true,
                1,
                0,
                1,
                1,
                List.of(new ProcessoExecucaoTrilha("MANDADO", "Mandado", "EXECUCAO", "MANDADOS", "OFICIAL", "OFICIAL_JUSTICA", 1, false, 2, "CUMPRIMENTO", "ALTO", List.of("MANDADO_CITACAO"), List.of("APRESENTACAO"), List.of("CERTIFICAR"), List.of("CPC"))),
                List.of(),
                List.of("CERTIFICAR"),
                Instant.now()
        ));

        ProcessoDslApplicationService service = new ProcessoDslApplicationService(processoUnificadoApplicationService, processoRecursalApplicationService, processoExecucaoApplicationService);
        var aggregate = service.detalhar(77L);

        assertThat(aggregate.version().semanticVersion()).isEqualTo("2026.1");
        assertThat(aggregate.blocks()).extracting("code").contains("COMPETENCIA", "FLUXO_BASE", "RECURSAL", "EXECUCAO");
        assertThat(aggregate.totalRules()).isPositive();
    }
}
