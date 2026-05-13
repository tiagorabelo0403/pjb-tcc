package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.eleitoral.domain.DiplomacaoResultado;
import com.tcc.pjb.backend.core.eleitoral.domain.RegistrarDiplomacaoCommand;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.util.List;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeitoEleitoralDiplomacaoScheduler {

    private final EleitoralTseProperties properties;
    private final TseResultadoClient resultadoClient;
    private final FeitoEleitoralService feitoEleitoralService;
    private final AuditLedgerService auditLedger;

    public FeitoEleitoralDiplomacaoScheduler(EleitoralTseProperties properties,
                                             TseResultadoClient resultadoClient,
                                             FeitoEleitoralService feitoEleitoralService,
                                             AuditLedgerService auditLedger) {
        this.properties = Objects.requireNonNull(properties);
        this.resultadoClient = Objects.requireNonNull(resultadoClient);
        this.feitoEleitoralService = Objects.requireNonNull(feitoEleitoralService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Scheduled(fixedRateString = "${pjb.eleitoral.tse.diplomacao.scheduler-fixed-rate-ms:86400000}")
    public void sincronizarDiplomacoes() {
        if (!properties.enabled() || properties.diplomacao() == null || !properties.diplomacao().autoExtincaoEnabled()) {
            return;
        }
        List<DiplomacaoResultado> pendentes = resultadoClient.consultarDiplomacoesPendentes();
        for (DiplomacaoResultado pendente : pendentes) {
            try {
                if (properties.dryRun()) {
                    auditLedger.appendSafely("ELEITORAL_DIPLOMACAO_SYNC_DRY_RUN", "PROCESSO", String.valueOf(pendente.processoId()),
                            "zona=" + pendente.zonaEleitoral() + " uf=" + pendente.uf() + " data=" + pendente.dataDiplomacao());
                    continue;
                }
                var status = feitoEleitoralService.statusSnapshot(pendente.processoId());
                if (status.diplomadoEm() != null || "EXTINTO".equalsIgnoreCase(status.statusEleitoral())) {
                    auditLedger.appendSafely("ELEITORAL_DIPLOMACAO_SYNC_SKIP", "PROCESSO", String.valueOf(pendente.processoId()),
                            "status=" + status.statusEleitoral());
                    continue;
                }
                feitoEleitoralService.registrarDiplomacao(new RegistrarDiplomacaoCommand(pendente.processoId(), pendente.dataDiplomacao()));
                auditLedger.appendSafely("ELEITORAL_DIPLOMACAO_SYNC_OK", "PROCESSO", String.valueOf(pendente.processoId()),
                        "zona=" + pendente.zonaEleitoral() + " uf=" + pendente.uf());
            } catch (Exception e) {
                auditLedger.appendSafely("ELEITORAL_DIPLOMACAO_SYNC_FAIL", "PROCESSO", String.valueOf(pendente.processoId()), e.getMessage());
            }
        }
    }
}
