package com.tcc.pjb.backend.model.dto.security.context;

import java.util.List;

public record SecurityContextResponse(Long userId,
                                      String email,
                                      String tipoUsuario,
                                      List<SecurityHatResponse> hats,
                                      SecurityStateResponse security,
                                      PjbAuthenticatedSessionResponse institutionalSession) {
}
