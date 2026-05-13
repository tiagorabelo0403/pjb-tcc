package com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application;

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
public class ProcessoVerticalPenalCustodiaApplicationService {

    private static final List<String> PROFILE_CODES = List.of(
            "MAGISTRADO_DIRETO",
            "PROMOTORIA__PROMOTORIA_TITULAR",
            "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
            "DELEGACIA__DELEGACIA_TITULAR",
            "POLICIA_PENAL__POLICIA_PENAL_CUSTODIA",
            "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_CUSTODIA",
            "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_DIRECAO"
    );

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;

    public ProcessoVerticalPenalCustodiaApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
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
                etapaAutuacaoFlagrante(unificado, documental),
                etapaAudienciaCustodia(unificado, prazo, trabalho),
                etapaManifestacoesTitulares(unificado, documental),
                etapaDecisaoEComunicacao(unificado, documental),
                etapaApresentacaoEscoltaECertidao(unificado, execucao, timeline),
                etapaRecursalOuExecucaoPenal(unificado, recursal, execucao, prazo)
        );
        LinkedHashSet<String> chips = new LinkedHashSet<>();
        chips.add("penal");
        chips.add("custodia");
        chips.add("cor=crimson");
        chips.addAll(timeline.eixosAtivos());
        LinkedHashSet<String> alertas = new LinkedHashSet<>(prazo.alertasEstruturais());
        alertas.addAll(trabalho.gates());
        alertas.addAll(execucao.alertas());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(unificado.competencia().fundamentos());
        fundamentos.add("A fatia penal custodial precisa unir flagrante, audiência de custódia, manifestação titular, comunicação à unidade e prova material de apresentação.");
        fundamentos.add("Custódia não é subfluxo oculto: ela precisa de painel, cor, certidão, escolta e trilha recursal próprias.");
        return new ProcessoVerticalAggregate(
                "PENAL_CUSTODIA",
                "Fatia vertical penal com custódia",
                unificado.identity(),
                unificado.competencia().ritoProcessual(),
                unificado.competencia().faseProcessual(),
                unificado.competencia().statusProcessual(),
                etapas.size(),
                lanes.size(),
                prazo.marcosCriticos() + execucao.totalBloqueantes(),
                etapas.stream().mapToLong(item -> item.handoffTo().size()).sum(),
                lanes,
                etapas,
                List.copyOf(chips),
                merge(merge(execucao.proximoMelhorPasso(), recursal.proximosPassos()), unificado.proximoMelhorAto()),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoVerticalEtapa etapaAutuacaoFlagrante(ProcessoUnificadoAggregate unificado, ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "AUTUACAO_FLAGRANTE",
                "Autuação do flagrante e prova material inicial",
                "AUDIENCIA_CUSTODIA",
                "DELEGACIA__DELEGACIA_TITULAR",
                "autuacao_flg",
                "crimson",
                documental.totalDocumentos() == 0,
                atos(unificado, "BASE", "MERITO"),
                List.of("validar_auto_prisao", "conferir_materialidade", "registrar_horario_apresentacao"),
                List.of("auto_de_prisao_em_flagrante", "nota_de_culpa", "prova_material_inicial"),
                List.of("MAGISTRADO_DIRETO", "PROMOTORIA__PROMOTORIA_TITULAR", "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR"),
                List.of("A custódia começa na prova material e na cadeia de apresentação, não apenas na audiência.")
        );
    }

    private ProcessoVerticalEtapa etapaAudienciaCustodia(ProcessoUnificadoAggregate unificado,
                                                         ProcessoPrazoAggregate prazo,
                                                         ProcessoTrabalhoAggregate trabalho) {
        return new ProcessoVerticalEtapa(
                "AUDIENCIA_DE_CUSTODIA",
                "Audiência de custódia e controle judicial imediato",
                "AUDIENCIA_CUSTODIA",
                "MAGISTRADO_DIRETO",
                "audiencia_custodia",
                "red",
                prazo.marcosCriticos() > 0 || trabalho.bloqueantes() > 0,
                atos(unificado, "MERITO", "BASE"),
                merge(prazo.proximaOndaOperacional(), List.of("validar_integridade_fisica", "ouvir_mp_e_defesa", "checar_legalidade_da_prisao")),
                List.of("ata_audiencia_custodia", "registro_mp", "registro_defesa"),
                List.of("POLICIA_PENAL__POLICIA_PENAL_CUSTODIA", "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_CUSTODIA"),
                List.of("Audiência de custódia exige relógio processual curto, painéis de urgência e fluxo explícito para unidade custodiante.")
        );
    }

    private ProcessoVerticalEtapa etapaManifestacoesTitulares(ProcessoUnificadoAggregate unificado,
                                                              ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "MANIFESTACOES_MP_E_DEFESA",
                "Manifestações do MP e da defesa técnica",
                "CONHECIMENTO",
                "PROMOTORIA__PROMOTORIA_TITULAR",
                "manifestacoes_titulares",
                "orange",
                documental.minutas() == 0,
                atos(unificado, "MERITO", "TRILHA", "RECURSAL"),
                List.of("garantir_vista_ao_mp", "garantir_vista_a_defesa", "segregar_assessoria_e_titularidade"),
                List.of("parecer_mp", "manifestacao_defesa", "documentos_medicos_ou_sociais_se_houver"),
                List.of("MAGISTRADO_DIRETO"),
                List.of("No penal custodial, parecer ministerial e defesa técnica precisam aparecer como eixos próprios do processo vivo.")
        );
    }

    private ProcessoVerticalEtapa etapaDecisaoEComunicacao(ProcessoUnificadoAggregate unificado,
                                                           ProcessoDocumentoAggregate documental) {
        return new ProcessoVerticalEtapa(
                "DECISAO_E_COMUNICACAO_CUSTODIAL",
                "Decisão, comunicação e ordens dirigidas à custódia",
                "CONHECIMENTO",
                "MAGISTRADO_DIRETO",
                "decisao_custodial",
                "purple",
                documental.assinados() == 0,
                atos(unificado, "MERITO", "EXECUCAO"),
                merge(documental.trilhaAssinavel(), List.of("assinar_decisao", "expedir_ordem_para_unidade", "comunicar_escolta_ou_alvara")),
                List.of("decisao_assinada", "mandado_ou_alvara", "comunicacao_unidade_custodiante"),
                List.of("UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_DIRECAO", "POLICIA_PENAL__POLICIA_PENAL_CUSTODIA"),
                List.of("A decisão penal com custódia tem efeito material imediato; por isso a comunicação operacional entra na mesma fatia.")
        );
    }

    private ProcessoVerticalEtapa etapaApresentacaoEscoltaECertidao(ProcessoUnificadoAggregate unificado,
                                                                    ProcessoExecucaoAggregate execucao,
                                                                    ProcessoTimelineAggregate timeline) {
        return new ProcessoVerticalEtapa(
                "APRESENTACAO_ESCOLTA_E_CERTIDAO",
                "Apresentação, escolta, custódia e certidão operacional",
                "EXECUCAO",
                "POLICIA_PENAL__POLICIA_PENAL_CUSTODIA",
                "apresentacao_escolta",
                "brown",
                execucao.totalBloqueantes() > 0,
                atos(unificado, "EXECUCAO", "BASE"),
                merge(execucao.proximoMelhorPasso(), timeline.proximoCiclo()),
                List.of("certidao_operacional", "registro_de_apresentacao", "prova_material_de_custodia"),
                List.of("UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_CUSTODIA", "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_DIRECAO"),
                List.of("A materialidade da custódia precisa ser certificada por quem executa a apresentação e por quem recebe o custodiado.")
        );
    }

    private ProcessoVerticalEtapa etapaRecursalOuExecucaoPenal(ProcessoUnificadoAggregate unificado,
                                                               ProcessoRecursalAggregate recursal,
                                                               ProcessoExecucaoAggregate execucao,
                                                               ProcessoPrazoAggregate prazo) {
        return new ProcessoVerticalEtapa(
                "RECURSAL_OU_EXECUCAO_PENAL",
                "Recursos, embargos e eventual transição para execução penal",
                "RECURSAL",
                "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
                "recursal_penal",
                "slate",
                prazo.marcosVencidos() > 0,
                merge(recursal.janelas().stream().map(item -> item.code()).toList(), execucao.trilhas().stream().map(item -> item.code()).toList()),
                merge(recursal.proximosPassos(), List.of("checar_tempestividade", "separar_recurso_de_execucao_penal")),
                List.of("recurso_ou_embargos", "peca_de_execucao_penal_quando_cabivel"),
                List.of("PROMOTORIA__PROMOTORIA_TITULAR", "MAGISTRADO_DIRETO"),
                List.of("A saída da custódia pode abrir trilha recursal ou de execução penal, mas nunca como bloco único indistinto.")
        );
    }

    private ProcessoVerticalLane toLane(ProcessoPapelPerfil perfil) {
        ArrayList<String> authorityBands = new ArrayList<>();
        if (!perfil.receber().isEmpty()) authorityBands.add("RECEBER");
        if (!perfil.preparar().isEmpty()) authorityBands.add("PREPARAR");
        if (!perfil.assinar().isEmpty()) authorityBands.add("ASSINAR");
        if (!perfil.certificar().isEmpty()) authorityBands.add("CERTIFICAR");
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

    private List<String> atos(ProcessoUnificadoAggregate unificado, String... axes) {
        return merge(unificado.atosPermitidos(), unificado.atosBloqueados()).stream()
                .filter(ato -> match(ato, axes))
                .map(ProcessoUnificadoAto::codigo)
                .distinct()
                .limit(12)
                .toList();
    }

    private boolean match(ProcessoUnificadoAto ato, String... axes) {
        String token = normalize(ato.categoria()) + '|' + normalize(ato.eixoOperacional()) + '|' + normalize(ato.titulo());
        for (String axis : axes) {
            if (token.contains(normalize(axis))) {
                return true;
            }
        }
        return ato.alertas().stream().map(this::normalize).anyMatch(item -> item.contains("CUSTOD"));
    }

    private List<String> topHints(ProcessoPapelPerfil perfil) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        perfil.receber().stream().limit(2).forEach(hints::add);
        perfil.certificar().stream().limit(2).forEach(hints::add);
        perfil.assinar().stream().limit(2).forEach(hints::add);
        perfil.recorrer().stream().limit(2).forEach(hints::add);
        return List.copyOf(hints);
    }

    private <T> List<T> merge(List<T> left, List<T> right) {
        LinkedHashSet<T> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return merged.stream().filter(java.util.Objects::nonNull).toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
