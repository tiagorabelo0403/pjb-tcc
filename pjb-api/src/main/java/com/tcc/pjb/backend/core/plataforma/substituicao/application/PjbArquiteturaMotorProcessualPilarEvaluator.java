package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.available;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.capacidade;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.pilar;

import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalEleitoralRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.JuizadoRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.TrabalhistaRecursalTemplate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.JuizadoLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.eleitoral.EleitoralLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.militar.MilitarLifecyclePack;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import com.tcc.pjb.backend.core.processual.routing.RecursalCollegiateResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Avalia o pilar "motor processual nacional" (Fatia F6 -- extraído de
 * PjbArquiteturaSubstituicaoNacionalApplicationService, que tinha 40 dependências de
 * construtor). Cada capacidade aqui é checada por presença do colaborador correspondente
 * no contexto Spring, exatamente como o pilar fazia antes da extração.
 */
@Component
public class PjbArquiteturaMotorProcessualPilarEvaluator {

    private final ObjectProvider<ProcessoVerticalCivelPrimeiroGrauApplicationService> verticalCivelProvider;
    private final ObjectProvider<ProcessoVerticalPenalCustodiaApplicationService> verticalPenalProvider;
    private final ObjectProvider<ProcessoVerticalExecucaoFiscalFazendariaApplicationService> verticalFazendaProvider;
    private final ObjectProvider<ProcessoTrabalhoApplicationService> trabalhoProvider;
    private final ObjectProvider<TrabalhistaRecursalTemplate> trabalhistaRecursalTemplateProvider;
    private final ObjectProvider<JuizadoLifecyclePack> juizadoLifecyclePackProvider;
    private final ObjectProvider<JuizadoRecursalTemplate> juizadoRecursalTemplateProvider;
    private final ObjectProvider<EleitoralLifecyclePack> eleitoralLifecyclePackProvider;
    private final ObjectProvider<TribunalRegionalEleitoralRuleProfile> eleitoralRuleProfileProvider;
    private final ObjectProvider<MilitarLifecyclePack> militarLifecyclePackProvider;
    private final ObjectProvider<ProcessoRecursalApplicationService> recursalProvider;
    private final ObjectProvider<NationalRecursalMeshEngine> recursalMeshProvider;
    private final ObjectProvider<ProcessoSigiloApplicationService> sigiloProvider;
    private final ObjectProvider<ProcessoSigiloInteligenteApplicationService> sigiloInteligenteProvider;
    private final ObjectProvider<ProcessoSigiloNotificacaoApplicationService> sigiloNotificacaoProvider;
    private final ObjectProvider<CitacaoIntimacaoEngine> citacaoIntimacaoEngineProvider;
    private final ObjectProvider<RecursalCollegiateResolver> recursalCollegiateResolverProvider;

    public PjbArquiteturaMotorProcessualPilarEvaluator(
            ObjectProvider<ProcessoVerticalCivelPrimeiroGrauApplicationService> verticalCivelProvider,
            ObjectProvider<ProcessoVerticalPenalCustodiaApplicationService> verticalPenalProvider,
            ObjectProvider<ProcessoVerticalExecucaoFiscalFazendariaApplicationService> verticalFazendaProvider,
            ObjectProvider<ProcessoTrabalhoApplicationService> trabalhoProvider,
            ObjectProvider<TrabalhistaRecursalTemplate> trabalhistaRecursalTemplateProvider,
            ObjectProvider<JuizadoLifecyclePack> juizadoLifecyclePackProvider,
            ObjectProvider<JuizadoRecursalTemplate> juizadoRecursalTemplateProvider,
            ObjectProvider<EleitoralLifecyclePack> eleitoralLifecyclePackProvider,
            ObjectProvider<TribunalRegionalEleitoralRuleProfile> eleitoralRuleProfileProvider,
            ObjectProvider<MilitarLifecyclePack> militarLifecyclePackProvider,
            ObjectProvider<ProcessoRecursalApplicationService> recursalProvider,
            ObjectProvider<NationalRecursalMeshEngine> recursalMeshProvider,
            ObjectProvider<ProcessoSigiloApplicationService> sigiloProvider,
            ObjectProvider<ProcessoSigiloInteligenteApplicationService> sigiloInteligenteProvider,
            ObjectProvider<ProcessoSigiloNotificacaoApplicationService> sigiloNotificacaoProvider,
            ObjectProvider<CitacaoIntimacaoEngine> citacaoIntimacaoEngineProvider,
            ObjectProvider<RecursalCollegiateResolver> recursalCollegiateResolverProvider) {
        this.verticalCivelProvider = Objects.requireNonNull(verticalCivelProvider);
        this.verticalPenalProvider = Objects.requireNonNull(verticalPenalProvider);
        this.verticalFazendaProvider = Objects.requireNonNull(verticalFazendaProvider);
        this.trabalhoProvider = Objects.requireNonNull(trabalhoProvider);
        this.trabalhistaRecursalTemplateProvider = Objects.requireNonNull(trabalhistaRecursalTemplateProvider);
        this.juizadoLifecyclePackProvider = Objects.requireNonNull(juizadoLifecyclePackProvider);
        this.juizadoRecursalTemplateProvider = Objects.requireNonNull(juizadoRecursalTemplateProvider);
        this.eleitoralLifecyclePackProvider = Objects.requireNonNull(eleitoralLifecyclePackProvider);
        this.eleitoralRuleProfileProvider = Objects.requireNonNull(eleitoralRuleProfileProvider);
        this.militarLifecyclePackProvider = Objects.requireNonNull(militarLifecyclePackProvider);
        this.recursalProvider = Objects.requireNonNull(recursalProvider);
        this.recursalMeshProvider = Objects.requireNonNull(recursalMeshProvider);
        this.sigiloProvider = Objects.requireNonNull(sigiloProvider);
        this.sigiloInteligenteProvider = Objects.requireNonNull(sigiloInteligenteProvider);
        this.sigiloNotificacaoProvider = Objects.requireNonNull(sigiloNotificacaoProvider);
        this.citacaoIntimacaoEngineProvider = Objects.requireNonNull(citacaoIntimacaoEngineProvider);
        this.recursalCollegiateResolverProvider = Objects.requireNonNull(recursalCollegiateResolverProvider);
    }

    public PjbArquiteturaSubstituicaoPilar avaliar() {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "motor.civel",
                "Civil comum e família com fatia vertical explícita",
                available(verticalCivelProvider),
                96,
                List.of("ProcessoVerticalCivelPrimeiroGrauApplicationService", "Ritos de família, tutela e conhecimento mapeados no catálogo"),
                List.of("Continuar materialização fina de família e sucessões em ondas próprias")
        ));
        capacidades.add(capacidade(
                "motor.penal",
                "Penal, custódia, execução e recursal criminal",
                available(verticalPenalProvider) && available(recursalProvider),
                94,
                List.of("ProcessoVerticalPenalCustodiaApplicationService", "ProcessoRecursalApplicationService"),
                List.of("Ampliar cobertura fina de júri, execução penal e incidentes especiais")
        ));
        capacidades.add(capacidade(
                "motor.fazenda",
                "Fazenda pública e execução fiscal",
                available(verticalFazendaProvider),
                93,
                List.of("ProcessoVerticalExecucaoFiscalFazendariaApplicationService", "Ritos tributários e previdenciários catalogados"),
                List.of("Fechar trilhas específicas de fazenda não executiva e RPPS em profundidade")
        ));
        capacidades.add(capacidade(
                "motor.trabalhista",
                "Trabalhista transversal com prazo, recursal e trilha de trabalho",
                available(trabalhoProvider) && available(trabalhistaRecursalTemplateProvider),
                86,
                List.of("ProcessoTrabalhoApplicationService", "TrabalhistaRecursalTemplate", "Ritos trabalhistas no catálogo"),
                List.of("Materializar fatia vertical trabalhista dedicada de ponta a ponta")
        ));
        capacidades.add(capacidade(
                "motor.juizados",
                "Juizados e turma recursal",
                available(juizadoLifecyclePackProvider) && available(juizadoRecursalTemplateProvider),
                84,
                List.of("JuizadoLifecyclePack", "JuizadoRecursalTemplate", "Ritos de juizado no catálogo"),
                List.of("Fechar diferenças finas entre JEC, JECRIM, JEF e Juizado da Fazenda Pública")
        ));
        capacidades.add(capacidade(
                "motor.eleitoral",
                "Eleitoral e colegiado TRE/TSE",
                available(eleitoralLifecyclePackProvider) && available(eleitoralRuleProfileProvider),
                82,
                List.of("EleitoralLifecyclePack", "TribunalRegionalEleitoralRuleProfile", "Ritos eleitorais catalogados"),
                List.of("Aprofundar fatia vertical eleitoral com registro, AIJE, AIME e contas")
        ));
        capacidades.add(capacidade(
                "motor.militar",
                "Militar comum e especial",
                available(militarLifecyclePackProvider),
                80,
                List.of("MilitarLifecyclePack", "Ritos militares catalogados"),
                List.of("Fechar fatia vertical militar própria com conselho e execução disciplinar")
        ));
        capacidades.add(capacidade(
                "motor.execucao-recursal-sigilo",
                "Execução, recursos, prevenção, redistribuição e sigilo",
                available(recursalProvider) && available(recursalMeshProvider) && available(sigiloProvider) && available(sigiloInteligenteProvider) && available(sigiloNotificacaoProvider),
                92,
                List.of("ProcessoRecursalApplicationService", "NationalRecursalMeshEngine", "ProcessoSigiloApplicationService", "sigilo inteligente e notificações"),
                List.of("Seguir endurecendo prevenção, redistribuição e incidentes altamente especializados")
        ));
        capacidades.add(capacidade(
                "motor.comunicacao-colegiado",
                "Comunicação judicial, intimação/citação e colegiados",
                available(citacaoIntimacaoEngineProvider) && available(recursalCollegiateResolverProvider),
                90,
                List.of("CitacaoIntimacaoEngine", "RecursalCollegiateResolver"),
                List.of("Fechar microrregras locais por tribunal e colegiado para rollout nacional")
        ));
        return pilar(
                "motor-processual-nacional",
                "Motor processual nacional realmente transversal",
                capacidades,
                List.of(
                        "Materializar fatias verticais explícitas para trabalhista, eleitoral e militar em nível equivalente ao cível/penal/fazenda.",
                        "Fechar catálogo operacional fino de juizados e colegiados locais sem quebrar o núcleo nacional."
                )
        );
    }
}
