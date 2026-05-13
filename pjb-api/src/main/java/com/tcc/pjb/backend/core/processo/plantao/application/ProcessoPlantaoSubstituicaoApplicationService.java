package com.tcc.pjb.backend.core.processo.plantao.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoRegra;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoSubstituicaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoResponsabilidadeOperacional;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPlantaoSubstituicaoApplicationService {

    private static final String DEFAULT_PROFILE = "MAGISTRATURA__MAGISTRADO_TITULAR";

    private final ProcessoRepository processoRepository;
    private final InstitutionalOperationalCoverageApplicationService institutionalOperationalCoverageApplicationService;
    private final ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService;
    private final ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoPlantaoSubstituicaoApplicationService(ProcessoRepository processoRepository,
                                                         InstitutionalOperationalCoverageApplicationService institutionalOperationalCoverageApplicationService,
                                                         ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService,
                                                         ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService,
                                                         ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.institutionalOperationalCoverageApplicationService = Objects.requireNonNull(institutionalOperationalCoverageApplicationService);
        this.processoSigiloInteligenteApplicationService = Objects.requireNonNull(processoSigiloInteligenteApplicationService);
        this.processoPreGravacaoApplicationService = Objects.requireNonNull(processoPreGravacaoApplicationService);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoPlantaoSubstituicaoAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoSigiloInteligenteAggregate sigiloInteligente = processoSigiloInteligenteApplicationService.avaliar(processoId);
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        String acaoReferencia = unificado.atosPermitidos().stream().findFirst().map(ProcessoUnificadoAto::codigo).orElse("VISUALIZAR_PROCESSO");
        ProcessoPreGravacaoAggregate preGravacao = processoPreGravacaoApplicationService.avaliar(processoId, DEFAULT_PROFILE, acaoReferencia);
        String unidadeCodigo = firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara(), processo.getTribunal());
        List<InstitutionalOperationalCoverageRule> regras = blank(unidadeCodigo)
                ? List.of()
                : institutionalOperationalCoverageApplicationService.listar(unidadeCodigo);

        ArrayList<ProcessoPlantaoRegra> regrasAtivas = new ArrayList<>();
        for (InstitutionalOperationalCoverageRule regra : regras) {
            regrasAtivas.add(new ProcessoPlantaoRegra(
                    regra.ruleId(),
                    regra.tipoCobertura().name(),
                    regra.status().name(),
                    regra.titularUsuarioId(),
                    regra.coberturaUsuarioId(),
                    regra.inicioVigencia(),
                    regra.fimVigencia(),
                    regra.capacidades().stream().map(Enum::name).sorted().toList(),
                    precedencia(regra.tipoCobertura().name()),
                    firstNonBlank(regra.motivo(), regra.observacoes())
            ));
        }
        regrasAtivas.sort(Comparator.comparing(ProcessoPlantaoRegra::precedencia).reversed().thenComparing(ProcessoPlantaoRegra::tipoCobertura));

        boolean plantaoAtivo = regrasAtivas.stream().anyMatch(item -> "PLANTAO".equals(item.tipoCobertura()));
        boolean substituicaoAtiva = regrasAtivas.stream().anyMatch(item -> item.tipoCobertura().contains("SUBSTITUICAO"));
        boolean delegacaoAtiva = regrasAtivas.stream().anyMatch(item -> item.tipoCobertura().contains("DELEGACAO"));
        boolean titularObrigatorio = sigiloInteligente.decretoExclusivoMagistrado() || preGravacao.stepUpTriggers() > 0 || sigiloInteligente.operacaoPolicialSigilosa();

        ArrayList<ProcessoResponsabilidadeOperacional> responsabilidades = new ArrayList<>();
        responsabilidades.add(new ProcessoResponsabilidadeOperacional(
                "MAGISTRADO_NATURAL",
                "Regra principal de decisão e decretos sensíveis do processo.",
                plantaoAtivo ? "PLANTAO_JUDICIAL" : "ROTINA",
                true,
                true,
                List.of("ASSINATURA_FORTE", "DECISAO_TITULAR", "REVISAO_DE_SIGILO"),
                List.of("CAIXA_PJB", "EMAIL_INSTITUCIONAL")
        ));
        if (sigiloInteligente.operacaoPolicialSigilosa()) {
            responsabilidades.add(new ProcessoResponsabilidadeOperacional(
                    "DELEGADO_COMPATIVEL",
                    "Operação policial sigilosa com acesso inicial restrito.",
                    "ANEL_RESTRITO",
                    false,
                    true,
                    List.of("CREDENCIAL_SIGILO_FORTE", "MINIMIZACAO_DE_DADOS"),
                    List.of("CAIXA_PJB_RESTRITA", "EMAIL_SEGURO")
            ));
        }
        if (substituicaoAtiva || delegacaoAtiva || plantaoAtivo) {
            responsabilidades.add(new ProcessoResponsabilidadeOperacional(
                    substituicaoAtiva ? "SUBSTITUTO_ATIVO" : plantaoAtivo ? "PLANTONISTA_ATIVO" : "DELEGADO_OPERACIONAL",
                    "Cobertura operacional ativa para evitar caixa sem posse e sem resposta institucional.",
                    substituicaoAtiva ? "SUBSTITUICAO" : plantaoAtivo ? "PLANTAO" : "DELEGACAO",
                    titularObrigatorio,
                    preGravacao.stepUpTriggers() > 0,
                    List.of("TRILHA_IMUTAVEL_DE_POSSE", "AUDITORIA_FORTE"),
                    List.of("CAIXA_PJB", "NOTIFICACAO_INTERNA", "EMAIL_INSTITUCIONAL")
            ));
        }
        if (!sigiloInteligente.operacaoPolicialSigilosa()) {
            responsabilidades.add(new ProcessoResponsabilidadeOperacional(
                    "SERVIDOR_DA_UNIDADE",
                    "Serviço ordinário da unidade com segregação entre preparar e decidir.",
                    "ROTINA",
                    false,
                    preGravacao.stepUpTriggers() > 0,
                    List.of("SEGREGACAO_TITULAR", "TRILHA_DE_PREPARACAO"),
                    List.of("CAIXA_PJB", "NOTIFICACAO_INTERNA")
            ));
        }

        LinkedHashSet<String> escalonamento = new LinkedHashSet<>();
        if (sigiloInteligente.operacaoPolicialSigilosa()) {
            escalonamento.add("JUIZ_NATURAL");
            escalonamento.add("DELEGADO_COMPATIVEL");
            escalonamento.add("NUCLEO_RESTRITO_DA_UNIDADE");
        } else if (plantaoAtivo) {
            escalonamento.add("PLANTONISTA_ATIVO");
            escalonamento.add("MAGISTRADO_NATURAL");
            escalonamento.add("SERVIDOR_DA_UNIDADE");
        } else if (substituicaoAtiva) {
            escalonamento.add("SUBSTITUTO_ATIVO");
            escalonamento.add("TITULAR_DE_ORIGEM");
            escalonamento.add("SECRETARIA_DA_UNIDADE");
        } else {
            escalonamento.add("UNIDADE_TITULAR");
            escalonamento.add("SECRETARIA_DA_UNIDADE");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(sigiloInteligente.fundamentos());
        fundamentos.addAll(preGravacao.fundamentos());
        fundamentos.add("Plantão e substituição agora são tratados como domínio processual pesado, com vigência, precedência, sobreposição e responsabilidade.");
        fundamentos.add("Toda cobertura ativa precisa preservar posse transitória, segregação de autoridade e trilha auditável do substituto ou plantonista.");

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (blank(unidadeCodigo)) {
            alertas.add("Processo sem unidade materializada impede leitura forte de plantão e substituição.");
        }
        if (titularObrigatorio && (substituicaoAtiva || plantaoAtivo)) {
            alertas.add("Mesmo com cobertura ativa, decreto e ato final continuam reservados ao titular ou magistrado natural.");
        }
        if (regrasAtivas.isEmpty()) {
            alertas.add("Nenhuma regra operacional ativa foi encontrada para a unidade do processo.");
        }

        String regimeAtivo = plantaoAtivo ? "PLANTAO" : substituicaoAtiva ? "SUBSTITUICAO" : delegacaoAtiva ? "DELEGACAO" : "ROTINA";
        String responsavelAtual = escalonamento.stream().findFirst().orElse("UNIDADE_TITULAR");
        return new ProcessoPlantaoSubstituicaoAggregate(
                processo.getId(),
                processo.getNumeroProcesso(),
                unidadeCodigo,
                regimeAtivo,
                plantaoAtivo,
                substituicaoAtiva,
                delegacaoAtiva,
                titularObrigatorio,
                responsavelAtual,
                List.copyOf(regrasAtivas),
                List.copyOf(responsabilidades),
                List.copyOf(escalonamento),
                List.copyOf(fundamentos),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private int precedencia(String tipoCobertura) {
        return switch (tipoCobertura) {
            case "PLANTAO" -> 300;
            case "SUBSTITUICAO_PROGRAMADA" -> 200;
            case "DELEGACAO_PROGRAMADA" -> 100;
            default -> 10;
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
