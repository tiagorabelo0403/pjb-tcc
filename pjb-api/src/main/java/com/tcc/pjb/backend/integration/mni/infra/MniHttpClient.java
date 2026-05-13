package com.tcc.pjb.backend.integration.mni.infra;

import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;

public interface MniHttpClient {

    MniHttpResponse enviarAutos(String tribunalDestino, String xml);
}
