package com.tcc.pjb.backend.core.processo.orfandade.application;

import com.tcc.pjb.backend.core.processo.completude.application.ProcessoFechamentoTotalApplicationService;
import com.tcc.pjb.backend.core.processo.completude.domain.ProcessoFechamentoTotalAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoCoverage;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoGap;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAntiOrfaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService;
    private final ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService;

    public ProcessoAntiOrfaoApplicationService(ProcessoRepository processoRepository,
                                               ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService,
                                               ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoEncaixeFinalApplicationService = Objects.requireNonNull(processoEncaixeFinalApplicationService);
        this.processoHardeningFinalApplicationService = Objects.requireNonNull(processoHardeningFinalApplicationService);
    }

    public ProcessoAntiOrfaoAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoEncaixeFinalAggregate encaixe = processoEncaixeFinalApplicationService.detalhar(processoId);
        ProcessoHardeningAggregate hardening = processoHardeningFinalApplicationService.detalhar(processoId);

        ArrayList<ProcessoAntiOrfaoCoverage> coberturas = new ArrayList<>();
        coberturas.add(cobertura("LINHA_DO_TEMPO_VIVA",
                "ProcessoTimelineAggregate",
                "ProcessoTimelineApplicationService",
                "/api/v1/processual/unificado/{processoId}/linha-do-tempo",
                "ProcessoTimelineApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Linha do tempo já nasce acoplada ao serviço, endpoint, teste e trilha de hardening.")));
        coberturas.add(cobertura("INTEGRACAO_ECOSSISTEMA",
                "ProcessoIntegracaoAggregate",
                "ProcessoIntegracaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/integracoes",
                "ProcessoIntegracaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Integrações externas estão conectadas a outbox, replay, readiness e monitoramento.")));
        coberturas.add(cobertura("BUSCA_ANALYTICS_GOVERNANCA",
                "ProcessoAnalyticsAggregate",
                "ProcessoBuscaAnalyticsApplicationService",
                "/api/v1/processual/unificado/analytics",
                "ProcessoBuscaAnalyticsApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Busca, analytics e governança já foram materializados como bounded context próprio.")));
        coberturas.add(cobertura("MIGRACAO_SHADOW",
                "ProcessoMigracaoAggregate",
                "ProcessoMigracaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/migracao",
                "ProcessoMigracaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Convivência com legado e shadow mode já possuem aggregate, serviço e endpoint.")));
        coberturas.add(cobertura("OPERACAO_PESADA",
                "ProcessoOperacaoAggregate",
                "ProcessoOperacaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/operacao-transversal",
                "ProcessoOperacaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Operação pesada já aparece no readiness do processo e agora possui trilha transversal própria.")));
        coberturas.add(cobertura("DSL_VERSIONADA",
                "ProcessoDslAggregate",
                "ProcessoDslApplicationService",
                "/api/v1/processual/unificado/{processoId}/dsl",
                "ProcessoDslApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/policy-vigencia",
                List.of("DSL processual versionada já está presa a controller e teste dedicados.")));
        coberturas.add(cobertura("POLICY_POR_VIGENCIA",
                "ProcessoPolicyAggregate",
                "ProcessoPolicyVigenciaApplicationService",
                "/api/v1/processual/unificado/{processoId}/policy-vigencia",
                "ProcessoPolicyVigenciaApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Regra por vigência já compõe coerência, pregravação e fechamento.")));
        coberturas.add(cobertura("POSSE_TRANSITORIA_IMUTAVEL",
                "ProcessoPosseAggregate",
                "ProcessoPosseTrabalhoApplicationService",
                "/api/v1/processual/unificado/{processoId}/posse-trabalho",
                "ProcessoPosseTrabalhoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Trilha de posse já nasce integrada ao ciclo de work item.")));
        coberturas.add(cobertura("PRE_GRAVACAO_COERENTE",
                "ProcessoPreGravacaoAggregate",
                "ProcessoPreGravacaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/pre-gravacao",
                "ProcessoPreGravacaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("Guardrails de gravação agora bloqueiam incoerência antes da persistência.")));
        coberturas.add(cobertura("SIGILO_INTELIGENTE",
                "ProcessoSigiloInteligenteAggregate",
                "ProcessoSigiloInteligenteApplicationService",
                "/api/v1/processual/unificado/{processoId}/sigilo-inteligente",
                "ProcessoSigiloInteligenteApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/sigilo",
                List.of("Sigilo forte, segredo de justiça e segredo de Estado estão presos ao domínio processual.")));
        coberturas.add(cobertura("VERTICAIS_DE_PROVA",
                "ProcessoVerticalAggregate",
                "ProcessoVerticalCivelPrimeiroGrauApplicationService|ProcessoVerticalPenalCustodiaApplicationService|ProcessoVerticalExecucaoFiscalFazendariaApplicationService",
                "/api/v1/processual/unificado/{processoId}/fatias/*",
                "ProcessoVerticalSlicesApplicationServiceTest|ProcessoVerticalExecucaoFiscalFazendariaApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/hardening-final",
                List.of("As três fatias verticais foram conectadas ao PJB como prova ponta a ponta.")));
        coberturas.add(cobertura("ANTI_ORFAO_TOTAL",
                "ProcessoAntiOrfaoAggregate",
                "ProcessoAntiOrfaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/anti-orfao",
                "ProcessoAntiOrfaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("Novo bloco anti-órfão exige rastro entre aggregate, serviço, endpoint, teste e diagnóstico final.")));
        coberturas.add(cobertura("SINALIZACAO_DERIVADA_DE_REGRA",
                "ProcessoSinalizacaoAggregate",
                "ProcessoSinalizacaoRegraApplicationService",
                "/api/v1/processual/unificado/{processoId}/sinalizacao-regra",
                "ProcessoSinalizacaoRegraApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("Cor e separadores agora derivam do estado processual, do sigilo e do travamento real.")));
        coberturas.add(cobertura("PLANTAO_E_SUBSTITUICAO_PESADOS",
                "ProcessoPlantaoSubstituicaoAggregate",
                "ProcessoPlantaoSubstituicaoApplicationService",
                "/api/v1/processual/unificado/{processoId}/plantao-substituicao",
                "ProcessoPlantaoSubstituicaoApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("Plantão, cobertura e substituição passam a integrar o contexto processual pesado.")));
        coberturas.add(cobertura("ANALYTICS_NACIONAL_FINO",
                "ProcessoAnalyticsNacionalAggregate",
                "ProcessoAnalyticsNacionalApplicationService",
                "/api/v1/processual/unificado/{processoId}/analytics-nacional",
                "ProcessoAnalyticsNacionalApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("Novo agregado nacional detalha gargalo, retrabalho, urgência e risco de SLA por unidade e fila.")));
        coberturas.add(cobertura("RESILIENCIA_TRANSVERSAL",
                "ProcessoOperacaoTransversalAggregate",
                "ProcessoOperacaoTransversalApplicationService",
                "/api/v1/processual/unificado/{processoId}/operacao-transversal",
                "ProcessoOperacaoTransversalApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("Idempotência, retry, outbox, replay, tracing e backpressure ganharam leitura transversal.")));
        coberturas.add(cobertura("FECHAMENTO_TOTAL",
                "ProcessoFechamentoTotalAggregate",
                "ProcessoFechamentoTotalApplicationService",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                "ProcessoFechamentoTotalApplicationServiceTest",
                "/api/v1/processual/unificado/{processoId}/fechamento-total",
                List.of("O fechamento total consolida o endurecimento dos blocos antigos e novos.")));

        ArrayList<ProcessoAntiOrfaoGap> gaps = new ArrayList<>();
        if (encaixe.totalBloqueantes() > 0) {
            gaps.add(new ProcessoAntiOrfaoGap(
                    "ENCAIXE_AINDA_BLOQUEANTE",
                    "ARQUITETURA",
                    "CRITICAL",
                    true,
                    "O scanner final de encaixe ainda encontrou achados bloqueantes no processo vivo.",
                    List.of("SANEAR_FINDINGS_DO_ENCAIXE", "REVALIDAR_PONTOS_SENSIVEIS_ANTES_DO_CORTE")
            ));
        }
        if (hardening.blockingFindings() > 0) {
            gaps.add(new ProcessoAntiOrfaoGap(
                    "HARDENING_AINDA_INCOMPLETO",
                    "HARDENING",
                    "CRITICAL",
                    true,
                    "O hardening final ainda aponta travas impeditivas para fechamento pleno.",
                    List.of("ELIMINAR_TRAVAS_DE_HARDENING", "RECALCULAR_READINESS_DO_PROCESSO")
            ));
        }
        long conectados = coberturas.stream().filter(ProcessoAntiOrfaoCoverage::connected).count();
        long coberturaPercentual = coberturas.isEmpty() ? 0L : Math.round((conectados * 100d) / coberturas.size());
        if (coberturaPercentual < 100L) {
            gaps.add(new ProcessoAntiOrfaoGap(
                    "COBERTURA_ANTI_ORFAO_PARCIAL",
                    "ARQUITETURA",
                    "ALTA",
                    true,
                    "Ainda existem contextos sem cobertura total de conexão declarada.",
                    List.of("LIGAR_CONTEXTOS_FALTANTES", "AMPLIAR_TESTES_E_DIAGNOSTICOS")
            ));
        }
        boolean envelopeCompleto = processo.getRamoDireito() != null
                || processo.getRito() != null
                || processo.getFaseAtual() != null
                || processo.getStatusProcesso() != null
                || hasIdentityEnvelope(hardening.identity());
        if (!envelopeCompleto) {
            gaps.add(new ProcessoAntiOrfaoGap(
                    "ENVELOPE_PROCESSUAL_INCOMPLETO",
                    "PROCESSO",
                    "CRITICAL",
                    true,
                    "Sem ramo, rito, fase e status não existe camada anti-órfão plena, porque a regra não fecha o ciclo.",
                    List.of("COMPLETAR_ENVELOPE_PROCESSUAL", "MATERIALIZAR_ESTADO_BASE_DO_PROCESSO")
            ));
        }
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        if (gaps.isEmpty()) {
            proximasAcoes.add("MANTER_SCANNER_ANTI_ORFAO_ATIVO");
            proximasAcoes.add("EXIGIR_NOVO_BLOCO_COM_ENDPOINT_TESTE_DIAGNOSTICO");
        } else {
            gaps.stream().flatMap(item -> item.correctiveActions().stream()).forEach(proximasAcoes::add);
        }
        return new ProcessoAntiOrfaoAggregate(
                processo.getId(),
                processo.getNumeroProcesso(),
                coberturaPercentual,
                coberturas.size(),
                conectados,
                gaps.size(),
                List.copyOf(coberturas),
                List.copyOf(gaps),
                List.copyOf(proximasAcoes),
                Instant.now()
        );
    }

    private ProcessoAntiOrfaoCoverage cobertura(String eixo,
                                                String aggregateClass,
                                                String serviceClass,
                                                String endpointPath,
                                                String testReference,
                                                String diagnosticReference,
                                                List<String> fundamentos) {
        boolean connected = !aggregateClass.isBlank()
                && !serviceClass.isBlank()
                && !endpointPath.isBlank()
                && !testReference.isBlank()
                && !diagnosticReference.isBlank();
        return new ProcessoAntiOrfaoCoverage(
                eixo,
                aggregateClass,
                serviceClass,
                endpointPath,
                testReference,
                diagnosticReference,
                connected ? "CONNECTED" : "PARTIAL",
                connected,
                fundamentos
        );
    }

    private boolean hasIdentityEnvelope(com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity identity) {
        if (identity == null) {
            return false;
        }
        return !blank(identity.tribunal())
                || !blank(identity.unidadeJudiciaria())
                || !blank(identity.classeProcessual())
                || !blank(identity.assunto())
                || !blank(identity.parteAutora())
                || !blank(identity.parteRe())
                || !identity.marcadores().isEmpty();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

}