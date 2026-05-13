package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.util.List;

public record SecurityGuardView(String classificacao,
                                String nivelSigiloAlvo,
                                boolean canalForteObrigatorio,
                                boolean certificadoObrigatorio,
                                boolean stepUpObrigatorio,
                                boolean restritoAAtuacaoInstitucional,
                                List<String> modosAssinaturaFortes,
                                List<String> invariantes,
                                List<String> alertas) {
}
