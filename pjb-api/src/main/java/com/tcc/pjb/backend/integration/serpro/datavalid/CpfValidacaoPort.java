package com.tcc.pjb.backend.integration.serpro.datavalid;

public interface CpfValidacaoPort {

    CpfValidacaoResult consultar(String cpf);
}
