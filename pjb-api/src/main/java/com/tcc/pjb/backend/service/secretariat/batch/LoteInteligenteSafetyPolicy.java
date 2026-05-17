package com.tcc.pjb.backend.service.secretariat.batch;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class LoteInteligenteSafetyPolicy {

    public LoteInteligenteSafetyPolicy() {}

    public record LoteSafetyInput(
            String acaoTipo,
            boolean temItemComSigiloAbsoluto,
            boolean todosComAssinaturaValida,
            boolean todosNoMesmoOrgao,
            int tamanhoLote,
            boolean eAtoJurisdicional
    ) {}

    public record LoteSafetyResultado(
            boolean aprovado,
            List<String> impedimentos,
            String mensagem
    ) {}

    public LoteSafetyResultado avaliar(LoteSafetyInput input) {
        List<String> impedimentos = new ArrayList<>();
        if (input.eAtoJurisdicional()) {
            impedimentos.add("Ato jurisdicional não pode ser processado em lote automaticamente — exige análise individual.");
        }
        if (input.temItemComSigiloAbsoluto()) {
            impedimentos.add("Lote contém processo com sigilo absoluto — excluir antes de prosseguir.");
        }
        if (!input.todosComAssinaturaValida() && acaoRequerAssinatura(input.acaoTipo())) {
            impedimentos.add("Nem todos os itens possuem assinatura válida configurada.");
        }
        if (!input.todosNoMesmoOrgao()) {
            impedimentos.add("Processos pertencem a órgãos distintos — verificar competência antes de lotear.");
        }
        if (input.tamanhoLote() > 500) {
            impedimentos.add("Lote excede 500 itens — dividir para reduzir risco operacional.");
        }
        boolean aprovado = impedimentos.isEmpty();
        String mensagem = aprovado
                ? "Lote aprovado para processamento cartorário."
                : "Lote com " + impedimentos.size() + " impedimento(s). Regularizar antes de prosseguir.";
        return new LoteSafetyResultado(aprovado, impedimentos, mensagem);
    }

    private boolean acaoRequerAssinatura(String acaoTipo) {
        return acaoTipo != null && (acaoTipo.contains("ASSINAR") || acaoTipo.contains("MINUTAR"));
    }
}
