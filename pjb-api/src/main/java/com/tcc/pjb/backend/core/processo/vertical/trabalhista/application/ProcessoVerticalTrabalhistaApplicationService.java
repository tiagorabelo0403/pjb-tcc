package com.tcc.pjb.backend.core.processo.vertical.trabalhista.application;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
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
public class ProcessoVerticalTrabalhistaApplicationService {

    private static final List<String> PROFILE_CODES = List.of(
            "MAGISTRADO_DIRETO",
            "ADVOGADO_DIRETO",
            "MINISTERIO_PUBLICO_DO_TRABALHO__MPT_TITULAR",
            "CEJUSC__CEJUSC_AGENDAMENTO",
            "CONTADORIA__CONTADORIA_OPERACAO",
            "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR"
    );

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;

    public ProcessoVerticalTrabalhistaApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                         ProcessoPrazoApplicationService processoPrazoApplicationService,
                                                         ProcessoTrabalhoApplicationService processoTrabalhoApplicationService,
                                                         ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                         ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                         ProcessoPapelApplicationService processoPapelApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
    }

    public ProcessoVerticalAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        ProcessoTrabalhoAggregate trabalho = processoTrabalhoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documental = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        List<ProcessoVerticalLane> lanes = PROFILE_CODES.stream()
                .map(code -> processoPapelApplicationService.detalharPerfil(processoId, code))
                .map(this::toLane)
                .toList();
        List<ProcessoVerticalEtapa> etapas = List.of(
                etapaReclamacaoEDistribuicao(unificado, prazo),
                etapaAudienciaUnoActu(unificado, trabalho, timeline),
                etapaInstrucaoEProva(unificado, prazo, documental),
                etapaSentencaEPublicacao(unificado, documental),
                etapaRecursoEDeposito(unificado, prazo, trabalho),
                etapaExecucaoELiquidacao(unificado, prazo, trabalho)
        );
        LinkedHashSet<String> chips = new LinkedHashSet<>();
        chips.add("trabalhista");
        chips.add(isSumarissimo(unificado) ? "sumarissimo" : "ordinario");
        chips.addAll(timeline.eixosAtivos());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(prazo.alertasEstruturais());
        alertas.addAll(trabalho.gates());
        alertas.addAll(documental.alertas());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.add("O rito trabalhista concentra distribuição, audiência e instrução em poucos atos; o saneamento e a tentativa de conciliação são obrigatórios antes de qualquer sentença.");
        fundamentos.add("Depósito recursal e custas são gate obrigatório para admissibilidade de recurso ordinário e de revista.");
        return new ProcessoVerticalAggregate(
                "TRABALHISTA_PRIMEIRO_GRAU",
                "Fatia vertical trabalhista de primeiro grau",
                unificado.identity(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                etapas.size(),
                lanes.size(),
                prazo.marcosCriticos() + trabalho.bloqueantes(),
                etapas.stream().mapToLong(e -> e.handoffTo().size()).sum(),
                lanes,
                etapas,
                List.copyOf(chips),
                merge(trabalho.proximoMelhorFluxo(), unificado.proximoMelhorAto()),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoVerticalEtapa etapaReclamacaoEDistribuicao(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "RECLAMACAO_E_DISTRIBUICAO",
                "Reclamação trabalhista, distribuição e competência",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "distribuicao_competencia",
                "blue",
                false,
                atosPorCategoria(unificado, "DISTRIBUICAO", "TRIAGEM", "BASE"),
                merge(unificado.competencia().reviewChecklist(), List.of("verificar_competencia_territorial", "verificar_valor_da_causa", "checar_tempestividade")),
                List.of("reclamacao_trabalhista", "documentos_iniciais", "procuracao"),
                List.of("ADVOGADO_DIRETO", "MINISTERIO_PUBLICO_DO_TRABALHO__MPT_TITULAR"),
                List.of("A reclamação trabalhista fixa o objeto do litígio e não pode ser ampliada após o oferecimento da defesa.", "Competência territorial trabalhista segue o local da prestação de serviços.")
        );
    }

    private ProcessoVerticalEtapa etapaAudienciaUnoActu(ProcessoUnificadoAggregate unificado, ProcessoTrabalhoAggregate trabalho, ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "AUDIENCIA_UNO_ACTU",
                "Audiência una: conciliação, instrução e julgamento",
                "CONHECIMENTO",
                "CEJUSC__CEJUSC_AGENDAMENTO",
                "audiencia_trabalhista",
                "green",
                trabalho.bloqueantes() > 0,
                atosPorCategoria(unificado, "MERITO", "BASE", "TRIAGEM"),
                merge(timeline.proximoCiclo(), List.of("tentar_conciliacao_obrigatoria", "colher_prova_oral", "encerrar_instrucao_na_mesma_sessao")),
                List.of("ata_de_audiencia", "termo_de_conciliacao_ou_recusa"),
                List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO"),
                List.of("No rito sumaríssimo toda a instrução ocorre em audiência única sem adiamentos; no ordinário são admitidas pautas separadas.", "Conciliação deve ser tentada no início e ao final da audiência de instrução.")
        );
    }

    private ProcessoVerticalEtapa etapaInstrucaoEProva(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "INSTRUCAO_E_PROVA",
                "Instrução complementar, perícia e prova documental",
                "INSTRUTORIA",
                "CONTADORIA__CONTADORIA_OPERACAO",
                "instrucao_trabalhista",
                "violet",
                prazo.marcosCriticos() > 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA"),
                merge(prazo.proximaOndaOperacional(), List.of("juntar_ctps", "juntar_contracheques", "aguardar_laudo_pericial_se_houver")),
                List.of("carteira_de_trabalho", "holerites", "quesitos_periciais"),
                List.of("MAGISTRADO_DIRETO", "ADVOGADO_DIRETO"),
                List.of("Perícia contábil ou médica pode estender o prazo da instrução; o prazo do laudo é de 30 dias prorrogáveis.")
        );
    }

    private ProcessoVerticalEtapa etapaSentencaEPublicacao(ProcessoUnificadoAggregate unificado, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "SENTENCA_TRABALHISTA",
                "Sentença, publicação e ciência das partes",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "sentenca_publicacao",
                "red",
                documental.minutas() == 0,
                atosPorCategoria(unificado, "MERITO", "TRILHA"),
                merge(documental.trilhaAssinavel(), List.of("publicar_pelo_pje_ou_dje", "abrir_prazo_para_recurso_ordinario")),
                List.of("sentenca_assinada", "publicacao_dj"),
                List.of("ADVOGADO_DIRETO", "MINISTERIO_PUBLICO_DO_TRABALHO__MPT_TITULAR"),
                List.of("Prazo para recurso ordinário é de 8 dias; petições avulsas não interrompem o prazo recursal trabalhista.")
        );
    }

    private ProcessoVerticalEtapa etapaRecursoEDeposito(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo, ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "RECURSO_E_DEPOSITO_RECURSAL",
                "Recurso ordinário, depósito recursal e contrarrazões",
                "RECURSAL",
                "ADVOGADO_DIRETO",
                "recurso_trabalhista",
                "orange",
                prazo.marcosVencidos() > 0,
                atosPorCategoria(unificado, "RECURSAL", "BASE"),
                merge(prazo.proximaOndaOperacional(), List.of("checar_deposito_recursal_obrigatorio", "checar_custas_pagas", "aguardar_contrarrazoes")),
                List.of("guia_deposito_recursal", "comprovante_custas", "recurso_ordinario"),
                List.of("PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "CONTADORIA__CONTADORIA_OPERACAO"),
                List.of("Depósito recursal é pressuposto de admissibilidade do recurso ordinário e não pode ser sanado após a interposição.", "Isenção de depósito recursal se aplica a empregador com menos de dois empregados na data da sentença.")
        );
    }

    private ProcessoVerticalEtapa etapaExecucaoELiquidacao(ProcessoUnificadoAggregate unificado, ProcessoPrazoAggregate prazo, ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "EXECUCAO_E_LIQUIDACAO",
                "Execução trabalhista, liquidação e satisfação do crédito",
                "EXECUCAO",
                "CONTADORIA__CONTADORIA_OPERACAO",
                "execucao_trabalhista",
                "amber",
                prazo.marcosVencidos() > 0 || trabalho.vencidos() > 0,
                atosPorCategoria(unificado, "EXECUCAO", "TRILHA", "MERITO"),
                merge(trabalho.proximoMelhorFluxo(), List.of("emitir_gru_execucao", "penhora_online_bacenjud", "liquidacao_de_sentenca_por_calculo")),
                List.of("memoria_de_calculo", "guia_gru_execucao", "certidao_objeto_e_pe"),
                List.of("MAGISTRADO_DIRETO", "CONTADORIA__CONTADORIA_OPERACAO"),
                List.of("Execução trabalhista corre nos próprios autos; não há processo autônomo de execução como no CPC.", "FGTS Digital integra o cálculo de liquidação e deve ser verificado antes da homologação.")
        );
    }

    private boolean isSumarissimo(ProcessoUnificadoAggregate unificado) {
        String rito = unificado.competencia().ritoProcessual();
        return rito != null && normalize(rito).contains("SUMARISSIMO");
    }

    private ProcessoVerticalLane toLane(ProcessoPapelPerfil perfil) {
        ArrayList<String> authorityBands = new ArrayList<>();
        if (!perfil.preparar().isEmpty()) authorityBands.add("PREPARAR");
        if (!perfil.aprovar().isEmpty()) authorityBands.add("APROVAR");
        if (!perfil.assinar().isEmpty()) authorityBands.add("ASSINAR");
        if (!perfil.peticionar().isEmpty()) authorityBands.add("PETICIONAR");
        if (!perfil.recorrer().isEmpty()) authorityBands.add("RECORRER");
        return new ProcessoVerticalLane(
                perfil.codigo(),
                perfil.nomeExibicao(),
                perfil.painel(),
                perfil.accentColor(),
                perfil.trustFloor(),
                List.copyOf(authorityBands),
                topHints(perfil),
                perfil.separadores(),
                perfil.guardas()
        );
    }

    private List<String> atosPorCategoria(ProcessoUnificadoAggregate unificado, String... eixos) {
        return merge(unificado.atosPermitidos(), unificado.atosBloqueados()).stream()
                .filter(ato -> matchesAny(ato, eixos))
                .map(ProcessoUnificadoAto::codigo)
                .distinct()
                .limit(10)
                .toList();
    }

    private boolean matchesAny(ProcessoUnificadoAto ato, String... eixos) {
        String composed = normalize(ato.categoria()) + '|' + normalize(ato.eixoOperacional()) + '|' + normalize(ato.workItemType());
        for (String eixo : eixos) {
            if (composed.contains(normalize(eixo))) {
                return true;
            }
        }
        return false;
    }

    private List<String> topHints(ProcessoPapelPerfil perfil) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        perfil.preparar().stream().limit(2).forEach(hints::add);
        perfil.aprovar().stream().limit(2).forEach(hints::add);
        perfil.assinar().stream().limit(2).forEach(hints::add);
        perfil.peticionar().stream().limit(2).forEach(hints::add);
        perfil.recorrer().stream().limit(2).forEach(hints::add);
        return List.copyOf(hints);
    }

    private <T> List<T> merge(List<T> left, List<T> right) {
        LinkedHashSet<T> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return merged.stream().filter(Objects::nonNull).toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U').replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
