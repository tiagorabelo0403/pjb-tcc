package com.tcc.pjb.backend.core.prazos.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditHealthView;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditResult;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoTimelineView;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoCalculationView;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthResult;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoRegimeView;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowResult;
import com.tcc.pjb.backend.core.prazos.calendario.domain.PrazoCalendarioHealthView;
import com.tcc.pjb.backend.core.prazos.policy.PrazoPolicyRegistry;
import com.tcc.pjb.backend.core.prazos.policy.domain.PrazoPolicyView;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrazoApplicationService {

    private final PrazosEngine prazosEngine;
    private final PrazoAuditTrailService auditTrailService;
    private final PrazoPolicyRegistry prazoPolicyRegistry;
    private final AuditLedgerService auditLedgerService;

    public PrazoApplicationService(PrazosEngine prazosEngine,
                                   PrazoAuditTrailService auditTrailService,
                                   PrazoPolicyRegistry prazoPolicyRegistry,
                                   AuditLedgerService auditLedgerService) {
        this.prazosEngine = Objects.requireNonNull(prazosEngine);
        this.auditTrailService = Objects.requireNonNull(auditTrailService);
        this.prazoPolicyRegistry = Objects.requireNonNull(prazoPolicyRegistry);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PrazoHealthResult health(String uf, String comarca) {
        return prazosEngine.health(new PrazoHealthQuery(uf, comarca));
    }

    @Transactional(readOnly = true)
    public PrazoCalculationView calcular(LocalDateTime inicio,
                                                                                 int quantidade,
                                                                                 PrazoRegime regime,
                                                                                 String uf,
                                                                                 String comarca) {
        return prazosEngine.calculationView(inicio, quantidade, regime, uf, comarca);
    }

    @Transactional(readOnly = true)
    public PrazoWindowResult window(LocalDateTime inicio,
                                                                            int quantidade,
                                                                            PrazoRegime regime,
                                                                            String uf,
                                                                            String comarca) {
        return prazosEngine.window(new PrazoWindowQuery(inicio, quantidade, regime, uf, comarca));
    }

    @Transactional(readOnly = true)
    public PrazoCalendarioHealthView calendarioHealth(String uf, String comarca) {
        return prazosEngine.calendarioHealthView(uf, comarca);
    }

    @Transactional(readOnly = true)
    public PrazoRegimeView regime(PrazoRegime regime) {
        return prazosEngine.regimeView(regime);
    }

    @Transactional(readOnly = true)
    public PrazoPolicyView policy(String ramo,
                                  String rito,
                                  boolean defensoria,
                                  boolean ministerioPublico,
                                  boolean fazenda) {
        RamoDireito ramoDireito = parseRamo(ramo);
        RitoProcessual ritoProcessual = parseRito(rito);
        PrazoRegime regime = prazoPolicyRegistry.resolveByPartyProfile(ramoDireito, ritoProcessual, defensoria, ministerioPublico, fazenda);
        return prazosEngine.policyView(ramo, rito, regime);
    }

    @Transactional(readOnly = true)
    public PrazoAuditResult audit(PrazoAuditQuery query) {
        Objects.requireNonNull(query);
        return auditTrailService.query(query);
    }

    @Transactional(readOnly = true)
    public PrazoAuditHealthView auditHealth(PrazoAuditQuery query) {
        var trail = auditTrailService.query(query).auditTrail();
        return auditTrailService.healthView(trail);
    }

    @Transactional(readOnly = true)
    public PrazoTimelineView timeline(PrazoAuditQuery query) {
        var trail = auditTrailService.query(query).auditTrail();
        var view = auditTrailService.timeline(trail);
        auditLedgerService.appendSafely("PRAZO_TIMELINE_QUERY", "PRAZO", String.valueOf(query.processoId()), null, "entries=" + view.entries().size());
        return view;
    }

    private RamoDireito parseRamo(String ramo) {
        if (ramo == null || ramo.isBlank()) {
            return null;
        }
        return RamoDireito.fromString(ramo);
    }

    private RitoProcessual parseRito(String rito) {
        if (rito == null || rito.isBlank()) {
            return null;
        }
        return RitoProcessual.fromString(rito);
    }
}
