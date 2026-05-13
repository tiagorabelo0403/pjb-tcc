package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaPapelAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRiscoMalhaAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
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
public class ProcessoPainelMalhaPapelApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoPainelMalhaNacionalApplicationService processoPainelMalhaNacionalApplicationService;
    private final ProcessoPainelRiscoMalhaApplicationService processoPainelRiscoMalhaApplicationService;

    public ProcessoPainelMalhaPapelApplicationService(ProcessoRepository processoRepository,
                                                      ProcessoPainelMalhaNacionalApplicationService processoPainelMalhaNacionalApplicationService,
                                                      ProcessoPainelRiscoMalhaApplicationService processoPainelRiscoMalhaApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoPainelMalhaNacionalApplicationService = Objects.requireNonNull(processoPainelMalhaNacionalApplicationService);
        this.processoPainelRiscoMalhaApplicationService = Objects.requireNonNull(processoPainelRiscoMalhaApplicationService);
    }

    @Transactional(readOnly = true)
    public ProcessoPainelMalhaPapelAggregate detalhar(Long processoId, TipoUsuario tipoUsuario) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        return detalhar(processoId, tipoUsuario, processo.getRamoDireito());
    }

    @Transactional(readOnly = true)
    public ProcessoPainelMalhaPapelAggregate detalhar(Long processoId, TipoUsuario tipoUsuario, RamoDireito ramoDireito) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        TipoUsuario papel = tipoUsuario == null ? TipoUsuario.CIDADAO : tipoUsuario;
        RamoDireito ramo = ramoDireito != null ? ramoDireito : processo.getRamoDireito();
        ProcessoPainelMalhaNacionalAggregate painelBase = processoPainelMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoPainelRiscoMalhaAggregate painelRisco = processoPainelRiscoMalhaApplicationService.detalhar(processoId);
        ArrayList<ProcessoPainelContextualWidget> widgets = new ArrayList<>();
        widgets.addAll(baseWidgets(painelBase, painelRisco));
        widgets.addAll(roleWidgets(processoId, ramo, papel, painelRisco));
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(painelBase.fundamentos());
        fundamentos.addAll(painelRisco.fundamentos());
        fundamentos.add("papel=" + papel.name());
        fundamentos.add("ramo=" + (ramo == null ? "NAO_INFORMADO" : ramo.name()));
        String status = painelRisco.possuiBloqueio() ? "CRITICO" : painelRisco.scoreGlobal() >= 70 ? "ALTO" : "ESTAVEL";
        return new ProcessoPainelMalhaPapelAggregate(
                processoId,
                processo.getNumero(),
                papel.name(),
                ramo == null ? "NAO_INFORMADO" : ramo.name(),
                status,
                List.copyOf(widgets),
                List.copyOf(fundamentos.stream().limit(80).toList()),
                Instant.now()
        );
    }

    private List<ProcessoPainelContextualWidget> baseWidgets(ProcessoPainelMalhaNacionalAggregate painelBase,
                                                             ProcessoPainelRiscoMalhaAggregate painelRisco) {
        ArrayList<ProcessoPainelContextualWidget> widgets = new ArrayList<>(painelBase.widgets().stream().limit(2).toList());
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_RISCO_GLOBAL",
                "Leitura consolidada da malha",
                "RISK",
                painelRisco.statusGeral(),
                painelRisco.possuiBloqueio() ? "RED" : painelRisco.scoreGlobal() >= 70 ? "AMBER" : "GREEN",
                painelRisco.scoreGlobal() + " pontos de risco",
                painelRisco.widgets().isEmpty() ? "Sem widgets adicionais" : painelRisco.widgets().getFirst().title(),
                painelRisco.fundamentos().stream().limit(4).toList(),
                "/api/v1/processual/painel/risco-malha/" + painelBase.processoId()
        ));
        return List.copyOf(widgets);
    }

    private List<ProcessoPainelContextualWidget> roleWidgets(Long processoId,
                                                             RamoDireito ramoDireito,
                                                             TipoUsuario papel,
                                                             ProcessoPainelRiscoMalhaAggregate painelRisco) {
        ArrayList<ProcessoPainelContextualWidget> widgets = new ArrayList<>();
        if (papel.isMagistratura()) {
            widgets.add(widget("MALHA_JUIZ_DECISAO", "Decisão assistida de prevenção/conexão", "DECISION", painelRisco.possuiBloqueio() ? "PRIORITARIA" : "PRONTA", painelRisco.possuiBloqueio() ? "RED" : "BLUE", "Ato sugerido: saneamento de malha", branchSubtitle(ramoDireito, "Controle jurisdicional e coerência entre feitos"), painelRisco.fundamentos(), "/api/v1/processual/distribuicao/" + processoId + "/malha"));
        } else if (papel.isServidorJudiciario() || papel.isAssessor()) {
            widgets.add(widget("MALHA_SECRETARIA", "Triagem de gabinete e secretaria", "QUEUE", painelRisco.possuiBloqueio() ? "TRIAGEM" : "OPERACIONAL", painelRisco.possuiBloqueio() ? "AMBER" : "BLUE", "Fila orientada pela malha", branchSubtitle(ramoDireito, "Saneamento, remessa e checklist de prevenção"), painelRisco.fundamentos().stream().limit(5).toList(), "/api/v1/processual/timeline/" + processoId + "/malha"));
        } else if (papel.isAdvocacia()) {
            widgets.add(widget("MALHA_ADVOCACIA", "Leitura estratégica para a advocacia", "ACTION", painelRisco.statusGeral(), painelRisco.scoreGlobal() >= 70 ? "AMBER" : "GREEN", "Riscos de conexão, sigilo e prova compartilhada", branchSubtitle(ramoDireito, "Monitor de concentração probatória e risco de reunião"), painelRisco.fundamentos().stream().limit(4).toList(), "/api/v1/processual/unificado/" + processoId + "/malha-nacional"));
        } else if (papel.isMinisterioPublico() || papel.isDefensoriaPublica() || papel.isProcuradoria()) {
            widgets.add(widget("MALHA_INSTITUCIONAL", "Radar institucional de coerência", "NETWORK", painelRisco.statusGeral(), painelRisco.possuiBloqueio() ? "RED" : "PURPLE", "Clusters, dependências e prova correlata", branchSubtitle(ramoDireito, "Atuação coordenada entre feitos e órgãos"), painelRisco.fundamentos().stream().limit(4).toList(), "/api/v1/processual/painel/malha-nacional/" + processoId));
        } else if (papel.isAuxiliarJustica() || papel.isPerito() || papel.isSaude()) {
            widgets.add(widget("MALHA_AUXILIAR", "Perímetro probatório e custódia", "CHAIN", painelRisco.statusGeral(), painelRisco.possuiBloqueio() ? "RED" : "TEAL", "Acesso condicionado pela malha", branchSubtitle(ramoDireito, "Cadeia de custódia, lacre lógico e compartilhamento"), painelRisco.fundamentos().stream().limit(4).toList(), "/api/v1/processual/sigilo/" + processoId + "/probatorio"));
        } else {
            widgets.add(widget("MALHA_CIDADAO", "Situação operacional do processo", "STATUS", painelRisco.statusGeral(), painelRisco.possuiBloqueio() ? "AMBER" : "GREEN", "Fluxo observado pela malha nacional", branchSubtitle(ramoDireito, "Entenda bloqueios, correlações e próximos passos"), painelRisco.fundamentos().stream().limit(3).toList(), "/api/v1/processual/publico/" + processoId + "/situacao"));
        }
        if (painelRisco.scoreGlobal() >= 80) {
            widgets.add(widget("MALHA_ANTIFRAUDE", "Radar antifraude recomendado", "SHIELD", painelRisco.statusGeral(), painelRisco.possuiBloqueio() ? "RED" : "AMBER", "Acionamento recomendado para governança e segurança", branchSubtitle(ramoDireito, "A malha consolidou indicadores suficientes para escalonamento"), painelRisco.fundamentos().stream().limit(4).toList(), "/api/v1/processual/anomalia/" + processoId + "/antifraude"));
        }
        return List.copyOf(widgets);
    }

    private ProcessoPainelContextualWidget widget(String code,
                                                  String title,
                                                  String kind,
                                                  String status,
                                                  String accent,
                                                  String headline,
                                                  String subtitle,
                                                  List<String> insights,
                                                  String path) {
        return new ProcessoPainelContextualWidget(code, title, kind, status, accent, headline, subtitle, insights, path);
    }

    private String branchSubtitle(RamoDireito ramoDireito, String base) {
        if (ramoDireito == null) {
            return base;
        }
        return switch (ramoDireito) {
            case PENAL, MILITAR -> base + " com reforço de custódia e sigilo sensível";
            case PREVIDENCIARIO -> base + " com reforço de perícia e prova documental massiva";
            case TRABALHISTA -> base + " com reforço de execução e litigância repetitiva";
            case FAMILIA, INFANCIA_JUVENTUDE -> base + " com reforço de proteção de dados e sigilo";
            default -> base;
        };
    }
}
