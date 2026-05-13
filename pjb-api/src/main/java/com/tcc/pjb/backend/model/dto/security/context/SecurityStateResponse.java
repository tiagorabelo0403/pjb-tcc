package com.tcc.pjb.backend.model.dto.security.context;

import java.time.LocalDateTime;
import java.util.List;

public record SecurityStateResponse(boolean frozen,
                                    LocalDateTime frozenUntil,
                                    boolean advogadoBaptized,
                                    LocalDateTime govVerifiedAt,
                                    boolean govEmailVerified,
                                    boolean govPhoneVerified,
                                    List<String> pendingSteps,
                                    SecurityDeviceResponse activeDevice,
                                    List<SecurityDeviceResponse> devices) {
}
