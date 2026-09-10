package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Mantém o índice cego de cpf/email em sincronia com o texto puro no exato momento em que o
 * Hibernate persiste ou atualiza — o {@code @Convert} só cifra no bind SQL, depois deste callback.
 *
 * <p>A invariante fica na fronteira de persistência de propósito: todo caminho que grava
 * {@link Usuario} (REST, MNI, marketplace, backfill, cascata, merge) recebe o índice calculado sem
 * depender de o chamador lembrar. Mover isso para a camada de serviço trocaria uma invariante
 * garantida por uma disciplina distribuída, e um único ponto esquecido produz linha cuja busca por
 * CPF deixa de encontrar o usuário.
 *
 * <p>Fica em listener gerenciado pelo Spring, e não em callback dentro da entidade, porque o
 * Hibernate resolve listeners pelo {@code SpringBeanContainer} que o Boot configura — o mesmo
 * mecanismo já usado por {@code AuditingEntityListener} e por
 * {@code SensitiveDataConverter}. Assim a chave mestra vem sempre do contexto dono, sem holder
 * estático de {@code ApplicationContext}.
 */
@Component
public class UsuarioBlindIndexListener {

    private final ObjectProvider<UsuarioBlindIndexService> blindIndexProvider;

    public UsuarioBlindIndexListener(ObjectProvider<UsuarioBlindIndexService> blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    @PrePersist
    @PreUpdate
    public void recalcularIndiceCego(Usuario usuario) {
        UsuarioBlindIndexService blindIndex = blindIndexProvider.getObject();
        usuario.aplicarIndiceCego(
                blindIndex.hashCpf(usuario.getCpf()),
                blindIndex.hashEmail(usuario.getEmail()));
    }
}
