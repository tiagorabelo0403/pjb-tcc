package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;

/**
 * {@code Usuario.cpf} é criptografado (índice cego via {@code cpfHash}); a busca cruzada por parte do
 * processo (autor/réu/usuário vinculado) usa a implementação real para comparar pelo hash no lado do
 * usuário, mantendo {@code Processo.parteAutoraCpf}/{@code parteReuCpf} em texto puro (dado da parte
 * no processo, escopo diferente do dado de conta do usuário).
 */
public interface ProcessoRepositoryCustom {

    List<Processo> findAllByPartesCpf(String cpf);
}
