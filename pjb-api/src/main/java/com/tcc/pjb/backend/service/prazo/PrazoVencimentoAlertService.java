package com.tcc.pjb.backend.service.prazo;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrazoVencimentoAlertService {

    public record AlertaPrazo(
            PrazoProcessualEngine.PrazoSnapshot prazo,
            String nivel,
            String mensagem,
            boolean requerAcaoCartoraria,
            boolean requerAcaoJurisdicional
    ) {}

    public List<AlertaPrazo> gerarAlertas(
            List<PrazoProcessualEngine.PrazoSnapshot> prazos) {
        return prazos.stream()
                .filter(p -> !p.situacao().eTerminado())
                .filter(p -> !"OK".equals(p.alertaNivel()))
                .map(this::construirAlerta)
                .sorted((a, b) -> Integer.compare(
                        nivelOrdem(b.nivel()), nivelOrdem(a.nivel())))
                .toList();
    }

    private AlertaPrazo construirAlerta(PrazoProcessualEngine.PrazoSnapshot p) {
        String msg = p.vencido()
                ? "Prazo vencido: " + p.descricao() + ". Decurso a certificar."
                : "Prazo vence em " + p.diasUteisRestantes() + " dia(s) útil(is): "
                        + p.descricao();
        boolean cartoraria = p.vencido()
                || p.diasUteisRestantes() <= 1;
        boolean jurisdicional = p.tipo() == PrazoTipo.PEREMPTÓRIO
                && (p.vencido() || p.diasUteisRestantes() <= 2);
        return new AlertaPrazo(p, p.alertaNivel(), msg, cartoraria, jurisdicional);
    }

    private int nivelOrdem(String nivel) {
        return switch (nivel) {
            case "CRITICO" -> 4;
            case "URGENTE" -> 3;
            case "ATENCAO" -> 2;
            default -> 1;
        };
    }
}
