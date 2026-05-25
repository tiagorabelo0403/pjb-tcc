package com.tcc.pjb.backend.service.distribuicao;

import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoInitialDistributionSnapshotService {

    private final DistribuicaoProcessualNacionalEngine distribuicaoProcessualNacionalEngine;
    private final ProcessoRepository processoRepository;

    public ProcessoInitialDistributionSnapshotService(DistribuicaoProcessualNacionalEngine distribuicaoProcessualNacionalEngine,
                                                      ProcessoRepository processoRepository) {
        this.distribuicaoProcessualNacionalEngine = Objects.requireNonNull(distribuicaoProcessualNacionalEngine);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public DistribuicaoProcessualNacionalEngine.DistributionSnapshot consolidar(Processo processo) {
        return consolidar(processo, true);
    }

    public DistribuicaoProcessualNacionalEngine.DistributionSnapshot consolidar(Processo processo, boolean persistir) {
        if (processo == null) {
            return null;
        }
        DistribuicaoProcessualNacionalEngine.DistributionSnapshot snapshot = distribuicaoProcessualNacionalEngine.snapshotInicial(processo);
        if (snapshot == null) {
            return null;
        }
        boolean changed = aplicarSnapshot(processo, snapshot);
        if (persistir && changed) {
            processoRepository.save(processo);
        }
        return snapshot;
    }

    private boolean aplicarSnapshot(Processo processo,
                                    DistribuicaoProcessualNacionalEngine.DistributionSnapshot snapshot) {
        boolean changed = false;
        String tribunalCodigo = tribunalCodigoSnapshot(snapshot.tribunalCodigoRoteado(), processo.getTribunalCodigoRoteado());
        changed |= replaceIfDifferent(processo.getTribunalCodigoRoteado(), tribunalCodigo, processo::setTribunalCodigoRoteado);
        changed |= replaceIfDifferent(processo.getTribunal(), firstNonBlank(tribunalCodigo, processo.getTribunal()), processo::setTribunal);
        changed |= replaceIfDifferent(processo.getComarca(), snapshot.comarcaDestino(), processo::setComarca);
        changed |= replaceIfDifferent(processo.getVara(), snapshot.varaDestino(), processo::setVara);
        changed |= replaceIfDifferent(processo.getUnidadeJudiciariaCodigo(), snapshot.varaDestino(), processo::setUnidadeJudiciariaCodigo);
        changed |= replaceIfDifferent(processo.getPreProtocoloStatus(), snapshot.status(), processo::setPreProtocoloStatus);
        changed |= replaceIfDifferent(processo.getCompetenciaTerritorialModo(), snapshot.competenciaTerritorialModo(), processo::setCompetenciaTerritorialModo);
        changed |= replaceIfDifferent(processo.getPreventionMode(), snapshot.preventionMode(), processo::setPreventionMode);
        changed |= replaceIfDifferent(processo.getLinkageMode(), snapshot.linkageMode(), processo::setLinkageMode);
        changed |= replaceIfDifferent(processo.getRoutingRiskLevel(), firstNonBlank(snapshot.routingRiskLevel(), processo.getRoutingRiskLevel()), processo::setRoutingRiskLevel);
        changed |= replaceIfDifferent(processo.getConnectorSystem(), firstNonBlank(snapshot.connectorSystem(), processo.getConnectorSystem()), processo::setConnectorSystem);

        String alertas = joinDistinct(processo.getConnectorSubmissionMessage(), snapshot.alertas());
        changed |= replaceIfDifferent(processo.getConnectorSubmissionMessage(), alertas, processo::setConnectorSubmissionMessage);
        String checklist = joinDistinct(processo.getConnectorSyncMessage(), snapshot.reviewChecklist());
        changed |= replaceIfDifferent(processo.getConnectorSyncMessage(), checklist, processo::setConnectorSyncMessage);
        String fundamentos = joinDistinct(processo.getSubmissionBlueprintStatus(), snapshot.fundamentos());
        changed |= replaceIfDifferent(processo.getSubmissionBlueprintStatus(), fundamentos, processo::setSubmissionBlueprintStatus);

        if (snapshot.status() != null && (snapshot.status().startsWith("DISTRIBUI") || snapshot.status().startsWith("REDIRECIONADO")) && processo.getDataDistribuicao() == null) {
            processo.setDataDistribuicao(LocalDateTime.now());
            changed = true;
        }
        if (changed) {
            processo.setDataAtualizacao(LocalDateTime.now());
        }
        return changed;
    }

    private String tribunalCodigoSnapshot(String roteado, String atual) {
        String normalized = normalize(roteado);
        if (normalized != null) {
            int separador = normalized.indexOf('_');
            if (separador > 0) {
                return normalized.substring(0, separador);
            }
            return normalized;
        }
        return atual;
    }

    private boolean replaceIfDifferent(String atual, String novo, java.util.function.Consumer<String> consumer) {
        String normalizedCurrent = normalize(atual);
        String normalizedNew = normalize(novo);
        if (Objects.equals(normalizedCurrent, normalizedNew) || normalizedNew == null) {
            return false;
        }
        consumer.accept(normalizedNew);
        return true;
    }

    private String joinDistinct(String current, java.util.List<String> values) {
        LinkedHashSet<String> itens = new LinkedHashSet<>();
        append(itens, current);
        if (values != null) {
            values.forEach(value -> append(itens, value));
        }
        return itens.isEmpty() ? null : String.join(" | ", itens);
    }

    private void append(LinkedHashSet<String> itens, String raw) {
        String normalized = normalize(raw);
        if (normalized == null) {
            return;
        }
        for (String token : normalized.split("\\s*\\|\\s*")) {
            String item = normalize(token);
            if (item != null) {
                itens.add(item);
            }
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
