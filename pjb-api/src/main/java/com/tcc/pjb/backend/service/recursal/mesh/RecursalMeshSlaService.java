package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaPolicy;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaPolicyCatalog;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;

@Service
public class RecursalMeshSlaService {

    private final CalendarioForenseTribunalService calendarioService;
    private final RecursalSlaPolicyCatalog policyCatalog;

    public RecursalMeshSlaService(CalendarioForenseTribunalService calendarioService) {
        this(calendarioService, new RecursalSlaPolicyCatalog());
    }

    RecursalMeshSlaService(CalendarioForenseTribunalService calendarioService, RecursalSlaPolicyCatalog policyCatalog) {
        this.calendarioService = calendarioService;
        this.policyCatalog = policyCatalog;
    }

    public Optional<RecursalSlaSnapshot> snapshot(RecursalAggregateState aggregate) {
        if (aggregate == null || aggregate.getCurrentState() == null || aggregate.getTribunalAtual() == null) {
            return Optional.empty();
        }
        Processo processo = aggregate.getProcesso();
        return snapshot(
                aggregate.getCurrentState(),
                aggregate.getTribunalAtual(),
                aggregate.getTribunalDetalhadoAtual(),
                aggregate.getUpdatedAt(),
                processo == null ? null : processo.getUf(),
                processo == null ? null : processo.getComarca()
        );
    }

    public Optional<RecursalSlaSnapshot> snapshot(RecursalStateSnapshot snapshot) {
        if (snapshot == null) {
            return Optional.empty();
        }
        return snapshot(snapshot.state(), snapshot.tribunalAtual(), snapshot.tribunalDetalhadoAtual(), snapshot.atualizadoEm(), null, null);
    }

    public Optional<RecursalSlaSnapshot> snapshot(RecursalLifecycleState estado,
                                                  RecursalTribunal tribunal,
                                                  RecursalTribunalDetalhado tribunalDetalhado,
                                                  Instant referencia,
                                                  String uf,
                                                  String comarca) {
        Optional<RecursalSlaPolicy> resolved = policyCatalog.resolve(estado, tribunal);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        RecursalSlaPolicy policy = resolved.get();
        LocalDate referenceDate = (referencia == null ? Instant.now() : referencia).atZone(ZoneOffset.UTC).toLocalDate();
        String tribunalCodigo = tribunalDetalhado == null ? tribunal.name() : tribunalDetalhado.name();
        var prazo = hasText(uf) || hasText(comarca)
                ? calendarioService.calcularPrazo(referenceDate, policy.diasUteis(), tribunalCodigo, normalize(uf), normalize(comarca))
                : calendarioService.calcularPrazo(referenceDate, policy.diasUteis(), tribunalCodigo);
        LocalDate dueDate = prazo.dataVencimento();
        LocalDate today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        boolean overdue = today.isAfter(dueDate);
        int overdueBusinessDays = overdue ? calendarioService.contarDiasUteis(dueDate.plusDays(1), today, tribunalCodigo) : 0;
        return Optional.of(new RecursalSlaSnapshot(
                estado,
                tribunal,
                policy.diasUteis(),
                policy.fatalParaPartes(),
                policy.fundamentoLegal(),
                referenceDate,
                dueDate,
                overdue,
                overdueBusinessDays,
                severity(policy.fatalParaPartes(), overdueBusinessDays)
        ));
    }

    private String severity(boolean fatalParaPartes, int overdueBusinessDays) {
        if (overdueBusinessDays <= 0) {
            return fatalParaPartes ? "MONITORAR_FATAL" : "MONITORAR_INTERNO";
        }
        if (fatalParaPartes) {
            return overdueBusinessDays >= 3 ? "CRITICO_PARTES" : "ALERTA_PARTES";
        }
        return overdueBusinessDays >= 5 ? "CRITICO_INTERNO" : "ALERTA_INTERNO";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
