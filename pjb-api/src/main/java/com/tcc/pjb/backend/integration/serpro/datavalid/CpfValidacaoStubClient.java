package com.tcc.pjb.backend.integration.serpro.datavalid;

public class CpfValidacaoStubClient implements CpfValidacaoPort {

    @Override
    public CpfValidacaoResult consultar(String cpf) {
        return new CpfValidacaoResult(CpfSituacao.REGULAR);
    }
}
