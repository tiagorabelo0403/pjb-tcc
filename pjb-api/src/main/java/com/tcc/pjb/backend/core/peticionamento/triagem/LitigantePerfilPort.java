package com.tcc.pjb.backend.core.peticionamento.triagem;

public interface LitigantePerfilPort {

    LitiganteRiskScore avaliar(String cpfCnpj);
}
