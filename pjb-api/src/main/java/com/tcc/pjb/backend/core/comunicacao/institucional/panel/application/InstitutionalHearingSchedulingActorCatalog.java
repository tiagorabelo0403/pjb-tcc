package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import java.util.LinkedHashSet;
import java.util.Locale;

class InstitutionalHearingSchedulingActorCatalog {
    LinkedHashSet<String> requestActors(InstitutionalProcessProfile processProfile,
                                                boolean legalInstitution,
                                                boolean technicalSupport,
                                                boolean secretariat,
                                                boolean scheduler,
                                                boolean management,
                                                boolean prisonFlow) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (legalInstitution && processProfile != null) {
            actors.add(processProfile.name());
        }
        if (technicalSupport) {
            actors.add("APOIO_DOCUMENTAL_INSTITUCIONAL");
        }
        if (secretariat) {
            actors.add("SECRETARIA_FORUM");
        }
        if (scheduler) {
            actors.add("CENTRAL_AUDIENCIA_OU_CEJUSC");
        }
        if (management) {
            actors.add("GESTAO_UNIDADE");
        }
        if (prisonFlow) {
            actors.add("CUSTODIA_PRISIONAL");
        }
        return actors;
    }

    LinkedHashSet<String> preparatoryActors(InstitutionalProcessProfile processProfile,
                                                    boolean technicalSupport,
                                                    boolean secretariat,
                                                    boolean scheduler,
                                                    boolean management,
                                                    boolean prisonFlow) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (technicalSupport) {
            actors.add("APOIO_DOCUMENTAL_INSTITUCIONAL");
        }
        if (secretariat) {
            actors.add("SECRETARIA_FORUM");
            actors.add("SECRETARIO_DE_AUDIENCIA");
            actors.add("SECRETARIA_RESPONSAVEL_PELA_INTIMACAO");
        }
        if (scheduler) {
            actors.add("SERVIDOR_AUDIENCIA");
            actors.add("CENTRAL_AUDIENCIA_OU_CEJUSC");
        }
        if (management) {
            actors.add("GESTAO_UNIDADE");
        }
        if (prisonFlow) {
            actors.add("CUSTODIA_PRISIONAL");
        }
        if (processProfile == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL) {
            actors.add("ASSESSORIA_DE_GABINETE_OU_UNIDADE");
        }
        return actors;
    }

    LinkedHashSet<String> communicationActors(InstitutionalProcessProfile processProfile,
                                                      boolean secretariat,
                                                      boolean scheduler,
                                                      boolean management,
                                                      boolean prisonFlow) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (secretariat) {
            actors.add("SECRETARIA_FORUM");
            actors.add("SECRETARIO_DE_AUDIENCIA");
            actors.add("SECRETARIA_RESPONSAVEL_PELA_INTIMACAO");
        }
        if (scheduler) {
            actors.add("SERVIDOR_AUDIENCIA");
            actors.add("CENTRAL_AUDIENCIA_OU_CEJUSC");
        }
        if (management) {
            actors.add("GESTAO_UNIDADE");
        }
        if (prisonFlow) {
            actors.add("CUSTODIA_PRISIONAL");
            actors.add("UNIDADE_PRISIONAL_OU_ESCOLTA");
        }
        if (processProfile == InstitutionalProcessProfile.CONCILIADOR || processProfile == InstitutionalProcessProfile.MEDIADOR) {
            actors.add("MEDIACAO_OU_CONCILIACAO");
        }
        return actors;
    }

    LinkedHashSet<String> operationalActors(InstitutionalProcessProfile processProfile,
                                                    boolean secretariat,
                                                    boolean scheduler,
                                                    boolean management,
                                                    boolean hybridJudicial) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (secretariat) {
            actors.add("SECRETARIA_FORUM");
            actors.add("SECRETARIO_DE_AUDIENCIA");
            actors.add("SECRETARIA_RESPONSAVEL_PELA_INTIMACAO");
        }
        if (scheduler) {
            actors.add("CENTRAL_AUDIENCIA_OU_CEJUSC");
            actors.add("SERVIDOR_AUDIENCIA");
        }
        if (management) {
            actors.add("GESTAO_UNIDADE");
        }
        if (hybridJudicial && processProfile != null) {
            actors.add(processProfile.name());
        }
        return actors;
    }

    LinkedHashSet<String> trackingActors(InstitutionalProcessProfile processProfile,
                                                 boolean legalInstitution,
                                                 boolean technicalSupport,
                                                 boolean secretariat,
                                                 boolean scheduler,
                                                 boolean management,
                                                 boolean prisonFlow,
                                                 boolean hybridJudicial) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (processProfile != null && legalInstitution) {
            actors.add(processProfile.name());
        }
        if (technicalSupport) {
            actors.add("APOIO_DOCUMENTAL_INSTITUCIONAL");
        }
        if (secretariat) {
            actors.add("SECRETARIA_FORUM");
        }
        if (scheduler) {
            actors.add("CENTRAL_AUDIENCIA_OU_CEJUSC");
        }
        if (management) {
            actors.add("GESTAO_UNIDADE");
        }
        if (prisonFlow) {
            actors.add("CUSTODIA_PRISIONAL");
        }
        if (hybridJudicial) {
            actors.add("MAGISTRADO_REFERENCIAL");
        }
        return actors;
    }

    LinkedHashSet<String> oversightActors(InstitutionalProcessProfile processProfile,
                                                  String scope,
                                                  boolean management,
                                                  boolean hybridJudicial,
                                                  boolean includeJudicial) {
        LinkedHashSet<String> actors = new LinkedHashSet<>();
        if (includeJudicial) {
            actors.add("MAGISTRADO_DO_FEITO");
            actors.add("MAGISTRADO_TITULAR_DO_FEITO");
            actors.add("GABINETE_DO_MAGISTRADO");
            actors.add("GABINETE_DA_VARA");
        }
        if (hybridJudicial) {
            actors.add("MAGISTRADO_COOPERANTE");
        }
        if (management || processProfile == InstitutionalProcessProfile.DIRETOR_FORUM) {
            actors.add("DIRETORIA_DA_UNIDADE");
        }
        if (scopeMatches(scope, "CORREG") || management) {
            actors.add("CORREGEDORIA_LOCAL");
        }
        return actors;
    }


    private boolean scopeMatches(String scope, String token) {
        return scope != null && token != null && normalize(scope).contains(token.trim().toUpperCase(Locale.ROOT));
    }

    private boolean matchesAny(String value, String... tokens) {
        if (value == null || tokens == null || tokens.length == 0) {
            return false;
        }
        String normalized = normalize(value);
        for (String token : tokens) {
            if (token != null && !token.isBlank() && normalized.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }
}
