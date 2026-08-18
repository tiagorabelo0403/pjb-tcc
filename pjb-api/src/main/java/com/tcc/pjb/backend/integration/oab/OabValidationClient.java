package com.tcc.pjb.backend.integration.oab;

import com.tcc.pjb.backend.core.validation.oab.OabInfo;
import com.tcc.pjb.backend.model.entity.Usuario;

public interface OabValidationClient {
    OabValidationResult validate(OabInfo info, Usuario usuario);
}
