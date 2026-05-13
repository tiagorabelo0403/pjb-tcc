package com.tcc.pjb.backend.core.processo.gemeo.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAnomaliaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaOrquestracaoApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaOrquestracaoAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalEstado;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalRisco;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoGemeoDigitalApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService;
    private final ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public ProcessoGemeoDigitalApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                  ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService,
                                                  ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService,
                                                  ProcessoMalhaParallelExecutor processoMalhaParallelExecutor,
                                                  ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoDistribuicaoMalhaOrquestracaoApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaOrquestracaoApplicationService);
        this.processoAnomaliaMalhaApplicationService = Objects.requireNonNull(processoAnomaliaMalhaApplicationService);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public ProcessoGemeoDigitalAggregate simular(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoMalhaParallelExecutor.Dupla<ProcessoDistribuicaoMalhaOrquestracaoAggregate, ProcessoAnomaliaMalhaAggregate> dupla = processoMalhaParallelExecutor.executar2(
                "gemeo-digital-processual",
                () -> processoDistribuicaoMalhaOrquestracaoApplicationService.executar(processoId),
                () -> processoAnomaliaMalhaApplicationService.detalhar(processoId)
        );
        ProcessoDistribuicaoMalhaOrquestracaoAggregate distribuicao = dupla.primeiro();
        ProcessoAnomaliaMalhaAggregate anomalia = dupla.segundo();
        List<ProcessoGemeoDigitalRisco> riscos = new ArrayList<>();
        if (distribuicao.bloqueada()) {
            riscos.add(new ProcessoGemeoDigitalRisco("gemeo.trava.distribuicao", "ALTO", "A distribuição está travada pela malha", "Remessa ou redistribuição antes do próximo ato", "executar-triagem-prevento"));
        }
        if (!anomalia.itens().isEmpty()) {
            riscos.add(new ProcessoGemeoDigitalRisco("gemeo.anomalia.ativa", "CRITICO", "Há anomalias processuais ativas", "Risco de fraude, nulidade ou manipulação de fila", "acionar-antifraude-e-revisao"));
        }
        if (contexto.sigiloReforcado()) {
            riscos.add(new ProcessoGemeoDigitalRisco("gemeo.sigilo.reforcado", "ALTO", "Fluxo com sigilo reforçado", "Próximos atos exigem visão contextual elevada", "executar-step-up-e-materializar-operacao"));
        }
        ProcessoGemeoDigitalEstado estado = riscos.stream().anyMatch(r -> "CRITICO".equals(r.nivel()))
                ? ProcessoGemeoDigitalEstado.CRITICO
                : distribuicao.bloqueada()
                ? ProcessoGemeoDigitalEstado.BLOQUEADO
                : riscos.isEmpty() ? ProcessoGemeoDigitalEstado.ESTAVEL : ProcessoGemeoDigitalEstado.ALERTA;
        LinkedHashSet<String> estados = new LinkedHashSet<>();
        estados.add(estado.name());
        estados.add(distribuicao.statusOrquestracao());
        if (contexto.sigiloReforcado()) {
            estados.add("SIGILO_REFORCADO");
        }
        String gargalo = distribuicao.bloqueada() ? "triagem de distribuição/prevenção" : anomalia.itens().isEmpty() ? "nenhum gargalo crítico" : "revisão antifraude";
        String proximoAto = distribuicao.bloqueada() ? "remeter ou redistribuir" : anomalia.itens().isEmpty() ? "prosseguir ato útil" : "sanear anomalia antes do próximo impulso";
        int custo = (distribuicao.bloqueada() ? 40 : 15) + (anomalia.itens().size() * 10) + (contexto.sigiloReforcado() ? 15 : 0);
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("processo.gemeo.digital", "Processo", String.valueOf(processoId), BigDecimal.valueOf(Math.max(1, 100 - custo)), riscos.toString(), estados.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(riscos.toString()), "PJB-GEMEO", gargalo);
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PROCESSO_GEMEO_DIGITAL_GERADO", "Processo", String.valueOf(processoId), Hashes.sha256Hex(riscos.toString()), "estado=" + estado.name() + ";custo=" + custo);
        }
        return new ProcessoGemeoDigitalAggregate(
                processoId,
                contexto.numeroReferencia(),
                estado,
                List.copyOf(estados),
                List.copyOf(riscos),
                gargalo,
                proximoAto,
                custo,
                Instant.now()
        );
    }
}
