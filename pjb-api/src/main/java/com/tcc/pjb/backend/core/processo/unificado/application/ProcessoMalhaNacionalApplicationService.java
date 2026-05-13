package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.identidade.vinculo.application.IdentidadeJuridicaVinculoApplicationService;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoAggregate;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoSolicitacao;
import com.tcc.pjb.backend.core.processo.conexao.application.ProcessoConexaoApplicationService;
import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoAggregate;
import com.tcc.pjb.backend.core.processo.dependencia.application.ProcessoDependenciaApplicationService;
import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoApplicationService;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloProbatorioApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloProbatorioAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalRisco;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaNacionalApplicationService {

    private final ProcessoRepository processoRepository;
    private final IdentidadeJuridicaVinculoApplicationService identidadeJuridicaVinculoApplicationService;
    private final ProcessoPrevencaoApplicationService processoPrevencaoApplicationService;
    private final ProcessoConexaoApplicationService processoConexaoApplicationService;
    private final ProcessoDependenciaApplicationService processoDependenciaApplicationService;
    private final ProcessoSigiloProbatorioApplicationService processoSigiloProbatorioApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;

    public ProcessoMalhaNacionalApplicationService(ProcessoRepository processoRepository,
                                                   IdentidadeJuridicaVinculoApplicationService identidadeJuridicaVinculoApplicationService,
                                                   ProcessoPrevencaoApplicationService processoPrevencaoApplicationService,
                                                   ProcessoConexaoApplicationService processoConexaoApplicationService,
                                                   ProcessoDependenciaApplicationService processoDependenciaApplicationService,
                                                   ProcessoSigiloProbatorioApplicationService processoSigiloProbatorioApplicationService,
                                                   ProcessoTimelineApplicationService processoTimelineApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.identidadeJuridicaVinculoApplicationService = Objects.requireNonNull(identidadeJuridicaVinculoApplicationService);
        this.processoPrevencaoApplicationService = Objects.requireNonNull(processoPrevencaoApplicationService);
        this.processoConexaoApplicationService = Objects.requireNonNull(processoConexaoApplicationService);
        this.processoDependenciaApplicationService = Objects.requireNonNull(processoDependenciaApplicationService);
        this.processoSigiloProbatorioApplicationService = Objects.requireNonNull(processoSigiloProbatorioApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
    }

    @Transactional(readOnly = true)
    public ProcessoMalhaNacionalAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        IdentidadeJuridicaVinculoAggregate vinculo = identidadeJuridicaVinculoApplicationService.analisar(new IdentidadeJuridicaVinculoSolicitacao(
                processoId,
                processo.getNumero(),
                true,
                "MALHA_NACIONAL",
                "PROCESSO_MALHA_NACIONAL"
        ));
        ProcessoVinculacaoAnaliseConsulta consulta = new ProcessoVinculacaoAnaliseConsulta(
                processoId,
                processo.getNumero(),
                "MALHA_NACIONAL",
                "PROCESSO_MALHA_NACIONAL"
        );
        ProcessoPrevencaoAggregate prevencao = processoPrevencaoApplicationService.analisar(consulta);
        ProcessoConexaoAggregate conexao = processoConexaoApplicationService.analisar(consulta);
        ProcessoDependenciaAggregate dependencia = processoDependenciaApplicationService.analisar(consulta);
        ProcessoSigiloProbatorioAggregate sigiloProbatorio = processoSigiloProbatorioApplicationService.avaliar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);

        ArrayList<ProcessoMalhaNacionalRisco> riscos = new ArrayList<>();
        LinkedHashSet<String> hotspots = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(vinculo.resolucao().conflitos());
        fundamentos.addAll(prevencao.fundamentos());
        fundamentos.addAll(conexao.fundamentos());
        fundamentos.addAll(dependencia.fundamentos());
        fundamentos.addAll(sigiloProbatorio.fundamentos());
        fundamentos.addAll(timeline.alertas());

        if (!vinculo.alertas().isEmpty()) {
            hotspots.add("IDENTIDADE");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "MALHA_IDENTIDADE_ATIVA",
                    "IDENTIDADE",
                    vinculo.alertas().stream().anyMatch(alerta -> alerta.contains("ambígua")) ? "ALTO" : "ATENCAO",
                    vinculo.alertas().stream().anyMatch(alerta -> alerta.contains("ambígua")),
                    "A malha de identidade projetou alertas processuais relevantes",
                    vinculo.alertas().getFirst(),
                    "Saneie a identidade e confirme representação antes de distribuir, reunir ou restringir.",
                    vinculo.alertas()
            ));
        }
        if (prevencao.haPrevencao()) {
            hotspots.add("PREVENCAO");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "PREVENCAO_DETECTADA",
                    "DISTRIBUICAO",
                    "CRITICO",
                    true,
                    "Há prevenção ou remessa prioritária para outro feito/unidade",
                    Objects.toString(prevencao.processoPrevento(), ""),
                    "Trave a distribuição livre e remeta conforme o prevento identificado.",
                    prevencao.fundamentos()
            ));
        }
        if (conexao.haConexao()) {
            hotspots.add("CONEXAO");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "CONEXAO_MATERIAL",
                    "CONEXAO",
                    conexao.totalConexos() >= 3 ? "ALTO" : "ATENCAO",
                    false,
                    "Há processos materialmente conectados ao feito raiz",
                    "Total correlato=" + conexao.totalConexos(),
                    "Avalie reunião de feitos, distribuição por dependência e expansão da prova comum.",
                    conexao.fundamentos()
            ));
        }
        if (dependencia.haDependencia()) {
            hotspots.add("DEPENDENCIA");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "DEPENDENCIA_PROCESSUAL",
                    "DEPENDENCIA",
                    dependencia.itens().stream().anyMatch(item -> item.bloquearFluxo()) ? "CRITICO" : "ALTO",
                    dependencia.itens().stream().anyMatch(item -> item.bloquearFluxo()),
                    "Há dependência processual com reflexo na marcha do feito",
                    dependencia.itens().isEmpty() ? "" : dependencia.itens().getFirst().natureza(),
                    "Encadeie o rito com o feito principal antes de praticar atos incompatíveis.",
                    dependencia.fundamentos()
            ));
        }
        if (sigiloProbatorio.exigeReclassificacao()) {
            hotspots.add("SIGILO_PROBATORIO");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "RECLASSIFICACAO_SIGILO_PROBATORIO",
                    "SIGILO",
                    "ALTO",
                    false,
                    "A prova recomenda patamar de sigilo superior ao do processo",
                    sigiloProbatorio.nivelAtual().name() + " -> " + sigiloProbatorio.nivelRecomendado().name(),
                    "Reclassifique o processo e ajuste o perímetro de acesso conforme a prova sensível.",
                    sigiloProbatorio.alertas()
            ));
        }
        if (sigiloProbatorio.provasCompartilhadas() > 0) {
            hotspots.add("EVIDENCIA");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "PROVA_COMPARTILHADA",
                    "EVIDENCIA",
                    "ATENCAO",
                    false,
                    "A prova já circula em outros feitos correlatos",
                    "Compartilhamentos=" + sigiloProbatorio.provasCompartilhadas(),
                    "Aplique cadeia de custódia, governança de acesso e reaproveitamento controlado.",
                    sigiloProbatorio.alertas()
            ));
        }
        if (timeline.totalBloqueantes() > 0) {
            hotspots.add("TIMELINE");
            riscos.add(new ProcessoMalhaNacionalRisco(
                    "BLOQUEIO_OPERACIONAL",
                    "TIMELINE",
                    "CRITICO",
                    true,
                    "A linha do tempo indica bloqueios no próximo passo útil",
                    "Bloqueios=" + timeline.totalBloqueantes(),
                    "Resolva as pendências críticas antes de avançar o rito.",
                    timeline.alertas()
            ));
        }
        return new ProcessoMalhaNacionalAggregate(
                processoId,
                processo.getNumero(),
                safeName(processo.getRamoDireito()),
                vinculo.grafo().vertices().size(),
                conexao.totalConexos() + (prevencao.haPrevencao() ? 1 : 0) + dependencia.itens().size(),
                sigiloProbatorio.documentosCriticos(),
                riscos.stream().filter(ProcessoMalhaNacionalRisco::bloqueante).count(),
                processo.getNivelSigilo(),
                sigiloProbatorio.nivelRecomendado(),
                riscos.stream().anyMatch(ProcessoMalhaNacionalRisco::bloqueante),
                List.copyOf(hotspots),
                List.copyOf(riscos),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    private String safeName(Object value) {
        return value == null ? "NAO_INFORMADO" : value.toString();
    }
}
