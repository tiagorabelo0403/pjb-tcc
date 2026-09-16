package com.tcc.pjb.backend.model.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Prova a matriz completa (5 ações x 10 funções) contra os booleanos do próprio enum
 * FuncaoServidorJudiciario. Fonte única usada por PjbAuthorizationFuncaoServidorFacade e por
 * FuncaoServidorApplicationService — este teste é a garantia de que os dois concordam entre si
 * (D-duas-tabelas-verdade-capacidade-servidor).
 */
class AcaoProcessualServidorTest {

    @Test
    void matrizCompletaBateComOsBooleanosDoEnum() {
        for (FuncaoServidorJudiciario funcao : FuncaoServidorJudiciario.values()) {
            assertThat(AcaoProcessualServidor.PROFERIR.permiteExecutarPor(funcao)).isEqualTo(funcao.podeProferir());
            assertThat(AcaoProcessualServidor.CONCLUIR.permiteExecutarPor(funcao)).isEqualTo(funcao.podeConcluir());
            assertThat(AcaoProcessualServidor.INTIMAR.permiteExecutarPor(funcao)).isEqualTo(funcao.podeIntimar());
            assertThat(AcaoProcessualServidor.DISTRIBUIR.permiteExecutarPor(funcao)).isEqualTo(funcao.podeDistribuir());
            assertThat(AcaoProcessualServidor.ARQUIVAR.permiteExecutarPor(funcao)).isEqualTo(funcao.podeArquivar());
        }
    }
}
