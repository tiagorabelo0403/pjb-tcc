package com.tcc.pjb.backend.service.profile;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.ExternalSystemStatus;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class PerfilExternalSystemService {

    public interface BnmpClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public interface CnibClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public interface RenajudClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public interface InfojudClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public interface SisbajudClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public interface SerasajudClient {
        Snapshot consultarCircunscricao(Usuario usuario);
    }

    public record Snapshot(boolean enabled, boolean realtime, int itemCount, List<String> highlights) {
        public Snapshot {
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }

    private final BnmpClient bnmpClient;
    private final CnibClient cnibClient;
    private final RenajudClient renajudClient;
    private final InfojudClient infojudClient;
    private final SisbajudClient sisbajudClient;
    private final SerasajudClient serasajudClient;
    private final PjbAuthorizationService authorizationService;

    public PerfilExternalSystemService(ObjectProvider<BnmpClient> bnmpClient,
                                       ObjectProvider<CnibClient> cnibClient,
                                       ObjectProvider<RenajudClient> renajudClient,
                                       ObjectProvider<InfojudClient> infojudClient,
                                       ObjectProvider<SisbajudClient> sisbajudClient,
                                       ObjectProvider<SerasajudClient> serasajudClient,
                                       PjbAuthorizationService authorizationService) {
        this.bnmpClient = bnmpClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração BNMP não configurada.")));
        this.cnibClient = cnibClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração CNIB não configurada.")));
        this.renajudClient = renajudClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração RENAJUD não configurada.")));
        this.infojudClient = infojudClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração INFOJUD não configurada.")));
        this.sisbajudClient = sisbajudClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração SISBAJUD não configurada.")));
        this.serasajudClient = serasajudClient.getIfAvailable(() -> usuario -> new Snapshot(false, false, 0, List.of("Integração SERASAJUD não configurada.")));
        this.authorizationService = authorizationService;
    }

    public List<ExternalSystemStatus> snapshotFor(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return List.of();
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        List<ExternalSystemStatus> systems = new ArrayList<>();

        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            systems.add(toStatus("BNMP", authorizationService.canConsultBnmp(usuario), bnmpClient.consultarCircunscricao(usuario), "WIDGET_LOGIN"));
            systems.add(toStatus("RENAJUD", authorizationService.canConsultRenajud(usuario), renajudClient.consultarCircunscricao(usuario), "PRECHECK_OPERACIONAL"));
        }

        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR || tipo.isCartorioExtrajudicial()) {
            systems.add(toStatus("CNIB", authorizationService.canConsultCnib(usuario), cnibClient.consultarCircunscricao(usuario), "PRECHECK_PENHORA"));
            systems.add(toStatus("RENAJUD", authorizationService.canConsultRenajud(usuario), renajudClient.consultarCircunscricao(usuario), "PRECHECK_CUMPRIMENTO"));
        }

        if (tipo.isMagistratura() || tipo.isAssessor()) {
            boolean delegated = tipo.isAssessor();
            systems.add(toStatus("INFOJUD", authorizationService.canRequestInfojud(usuario, delegated), infojudClient.consultarCircunscricao(usuario), delegated ? "DELEGADO" : "DIRETO"));
            systems.add(toStatus("SISBAJUD", authorizationService.canRequestSisbajud(usuario, delegated), sisbajudClient.consultarCircunscricao(usuario), delegated ? "DELEGADO" : "DIRETO"));
            systems.add(toStatus("SERASAJUD", authorizationService.canRequestSerasajud(usuario, delegated), serasajudClient.consultarCircunscricao(usuario), delegated ? "DELEGADO" : "ORDEM_JUDICIAL"));
        }

        if (tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria()) {
            systems.add(toStatus("SERASAJUD", authorizationService.canRequestSerasajud(usuario, false), serasajudClient.consultarCircunscricao(usuario), "SOLICITACAO"));
        }

        if (tipo == TipoUsuario.LEILOEIRO_JUDICIAL) {
            systems.add(toStatus("CNIB", authorizationService.canConsultCnib(usuario), cnibClient.consultarCircunscricao(usuario), "REFERENCIA"));
        }

        return List.copyOf(systems);
    }

    private static ExternalSystemStatus toStatus(String system, boolean permitted, Snapshot snapshot, String mode) {
        Snapshot safe = snapshot == null ? new Snapshot(false, false, 0, List.of()) : snapshot;
        return new ExternalSystemStatus(system, safe.enabled(), permitted, safe.realtime(), mode, safe.itemCount(), safe.highlights());
    }
}
