package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class InstitutionalOperationalDeskCounterpartScopeResolver {

    private final InstitutionalOperationalDeskSupport support;

    InstitutionalOperationalDeskCounterpartScopeResolver(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    List<String> resolve(InstitutionalOperationalDeskSnapshot snapshot) {
        InstitutionalProcessProfile processProfile = snapshot.processProfile();
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        if (snapshot.secretariatWorkflowEnabled() || snapshot.communicationWorkflowEnabled()) {
            scopes.add("PARTES_E_REPRESENTANTES_PROCESSUAIS");
        }
        if (snapshot.mandateWorkflowEnabled()) {
            scopes.add("CENTRAL_DE_MANDADOS_E_OFICIAIS");
        }
        if (snapshot.assessorWorkflowEnabled() || snapshot.opinionWorkflowEnabled()) {
            scopes.add("GABINETE_E_MAGISTRADO_RESPONSAVEL");
        }
        if (snapshot.triageWorkflowEnabled()) {
            scopes.add("DISTRIBUICAO_PROTOCOLO_E_CLASSIFICACAO");
            scopes.add("SERVICO_DE_AUTUACAO_PREVENCAO_E_REDISTRIBUICAO");
        }
        if (snapshot.calculatorWorkflowEnabled()) {
            scopes.add("CONTADORIA_E_CALCULOS");
        }
        if (snapshot.prisonFlow() || support.containsToken(snapshot.judicialAxis(), "PENAL", "CUSTODIA")) {
            scopes.add("UNIDADE_PRISIONAL_POLICIA_PENAL_ESCOLTA");
        }
        if (support.containsToken(snapshot.scope(), "PROMOTORIA", "MINISTERIO_PUBLICO") || processProfile == InstitutionalProcessProfile.PROMOTOR) {
            scopes.add("MINISTERIO_PUBLICO");
        }
        if (support.containsToken(snapshot.scope(), "DEFENSORIA") || processProfile == InstitutionalProcessProfile.DEFENSOR) {
            scopes.add("DEFENSORIA_PUBLICA");
        }
        if (support.containsToken(snapshot.scope(), "PROCURADORIA", "AGU", "FAZENDA") || processProfile == InstitutionalProcessProfile.PROCURADOR) {
            scopes.add("PROCURADORIAS_E_ADVOCACIA_PUBLICA");
        }
        if (support.containsToken(snapshot.scope(), "DELEGACIA", "POLICIA") || processProfile == InstitutionalProcessProfile.DELEGADO) {
            scopes.add("DELEGACIA_E_POLICIA_JUDICIARIA");
        }
        if (support.containsToken(snapshot.unitKind(), "CEJUSC")) {
            scopes.add("AUTOCOMPOSICAO_E_CONCILIACAO");
        }
        if (snapshot.secretariatWorkflowEnabled() || snapshot.communicationWorkflowEnabled()) {
            scopes.add("DIARIO_ELETRONICO_E_CANAIS_DE_INTIMACAO");
        }
        if (support.containsToken(snapshot.unitKind(), "CENTRAL_MANDADOS", "CEJUSC")
                || support.containsToken(snapshot.scope(), "CENTRAL_AUDIENCIAS", "CEJUSC")) {
            scopes.add("SALAS_PAUTA_E_RECURSOS_DE_AUDIENCIA");
        }
        if (snapshot.management()) {
            scopes.add("DIRETORIA_FORUM_E_GESTAO_DA_UNIDADE");
        }
        return List.copyOf(scopes);
    }
}
