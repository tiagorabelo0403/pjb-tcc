package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import static com.tcc.pjb.backend.core.plataforma.sustentacao.application.PjbPlataformaSustentacaoDiagnosticoSupport.average;
import static com.tcc.pjb.backend.core.plataforma.sustentacao.application.PjbPlataformaSustentacaoDiagnosticoSupport.cleanMap;
import static com.tcc.pjb.backend.core.plataforma.sustentacao.application.PjbPlataformaSustentacaoDiagnosticoSupport.eixo;

import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PjbPlataformaMigracaoShadowDiagnosticoService {

    private final ProcessoRepository processoRepository;
    private final ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService;
    private final ProcessoMigracaoApplicationService processoMigracaoApplicationService;

    public PjbPlataformaMigracaoShadowDiagnosticoService(ProcessoRepository processoRepository,
                                                         ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService,
                                                         ProcessoMigracaoApplicationService processoMigracaoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoMigracaoFactoryApplicationService = Objects.requireNonNull(processoMigracaoFactoryApplicationService);
        this.processoMigracaoApplicationService = Objects.requireNonNull(processoMigracaoApplicationService);
    }

    public PjbPlataformaSustentacaoEixo avaliar() {
        List<Processo> processos = processoRepository.findAll(PageRequest.of(0, 6)).getContent();
        ArrayList<Map<String, Object>> amostras = new ArrayList<>();
        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        int scoreTotal = 0;
        if (processos.isEmpty()) {
            bloqueadores.add("sem_amostra_de_processo_para_shadow_compare");
        }
        for (Processo processo : processos) {
            try {
                ProcessoMigracaoFabricaAggregate fabrica = processoMigracaoFactoryApplicationService.planejar(processo.getId());
                ProcessoMigracaoAggregate migracao = processoMigracaoApplicationService.detalhar(processo.getId());
                int score = average(fabrica.scoreGeral(), "READY_FOR_CUTOVER".equalsIgnoreCase(migracao.readiness()) ? 94 : "READY_FOR_SHADOW".equalsIgnoreCase(migracao.readiness()) ? 76 : 52, migracao.canCutOver() ? 92 : 60);
                scoreTotal += score;
                LinkedHashMap<String, Object> linha = new LinkedHashMap<>();
                linha.put("processoId", processo.getId());
                linha.put("scoreFactory", fabrica.scoreGeral());
                linha.put("factoryStatus", fabrica.statusGeral().name());
                linha.put("migrationReadiness", migracao.readiness());
                linha.put("canCutOver", migracao.canCutOver());
                linha.put("comparacoes", migracao.comparacoes().size());
                linha.put("bloqueiosFactory", fabrica.bloqueios());
                amostras.add(cleanMap(linha));
                if (!migracao.canCutOver()) {
                    bloqueadores.add("shadow_compare_bloqueado:processo=" + processo.getId());
                }
            } catch (RuntimeException ex) {
                bloqueadores.add("shadow_compare_falhou:processo=" + processo.getId());
            }
        }
        int score = processos.isEmpty() ? 35 : Math.max(0, Math.min(100, scoreTotal / Math.max(1, amostras.size())));
        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        sinais.add("processosAmostrados=" + processos.size());
        sinais.add("processosComShadowCompare=" + amostras.size());
        sinais.add("bloqueadoresShadow=" + bloqueadores.size());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        proximasAcoes.add("EXECUTAR_RECONCILIACAO_AUTOMATICA_DE_METADADOS_ANTES_DO_CUTOVER");
        proximasAcoes.add("FORMALIZAR_RELATORIO_DE_DIVERGENCIA_LEGADO_VS_PJB_POR_LOTE_DE_MIGRACAO");
        LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
        evidencias.put("amostras", amostras);
        evidencias.put("totalProcessosPersistidos", processoRepository.count());
        return eixo(
                "migracao.shadow-compare",
                "Shadow compare, reconciliação e migração",
                score,
                !processos.isEmpty() && bloqueadores.isEmpty(),
                sinais,
                bloqueadores,
                proximasAcoes,
                evidencias
        );
    }
}
