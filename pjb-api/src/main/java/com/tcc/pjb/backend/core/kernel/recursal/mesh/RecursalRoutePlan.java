package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public record RecursalRoutePlan(
        String profileName,
        RecursalTribunal tribunalOrigem,
        RecursalTribunalDetalhado tribunalDetalhadoOrigem,
        RecursalAuthority autoridadeOrigemAdmissibilidade,
        RecursalTribunal tribunalDestino,
        RecursalTribunalDetalhado tribunalDetalhadoDestino,
        InstanceLevel instanciaDestino,
        RecursalAuthority autoridadeDestinoAdmissibilidade,
        RecursalAuthority autoridadeJulgamentoMerito,
        PreparoDisposition preparo,
        AdmissibilityDisposition admissibilidade,
        PreventionDisposition prevencao,
        RemessaDisposition remessa,
        RecursalRouteKind routeKind) {

    public RecursalRoutePlan(
            String profileName,
            RecursalTribunal tribunalOrigem,
            RecursalTribunalDetalhado tribunalDetalhadoOrigem,
            RecursalAuthority autoridadeOrigemAdmissibilidade,
            RecursalTribunal tribunalDestino,
            RecursalTribunalDetalhado tribunalDetalhadoDestino,
            InstanceLevel instanciaDestino,
            RecursalAuthority autoridadeDestinoAdmissibilidade,
            RecursalAuthority autoridadeJulgamentoMerito,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao,
            RemessaDisposition remessa) {
        this(profileName,
                tribunalOrigem,
                tribunalDetalhadoOrigem,
                autoridadeOrigemAdmissibilidade,
                tribunalDestino,
                tribunalDetalhadoDestino,
                instanciaDestino,
                autoridadeDestinoAdmissibilidade,
                autoridadeJulgamentoMerito,
                preparo,
                admissibilidade,
                prevencao,
                remessa,
                null);
    }

    public RecursalRoutePlan {
        Objects.requireNonNull(profileName, "profileName");
        Objects.requireNonNull(tribunalOrigem, "tribunalOrigem");
        Objects.requireNonNull(tribunalDetalhadoOrigem, "tribunalDetalhadoOrigem");
        Objects.requireNonNull(tribunalDestino, "tribunalDestino");
        Objects.requireNonNull(tribunalDetalhadoDestino, "tribunalDetalhadoDestino");
        Objects.requireNonNull(instanciaDestino, "instanciaDestino");
        Objects.requireNonNull(autoridadeJulgamentoMerito, "autoridadeJulgamentoMerito");
        Objects.requireNonNull(preparo, "preparo");
        Objects.requireNonNull(admissibilidade, "admissibilidade");
        Objects.requireNonNull(prevencao, "prevencao");
        Objects.requireNonNull(remessa, "remessa");
        routeKind = routeKind == null ? fallbackRouteKind(tribunalOrigem, tribunalDestino, instanciaDestino, remessa) : routeKind;
    }

    public boolean julgamentoColegiado() {
        return autoridadeJulgamentoMerito.colegiado();
    }

    public boolean mesmaCorte() {
        return tribunalOrigem == tribunalDestino;
    }

    private static RecursalRouteKind fallbackRouteKind(RecursalTribunal origem,
                                                       RecursalTribunal destino,
                                                       InstanceLevel instanciaDestino,
                                                       RemessaDisposition remessa) {
        if (origem == destino && remessa.mesmosAutos()) {
            return RecursalRouteKind.INTERNAL_SAME_AUTOS;
        }
        if (instanciaDestino == InstanceLevel.EXTRAORDINARY || destino == RecursalTribunal.STF) {
            return RecursalRouteKind.EXTRAORDINARY_EXCEPTIONAL;
        }
        if (instanciaDestino == InstanceLevel.SUPERIOR || destino == RecursalTribunal.STJ || destino == RecursalTribunal.TST) {
            return RecursalRouteKind.SUPERIOR_EXCEPTIONAL;
        }
        return remessa.externa() ? RecursalRouteKind.SECOND_INSTANCE_EXTERNAL : RecursalRouteKind.INTERNAL_SAME_AUTOS;
    }
}
