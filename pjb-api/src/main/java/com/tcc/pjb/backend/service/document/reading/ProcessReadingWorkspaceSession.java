package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.entity.Usuario;

record ProcessReadingWorkspaceSession(ProcessReadingWorkspaceContext context,
                                      Usuario usuario,
                                      ProcessReadingModeProfile modeProfile,
                                      ProcessReadingPresetProfile presetProfile) {
}
