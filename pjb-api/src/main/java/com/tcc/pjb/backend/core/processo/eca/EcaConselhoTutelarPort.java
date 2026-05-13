package com.tcc.pjb.backend.core.processo.eca;

public interface EcaConselhoTutelarPort {

    void notificarApreensao(String custodiaId, String municipio, String adolescenteDocumentoAnonimizado);

    void notificarMedidaAplicada(String processoId, String medidaSocioeducativa, String municipio);
}
