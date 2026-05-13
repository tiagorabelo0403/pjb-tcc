package com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalAggregate;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalEtapa;
import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalLane;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoVerticalExecucaoFiscalFazendariaApplicationService {

    private static final List<String> PROFILE_CODES = List.of(
            "MAGISTRADO_DIRETO",
            "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
            "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_ASSESSORIA",
            "CONTADORIA__CONTADORIA_OPERACAO",
            "ADVOGADO_DIRETO"
    );

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;

    public ProcessoVerticalExecucaoFiscalFazendariaApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                                      ProcessoPrazoApplicationService processoPrazoApplicationService,
                                                                      ProcessoTrabalhoApplicationService processoTrabalhoApplicationService,
                                                                      ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                                      ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                                      ProcessoRecursalApplicationService processoRecursalApplicationService,
                                                                      ProcessoExecucaoApplicationService processoExecucaoApplicationService,
                                                                      ProcessoPapelApplicationService processoPapelApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
    }

    public ProcessoVerticalAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        ProcessoTrabalhoAggregate trabalho = processoTrabalhoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documental = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoRecursalAggregate recursal = processoRecursalApplicationService.detalhar(processoId);
        ProcessoExecucaoAggregate execucao = processoExecucaoApplicationService.detalhar(processoId);
        List<ProcessoVerticalLane> lanes = PROFILE_CODES.stream()
                .map(code -> processoPapelApplicationService.detalharPerfil(processoId, code))
                .map(this::toLane)
                .toList();
        List<ProcessoVerticalEtapa> etapas = List.of(
                etapaInscricaoEDistribuicao(unificado, prazo),
                etapaCitacaoEGarantia(unificado, prazo, trabalho),
                etapaManifestacaoFazendaria(unificado, documental),
                etapaConferenciaContadoria(unificado, documental, trabalho),
                etapaConstricaoESatisfacao(unificado, execucao, timeline),
                etapaRecursalEFechamento(unificado, recursal, prazo)
        );
        LinkedHashSet<String> chips = new LinkedHashSet<>();
        chips.add("fazendario");
        chips.add("execucao_fiscal");
        chips.add(colorChip(unificado));
        chips.addAll(timeline.eixosAtivos());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(prazo.alertasEstruturais());
        alertas.addAll(trabalho.gates());
        alertas.addAll(documental.alertas());
        alertas.addAll(execucao.alertas());
        alertas.addAll(recursal.alertas());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.add("A execução fiscal fazendária exige trilha própria de CDA, garantia, contadoria, constrição, recursal e baixa sem misturar conhecimento comum.");
        fundamentos.add("A camada fazendária conecta procuradoria pública, contadoria e magistratura com rigor recursal e executivo já materializado no PJB.");
        return new ProcessoVerticalAggregate(
                "EXECUCAO_FISCAL_FAZENDARIA",
                "Fatia vertical de execução fiscal e fazendária",
                unificado.identity(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                etapas.size(),
                lanes.size(),
                prazo.marcosCriticos() + trabalho.bloqueantes() + execucao.totalBloqueantes(),
                etapas.stream().mapToLong(item -> item.handoffTo().size()).sum(),
                lanes,
                etapas,
                List.copyOf(chips),
                merge(merge(trabalho.proximoMelhorFluxo(), recursal.proximosPassos()), unificado.proximoMelhorAto()),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoVerticalEtapa etapaInscricaoEDistribuicao(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "INSCRICAO_E_DISTRIBUICAO",
                "Inscrição, CDA, distribuição e competência fazendária",
                "ENTRADA",
                "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
                "fazenda_entrada",
                "amber",
                prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "BASE", "DISTRIBUICAO", "TRIAGEM"),
                merge(unificado.competencia().reviewChecklist(), List.of("validar_cda_e_legitimidade", "confirmar_vara_fazendaria_ou_execucao_fiscal")),
                List.of("cda", "peticao_inicial_fazendaria", "demonstrativo_debito"),
                List.of("CONTADORIA__CONTADORIA_OPERACAO", "MAGISTRADO_DIRETO"),
                List.of("A entrada fazendária precisa nascer com a certidão de dívida, competência e rito executivo íntegros.")
        );
    }

    private ProcessoVerticalEtapa etapaCitacaoEGarantia(ProcessoUnificadoAggregate unificado,
                                                        ProcessoPrazoAggregate prazo,
                                                        ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "CITACAO_E_GARANTIA",
                "Citação, garantia do juízo e triagem executiva",
                "EXECUCAO",
                "MAGISTRADO_DIRETO",
                "garantia_execucao",
                "red",
                trabalho.bloqueantes() > 0 || prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "EXECUCAO", "TRILHA", "BASE"),
                merge(trabalho.gates(), List.of("validar_citacao_regular", "checar_garantia_ou_pedido_de_constricao")),
                List.of("mandado_de_citacao", "prova_de_ciencia", "garantia_juizo_ou_requerimento_constritivo"),
                List.of("PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_ASSESSORIA", "ADVOGADO_DIRETO"),
                List.of("A fase de garantia do juízo concentra nulidades e deve ser monitorada como etapa própria.")
        );
    }

    private ProcessoVerticalEtapa etapaManifestacaoFazendaria(ProcessoUnificadoAggregate unificado,
                                                              ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "MANIFESTACAO_FAZENDARIA",
                "Manifestação fazendária, impugnação e saneamento executivo",
                "EXECUCAO",
                "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
                "manifestacao_fazendaria",
                "violet",
                documental.minutas() == 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA", "EXECUCAO"),
                merge(documental.trilhaAssinavel(), List.of("preparar_manifestacao_fazendaria", "checar_excecao_preexecutividade_ou_embargos")),
                List.of("manifestacao_fazenda", "minuta_de_resposta", "documentos_de_calculo"),
                List.of("MAGISTRADO_DIRETO", "CONTADORIA__CONTADORIA_OPERACAO"),
                List.of("A Procuradoria precisa trilha própria para impugnação, acordo público e racionalização de cobrança.")
        );
    }

    private ProcessoVerticalEtapa etapaConferenciaContadoria(ProcessoUnificadoAggregate unificado,
                                                             ProcessoDocumentoAggregate documental,
                                                             ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "CONTADORIA_E_CALCULO",
                "Conferência de contadoria, atualização e plano de satisfação",
                "EXECUCAO",
                "CONTADORIA__CONTADORIA_OPERACAO",
                "contadoria_calculo",
                "blue",
                documental.totalDocumentos() == 0,
                atosPorCategoria(unificado, "TECNICO", "EXECUCAO", "TRILHA"),
                merge(trabalho.proximoMelhorFluxo(), List.of("atualizar_memoria_de_calculo", "fixar_saldo_executivo")),
                List.of("memoria_calculo", "parecer_contadoria", "demonstrativo_atualizado"),
                List.of("PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "MAGISTRADO_DIRETO"),
                List.of("Execução fiscal com contadoria frouxa compromete bloqueio, parcelamento e satisfação final.")
        );
    }

    private ProcessoVerticalEtapa etapaConstricaoESatisfacao(ProcessoUnificadoAggregate unificado,
                                                             ProcessoExecucaoAggregate execucao,
                                                             ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "CONSTRICAO_E_SATISFACAO",
                "Constrição, expropriação, pagamento e satisfação",
                "EXECUCAO",
                "MAGISTRADO_DIRETO",
                "constricao_satisfacao",
                "red",
                execucao.totalBloqueantes() > 0 || timeline.totalBloqueantes() > 0,
                atosPorCategoria(unificado, "EXECUCAO", "TRILHA"),
                merge(execucao.proximoMelhorPasso(), List.of("validar_bloqueio_penhora_expropriacao", "registrar_pagamento_ou_extincao")),
                List.of("ordem_constritiva", "certidao_cumprimento", "comprovante_pagamento_ou_penhora"),
                List.of("PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "ADVOGADO_DIRETO"),
                List.of("A satisfação fazendária precisa distinguir bloqueio, parcelamento, extinção e baixa para não contaminar o estoque.")
        );
    }

    private ProcessoVerticalEtapa etapaRecursalEFechamento(ProcessoUnificadoAggregate unificado,
                                                           ProcessoRecursalAggregate recursal,
                                                           ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "RECURSAL_E_FECHAMENTO",
                "Recursal fazendário, embargos e fechamento executivo",
                "RECURSAL",
                "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
                "recursal_fazenda",
                "orange",
                recursal.alertas().size() > 0 || prazo.marcosVencidos() > 0,
                atosPorCategoria(unificado, "RECURSAL", "EMBARGOS", "EXECUCAO"),
                merge(recursal.proximosPassos(), List.of("checar_cabimento_recursal", "confirmar_baixa_ou_extincao_com_reserva")),
                List.of("recurso_fazendario", "contrarrazoes", "embargos_execucao_fiscal"),
                List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO"),
                List.of("A fatia fazendária fecha com a trilha recursal própria e com a baixa executiva explicável.")
        );
    }

    private ProcessoVerticalLane toLane(ProcessoPapelPerfil perfil) {
        LinkedHashSet<String> authorityBands = new LinkedHashSet<>();
        if (!perfil.receber().isEmpty()) authorityBands.add("RECEBER");
        if (!perfil.preparar().isEmpty()) authorityBands.add("PREPARAR");
        if (!perfil.aprovar().isEmpty()) authorityBands.add("APROVAR");
        if (!perfil.assinar().isEmpty()) authorityBands.add("ASSINAR");
        if (!perfil.peticionar().isEmpty()) authorityBands.add("PETICIONAR");
        if (!perfil.certificar().isEmpty()) authorityBands.add("CERTIFICAR");
        if (!perfil.redistribuir().isEmpty()) authorityBands.add("REDISTRIBUIR");
        if (!perfil.recorrer().isEmpty()) authorityBands.add("RECORRER");
        if (!perfil.embargar().isEmpty()) authorityBands.add("EMBARGAR");
        return new ProcessoVerticalLane(
                perfil.codigo(),
                perfil.nomeExibicao(),
                perfil.painel(),
                perfil.accentColor(),
                perfil.trustFloor(),
                List.copyOf(authorityBands),
                merge(merge(perfil.assinar(), perfil.recorrer()), perfil.sugerir()),
                perfil.separadores(),
                perfil.guardas()
        );
    }

    private List<String> atosPorCategoria(ProcessoUnificadoAggregate unificado, String... categorias) {
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        for (String categoria : categorias) {
            if (categoria != null && !categoria.isBlank()) {
                expected.add(normalize(categoria));
            }
        }
        return unificado.atosPermitidos().stream()
                .filter(item -> expected.contains(normalize(item.categoria())) || expected.contains(normalize(item.eixoOperacional())) || expected.contains(normalize(item.workItemType())))
                .map(ProcessoUnificadoAto::codigo)
                .distinct()
                .toList();
    }

    private List<String> merge(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return List.copyOf(merged);
    }

    private String colorChip(ProcessoUnificadoAggregate unificado) {
        String fase = normalize(unificado.competencia().faseProcessual());
        if (fase.contains("RECURSAL")) {
            return "recursal";
        }
        if (fase.contains("EXECUCAO")) {
            return "execucao";
        }
        return "fazendario";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
