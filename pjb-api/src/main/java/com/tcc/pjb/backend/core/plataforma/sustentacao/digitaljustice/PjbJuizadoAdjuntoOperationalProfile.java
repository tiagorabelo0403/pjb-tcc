package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.Set;

public record PjbJuizadoAdjuntoOperationalProfile(String unitCode,
                                                  Set<String> territoryCodes,
                                                  boolean pjeEnabled,
                                                  boolean protocolOptionAvailable,
                                                  boolean publicGuidancePublished,
                                                  boolean forumDirectorateSupportReady,
                                                  boolean ownSecretariatAvailable,
                                                  boolean magistratesDesignated,
                                                  boolean digitalHearingEnabled,
                                                  int activeCaseLoad,
                                                  int monthlyCapacity) {
}
