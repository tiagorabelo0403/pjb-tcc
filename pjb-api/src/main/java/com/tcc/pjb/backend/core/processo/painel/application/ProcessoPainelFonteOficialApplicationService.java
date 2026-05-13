package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialItem;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelFonteOficialApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoPainelFonteOficialApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoPainelFonteOficialAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        String ramo = normalize(unificado.competencia().ramoDireito());
        ArrayList<ProcessoPainelFonteOficialItem> itens = new ArrayList<>();
        addBase(itens);
        switch (ramo) {
            case "TRABALHISTA" -> addTrabalhista(itens);
            case "PREVIDENCIARIO" -> addPrevidenciario(itens);
            case "PENAL", "PENAL_MILITAR", "MILITAR" -> addPenal(itens);
            case "FAZENDA_PUBLICA", "TRIBUTARIO", "FAZENDARIO" -> addFazenda(itens);
            default -> addCivel(itens);
        }
        LinkedHashSet<String> garantias = new LinkedHashSet<>();
        garantias.add("FONTE_OFICIAL_POR_WIDGET");
        garantias.add("FALLBACK_EXPLICITO");
        garantias.add("IDEMPOTENCIA_TRANSVERSAL");
        garantias.add("REPLAY_CONTROLADO");
        garantias.add("TRILHA_FORENSE_IMUTAVEL");
        return new ProcessoPainelFonteOficialAggregate(
                processoId,
                unificado.identity().numeroProcesso(),
                ramo,
                List.copyOf(itens),
                List.copyOf(garantias),
                Instant.now()
        );
    }

    private void addBase(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("TIMELINE_VIVA", "PROCESSO", List.of("AUDIT_LEDGER", "TIMELINE_VIVA", "EXPLAINABILITY"), "ULTIMO_ESTADO_CACHE", "CHAVE_PROCESSO_EVENTO"));
        itens.add(item("OPERACAO_PESADA", "OPERACAO", List.of("OUTBOX", "RETRY", "IDEMPOTENCY", "REPLAY"), "DEGRADACAO_GRACIOSA", "CHAVE_PROCESSO_OPERACAO"));
        itens.add(item("ROTA_TATICA", "GOVERNANCA", List.of("TIMELINE_VIVA", "ANALYTICS_NACIONAL", "PRE_GRAVACAO"), "REPLAY_CONTROLADO", "CHAVE_PROCESSO_TATICA"));
        itens.add(item("TELEMETRIA_CONECTORES", "INTEGRACAO", List.of("CONTROL_PLANE", "DATA_PLANE", "OBSERVABILITY"), "ULTIMO_ESTADO_CACHE", "CHAVE_CONECTOR_TRIBUNAL"));
    }

    private void addCivel(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("COCKPIT_CONSTRICAO", "EXECUCAO_CIVEL", List.of("SISBAJUD", "RENAJUD", "INFOJUD"), "MANUAL_ASSISTIDO", "CHAVE_CONSTRICAO_PROCESSO"));
        itens.add(item("DEMANDAS_REPETITIVAS", "PRECEDENTES", List.of("IRDR", "TEMAS_STJ_STF", "JURIMETRIA"), "ULTIMO_ESTADO_CACHE", "CHAVE_PRECEDENTE_PROCESSO"));
        itens.add(item("PAUTA_AUDIENCIAS", "CONCILIACAO", List.of("AGENDA_AUDIENCIA", "PAUTA_JEC"), "REPLAY_CONTROLADO", "CHAVE_AUDIENCIA_PROCESSO"));
    }

    private void addPenal(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("RADAR_REUS_PRESOS", "CUSTODIA", List.of("CUSTODIA_PROCESSUAL", "BNMP", "MANDADOS"), "MANUAL_ASSISTIDO", "CHAVE_CUSTODIA_PROCESSO"));
        itens.add(item("PRESCRICAO_ATIVA", "PENAL", List.of("REGRAS_VIGENTES", "TIMELINE_VIVA", "JURIMETRIA"), "ULTIMO_ESTADO_CACHE", "CHAVE_PRESCRICAO_PROCESSO"));
        itens.add(item("MALHA_MANDADOS", "PENAL", List.of("MANDADOS", "BNMP", "COMUNICACOES_ELETRONICAS"), "REPLAY_CONTROLADO", "CHAVE_MANDADO_PROCESSO"));
    }

    private void addTrabalhista(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("RADAR_EXECUCAO_FRUSTRADA", "TRABALHISTA", List.of("EXECUCAO_TRABALHISTA", "IDPJ", "BENS"), "MANUAL_ASSISTIDO", "CHAVE_EXECUCAO_TRABALHISTA"));
        itens.add(item("BNDT_ATIVA", "TRABALHISTA", List.of("BNDT", "EXECUCAO_TRABALHISTA", "GOVERNANCA_DEVEDOR"), "REPLAY_CONTROLADO", "CHAVE_BNDT_PROCESSO"));
        itens.add(item("DEPOSITOS_JUDICIAIS", "TRABALHISTA", List.of("CAIXA_JUDICIAL", "BANCO_DO_BRASIL", "ALVARA_ELETRONICO"), "ULTIMO_ESTADO_CACHE", "CHAVE_DEPOSITO_PROCESSO"));
    }

    private void addFazenda(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("GESTOR_LOTES_FISCAIS", "FAZENDA", List.of("EXECUCAO_FISCAL", "CDA", "PRESCRICAO_INTERCORRENTE"), "REPLAY_CONTROLADO", "CHAVE_LOTE_FISCAL"));
        itens.add(item("RADAR_GARANTIAS", "FAZENDA", List.of("SEGURO_GARANTIA", "FIANCA_BANCARIA", "PENHORA"), "ULTIMO_ESTADO_CACHE", "CHAVE_GARANTIA_PROCESSO"));
        itens.add(item("MONITOR_EMBARGOS", "FAZENDA", List.of("EMBARGOS_EXECUCAO", "EFEITO_SUSPENSIVO", "TIMELINE_VIVA"), "MANUAL_ASSISTIDO", "CHAVE_EMBARGOS_PROCESSO"));
    }

    private void addPrevidenciario(List<ProcessoPainelFonteOficialItem> itens) {
        itens.add(item("TRILHO_INSS_CNIS", "PREVIDENCIARIO", List.of("CNIS", "SABI", "PLENUS", "INSS"), "ULTIMO_ESTADO_CACHE", "CHAVE_PREVID_PROCESSO"));
        itens.add(item("FILA_PERICIAS", "PREVIDENCIARIO", List.of("AGENDA_PERICIAL", "LAUDO_MEDICO", "PERITOS_CREDENCIADOS"), "REPLAY_CONTROLADO", "CHAVE_PERICIA_PROCESSO"));
        itens.add(item("ESTEIRA_RPV_PRECATORIO", "PREVIDENCIARIO", List.of("RPV", "PRECATORIO", "EXECUCAO_FAZENDARIA"), "MANUAL_ASSISTIDO", "CHAVE_PAGAMENTO_PROCESSO"));
    }

    private ProcessoPainelFonteOficialItem item(String widgetCode,
                                                String dominio,
                                                List<String> sources,
                                                String fallbackMode,
                                                String idempotencyMode) {
        return new ProcessoPainelFonteOficialItem(widgetCode, dominio, sources, fallbackMode, idempotencyMode, "REPLAY_CONTROLADO", "TRILHA_FORENSE_IMUTAVEL");
    }

    private String normalize(String value) {
        if (value == null) {
            return "NAO_INFORMADO";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "NAO_INFORMADO" : normalized;
    }
}
